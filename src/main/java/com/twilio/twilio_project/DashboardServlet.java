package com.twilio.twilio_project;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "dashboardServlet", value = "/dashboard")
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String smsSuccess = (String) session.getAttribute("smsSuccess");
        String smsError = (String) session.getAttribute("smsError");
        if (smsSuccess != null) {
            request.setAttribute("smsSuccess", smsSuccess);
            session.removeAttribute("smsSuccess");
        }
        if (smsError != null) {
            request.setAttribute("smsError", smsError);
            session.removeAttribute("smsError");
        }

        int userId = (int) session.getAttribute("userId");

        String searchFrom = request.getParameter("searchFrom");
        String searchTo = request.getParameter("searchTo");
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");

        if ((searchFrom != null && !searchFrom.trim().isEmpty()) ||
            (searchTo != null && !searchTo.trim().isEmpty()) ||
            (startDate != null && !startDate.trim().isEmpty()) ||
            (endDate != null && !endDate.trim().isEmpty())) {
            request.setAttribute("smsHistory", UserRepository.searchSmsHistoryByUserId(userId, searchFrom, searchTo, startDate, endDate));
            
            // Pass search params back to view so the form stays populated
            request.setAttribute("searchFrom", searchFrom);
            request.setAttribute("searchTo", searchTo);
            request.setAttribute("startDate", startDate);
            request.setAttribute("endDate", endDate);
        } else {
            request.setAttribute("smsHistory", UserRepository.findSmsHistoryByUserId(userId));
        }

        try {
            CustomerTwilioConfig twilio = UserRepository.findTwilioConfigByUserId(userId);
            if (twilio != null && twilio.getSenderId() != null) {
                request.setAttribute("senderId", twilio.getSenderId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        request.getRequestDispatcher("dashboard.jsp").forward(request, response);
    }
}
