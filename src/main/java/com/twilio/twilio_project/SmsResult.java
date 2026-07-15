package com.twilio.twilio_project; // Model — SMS send result (messageId, success/fail, error detail)

import com.twilio.rest.api.v2010.account.Message;

// Holds the outcome of an SMS send attempt. Three constructor patterns:
//   SmsResult(String messageId)           — Twilio success (msg has SID)
//   SmsResult(String messageId, String providerRefId) — SMPP success (msg has provider ref)
//   SmsResult(boolean success, String error)           — failure
// providerRefId links delivery receipts (DLRs) back to the original outbound message.
public class SmsResult {
    private final String messageId;
    private final boolean success;
    private final String error;
    private final String providerRefId;

    public SmsResult(String messageId) {
        this.messageId = messageId;
        this.success = true;
        this.error = null;
        this.providerRefId = null;
    }

    public SmsResult(String messageId, String providerRefId) {
        this.messageId = messageId;
        this.success = true;
        this.error = null;
        this.providerRefId = providerRefId;
    }

    public SmsResult(boolean success, String error) {
        this.messageId = null;
        this.success = success;
        this.error = error;
        this.providerRefId = null;
    }

    public String getMessageId() { return messageId; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public String getProviderRefId() { return providerRefId; }
}
