<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ page import="java.util.List" %>
        <!DOCTYPE html>
        <html lang="en">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Dashboard - Twilio SMS</title>
            <link rel="stylesheet" href="css/style.css?v=3">
            <style>
                .dash {
                    max-width: 1600px;
                    margin: 0 auto;
                    padding: 1.5rem 3rem;
                }

                .topbar {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 1.5rem;
                    padding: 1rem 0;
                    border-bottom: 1px solid var(--border);
                }

                .topbar h1 {
                    margin: 0;
                    font-size: 1.5rem;
                }

                .topbar h1 span {
                    color: var(--accent);
                }

                .topbar-right {
                    display: flex;
                    align-items: center;
                    gap: 0.75rem;
                }

                .topbar-right .welcome {
                    color: var(--text-secondary);
                    font-size: 0.9rem;
                }

                .grid {
                    display: grid;
                    grid-template-columns: 340px 1fr;
                    gap: 1.5rem;
                    align-items: start;
                }

                .search-bar {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
                    gap: 0.75rem;
                    margin-bottom: 1rem;
                    align-items: end;
                }

                .search-bar .actions {
                    display: flex;
                    gap: 0.5rem;
                }

                .delete-btn {
                    background: var(--red-bg);
                    color: var(--red);
                    padding: 0.25rem 0.6rem;
                    font-size: 0.8rem;
                    border: 1px solid rgba(248, 81, 73, 0.25);
                    box-shadow: none;
                }

                .delete-btn:hover {
                    background: rgba(248, 81, 73, 0.3);
                }

                @media (max-width: 1024px) {
                    .grid {
                        grid-template-columns: 1fr;
                    }
                }

                @media (max-width: 768px) {
                    .topbar {
                        flex-direction: column;
                        align-items: flex-start;
                        gap: 0.75rem;
                    }

                    .topbar-right {
                        width: 100%;
                        justify-content: space-between;
                    }
                }
            </style>
        </head>

        <body>
            <div class="dash">
                <div class="topbar">
                    <h1>Twilio <span>SMS</span></h1>
                    <div class="topbar-right">
                        <span class="welcome">Welcome!</span>
                        <a href="profile" class="btn">Profile</a>
                        <a href="logout" class="btn btn-secondary">Logout</a>
                    </div>
                </div>
                <div class="grid">
                    <div class="card">
                        <h2 class="mb-2">Send SMS</h2>
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
                                                <input type="tel" id="from" value="<%= request.getAttribute(" senderId")
                                                    !=null ? request.getAttribute("senderId") : "" %>"
                                                placeholder="Twilio sender number" readonly>
                                            </div>
                                            <div class="form-group">
                                                <label for="recipient">To</label>
                                                <input type="tel" id="recipient" name="recipient"
                                                    placeholder="+1234567890" required>
                                            </div>
                                            <div class="form-group">
                                                <label for="message">Message</label>
                                                <textarea id="message" name="message" placeholder="Type your message..."
                                                    required></textarea>
                                            </div>
                                            <button type="submit" style="width:100%;">Send Message</button>
                                        </form>
                    </div>
                    <div class="card">
                        <h2 class="mb-2">History</h2>
                        <form action="dashboard" method="get" class="search-bar">
                            <div class="form-group mb-0">
                                <label for="searchFrom">From</label>
                                <input type="text" id="searchFrom" name="searchFrom" value="<%= request.getAttribute("
                                    searchFrom") !=null ? request.getAttribute("searchFrom") : "" %>">
                            </div>
                            <div class="form-group mb-0">
                                <label for="searchTo">To</label>
                                <input type="text" id="searchTo" name="searchTo" value="<%= request.getAttribute("
                                    searchTo") !=null ? request.getAttribute("searchTo") : "" %>">
                            </div>
                            <div class="form-group mb-0">
                                <label for="startDate">Start</label>
                                <input type="date" id="startDate" name="startDate" value="<%= request.getAttribute("
                                    startDate") !=null ? request.getAttribute("startDate") : "" %>">
                            </div>
                            <div class="form-group mb-0">
                                <label for="endDate">End</label>
                                <input type="date" id="endDate" name="endDate" value="<%= request.getAttribute("
                                    endDate") !=null ? request.getAttribute("endDate") : "" %>">
                            </div>
                            <div class="actions">
                                <button type="submit">Search</button>
                                <a href="dashboard" class="btn btn-secondary">Clear</a>
                            </div>
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
                                    <% List<?> smsList = (List
                                        <?>) request.getAttribute("smsHistory");
                       if (smsList != null && !smsList.isEmpty()) {
                           for (Object item : smsList) {
                               java.util.Map<String, Object> sms = (java.util.Map<String, Object>) item; %>
                    <tr>
                        <td><%= sms.get("from") %></td>
                        <td><%= sms.get("recipient") %></td>
                        <td><%= sms.get("message") %></td>
                        <td><span class="status-<%= ((String)sms.get("status")).toLowerCase() %>"><%= sms.get("status") %></span></td>
                        <td><%= sms.get("sentAt") %></td>
                        <td>
                            <form action="delete-sms" method="post" style="margin:0;" onsubmit="return confirm('Delete this record?');">
                                <input type="hidden" name="smsId" value="<%= sms.get("id") %>">
                                <button type="submit" class="delete-btn">Delete</button>
                            </form>
                        </td>
                    </tr>
                    <%  } } else { %>
                    <tr><td colspan="6" class="text-center text-muted">No messages found</td></tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
</body>
</html>