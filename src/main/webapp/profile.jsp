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
<body>
    <div class="auth-container">
        <div class="auth-card">
            <div class="auth-header">
                <h1>Profile</h1>
                <a href="dashboard" class="back-link">&larr; Back to Dashboard</a>
            </div>

            <% if (request.getAttribute("errorMessage") != null) { %>
                <div class="message error"><%= request.getAttribute("errorMessage") %></div>
            <% } %>
            <% if (request.getAttribute("successMessage") != null) { %>
                <div class="message success"><%= request.getAttribute("successMessage") %></div>
            <% } %>

            <% Map<String, String> profile = (Map<String, String>) request.getAttribute("profile"); %>

            <form action="profile" method="post" class="auth-form">
                <div class="form-section">
                    <h2>Account</h2>
                    <div class="form-group">
                        <label for="username">Username (Cannot be changed)</label>
                        <input type="text" id="username" value="<%= profile.get("username") %>" disabled>
                    </div>
                    <div class="form-group">
                        <label for="password">New Password (Leave blank to keep current)</label>
                        <input type="password" id="password" name="password" placeholder="Enter a new password">
                    </div>
                </div>

                <div class="form-section">
                    <h2>Personal Details</h2>
                    <div class="form-group">
                        <label for="fullName">Full Name</label>
                        <input type="text" id="fullName" name="fullName" value="<%= profile.get("fullName") %>" required>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="birthday">Birthday</label>
                            <input type="date" id="birthday" name="birthday" value="<%= profile.get("birthday") != null ? profile.get("birthday") : "" %>" required>
                        </div>
                        <div class="form-group">
                            <label for="msisdn">Phone (MSISDN)</label>
                            <input type="tel" id="msisdn" name="msisdn" value="<%= profile.get("msisdn") %>" required>
                        </div>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="job">Job</label>
                            <input type="text" id="job" name="job" value="<%= profile.get("job") %>" required>
                        </div>
                        <div class="form-group">
                            <label for="email">Email</label>
                            <input type="email" id="email" name="email" value="<%= profile.get("email") %>" required>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="address">Address</label>
                        <input type="text" id="address" name="address" value="<%= profile.get("address") %>" required>
                    </div>
                </div>

                <div class="form-section">
                    <h2>Twilio Credentials</h2>
                    <div class="form-group">
                        <label for="twilioSid">Account SID</label>
                        <input type="text" id="twilioSid" name="twilioSid" value="<%= profile.get("twilioSid") %>" required>
                    </div>
                    <div class="form-group">
                        <label for="twilioToken">Auth Token</label>
                        <input type="password" id="twilioToken" name="twilioToken" value="<%= profile.get("twilioToken") %>" required>
                    </div>
                    <div class="form-group">
                        <label for="twilioSender">Sender ID (From Number)</label>
                        <input type="tel" id="twilioSender" name="twilioSender" value="<%= profile.get("twilioSender") %>" required>
                    </div>
                </div>

                <button type="submit">Save Changes</button>
            </form>
        </div>
    </div>
</body>
</html>
