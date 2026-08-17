package com.example.bandbridge;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BridgeHttpServer {
    public static final int SERVICE_NOTIFICATION_ID = 8787;
    private static final int MAX_BODY = 4 * 1024 * 1024;
    private static final int MAX_HEADERS = 16 * 1024;
    private final int port;
    private final String token;
    private final Handler handler;
    private volatile boolean running;
    private ServerSocket serverSocket;

    public BridgeHttpServer(int port, String token, Handler handler) {
        this.port = port;
        this.token = token;
        this.handler = handler;
    }

    public void start() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                serverSocket.setSoTimeout(1000);
                while (running) {
                    try { handle(serverSocket.accept()); }
                    catch (SocketTimeoutException ignored) { }
                    catch (Exception ignored) { }
                }
            } catch (IOException ignored) {
                running = false;
            } finally {
                closeServer();
            }
        }, "band-bridge-http").start();
    }

    public void stop() {
        running = false;
        closeServer();
    }

    private void handle(Socket socket) {
        try (Socket client = socket) {
            client.setSoTimeout(5000);
            InputStream input = client.getInputStream();
            byte[] headerBytes = readHeaders(input);
            if (headerBytes == null) return;
            String headerText = new String(headerBytes, StandardCharsets.UTF_8);
            String[] lines = headerText.split("\\r\\n");
            String[] requestLine = lines[0].split(" ");
            if (requestLine.length < 2) { write(client, Response.json(400, "{\"error\":\"bad request\"}")); return; }
            Map<String, String> headers = new HashMap<>();
            for (int index = 1; index < lines.length; index++) {
                int colon = lines[index].indexOf(':');
                if (colon > 0) headers.put(lines[index].substring(0, colon).trim().toLowerCase(Locale.US), lines[index].substring(colon + 1).trim());
            }
            boolean pairingRequest = "POST".equals(requestLine[0]) && "/v1/pair".equals(requestLine[1]);
            if (!pairingRequest && !("Bearer " + token).equals(headers.get("authorization"))) { write(client, Response.json(401, "{\"error\":\"unauthorized\"}")); return; }
            int length = parseLength(headers.get("content-length"));
            if (length > MAX_BODY) { write(client, Response.json(413, "{\"error\":\"request too large\"}")); return; }
            byte[] bodyBytes = readBody(input, length);
            Response response = handler.handle(requestLine[0], requestLine[1], new String(bodyBytes, StandardCharsets.UTF_8));
            write(client, response);
        } catch (Exception ignored) { }
    }

    private byte[] readHeaders(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int matched = 0;
        while (output.size() < MAX_HEADERS) {
            int value = input.read();
            if (value < 0) return null;
            output.write(value);
            if ((matched == 0 && value == '\r') || (matched == 2 && value == '\r')) matched++;
            else if ((matched == 1 && value == '\n') || (matched == 3 && value == '\n')) matched++;
            else matched = value == '\r' ? 1 : 0;
            if (matched == 4) {
                byte[] bytes = output.toByteArray();
                byte[] headers = new byte[bytes.length - 4];
                System.arraycopy(bytes, 0, headers, 0, headers.length);
                return headers;
            }
        }
        return null;
    }

    private int parseLength(String value) {
        try { return value == null ? 0 : Math.max(0, Integer.parseInt(value)); }
        catch (NumberFormatException ignored) { return MAX_BODY + 1; }
    }

    private byte[] readBody(InputStream input, int length) throws IOException {
        byte[] body = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = input.read(body, offset, length - offset);
            if (count < 0) throw new IOException("unexpected end of body");
            offset += count;
        }
        return body;
    }

    private void write(Socket socket, Response response) throws IOException {
        OutputStream output = socket.getOutputStream();
        byte[] body = response.body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + response.status + " "+ response.reason + "\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: " + body.length + "\r\nConnection: close\r\n\r\n";
        output.write(head.getBytes(StandardCharsets.UTF_8));
        output.write(body);
        output.flush();
    }

    private void closeServer() {
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) { }
        serverSocket = null;
    }

    public interface Handler { Response handle(String method, String path, String body); }

    public static class Response {
        final int status;
        final String reason;
        final String body;
        Response(int status, String reason, String body) { this.status = status; this.reason = reason; this.body = body; }
        static Response json(int status, String body) { return new Response(status, status == 200 ? "OK" : status == 202 ? "Accepted" : "Error", body); }
    }
}
