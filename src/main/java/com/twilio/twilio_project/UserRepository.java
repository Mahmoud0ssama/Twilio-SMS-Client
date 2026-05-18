package com.twilio.twilio_project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static CustomerTwilioConfig findTwilioConfigByUserId(int userId) throws SQLException {
        String sql = "SELECT twilio_account_sid, twilio_auth_token, twilio_sender_id "
                + "FROM users WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new CustomerTwilioConfig(
                            rs.getString("twilio_account_sid"),
                            rs.getString("twilio_auth_token"),
                            rs.getString("twilio_sender_id"));
                }
            }
        }
        return null;
    }

    public static void createCustomer(PendingRegistration pending) throws SQLException {
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
            stmt.executeUpdate();
        }
    }

    public static List<Map<String, Object>> findSmsHistoryByUserId(int userId) {
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT id, from_phone, to_phone, message, status, sent_at FROM sms_history "
                + "WHERE user_id = ? ORDER BY sent_at DESC";

        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> sms = new HashMap<>();
                    sms.put("id", rs.getInt("id"));
                    sms.put("from", rs.getString("from_phone"));
                    sms.put("recipient", rs.getString("to_phone"));
                    sms.put("message", rs.getString("message"));
                    sms.put("status", rs.getString("status"));
                    sms.put("sentAt", rs.getTimestamp("sent_at"));
                    history.add(sms);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    public static void recordSms(int userId, String fromPhone, String toPhone, String message, String status)
            throws SQLException {
        String sql = "INSERT INTO sms_history (user_id, from_phone, to_phone, message, status) "
                + "VALUES (?, ?, ?, ?, ?::message_status)";
        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, fromPhone);
            stmt.setString(3, toPhone);
            stmt.setString(4, message);
            stmt.setString(5, status);
            stmt.executeUpdate();
        }
    }

    public static List<Map<String, Object>> searchSmsHistoryByUserId(int userId, String from, String to, String startDate, String endDate) {
        List<Map<String, Object>> history = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, from_phone, to_phone, message, status, sent_at FROM sms_history WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (from != null && !from.trim().isEmpty()) {
            sql.append(" AND from_phone ILIKE ?");
            params.add("%" + from.trim() + "%");
        }
        if (to != null && !to.trim().isEmpty()) {
            sql.append(" AND to_phone ILIKE ?");
            params.add("%" + to.trim() + "%");
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append(" AND sent_at >= ?::timestamp");
            params.add(startDate.trim() + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append(" AND sent_at <= ?::timestamp");
            params.add(endDate.trim() + " 23:59:59");
        }

        sql.append(" ORDER BY sent_at DESC");

        try (Connection conn = DBUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> sms = new HashMap<>();
                    sms.put("id", rs.getInt("id"));
                    sms.put("from", rs.getString("from_phone"));
                    sms.put("recipient", rs.getString("to_phone"));
                    sms.put("message", rs.getString("message"));
                    sms.put("status", rs.getString("status"));
                    sms.put("sentAt", rs.getTimestamp("sent_at"));
                    history.add(sms);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    public static void deleteSmsByIdAndUserId(int smsId, int userId) throws SQLException {
        String sql = "DELETE FROM sms_history WHERE id = ? AND user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, smsId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public static Map<String, String> getUserProfile(int userId) throws SQLException {
        String sql = "SELECT username, full_name, birthday, msisdn, job, email, address, "
                   + "twilio_account_sid, twilio_auth_token, twilio_sender_id "
                   + "FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> profile = new HashMap<>();
                    profile.put("username", rs.getString("username"));
                    profile.put("fullName", rs.getString("full_name"));
                    if (rs.getDate("birthday") != null) {
                        profile.put("birthday", rs.getDate("birthday").toString());
                    }
                    profile.put("msisdn", rs.getString("msisdn"));
                    profile.put("job", rs.getString("job"));
                    profile.put("email", rs.getString("email"));
                    profile.put("address", rs.getString("address"));
                    profile.put("twilioSid", rs.getString("twilio_account_sid"));
                    profile.put("twilioToken", rs.getString("twilio_auth_token"));
                    profile.put("twilioSender", rs.getString("twilio_sender_id"));
                    return profile;
                }
            }
        }
        return null;
    }

    public static void updateUserProfile(int userId, Map<String, String> profile) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        List<Object> params = new ArrayList<>();

        sql.append("full_name = ?, "); params.add(profile.get("fullName"));

        if (profile.get("birthday") != null && !profile.get("birthday").isEmpty()) {
            sql.append("birthday = ?::date, "); params.add(profile.get("birthday"));
        } else {
            sql.append("birthday = NULL, ");
        }

        sql.append("msisdn = ?, "); params.add(profile.get("msisdn"));
        sql.append("job = ?, "); params.add(profile.get("job"));
        sql.append("email = ?, "); params.add(profile.get("email"));
        sql.append("address = ?, "); params.add(profile.get("address"));
        sql.append("twilio_account_sid = ?, "); params.add(profile.get("twilioSid"));
        sql.append("twilio_auth_token = ?, "); params.add(profile.get("twilioToken"));
        sql.append("twilio_sender_id = ? "); params.add(profile.get("twilioSender"));

        if (profile.containsKey("passwordHash") && profile.get("passwordHash") != null) {
            sql.append(", password_hash = ? ");
            params.add(profile.get("passwordHash"));
        }

        sql.append("WHERE id = ?");
        params.add(userId);

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
        }
    }
}
