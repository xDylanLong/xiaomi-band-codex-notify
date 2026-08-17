package com.example.bandbridge;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    static final String PREFS = "bridge_prefs";
    static final String TOKEN_KEY = "token";
    private TextView status;
    private TextView pairingCodeView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPairingCode();
        buildUi();
        startBridge();
        requestNotificationPermissionIfNeeded();
        setStatus("bridge 已自动启动：电脑和手机连接同一 Wi-Fi 即可");
    }

    private void buildUi() {
        int padding = dp(20);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding, padding, padding);

        ImageView logo = new ImageView(this);
        int logoResource = getResources().getIdentifier("logo", "drawable", getPackageName());
        logo.setImageResource(logoResource);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        content.addView(logo, new LinearLayout.LayoutParams(-1, dp(96)));

        content.addView(text("小米手环Codex通知", 24, true), wrap());
        TextView subtitle = text("Codex 完成任务 → 手机通知 → Mi Fitness → 手环", 14, false);
        subtitle.setPadding(0, dp(7), 0, dp(18));
        content.addView(subtitle, wrap());

        status = text("正在启动 bridge…", 13, false);
        status.setPadding(0, 0, 0, dp(14));
        content.addView(status, wrap());

        content.addView(text("Codex 匹配码", 13, true), wrap());
        pairingCodeView = text(getPairingCode(), 34, true);
        pairingCodeView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        pairingCodeView.setGravity(Gravity.CENTER);
        pairingCodeView.setLetterSpacing(0.18f);
        pairingCodeView.setPadding(0, dp(7), 0, dp(3));
        content.addView(pairingCodeView, wrap());
        content.addView(text("在 Codex 中直接输入这 4 个数字，例如：连接我的小米手环，匹配码 4821", 13, false), wrap());
        content.addView(buttonWithAction("刷新匹配码", view -> refreshPairingCode()), wrap());

        content.addView(text("手机端", 13, true), wrap());
        content.addView(buttonWithAction("发送测试通知", view -> sendTestNotification()), wrap());
        content.addView(buttonWithAction("打开本 App 通知设置", view -> openNotificationSettings()), wrap());

        TextView addresses = text("LAN 地址\n" + joinAddresses(), 12, false);
        addresses.setTypeface(Typeface.MONOSPACE);
        addresses.setPadding(0, dp(17), 0, dp(5));
        content.addView(addresses, wrap());
        content.addView(text("bridge 会在打开 App 时自动启动；Android 的通知监听属于高级选项，默认关闭。", 12, false), wrap());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
    }

    private void sendTestNotification() {
        final String host = firstHost();
        final String token = getToken();
        if (host.length() == 0) {
            setStatus("没有找到局域网地址，请先连接 Wi-Fi");
            return;
        }
        setStatus("正在发送测试通知…");
        new Thread(() -> {
            int code = 0;
            try {
                URL url = new URL("http://" + host + ":" + BridgeService.PORT + "/v1/notify");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2500);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Content-Type", "application/json");
                byte[] body = "{\"type\":\"notify\",\"source\":\"android-app\",\"title\":\"小米手环Codex通知\",\"body\":\"手机连接测试成功\"}".getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream output = connection.getOutputStream()) { output.write(body); }
                code = connection.getResponseCode();
                connection.disconnect();
            } catch (Exception ignored) { }
            final int result = code;
            runOnUiThread(() -> setStatus(result == 202 ? "测试通知已发送，请检查手环" : "测试失败，请确认 bridge 已运行"));
        }, "band-test-notification").start();
    }

    private void startBridge() {
        Intent intent = new Intent(this, BridgeService.class);
        intent.putExtra(BridgeService.EXTRA_TOKEN, getToken());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
    }

    private String getToken() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String token = prefs.getString(TOKEN_KEY, null);
        if (token == null) {
            token = UUID.randomUUID().toString().replace("-", "");
            prefs.edit().putString(TOKEN_KEY, token).apply();
        }
        return token;
    }

    private String getPairingCode() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String code = prefs.getString(BridgeService.PAIRING_CODE_KEY, null);
        if (code == null || !code.matches("\\d{4}")) {
            code = newPairingCode();
            prefs.edit().putString(BridgeService.PAIRING_CODE_KEY, code).apply();
        }
        return code;
    }

    private String newPairingCode() {
        return String.format(Locale.US, "%04d", new SecureRandom().nextInt(10000));
    }

    private void refreshPairingCode() {
        String code = newPairingCode();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(BridgeService.PAIRING_CODE_KEY, code)
                .apply();
        if (pairingCodeView != null) pairingCodeView.setText(code);
        startBridge();
        setStatus("匹配码已刷新，请在 Codex 中输入新的 4 位数字");
    }

    private String firstHost() {
        List<String> hosts = new ArrayList<>();
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (java.net.InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) hosts.add(address.getHostAddress());
                }
            }
        } catch (Exception ignored) { }
        for (String host : hosts) if (host.startsWith("192.168.")) return host;
        return hosts.isEmpty() ? "" : hosts.get(0);
    }

    private String joinAddresses() {
        String host = firstHost();
        return host.length() == 0 ? "连接 Wi-Fi 后重新打开 App" : "http://" + host + ":" + BridgeService.PORT;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button buttonWithAction(String label, android.view.View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setGravity(Gravity.CENTER);
        button.setOnClickListener(action);
        return button;
    }

    private LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void setStatus(String value) { if (status != null) status.setText(value); }
}
