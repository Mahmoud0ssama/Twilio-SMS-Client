package com.twilio.twilio_project; // SMPP session pool — connect, bind, send, receive DLRs from SMSC

import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

// Manages SMPP sessions to the SMSC (smscsim in docker profile, localhost in local profile). Sessions are pooled by host:port:systemId.
// Handles: connect/bind, submit, delivery receipts (DLRs), inbound SMS, session reconnection.
// Each session has a 30s enquire-link timer and 10s transaction timer.
// Session state listener auto-removes closed sessions from the pool.
public class SmppSessionManager {
    private static final Logger log = LoggerFactory.getLogger(SmppSessionManager.class);
    private static final ConcurrentHashMap<String, SMPPSession> sessions = new ConcurrentHashMap<>();

    public static class SmppConfig {
        public final String host;
        public final int port;
        public final String systemId;
        public final String password;
        public final String addressRange;

        public SmppConfig(String host, int port, String systemId, String password, String addressRange) {
            this.host = host;
            this.port = port;
            this.systemId = systemId;
            this.password = password;
            this.addressRange = addressRange;
        }

        String key() {
            return host + ":" + port + ":" + systemId;
        }
    }

    // Get or create a bound session. Reuses existing sessions if still connected.
    public static synchronized SMPPSession getSession(SmppConfig cfg) throws IOException {
        String key = cfg.key();
        SMPPSession session = sessions.get(key);
        if (session != null && session.getSessionState().isBound()) {
            return session;
        }
        // Clean up stale session before creating new one
        if (session != null) {
            try { session.unbindAndClose(); } catch (Exception ignored) {}
        }
        session = new SMPPSession();
        session.setEnquireLinkTimer(30000);
        session.setTransactionTimer(10000);
        // Auto-remove from pool when session closes
        session.addSessionStateListener((newState, oldState, source) -> {
            if (newState.equals(SessionState.CLOSED)) {
                sessions.remove(key, source);
                log.warn("SMPP session {} closed, removed from pool", key);
            }
        });
        // Listen for delivery receipts (DLRs) and inbound mobile-originated messages
        session.setMessageReceiverListener(new MessageReceiverListener() {
            @Override
            public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {
                try {
                    byte esmClass = deliverSm.getEsmClass();
                    // esmClass=4 indicates delivery receipt; otherwise it's an inbound message
                    if (esmClass == 4) {
                        SmpEventLogger.log("INFO", "DLR", "From " + deliverSm.getSourceAddr()
                            + " " + new String(deliverSm.getShortMessage(), java.nio.charset.StandardCharsets.ISO_8859_1));
                        handleDeliveryReceipt(deliverSm);
                    } else {
                        SmpEventLogger.log("INFO", "MO", "From " + deliverSm.getSourceAddr()
                            + " to " + deliverSm.getDestAddress());
                        handleInboundMessage(deliverSm);
                    }
                } catch (Exception e) {
                    SmpEventLogger.log("ERROR", "DELIVER_SM", e.getMessage());
                    log.error("Error processing DELIVER_SM: {}", e.getMessage());
                }
            }

            @Override
            public void onAcceptAlertNotification(AlertNotification alertNotification) {
                log.info("SMPP alert: {}", alertNotification);
            }

            @Override
            public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
                return null;
            }
        });
        session.connectAndBind(cfg.host, cfg.port,
                new BindParameter(BindType.BIND_TRX, cfg.systemId, cfg.password, "",
                        TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, cfg.addressRange != null ? cfg.addressRange : ""));
        sessions.put(key, session);
        log.info("SMPP session bound to {} ({})", key, session.getSessionState());
        SmpEventLogger.log("INFO", "BIND", "Bound to " + key + " state=" + session.getSessionState());
        return session;
    }

    // Parse DLR from SMSC, extract providerRefId and final status, update sms_history.
    private static void handleDeliveryReceipt(DeliverSm deliverSm) {
        try {
            String msgStr = new String(deliverSm.getShortMessage(), "UTF-8");
            org.jsmpp.bean.DeliveryReceipt receipt = new org.jsmpp.bean.DeliveryReceipt(msgStr);
            String providerRefId = receipt.getId();
            String status = receipt.getFinalStatus() != null ? receipt.getFinalStatus().name() : "DELIVRD";
            UserRepository.updateSmsStatusByProviderRefId(providerRefId, status);
            log.info("DLR for {}: {}", providerRefId, status);
        } catch (Exception e) {
            log.warn("Failed to parse DLR: {}", e.getMessage());
        }
    }

    // Decode SMPP message based on data coding (0x08 = UTF-16BE, default = UTF-8).
    private static String decodeShortMessage(byte[] msgBytes, byte dataCoding) {
        try {
            return switch (dataCoding) {
                case 0x08 -> new String(msgBytes, "UTF-16BE");
                default -> new String(msgBytes, "UTF-8");
            };
        } catch (Exception e) {
            return new String(msgBytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        }
    }

    // Handle mobile-originated (MO) SMS: find user by destination phone, save to sms_history.
    private static void handleInboundMessage(DeliverSm deliverSm) {
        try {
            String from = deliverSm.getSourceAddr();
            String to = deliverSm.getDestAddress();
            byte[] msgBytes = deliverSm.getShortMessage();
            String message = decodeShortMessage(msgBytes, deliverSm.getDataCoding());
            int userId = UserRepository.findUserIdByPhone(to);
            if (userId > 0) {
                UserRepository.saveInboundSms(userId, from, to, message);
                log.info("Inbound SMS for user {} from {}: {}", userId, from, message);
            } else {
                log.warn("No user found for inbound SMS to {}", to);
            }
        } catch (Exception e) {
            log.warn("Failed to process inbound SMS: {}", e.getMessage());
        }
    }

    // Submit a short message via the SMPP session. Returns provider message ID for DLR correlation.
    public static String submit(SmppConfig cfg, String to, String message, String from) throws IOException {
        try {
            SMPPSession session = getSession(cfg);
            String sourceAddr = from != null && !from.isEmpty() ? from : cfg.addressRange;

            SubmitSmResult result = session.submitShortMessage(
                    "CMT",
                    TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN,
                    sourceAddr,
                    TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN,
                    to,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE),
                    (byte) 0,
                    new GeneralDataCoding(),
                    (byte) 0,
                    message.getBytes()
            );
            SmpEventLogger.log("INFO", "SUBMIT", "To " + to + " msgId=" + result.getMessageId());
            return result.getMessageId();
        } catch (Exception e) {
            SmpEventLogger.log("ERROR", "SUBMIT", "To " + to + ": " + e.getMessage());
            throw new IOException("SMPP submit failed: " + e.getMessage(), e);
        }
    }

    // Gracefully close all sessions (used on shutdown).
    public static void closeAll() {
        sessions.forEach((key, session) -> {
            try { session.unbindAndClose(); } catch (Exception ignored) {}
        });
        sessions.clear();
    }
}
