<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register - Twilio SMS</title>
    <link rel="stylesheet" href="css/style.css?v=3">
    <style>
        body {
            display: flex;
            justify-content: center;
            align-items: flex-start;
            min-height: 100vh;
            padding: 2rem 1rem;
        }
        .auth-container { width: 100%; max-width: 640px; }
        .auth-header { text-align: center; margin-bottom: 2rem; }
        .auth-header h1 { font-size: 1.75rem; margin-bottom: 0.35rem; }
        .auth-header p { color: var(--text-secondary); font-size: 0.95rem; }
        .auth-form button { width: 100%; margin-top: 0.75rem; }
        .auth-footer { text-align: center; margin-top: 1.5rem; color: var(--text-secondary); font-size: 0.9rem; }
        .form-section {
            margin-bottom: 1.5rem;
            padding-bottom: 1.25rem;
            border-bottom: 1px solid var(--border);
        }
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
                <h1>Create Account</h1>
                <p>Set up your Twilio SMS account</p>
            </div>

            <% if (request.getAttribute("error") != null) { %>
                <div class="message error"><%= request.getAttribute("error") %></div>
            <% } %>

            <form action="register" method="post" class="auth-form">
                <div class="form-section">
                    <h2>Account</h2>
                    <div class="form-group">
                        <label for="username">Username</label>
                        <input type="text" id="username" name="username" value="<%= request.getAttribute("username") != null ? request.getAttribute("username") : "" %>" placeholder="Choose a username" required autofocus>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="password">Password</label>
                            <input type="password" id="password" name="password" placeholder="Create a password" required>
                        </div>
                        <div class="form-group">
                            <label for="confirm_password">Confirm Password</label>
                            <input type="password" id="confirm_password" name="confirm_password" placeholder="Confirm password" required>
                        </div>
                    </div>
                </div>

                <div class="form-section">
                    <h2>Profile</h2>
                    <div class="form-group">
                        <label for="full_name">Full Name</label>
                        <input type="text" id="full_name" name="full_name" value="<%= request.getAttribute("full_name") != null ? request.getAttribute("full_name") : "" %>" placeholder="John Doe" required>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="birthday">Birthday</label>
                            <input type="date" id="birthday" name="birthday" value="<%= request.getAttribute("birthday") != null ? request.getAttribute("birthday") : "" %>" required>
                        </div>
                        <div class="form-group">
                            <label for="msisdn">Phone (MSISDN)</label>
                            <input type="tel" id="msisdn" name="msisdn" value="<%= request.getAttribute("msisdn") != null ? request.getAttribute("msisdn") : "" %>" placeholder="+1234567890" required>
                        </div>
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="job">Job</label>
                            <input type="text" id="job" name="job" value="<%= request.getAttribute("job") != null ? request.getAttribute("job") : "" %>" placeholder="Software Engineer" required>
                        </div>
                        <div class="form-group">
                            <label for="email">Email</label>
                            <input type="email" id="email" name="email" value="<%= request.getAttribute("email") != null ? request.getAttribute("email") : "" %>" placeholder="you@example.com" required>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="address">Address</label>
                        <input type="text" id="address" name="address" value="<%= request.getAttribute("address") != null ? request.getAttribute("address") : "" %>" placeholder="123 Main St, City, Country" required>
                    </div>
                </div>

                <div class="form-section">
                    <h2>Twilio Credentials</h2>
                    <div class="form-group">
                        <label for="twilio_account_sid">Account SID</label>
                        <input type="text" id="twilio_account_sid" name="twilio_account_sid" value="<%= request.getAttribute("twilio_account_sid") != null ? request.getAttribute("twilio_account_sid") : "" %>" placeholder="ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" required>
                    </div>
                    <div class="form-group">
                        <label for="twilio_auth_token">Auth Token</label>
                        <input type="password" id="twilio_auth_token" name="twilio_auth_token" placeholder="Your Twilio auth token" required>
                    </div>
                    <div class="form-group">
                        <label for="twilio_sender_id">Sender ID (From Number)</label>
                        <input type="tel" id="twilio_sender_id" name="twilio_sender_id" value="<%= request.getAttribute("twilio_sender_id") != null ? request.getAttribute("twilio_sender_id") : "" %>" placeholder="+1234567890" required>
                    </div>
                    <div class="message warning" style="font-size: 0.85rem; margin-bottom: 0;">
                        A verification code will be sent to your phone using these credentials.
                    </div>
                </div>

                <button type="submit">Continue to Verification</button>
            </form>

            <div class="auth-footer">
                <p>Already have an account? <a href="login">Sign In</a></p>
            </div>
        </div>
    </div>
</body>
</html>
