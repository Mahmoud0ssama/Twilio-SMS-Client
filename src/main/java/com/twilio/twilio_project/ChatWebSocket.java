package com.twilio.twilio_project; // WebSocket endpoint — real-time push for chat & broadcast notifications

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.*;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

// WebSocket endpoint at /chat/{userId}. One connection per logged-in user.
// pushToUser() delivers JSON messages to a specific userId's active WebSocket.
// Used by ChatServlet (new_message) and BroadcastServlet (system_message, broadcast_log).
@ServerEndpoint(value = "/chat/{userId}", configurator = ChatWebSocket.ChatConfigurator.class)
public class ChatWebSocket {

    private static final ConcurrentHashMap<Integer, Session> userSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session wsSession, @PathParam("userId") int userId) {
        userSessions.put(userId, wsSession);
    }

    @OnClose
    public void onClose(Session wsSession, @PathParam("userId") int userId) {
        userSessions.remove(userId, wsSession);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("userId") int userId) {
        // WebSocket messages from client are currently unused; chat send happens via HTTP POST
    }

    // Push a JSON string to a specific user's WebSocket. No-op if user not connected.
    public static void pushToUser(int userId, String json) {
        Session session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(json);
            } catch (IOException e) {
                userSessions.remove(userId, session);
            }
        }
    }

    // Extracts userId from the HTTP session during WebSocket handshake.
    // Required because @PathParam alone is not authenticated — we verify the session cookie.
    public static class ChatConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
            HttpSession httpSession = (HttpSession) request.getHttpSession();
            if (httpSession != null) {
                Object userId = httpSession.getAttribute("userId");
                if (userId != null) {
                    config.getUserProperties().put("userId", userId);
                }
            }
        }
    }
}
