package com.twilio.twilio_project; // Twilio SmsProvider adapter — delegates send to Twilio REST API

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

// Adapter that wraps the Twilio REST API into the shape SmsRouter expects.
// Twilio.init() must be called by the caller before invoking send() —
// this class does not manage Twilio credentials itself.
public class TwilioSmsProvider {

    public SmsResult send(String to, String message, String from) {
        try {
            Message msg = Message.creator(new PhoneNumber(to), new PhoneNumber(from), message).create();
            return new SmsResult(msg.getSid());
        } catch (Exception e) {
            return new SmsResult(false, "Twilio error: " + e.getMessage());
        }
    }
}
