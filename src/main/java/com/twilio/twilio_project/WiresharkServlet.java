package com.twilio.twilio_project; // Admin packet capture — start/stop tcpdump, view/export .pcap for analysis

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@WebServlet(name = "wiresharkServlet", value = "/admin/wireshark/*")
public class WiresharkServlet extends HttpServlet {

    private static final Gson gson = new Gson();
    private static final Path PCAP_DIR = Paths.get("/tmp");
    private static final String PCAP_FILE = "smpp_capture.pcap";

    private static final ConcurrentHashMap<String, CaptureState> captures = new ConcurrentHashMap<>();
    private static Boolean tsharkAvailable = null;

    private static class CaptureState {
        final Process process;
        final long startTime;
        final AtomicBoolean stopped = new AtomicBoolean(false);

        CaptureState(Process process) {
            this.process = process;
            this.startTime = System.currentTimeMillis();
        }

        boolean isRunning() {
            return !stopped.get() && process.isAlive();
        }
    }

    /** Count packets by reading PCAP file directly — no tshark needed. */
    private static int countPcapPackets(Path pcap) {
        if (!Files.exists(pcap)) return 0;
        try (RandomAccessFile f = new RandomAccessFile(pcap.toFile(), "r")) {
            if (f.length() < 24) return 0;
            f.seek(20);
            int linkType = readIntLE(f);
            // skip 24-byte global header
            f.seek(24);
            int count = 0;
            while (f.getFilePointer() + 16 <= f.length()) {
                int inclLen = readIntLE(f); // ts_sec(4) + ts_usec(4) + incl_len(4)
                f.skipBytes(8); // skip ts_sec, ts_usec
                inclLen = readIntLE(f); // incl_len
                f.skipBytes(4);        // orig_len
                if (inclLen > 65535) break; // sanity
                long pos = f.getFilePointer() + inclLen;
                if (pos > f.length()) break;
                f.seek(pos);
                count++;
            }
            return count;
        } catch (Exception e) {
            return -1;
        }
    }

    private static int readIntLE(RandomAccessFile f) throws IOException {
        byte[] b = new byte[4];
        f.readFully(b);
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static boolean isTsharkAvailable() {
        if (tsharkAvailable != null) return tsharkAvailable;
        try {
            Process p = new ProcessBuilder("which", "tshark").redirectErrorStream(true).start();
            tsharkAvailable = p.waitFor() == 0;
        } catch (Exception e) {
            tsharkAvailable = false;
        }
        return tsharkAvailable;
    }

    private static String exec(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");

        HttpSession session = req.getSession(false);
        if (session == null || !"administrator".equals(session.getAttribute("userRole"))) {
            resp.setStatus(403);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Admins only\"}");
            return;
        }

        String path = req.getPathInfo();
        if (path == null) path = "/";

        try {
            switch (path) {
                case "/start" -> handleStart(resp);
                case "/stop" -> handleStop(resp);
                case "/status" -> handleStatus(resp);
                case "/packets" -> handlePackets(resp);
                case "/download" -> handleDownload(req, resp);
                case "/launch" -> handleLaunch(resp);
                default -> {
                    resp.setStatus(404);
                    resp.getWriter().write("{\"status\":\"error\",\"message\":\"Unknown action\"}");
                }
            }
        } catch (Exception e) {
            resp.setStatus(500);
            JsonObject err = new JsonObject();
            err.addProperty("status", "error");
            err.addProperty("message", e.getMessage());
            resp.getWriter().write(gson.toJson(err));
        }
    }

    private void handleStart(HttpServletResponse resp) throws IOException {
        CaptureState existing = captures.get(PCAP_FILE);
        if (existing != null && existing.isRunning()) {
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"Capture already running\"}");
            return;
        }

        Path pcap = PCAP_DIR.resolve(PCAP_FILE);
        Files.deleteIfExists(pcap);

        ProcessBuilder pb = new ProcessBuilder(
            "sg", "wireshark", "-c",
            "/usr/bin/dumpcap -i lo -f \"port 8080 or port 2776 or port 12775 or port 5173\" -w " + pcap + " -F pcap"
        );
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        captures.put(PCAP_FILE, new CaptureState(proc));

