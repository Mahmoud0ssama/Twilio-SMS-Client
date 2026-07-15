package com.twilio.twilio_project; // SPA fallback — serves index.html for non-API non-file routes

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

// Vite SPA fallback filter. Since the frontend uses client-side routing (SvelteKit),
// any direct navigation to /some/route must return index.html rather than 404.
// The filter passes through: root (/), static files (containing "."), and known API GET paths.
// Everything else GET gets forwarded to /index.html.
public class SpaFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        String path = request.getRequestURI().substring(request.getContextPath().length());

        boolean isGet = "GET".equalsIgnoreCase(request.getMethod());
        boolean isRoot = path.equals("/");
        boolean isStatic = path.contains(".");
        boolean isApiGet = path.equals("/profile") || path.equals("/dashboard")
                        || path.equals("/logout")
                        || path.startsWith("/admin/dashboard") || path.startsWith("/admin/customer")
                        || path.startsWith("/admin/smpp-logs")
                        || path.startsWith("/admin/wireshark")
                        || path.startsWith("/api/chat/");

        if (!isGet) {
            chain.doFilter(req, res);
        } else if (isRoot || isStatic || isApiGet) {
            chain.doFilter(req, res);
        } else {
            request.getRequestDispatcher("/index.html").forward(req, res);
        }
    }

    @Override
    public void destroy() {}
}
