package com.twilio.twilio_project; // Admin CRUD — view, edit, delete customer profiles

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// GET /admin/customer?id=N             — return profile or sms_history for a customer
// GET /admin/customer?id=N&action=delete — delete a customer
// POST /admin/customer {actionType}    — add, edit, or delete a customer
// Role-gated: administrator only. Edit guards each field against empty-string overwrite.
@WebServlet(name = "adminCustomerServlet", value = "/admin/customer")
public class AdminCustomerServlet extends HttpServlet {

    private final Gson gson = new Gson();

    // GET /admin/customer — load profile, sms_history, or delete by action param.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || !"administrator".equals(session.getAttribute("userRole"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idStr = request.getParameter("id");
        if (idStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String action = request.getParameter("action");

        // Fetch customer SMS history (outbound + inbound)
        if ("sms_history".equals(action)) {
            try {
                int id = Integer.parseInt(idStr.trim());
                List<Map<String, Object>> outbound = UserRepository.findSmsHistoryByUserId(id);
                List<Map<String, Object>> inbound = UserRepository.findInboundSmsByUserId(id);
                JsonObject data = new JsonObject();
                data.addProperty("status", "success");
                data.add("outboundHistory", gson.toJsonTree(outbound));
                data.add("inboundHistory", gson.toJsonTree(inbound));
                response.getWriter().write(gson.toJson(data));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"status\":\"error\"}");
            }
            return;
        }

        // Delete customer by ID (via GET for Svelte fetch convenience)
        if ("delete".equals(action)) {
            try {
                int id = Integer.parseInt(idStr.trim());
                int deleted = UserRepository.deleteUserById(id);
                if (deleted == 0) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"User not found\"}");
                    return;
                }
                response.getWriter().write("{\"status\":\"success\"}");
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Deletion failure\"}");
            }
            return;
        }

        // Default: load customer profile for editing
        try {
            int id = Integer.parseInt(idStr.trim());
            Map<String, String> profile = UserRepository.getUserProfile(id);
            if (profile != null) {
                JsonObject data = new JsonObject();
                data.addProperty("status", "success");
                data.add("custProfile", gson.toJsonTree(profile));
                response.getWriter().write(gson.toJson(data));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // POST /admin/customer — create, edit, or delete a customer based on actionType.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || !"administrator".equals(session.getAttribute("userRole"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            String body = UserRepository.readRequestBody(request);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            if (json == null || !json.has("actionType")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Missing actionType\"}");
                return;
            }
            String action = json.get("actionType").getAsString();

            // Handle DELETE action
            if ("delete".equals(action)) {
                int id = json.has("customerId") ? json.get("customerId").getAsInt() : 0;
                if (id <= 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid customer ID\"}");
                    return;
                }
                int deleted = UserRepository.deleteUserById(id);
                if (deleted == 0) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"User not found\"}");
                    return;
                }
                response.getWriter().write("{\"status\":\"success\"}");
                return;
            }

            // Gather profile fields from request body
            String username = json.has("username") ? json.get("username").getAsString().trim() : "";
            String fullName = json.has("fullName") ? json.get("fullName").getAsString().trim() : "";
            String birthdayRaw = json.has("birthday") ? json.get("birthday").getAsString().trim() : "";
            String msisdn = json.has("msisdn") ? PhoneUtil.normalize(json.get("msisdn").getAsString().trim()) : "";
            String job = json.has("job") ? json.get("job").getAsString().trim() : "";
            String email = json.has("email") ? json.get("email").getAsString().trim() : "";

            if ("add".equals(action)) {
                // Create new customer
                String password = json.has("password") ? json.get("password").getAsString() : "";
                if (username.isEmpty() || password.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"Username and password required\"}");
                    return;
                }
                String passwordHash = PasswordUtil.hash(password);
                UserRepository.createUser(username, passwordHash, fullName, birthdayRaw, msisdn, job, email);
                response.getWriter().write("{\"status\":\"success\"}");
            } else if ("edit".equals(action)) {
                // Update existing customer — only non-empty fields are updated
                int id = json.has("customerId") ? json.get("customerId").getAsInt() : 0;
                if (id <= 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid customer ID\"}");
                    return;
                }
                Map<String, String> profile = new HashMap<>();
                if (!username.isEmpty()) profile.put("username", username);
                if (!fullName.isEmpty()) profile.put("fullName", fullName);
                if (birthdayRaw != null && !birthdayRaw.isEmpty()) profile.put("birthday", birthdayRaw);
                if (!msisdn.isEmpty()) profile.put("msisdn", msisdn);
                if (!job.isEmpty()) profile.put("job", job);
                if (!email.isEmpty()) profile.put("email", email);
                UserRepository.updateUserProfile(id, profile);
                response.getWriter().write("{\"status\":\"success\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Unknown actionType\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Server write error\"}");
        }
    }
}
