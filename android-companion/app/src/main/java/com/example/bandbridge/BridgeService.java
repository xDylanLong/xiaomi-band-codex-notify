package com.example.bandbridge;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.json.JSONObject;

import java.util.Locale;

public class BridgeService extends Service implements BridgeHttpServer.Handler {
    public static final int PORT = 8787;
    public static final String EXTRA_TOKEN = "token";
    private BridgeHttpServer server;
    private String token;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationPublisher.ensureChannels(this);
        startForeground(BridgeHttpServer.SERVICE_NOTIFICATION_ID, NotificationPublisher.serviceNotification(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        token = intent != null ? intent.getStringExtra(EXTRA_TOKEN) : null;
        if (token == null) token = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE).getString(MainActivity.TOKEN_KEY, "");
        if (server == null) {
            server = new BridgeHttpServer(PORT, token, this);
            server.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (server != null) server.stop();
        server = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public BridgeHttpServer.Response handle(String method, String path, String body) {
        if ("GET".equals(method) && "/v1/health".equals(path)) {
            return BridgeHttpServer.Response.json(200, "{\"ok\":true,\"service\":\"band10pro-bridge\",\"protocol\":1}");
        }
        if (!"POST".equals(method) || !("/v1/notify".equals(path) || "/v1/plan".equals(path))) {
            return BridgeHttpServer.Response.json(404, "{\"error\":\"not found\"}");
        }
        try {
            JSONObject payload = new JSONObject(body);
            String title = trim(payload.optString("title", "Band Bridge"), 120);
            String message = trim(payload.optString("body", ""), 4000);
            String imageBase64 = payload.optString("imageBase64", null);
            String imageMime = payload.optString("imageMime", "image/png");
            if (message.length() == 0 && payload.has("plan")) message = payload.getJSONObject("plan").toString();
            NotificationPublisher.publish(this, title, message, imageBase64, imageMime);
            return BridgeHttpServer.Response.json(202, "{\"ok\":true,\"message\":\"Android notification posted\"}");
        } catch (Exception error) {
            return BridgeHttpServer.Response.json(400, "{\"error\":\"invalid JSON or notification payload\"}");
        }
    }

    private String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
