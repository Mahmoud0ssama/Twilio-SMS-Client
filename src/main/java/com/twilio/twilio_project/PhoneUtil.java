package com.twilio.twilio_project; // Phone number helpers — E.164 normalization, validation

// Normalizes phone numbers to E.164 (+<country><number>) for consistent storage and SMPP/Twilio interop.
// Strips whitespace, dashes, parentheses. Prepends + if missing.
public final class PhoneUtil {

    private PhoneUtil() {
    }

    // Strip formatting chars, ensure leading +. Returns input unchanged if null/empty.
    public static String normalize(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        String normalized = phone.replaceAll("[\\s\\-()]+", "");
        if (!normalized.startsWith("+")) {
            normalized = "+" + normalized;
        }
        return normalized;
    }

    // Basic E.164 shape check: + followed by 5-15 digits. Not a live carrier check.
    public static boolean validateE164(String phone) {
        return phone != null && phone.matches("^\\+\\d{5,15}$");
    }
}
