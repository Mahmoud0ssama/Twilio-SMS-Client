package com.twilio.twilio_project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public final class UserRepository {

    private UserRepository() {
    }

    public static boolean existsByUsernameEmailOrMsisdn(String username, String email, String msisdn)
            throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? OR email = ? OR msisdn = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, msisdn);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static int createCustomer(PendingRegistration pending) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, full_name, birthday, msisdn, job, email, "
                + "address, twilio_account_sid, twilio_auth_token, twilio_sender_id, msisdn_validated) "
                + "VALUES (?, ?, 'customer'::user_role, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";

        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pending.getUsername());
            stmt.setString(2, pending.getPasswordHash());
            stmt.setString(3, pending.getFullName());
            stmt.setDate(4, pending.getBirthday());
            stmt.setString(5, pending.getMsisdn());
            stmt.setString(6, pending.getJob());
            stmt.setString(7, pending.getEmail());
            stmt.setString(8, pending.getAddress());
            stmt.setString(9, pending.getTwilioAccountSid());
            stmt.setString(10, pending.getTwilioAuthToken());
            stmt.setString(11, pending.getTwilioSenderId());
            int rows = stmt.executeUpdate();
            return rows;
        }
    }
}
