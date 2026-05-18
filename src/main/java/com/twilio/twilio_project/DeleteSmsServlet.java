package com.twilio.twilio_project;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "deleteSmsServlet", value = "/delete-sms")
public class DeleteSmsServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        String smsIdStr = request.getParameter("smsId");

        if (smsIdStr != null && !smsIdStr.trim().isEmpty()) {
            try {
                int smsId = Integer.parseInt(smsIdStr.trim());
                UserRepository.deleteSmsByIdAndUserId(smsId, userId);
                session.setAttribute("smsSuccess", "SMS deleted successfully.");
            } catch (NumberFormatException | SQLException e) {
                e.printStackTrace();
                session.setAttribute("smsError", "Failed to delete SMS.");
            }
        } else {
            session.setAttribute("smsError", "Invalid SMS ID.");
        }

        response.sendRedirect("dashboard");
    }
}
