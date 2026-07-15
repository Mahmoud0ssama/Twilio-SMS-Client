package com.twilio.twilio_project; // SMPP event ring buffer — connect/disconnect/error logs for admin console

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// In-memory bounded ring buffer for SMPP session events.
// Avoids needing an external log aggregator — admin reads these via /admin/smpp-logs.
// synchronizedList for thread-safe concurrent writes from SmppSessionManager callbacks.
public final class SmpEventLogger {
    private static final int MAX_ENTRIES = 500;
    private static final List<LogEntry> buffer = Collections.synchronizedList(new ArrayList<>());

    private SmpEventLogger() {}

    public static void log(String level, String event, String detail) {
        LogEntry entry = new LogEntry(Instant.now().toString(), level, event, detail);
        synchronized (buffer) {
            buffer.add(entry);
            if (buffer.size() > MAX_ENTRIES) {
                buffer.remove(0);
            }
        }
    }

    public static List<LogEntry> getLogs() {
        synchronized (buffer) {
            return new ArrayList<>(buffer);
        }
    }

    public static class LogEntry {
        public final String timestamp;
        public final String level;
        public final String event;
        public final String detail;

        public LogEntry(String timestamp, String level, String event, String detail) {
            this.timestamp = timestamp;
            this.level = level;
            this.event = event;
            this.detail = detail;
        }
    }
}
