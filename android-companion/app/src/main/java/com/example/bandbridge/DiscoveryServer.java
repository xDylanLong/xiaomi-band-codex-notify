package com.example.bandbridge;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Small LAN-only pairing endpoint. It never answers a discovery request without the 4-digit code. */
public class DiscoveryServer {
    public static final int PORT = 8788;
    private static final int MAX_PACKET = 2048;
    private static final long WINDOW_MS = 30_000L;
    private static final int MAX_RESPONSES_PER_WINDOW = 5;

    private final int port;
    private final String pairingCode;
    private volatile boolean running;
    private DatagramSocket socket;
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public DiscoveryServer(int port, String pairingCode) {
        this.port = port;
        this.pairingCode = pairingCode;
    }

    public void start() {
        running = true;
        new Thread(() -> {
            try {
                socket = new DatagramSocket(port);
                socket.setBroadcast(true);
                socket.setSoTimeout(1000);
                byte[] buffer = new byte[MAX_PACKET];
                while (running) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        respondIfPaired(packet);
                    } catch (java.net.SocketTimeoutException ignored) {
                        // Check running periodically so stop() is responsive.
                    } catch (Exception ignored) {
                        if (running) continue;
                    }
                }
            } catch (Exception ignored) {
                // HTTP bridge remains usable even if the optional discovery port is unavailable.
            } finally {
                closeSocket();
            }
        }, "band-bridge-discovery").start();
    }

    public void stop() {
        running = false;
        closeSocket();
        attempts.clear();
    }

    private void respondIfPaired(DatagramPacket packet) {
        try {
            JSONObject request = new JSONObject(new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8));
            if (!"discover".equals(request.optString("type")) || !pairingCode.equals(request.optString("code"))) return;
            if (!allowed(packet.getAddress().getHostAddress())) return;

            JSONObject response = new JSONObject()
                    .put("ok", true)
                    .put("service", "xiaomi-band-codex-notify")
                    .put("host", localHost())
                    .put("port", BridgeService.PORT);
            byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
            DatagramPacket reply = new DatagramPacket(bytes, bytes.length, packet.getAddress(), packet.getPort());
            if (socket != null) socket.send(reply);
        } catch (Exception ignored) {
            // Ignore malformed or oversized LAN discovery packets.
        }
    }

    private boolean allowed(String host) {
        long now = System.currentTimeMillis();
        AttemptWindow window = attempts.computeIfAbsent(host, ignored -> new AttemptWindow(now));
        synchronized (window) {
            if (now - window.startedAt >= WINDOW_MS) {
                window.startedAt = now;
                window.responses = 0;
            }
            if (window.responses >= MAX_RESPONSES_PER_WINDOW) return false;
            window.responses++;
            return true;
        }
    }

    private String localHost() {
        try {
            String first = "";
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (!(address instanceof Inet4Address) || address.isLoopbackAddress()) continue;
                    String host = address.getHostAddress();
                    if (first.length() == 0) first = host;
                    if (host.startsWith("192.168.")) return host;
                }
            }
            return first;
        } catch (Exception ignored) {
            return "";
        }
    }

    private void closeSocket() {
        try { if (socket != null) socket.close(); }
        catch (Exception ignored) { }
        socket = null;
    }

    private static class AttemptWindow {
        long startedAt;
        int responses;
        AttemptWindow(long startedAt) { this.startedAt = startedAt; }
    }
}
