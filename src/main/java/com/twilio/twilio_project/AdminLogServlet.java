package com.twilio.twilio_project;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "adminLogServlet", value = "/admin/smpp-logs")
public class AdminLogServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");

        HttpSession session = req.getSession(false);
        if (session == null || !"administrator".equals(session.getAttribute("userRole"))) {
            resp.setStatus(403);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Admins only\"}");
            return;
        }

        List<SmpEventLogger.LogEntry> logs = SmpEventLogger.getLogs();
        JsonArray arr = new JsonArray();
        for (int i = logs.size() - 1; i >= 0; i--) {
            SmpEventLogger.LogEntry e = logs.get(i);
            JsonObject jo = new JsonObject();
            jo.addProperty("timestamp", e.timestamp);
            jo.addProperty("level", e.level);
            jo.addProperty("event", e.event);
            jo.addProperty("detail", e.detail);
            arr.add(jo);
        }
        JsonObject res = new JsonObject();
        res.addProperty("status", "success");
        res.add("logs", arr);
        resp.getWriter().write(gson.toJson(res));
    }
}
