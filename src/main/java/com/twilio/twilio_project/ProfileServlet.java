package com.twilio.twilio_project;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "profileServlet", value = "/profile")
public class ProfileServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        try {
            Map<String, String> profile = UserRepository.getUserProfile(userId);
            if (profile != null) {
                request.setAttribute("profile", profile);
                request.getRequestDispatcher("profile.jsp").forward(request, response);
            } else {
                response.sendRedirect("dashboard");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        int userId = (int) session.getAttribute("userId");
        Map<String, String> profile = new HashMap<>();
        
        profile.put("fullName", request.getParameter("fullName"));
        profile.put("birthday", request.getParameter("birthday"));
        profile.put("msisdn", request.getParameter("msisdn"));
        profile.put("job", request.getParameter("job"));
        profile.put("email", request.getParameter("email"));
        profile.put("address", request.getParameter("address"));
        profile.put("twilioSid", request.getParameter("twilioSid"));
        profile.put("twilioToken", request.getParameter("twilioToken"));
        profile.put("twilioSender", request.getParameter("twilioSender"));

        String password = request.getParameter("password");
        if (password != null && !password.trim().isEmpty()) {
            profile.put("passwordHash", PasswordUtil.hash(password));
        }

        try {
            UserRepository.updateUserProfile(userId, profile);
            request.setAttribute("successMessage", "Profile updated successfully.");
            request.setAttribute("profile", UserRepository.getUserProfile(userId));
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Error updating profile.");
            request.setAttribute("profile", profile);
        }

        request.getRequestDispatcher("profile.jsp").forward(request, response);
    }
}
