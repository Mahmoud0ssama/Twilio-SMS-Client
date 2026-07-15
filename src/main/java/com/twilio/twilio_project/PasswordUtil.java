package com.twilio.twilio_project; // Password hashing / verification via bcrypt

import org.mindrot.jbcrypt.BCrypt;

// Wraps jBCrypt for password security. Never stores plaintext passwords.
// null-safe matches() prevents uncaught exceptions from null inputs.
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static boolean matches(String plainPassword, String passwordHash) {
        if (plainPassword == null || passwordHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