        JsonObject res = new JsonObject();
        res.addProperty("status", "success");
        res.addProperty("message", "Capture started on lo (ports 8080, 2776, 12775, 5173)");
        resp.getWriter().write(gson.toJson(res));
    }

    private void handleStop(HttpServletResponse resp) throws IOException {
        CaptureState state = captures.get(PCAP_FILE);
        if (state == null || !state.isRunning()) {
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"No active capture\"}");
            return;
        }
        state.stopped.set(true);
        state.process.destroyForcibly();
        captures.remove(PCAP_FILE);

        Path pcap = PCAP_DIR.resolve(PCAP_FILE);
        JsonObject res = new JsonObject();
        res.addProperty("status", "success");
        res.addProperty("message", "Capture stopped");
        long elapsed = (System.currentTimeMillis() - state.startTime) / 1000;
        res.addProperty("durationSec", elapsed);
        res.addProperty("packetCount", countPcapPackets(pcap));
        res.addProperty("fileSize", Files.exists(pcap) ? Files.size(pcap) : 0);
        resp.getWriter().write(gson.toJson(res));
    }

    private void handleStatus(HttpServletResponse resp) throws IOException {
        CaptureState state = captures.get(PCAP_FILE);
        boolean running = state != null && state.isRunning();
        Path pcap = PCAP_DIR.resolve(PCAP_FILE);
        boolean fileExists = Files.exists(pcap);

        JsonObject res = new JsonObject();
        res.addProperty("status", "success");
        res.addProperty("running", running);
        res.addProperty("fileExists", fileExists);
        res.addProperty("tshark", isTsharkAvailable());
        if (running) {
            long elapsed = (System.currentTimeMillis() - state.startTime) / 1000;
            res.addProperty("durationSec", elapsed);
            res.addProperty("packetCount", countPcapPackets(pcap));
            res.addProperty("fileSize", fileExists ? Files.size(pcap) : 0);
        } else if (fileExists) {
            res.addProperty("packetCount", countPcapPackets(pcap));
            res.addProperty("fileSize", Files.size(pcap));
        }
        resp.getWriter().write(gson.toJson(res));
    }

    private void handlePackets(HttpServletResponse resp) throws IOException {
        Path pcap = PCAP_DIR.resolve(PCAP_FILE);
        if (!Files.exists(pcap)) {
            resp.getWriter().write("{\"status\":\"success\",\"packets\":[]}");
            return;
        }

        if (!isTsharkAvailable()) {
            JsonObject res = new JsonObject();
            res.addProperty("status", "success");
            res.addProperty("tshark", false);
            res.addProperty("packetCount", countPcapPackets(pcap));
            res.add("packets", new JsonArray());
            resp.getWriter().write(gson.toJson(res));
            return;
        }

        String tsharkOut = exec("tshark", "-r", pcap.toString(), "-T", "json");
        JsonArray raw = new JsonArray();
        if (tsharkOut != null && !tsharkOut.isEmpty()) {
            try {
                raw = gson.fromJson(tsharkOut, JsonArray.class);
            } catch (Exception ignored) {}
        }

        JsonArray packets = new JsonArray();
        for (int i = 0; i < raw.size(); i++) {
            JsonObject pkt = new JsonObject();
            JsonObject src = raw.get(i).getAsJsonObject().getAsJsonObject("_source");
            if (src == null) continue;
            JsonObject layers = src.getAsJsonObject("layers");
            if (layers == null) continue;

            JsonObject frame = layers.getAsJsonObject("frame");
            if (frame != null) {
                String rel = getFirst(frame, "frame.time_relative");
                if (rel != null) pkt.addProperty("time", Double.parseDouble(rel));
            }

            JsonObject ip = layers.getAsJsonObject("ip");
            if (ip != null) {
                pkt.addProperty("src", getFirst(ip, "ip.src"));
                pkt.addProperty("dst", getFirst(ip, "ip.dst"));
            }

            JsonObject tcp = layers.getAsJsonObject("tcp");
            String srcPort = null, dstPort = null;
            if (tcp != null) {
                srcPort = getFirst(tcp, "tcp.srcport");
                dstPort = getFirst(tcp, "tcp.dstport");
            }

            JsonObject smpp = layers.getAsJsonObject("smpp");
            JsonObject http = layers.getAsJsonObject("http");
            JsonObject json = layers.getAsJsonObject("json");

            if (smpp != null) {
                pkt.addProperty("proto", "SMPP");
                String cmdId = getFirst(smpp, "smpp.command_id");
                if (cmdId != null) {
                    pkt.addProperty("cmd", smppCommandName(cmdId));
                    pkt.addProperty("cmdRaw", cmdId);
                }
                String srcAddr = getFirst(smpp, "smpp.source_addr");
                if (srcAddr != null) pkt.addProperty("srcAddr", srcAddr);
                String dstAddr = getFirst(smpp, "smpp.destination_addr");
                if (dstAddr != null) pkt.addProperty("dstAddr", dstAddr);
                String msg = getFirst(smpp, "smpp.message");
                if (msg != null) {
                    String decoded = decodeHexSMPPMessage(msg);
                    if (decoded != null) pkt.addProperty("message", decoded.length() > 80 ? decoded.substring(0, 80) + "..." : decoded);
                }
                String msgId = getFirst(smpp, "smpp.message_id");
                if (msgId != null) pkt.addProperty("detail", "msg_id=" + msgId);
                String cmdStatus = getFirst(smpp, "smpp.command_status");
                if (cmdStatus != null && !"0x00000000".equals(cmdStatus)) {
                    pkt.addProperty("detail", (pkt.has("detail") ? pkt.get("detail").getAsString() + " " : "") + "status=" + cmdStatus);
                }
            } else if (http != null) {
                pkt.addProperty("proto", "HTTP");
                String method = getFirst(http, "http.request.method");
                String uri = getFirst(http, "http.request.uri");
                String code = getFirst(http, "http.response.code");
                if (method != null && uri != null) {
                    pkt.addProperty("cmd", method + " " + uri);
                    if (json != null) {
                        String jbody = getFirst(json, "json.object");
                        if (jbody != null) pkt.addProperty("message", jbody.length() > 120 ? jbody.substring(0, 120) + "..." : jbody);
                    }
                } else if (code != null) {
                    pkt.addProperty("cmd", code);
                    String reason = getFirst(http, "http.response.phrase");
                    pkt.addProperty("message", reason != null ? reason : "");
                }
            } else {
                if (srcPort != null && dstPort != null) {
                    if ("8080".equals(srcPort) || "8080".equals(dstPort)) {
                        pkt.addProperty("proto", "TCP:8080");
                    } else if ("2776".equals(srcPort) || "2776".equals(dstPort)) {
                        pkt.addProperty("proto", "SMPP(ctrl)");
                    } else if ("12775".equals(srcPort) || "12775".equals(dstPort)) {
                        pkt.addProperty("proto", "HTTP:12775");
                    } else if ("5173".equals(srcPort) || "5173".equals(dstPort)) {
                        pkt.addProperty("proto", "HTTP:5173");
                    }
                }
                pkt.addProperty("cmd", "TCP");
            }

            packets.add(pkt);
        }

        JsonObject res = new JsonObject();
        res.addProperty("status", "success");
        res.addProperty("tshark", true);
        res.add("packets", packets);
        resp.getWriter().write(gson.toJson(res));
    }

    /** Launch native Wireshark GUI on the server with project traffic filter. */
    private void handleLaunch(HttpServletResponse resp) throws IOException {
        try {
            String display = System.getenv("DISPLAY");
            if (display == null || display.isEmpty()) {
                resp.getWriter().write("{\"status\":\"error\",\"message\":\"No DISPLAY set — cannot launch GUI\"}");
                return;
            }
            // Use sg to get wireshark group permissions for the capture child.
            // dumpcap on this system has cap_net_admin,cap_net_raw=eip, so
            // spawning it under the wireshark group via sg is sufficient.
            ProcessBuilder pb = new ProcessBuilder(
                "sg", "wireshark", "-c",
                "nohup wireshark -i lo -f \"port 8080 or port 2776 or port 12775 or port 5173\" -k > /dev/null 2>&1 &"
            );
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.to(new File("/dev/null")));
            pb.redirectError(ProcessBuilder.Redirect.to(new File("/dev/null")));
            pb.start();

            JsonObject res = new JsonObject();
            res.addProperty("status", "success");
            res.addProperty("message", "Wireshark launched on lo (ports 8080, 2776, 12775, 5173)");
            resp.getWriter().write(gson.toJson(res));
        } catch (Exception e) {
            resp.setStatus(500);
            JsonObject err = new JsonObject();
            err.addProperty("status", "error");
            err.addProperty("message", "Failed to launch Wireshark: " + e.getMessage());
            resp.getWriter().write(gson.toJson(err));
        }
    }

    private static String decodeHexSMPPMessage(String hex) {
        if (hex == null || hex.isEmpty()) return null;
        try {
            String[] parts = hex.split(":");
            byte[] bytes = new byte[parts.length];
            for (int i = 0; i < parts.length; i++) {
                bytes[i] = (byte) Integer.parseInt(parts[i], 16);
            }
            return new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getFirst(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        JsonElement el = obj.get(key);
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            return arr.size() > 0 && !arr.get(0).isJsonNull() ? arr.get(0).getAsString() : null;
        }
        return el.isJsonNull() ? null : el.getAsString();
    }

    private static String smppCommandName(String hex) {
        return switch (hex) {
            case "0x00000001" -> "BIND_RECEIVER";
            case "0x80000001" -> "BIND_RECEIVER_RESP";
            case "0x00000002" -> "BIND_TRANSMITTER";
            case "0x80000002" -> "BIND_TRANSMITTER_RESP";
            case "0x00000009" -> "BIND_TRX";
            case "0x80000009" -> "BIND_TRX_RESP";
            case "0x00000004" -> "SUBMIT_SM";
            case "0x80000004" -> "SUBMIT_SM_RESP";
            case "0x00000005" -> "DELIVER_SM";
            case "0x80000005" -> "DELIVER_SM_RESP";
            case "0x00000015" -> "ENQUIRE_LINK";
            case "0x80000015" -> "ENQUIRE_LINK_RESP";
            case "0x00000021" -> "UNBIND";
            case "0x80000021" -> "UNBIND_RESP";
            default -> hex;
        };
    }

    private void handleDownload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path pcap = PCAP_DIR.resolve(PCAP_FILE);
        if (!Files.exists(pcap)) {
            resp.setStatus(404);
            resp.getWriter().write("{\"status\":\"error\",\"message\":\"No capture file\"}");
            return;
        }
        resp.setContentType("application/vnd.tcpdump.pcap");
        resp.setHeader("Content-Disposition", "attachment; filename=\"smpp_capture.pcap\"");
        Files.copy(pcap, resp.getOutputStream());
    }
}
