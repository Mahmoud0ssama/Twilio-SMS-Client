package com.twilio.twilio_project; // OTP verification — validate code, resend code, finalize registration

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

// POST /verify-msisdn — completes the two-step registration flow.
// Reads PendingRegistration from session, checks code validity and expiry.
// ?action=resend generates a new code and re-sends via SMS (or dev bypass).
@WebServlet(name = "verifyMsisdnServlet", value = "/verify-msisdn")
public class VerifyMsisdnServlet extends HttpServlet {

    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
    private static final long VERIFICATION_TTL_MS = java.util.concurrent.TimeUnit.MINUTES.toMillis(10);
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        PendingRegistration pending = null;
        if (session != null) {
            pending = (PendingRegistration) session.getAttribute(RegisterServlet.SESSION_PENDING_REGISTRATION);
        }

        if (pending == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"No pending registration session found\"}");
            return;
        }

        // Handle OTP resend: generate new code, send via SMS, update session
        String action = request.getParameter("action");
        if ("resend".equals(action)) {
            try {
                String newCode = String.format("%06d", RANDOM.nextInt(1_000_000));
                String smsBody = "Your new Twilio SMS verification code is: " + newCode;

                String devBypass = System.getenv("DEV_BYPASS_SMS");
                if ("true".equalsIgnoreCase(devBypass)) {
                    System.out.println("DEV_BYPASS_SMS: new verification code is " + newCode);
                } else {
                    TwilioSmsService.send(
                            pending.getTwilioAccountSid(),
                            pending.getTwilioAuthToken(),
                            pending.getTwilioSenderId(),
                            pending.getMsisdn(),
                            smsBody
                    );
                }

                pending.setVerificationCode(newCode);
                pending.setVerificationExpiresAt(System.currentTimeMillis() + VERIFICATION_TTL_MS);
                session.setAttribute(RegisterServlet.SESSION_PENDING_REGISTRATION, pending);

                response.getWriter().write("{\"status\":\"success\",\"message\":\"A new code has been sent\"}");
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Failed to resend code\"}");
            }
            return;
        }

        // Normal verification: check code and expiry, create account on success
        try {
            String body = UserRepository.readRequestBody(request);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String code = json.has("code") ? json.get("code").getAsString().trim() : "";

            if (pending.isVerificationExpired()) {
                session.removeAttribute(RegisterServlet.SESSION_PENDING_REGISTRATION);
                response.setStatus(HttpServletResponse.SC_GONE);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Verification code has expired\"}");
                return;
            }

            if (!pending.getVerificationCode().equals(code)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid verification code\"}");
                return;
            }

            UserRepository.createCustomer(pending);
            session.removeAttribute(RegisterServlet.SESSION_PENDING_REGISTRATION);
            response.getWriter().write("{\"status\":\"success\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Server write error\"}");
        }
    }
}
