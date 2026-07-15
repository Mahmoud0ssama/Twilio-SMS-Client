package com.twilio.twilio_project; // Session auth filter — blocks unauthenticated requests to /api/*, /admin/*

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

// Filters all requests. Unauthenticated requests to protected paths (/api/*, /admin/*)
// are rejected with 401 before reaching the servlet.
// Login, register, and Twilio webhook are excluded from auth checks.
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI().substring(request.getContextPath().length());

        // Paths that don't require authentication
        boolean isPublic = path.equals("/login") || path.equals("/register")
                || path.equals("/verify-msisdn")
                || path.equals("/webhook/sms")
                || path.equals("/") || path.startsWith("/assets/");

        if (!isPublic) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized\"}");
                return;
            }
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {}
}
