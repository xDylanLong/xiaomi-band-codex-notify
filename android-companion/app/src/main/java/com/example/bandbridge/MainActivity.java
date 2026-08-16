package com.example.bandbridge;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {
    static final String PREFS = "bridge_prefs";
    static final String TOKEN_KEY = "token";
    private TextView status;
    private TextView tokenView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        requestNotificationPermissionIfNeeded();
    }

    private void buildUi() {
        int padding = dp(20);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding, padding, padding);

        TextView title = text("Dylan小米手环", 24, true);
        content.addView(title, wrap());
        TextView subtitle = text("电脑 / Codex → Android 通知 → Mi Fitness → 手环", 14, false);
        subtitle.setPadding(0, dp(8), 0, dp(22));
        content.addView(subtitle, wrap());

        content.addView(text("LAN 地址", 13, true), wrap());
        TextView addresses = text(joinAddresses(), 14, false);
        addresses.setTypeface(Typeface.MONOSPACE);
        addresses.setPadding(0, dp(7), 0, dp(16));
        content.addView(addresses, wrap());

        content.addView(text("Bearer token", 13, true), wrap());
        tokenView = text(getToken(), 13, false);
        tokenView.setTypeface(Typeface.MONOSPACE);
        tokenView.setTextIsSelectable(true);
        tokenView.setPadding(0, dp(7), 0, dp(16));
        content.addView(tokenView, wrap());

        Button copy = button("复制 token");
        copy.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Band bridge token", getToken()));
            setStatus("token 已复制");
        });
        content.addView(copy, wrap());

        Button start = button("启动 LAN bridge");
        start.setOnClickListener(view -> {
            startBridge();
            setStatus("bridge 已启动：端口 " + BridgeService.PORT);
        });
        content.addView(start, wrap());

        Button stop = button("停止 LAN bridge");
        stop.setOnClickListener(view -> {
            stopService(new Intent(this, BridgeService.class));
            setStatus("bridge 已停止");
        });
        content.addView(stop, wrap());

        Button permissions = button("打开本 App 通知设置");
        permissions.setOnClickListener(view -> openNotificationSettings());
        content.addView(permissions, wrap());

        status = text("先在 Mi Fitness 中允许本 App 的通知，再启动 bridge。", 13, false);
        status.setPadding(0, dp(22), 0, dp(10));
        content.addView(status, wrap());

        TextView command = text("电脑端示例:\nnode bridge/bandctl.mjs notify --host <手机IP> --token <token> --title Codex --body '任务完成'", 12, false);
        command.setTypeface(Typeface.MONOSPACE);
        command.setPadding(0, dp(10), 0, dp(10));
        content.addView(command, wrap());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
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

    private String joinAddresses() {
        List<String> values = new ArrayList<>();
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (java.net.InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) values.add("http://" + address.getHostAddress() + ":" + BridgeService.PORT);
                }
            }
        } catch (Exception ignored) { }
        return values.isEmpty() ? "连接 Wi-Fi 后重新打开页面" : String.join("\n", values);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setGravity(Gravity.CENTER);
        return button;
    }

    private LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void setStatus(String value) { if (status != null) status.setText(value); }
}
