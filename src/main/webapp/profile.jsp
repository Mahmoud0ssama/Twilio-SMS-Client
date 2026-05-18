<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.Map" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Profile - Twilio SMS</title>
    <link rel="stylesheet" href="css/style.css?v=3">
    <style>
        body { display: flex; justify-content: center; align-items: flex-start; min-height: 100vh; padding: 2rem 1rem; }
        .auth-container { width: 100%; max-width: 640px; }
        .auth-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
        .auth-header h1 { font-size: 1.75rem; margin: 0; }
        .back-link { color: var(--text-secondary); font-size: 0.9rem; }
        .auth-form button { width: 100%; margin-top: 0.75rem; }
        .form-section { margin-bottom: 1.5rem; padding-bottom: 1.25rem; border-bottom: 1px solid var(--border); }
        .form-section:last-of-type { border-bottom: none; padding-bottom: 0; margin-bottom: 0.5rem; }
        .form-section h2 { font-size: 0.95rem; margin-bottom: 1rem; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.04em; }
        .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 1rem; }
        @media (max-width: 600px) { .form-grid { grid-template-columns: 1fr; } }
    </style>
</head>
<%!
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
%>
<body>
    <div class="auth-container">
        <div class="auth-card">
            <div class="auth-header">
                <h1>Profile</h1>
                <a href="dashboard" class="back-link">&larr; Back to Dashboard</a>
            </div>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="message error">
                    <%= esc(request.getAttribute("errorMessage").toString()) %>
                </div>
            <% } %>
            <% if (request.getAttribute("successMessage") != null) { %>
                <div class="message success">
                    <%= esc(request.getAttribute("successMessage").toString()) %>
                </div>
            <% } %>

            <%
                Map<String, String> profile = (Map<String, String>) request.getAttribute("profile");
            %>

            <% if (profile == null) { %>
                <div class="message error">Failed to load profile. Please try again.</div>
            <% } else { %>

            <form action="profile" method="post" class="auth-form">
                <input type="hidden" name="csrfToken" value='<%= session.getAttribute("csrfToken") %>'>

                <div class="form-section">
                    <h2>Account</h2>
                    <div class="form-group">
                        <label for="username">Username (Cannot be changed)</label>
                        <input type="text" id="username"
                            value="<%= esc(profile.get("username")) %>"
                            autocomplete="username"
                            disabled>
                    </div>
                    <div class="form-group">
                        <label for="password">New Password (Leave blank to keep current)</label>
                        <input type="password" id="password" name="password"
                            placeholder="Enter a new password"
                            autocomplete="new-password">
                    </div>
                </div>

                <div class="form-section">
                    <h2>Personal Details</h2>
                    <div class="form-group">
                        <label for="fullName">Full Name</label>
                        <input type="text" id="fullName" name="fullName"
                            value="<%= esc(profile.get("fullName")) %>"
                            required>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="birthday">Birthday</label>
                            <input type="date" id="birthday" name="birthday"
                                value="<%= esc(profile.get("birthday")) %>"
                                required>
                        </div>
                        <div class="form-group">
                            <label for="msisdn">Phone (MSISDN)</label>
                            <input type="tel" id="msisdn" name="msisdn"
                                value="<%= esc(profile.get("msisdn")) %>"
                                autocomplete="tel"
                                required>
                        </div>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="job">Job</label>
                            <input type="text" id="job" name="job"
                                value="<%= esc(profile.get("job")) %>"
                                required>
                        </div>
                        <div class="form-group">
                            <label for="email">Email</label>
                            <input type="email" id="email" name="email"
                                value="<%= esc(profile.get("email")) %>"
                                autocomplete="email"
                                required>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="address">Address</label>
                        <input type="text" id="address" name="address"
                            value="<%= esc(profile.get("address")) %>"
                            autocomplete="street-address"
                            required>
                    </div>
                </div>

                <div class="form-section">
                    <h2>Twilio Credentials</h2>
                    <div class="form-group">
                        <label for="twilioSid">Account SID</label>
                        <input type="text" id="twilioSid" name="twilioSid"
                            value="<%= esc(profile.get("twilioSid")) %>"
                            autocomplete="off"
                            required>
                    </div>
                    <div class="form-group">
                        <label for="twilioToken">Auth Token (Leave blank to keep current)</label>
                        <input type="password" id="twilioToken" name="twilioToken"
                            placeholder="Enter new token to update"
                            autocomplete="off">
                    </div>
                    <div class="form-group">
                        <label for="twilioSender">Sender ID (From Number)</label>
                        <input type="tel" id="twilioSender" name="twilioSender"
                            value="<%= esc(profile.get("twilioSender")) %>"
                            autocomplete="off"
                            required>
                    </div>
                </div>

                <button type="submit">Save Changes</button>
            </form>

            <% } %>
        </div>
    </div>
</body>
</html>