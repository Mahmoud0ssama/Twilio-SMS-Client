<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ page import="java.util.List" %>
        <!DOCTYPE html>
        <html lang="en">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Dashboard - Twilio SMS</title>
            <link rel="stylesheet" href="css/style.css">
            <style>
                body {
                    padding: 1.5rem;
                }

                .header {
                    background: var(--white);
                    padding: 1.5rem;
                    border-radius: var(--border-radius);
                    box-shadow: var(--shadow);
                    margin-bottom: 2rem;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    flex-wrap: wrap;
                    gap: 1rem;
                }

                .header h1 {
                    margin: 0;
                    font-size: 1.75rem;
                }

                .header-right {
                    display: flex;
                    align-items: center;
                    gap: 1rem;
                }

                .welcome {
                    color: var(--text-muted);
                    font-weight: 500;
                }

                .logout-btn {
                    background-color: var(--secondary-color);
                    color: var(--white);
                    padding: 0.75rem 1.5rem;
                    text-decoration: none;
                    border-radius: 4px;
                    transition: background-color 0.3s ease;
                    display: inline-block;
                }

                .logout-btn:hover {
                    background-color: var(--secondary-dark);
                }

                .main-content {
                    display: grid;
                    grid-template-columns: 1fr 2fr;
                    gap: 2rem;
                }

                .card {
                    background: var(--white);
                    padding: 1.5rem;
                    border-radius: var(--border-radius);
                    box-shadow: var(--shadow);
                }

                .card h2 {
                    margin-top: 0;
                }

                .send-sms {
                    height: fit-content;
                }

                .sms-history {
                    overflow-x: auto;
                }

                .table-wrapper {
                    overflow-x: auto;
                }

                @media (max-width: 1024px) {
                    .main-content {
                        grid-template-columns: 1fr;
                    }

                    .send-sms {
                        height: auto;
                    }
                }

                @media (max-width: 768px) {
                    .header {
                        flex-direction: column;
                        align-items: flex-start;
                    }

                    .header-right {
                        width: 100%;
                        justify-content: space-between;
                    }
                }
            </style>
        </head>

        <body>
            <div class="header">
                <h1>Twilio SMS Dashboard</h1>
                <div class="header-right">
                    <span class="welcome">Welcome!</span>
                    <a href="profile" class="logout-btn" style="background-color: var(--primary-color);">Profile</a>
                    <a href="logout" class="logout-btn">Logout</a>
                </div>
            </div>

            <div class="main-content">
                <div class="card send-sms">
                    <h2>Send SMS</h2>
                    <% if (request.getAttribute("smsSuccess") !=null) { %>
                        <div class="message success">
                            <%= request.getAttribute("smsSuccess") %>
                        </div>
                        <% } %>
                            <% if (request.getAttribute("smsError") !=null) { %>
                                <div class="message error">
                                    <%= request.getAttribute("smsError") %>
                                </div>
                                <% } %>
                                    <form action="send-sms" method="post">
                                        <div class="form-group">
                                            <label for="from">From</label>
                                            <input type="tel" id="from"
                                                value="<%= request.getAttribute("senderId") != null ? request.getAttribute("senderId") : "" %>"
                                                placeholder="Your Twilio sender number" readonly>
                                        </div>
                                        <div class="form-group">
                                            <label for="recipient">To</label>
                                            <input type="tel" id="recipient" name="recipient" placeholder="+1234567890"
                                                required>
                                        </div>
                                        <div class="form-group">
                                            <label for="message">Body</label>
                                            <textarea id="message" name="message"
                                                placeholder="Enter your message here..." required></textarea>
                                        </div>
                                        <button type="submit" style="width: 100%;">Send SMS</button>
                                    </form>
                </div>

                <div class="card sms-history">
                    <h2>SMS History</h2>
                    <form action="dashboard" method="get" style="margin-bottom: 1rem; display: flex; gap: 1rem; flex-wrap: wrap; align-items: flex-end;">
                        <div class="form-group" style="margin-bottom: 0;">
                            <label for="searchFrom" style="font-size: 0.85rem;">From</label>
                            <input type="text" id="searchFrom" name="searchFrom" value="<%= request.getAttribute("searchFrom") != null ? request.getAttribute("searchFrom") : "" %>" style="padding: 0.5rem;">
                        </div>
                        <div class="form-group" style="margin-bottom: 0;">
                            <label for="searchTo" style="font-size: 0.85rem;">To</label>
                            <input type="text" id="searchTo" name="searchTo" value="<%= request.getAttribute("searchTo") != null ? request.getAttribute("searchTo") : "" %>" style="padding: 0.5rem;">
                        </div>
                        <div class="form-group" style="margin-bottom: 0;">
                            <label for="startDate" style="font-size: 0.85rem;">Start Date</label>
                            <input type="date" id="startDate" name="startDate" value="<%= request.getAttribute("startDate") != null ? request.getAttribute("startDate") : "" %>" style="padding: 0.5rem;">
                        </div>
                        <div class="form-group" style="margin-bottom: 0;">
                            <label for="endDate" style="font-size: 0.85rem;">End Date</label>
                            <input type="date" id="endDate" name="endDate" value="<%= request.getAttribute("endDate") != null ? request.getAttribute("endDate") : "" %>" style="padding: 0.5rem;">
                        </div>
                        <button type="submit" style="padding: 0.5rem 1rem;">Search</button>
                        <a href="dashboard" style="padding: 0.5rem 1rem; text-decoration: none; border: 1px solid var(--border-color); border-radius: 4px; color: var(--text-color);">Clear</a>
                    </form>
                    <div class="table-wrapper">
                        <table>
                            <thead>
                                <tr>
                                    <th>From</th>
                                    <th>To</th>
                                    <th>Body</th>
                                    <th>Status</th>
                                    <th>Sent At</th>
                                    <th>Action</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% List<?> smsList = (List<?>) request.getAttribute("smsHistory");
                                if (smsList != null && !smsList.isEmpty()) {
                                    for (Object item : smsList) {
                                        java.util.Map<String, Object> sms = (java.util.Map<String, Object>) item;
                                %>
                                <tr>
                                    <td><%= sms.get("from") %></td>
                                    <td><%= sms.get("recipient") %></td>
                                    <td><%= sms.get("message") %></td>
                                    <td><span class="status-<%= sms.get("status") %>"><%= sms.get("status") %></span></td>
                                    <td><%= sms.get("sentAt") %></td>
                                    <td>
                                        <form action="delete-sms" method="post" style="margin: 0; display: inline;" onsubmit="return confirm('Are you sure you want to delete this SMS?');">
                                            <input type="hidden" name="smsId" value="<%= sms.get("id") %>">
                                            <button type="submit" style="background-color: #dc3545; padding: 0.25rem 0.5rem; font-size: 0.8rem; width: auto; color: white; border: none; border-radius: 4px; cursor: pointer;">Delete</button>
                                        </form>
                                    </td>
                                </tr>
                                <% }
                                } else { %>
                                <tr>
                                    <td colspan="6" class="text-center text-muted">No SMS history found</td>
                                </tr>
                                <% } %>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>
</html>