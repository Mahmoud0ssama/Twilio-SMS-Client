<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Twilio SMS - Send Messages Easily</title>
    <link rel="stylesheet" href="css/style.css?v=3">
    <style>
        .page { max-width: 1100px; margin: 0 auto; padding: 2rem 1.5rem; }

        .navbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 1rem 1.5rem;
            margin-bottom: 4rem;
        }
        .navbar .logo {
            font-size: 1.4rem;
            font-weight: 700;
            color: var(--text-primary);
        }
        .navbar .logo span { color: var(--accent); }
        .nav-links { display: flex; gap: 0.75rem; }

        /* Hero */
        .hero {
            text-align: center;
            padding: 5rem 1rem 6rem;
        }
        .hero h1 {
            font-size: 3rem;
            font-weight: 700;
            margin-bottom: 1rem;
            letter-spacing: -0.02em;
        }
        .hero h1 span { color: var(--accent); }
        .hero p {
            font-size: 1.15rem;
            color: var(--text-secondary);
            max-width: 520px;
            margin: 0 auto 2.5rem;
        }
        .hero .btn { padding: 0.75rem 2rem; font-size: 1rem; border-radius: 8px; }

        /* Features */
        .features { padding: 3rem 0; }
        .features h2 { text-align: center; font-size: 1.75rem; margin-bottom: 2.5rem; }
        .feature-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 1.5rem;
        }
        .feature-card { text-align: center; padding: 2rem 1.5rem; }
        .feature-card:hover { border-color: var(--border-hover); }
        .feature-icon {
            font-size: 2.5rem;
            margin-bottom: 1rem;
            display: block;
        }
        .feature-card h3 { font-size: 1.1rem; margin-bottom: 0.5rem; }
        .feature-card p { color: var(--text-secondary); font-size: 0.9rem; }

        /* Footer */
        .footer {
            text-align: center;
            padding: 2.5rem 0;
            margin-top: 5rem;
            border-top: 1px solid var(--border);
            color: var(--text-secondary);
            font-size: 0.85rem;
        }

        @media (max-width: 768px) {
            .navbar { flex-direction: column; gap: 1rem; }
            .hero h1 { font-size: 2rem; }
            .hero { padding: 3rem 0.5rem 4rem; }
        }
    </style>
</head>
<body>
    <div class="page">
        <nav class="card navbar">
            <div class="logo">📱 Twilio <span>SMS</span></div>
            <div class="nav-links">
                <a href="login" class="btn btn-secondary">Login</a>
                <a href="register" class="btn">Register</a>
            </div>
        </nav>

        <section class="hero">
            <h1>Send SMS Messages<br>with <span>Ease</span></h1>
            <p>A simple, powerful platform to send and track SMS messages to your contacts worldwide.</p>
            <a href="register" class="btn">Get Started Free →</a>
        </section>

        <section class="features">
            <h2>Why Choose Us</h2>
            <div class="feature-grid">
                <div class="card feature-card">
                    <span class="feature-icon">✉️</span>
                    <h3>Easy to Use</h3>
                    <p>Send messages with a few clicks. No complicated setup required.</p>
                </div>
                <div class="card feature-card">
                    <span class="feature-icon">📊</span>
                    <h3>Track History</h3>
                    <p>Full history with real-time delivery status for every message.</p>
                </div>
                <div class="card feature-card">
                    <span class="feature-icon">🔒</span>
                    <h3>Secure</h3>
                    <p>Protected with authentication and industry-standard security.</p>
                </div>
            </div>
        </section>

        <footer class="footer">
            <p>&copy; 2026 Twilio SMS. All rights reserved.</p>
        </footer>
    </div>
</body>
</html>