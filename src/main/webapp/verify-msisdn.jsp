<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verify Phone - Twilio SMS</title>
    <link rel="stylesheet" href="css/style.css?v=3">
    <style>
        body { display: flex; justify-content: center; align-items: center; min-height: 100vh; padding: 1rem; }
        .auth-container { width: 100%; max-width: 400px; }
        .auth-header { text-align: center; margin-bottom: 2rem; }
        .auth-header h1 { font-size: 1.75rem; margin-bottom: 0.35rem; }
        .auth-header p { color: var(--text-secondary); font-size: 0.95rem; }
        .auth-form button { width: 100%; margin-top: 0.5rem; }
        .code-input { font-size: 1.5rem !important; letter-spacing: 0.5rem; text-align: center; font-weight: 600; }
        .form-actions { display: flex; flex-direction: column; gap: 0.75rem; margin-top: 1.5rem; text-align: center; }
        .link-btn { background: none; border: none; color: var(--text-secondary); cursor: pointer; font-size: 0.9rem; padding: 0; box-shadow: none; }
        .link-btn:hover { color: var(--text-primary); text-decoration: underline; transform: none; box-shadow: none; background: none; filter: none; }
    </style>
</head>
<body>
    <div class="auth-container">
        <div class="auth-card">
            <div class="auth-header">
                <h1>Verify Phone</h1>
                <p>Enter the 6-digit code sent to
                    <strong style="color: var(--text-primary);"><%= request.getAttribute("msisdn") != null ? request.getAttribute("msisdn") : "your number" %></strong>
                </p>
            </div>

            <% if (request.getAttribute("message") != null) { %>
                <div class="message success"><%= request.getAttribute("message") %></div>
            <% } %>
            <% if (request.getAttribute("error") != null) { %>
                <div class="message error"><%= request.getAttribute("error") %></div>
            <% } %>

            <form action="verify-msisdn" method="post" class="auth-form">
                <div class="form-group">
                    <input type="text" id="code" name="code" class="code-input" placeholder="000000"
                        pattern="[0-9]{6}" maxlength="6" inputmode="numeric" required autofocus>
                </div>
                <button type="submit">Verify & Complete</button>
            </form>

            <div class="form-actions">
                <form action="verify-msisdn" method="post" style="display:inline;">
                    <input type="hidden" name="action" value="resend">
                    <button type="submit" class="link-btn">Didn't receive code? Resend</button>
                </form>
                <form action="verify-msisdn" method="post" style="display:inline;">
                    <input type="hidden" name="action" value="cancel">
                    <button type="submit" class="link-btn" style="color: var(--red);">Cancel Registration</button>
                </form>
            </div>
        </div>
    </div>
</body>
</html>
