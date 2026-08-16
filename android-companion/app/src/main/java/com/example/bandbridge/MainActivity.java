package com.example.bandbridge;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
    private static final String SETUP_COMPLETE_KEY = "setup_complete";
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

        ImageView logo = new ImageView(this);
        int logoResource = getResources().getIdentifier("logo", "drawable", getPackageName());
        logo.setImageResource(logoResource);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        content.addView(logo, new LinearLayout.LayoutParams(-1, dp(120)));

        TextView title = text("小米手环Codex通知", 24, true);
        content.addView(title, wrap());
        TextView subtitle = text("Codex 完成任务 → 手机通知 → Mi Fitness → 手环", 14, false);
        subtitle.setPadding(0, dp(8), 0, dp(22));
        content.addView(subtitle, wrap());

        boolean setupComplete = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(SETUP_COMPLETE_KEY, false);
        if (!setupComplete) {
            content.addView(text("三步开始", 16, true), wrap());
            content.addView(text("允许通知、可选开启手机通知监听，然后启动局域网 bridge。Codex 电脑端再安装一次 Stop hook。", 13, false), wrap());
            content.addView(buttonWithAction("1 允许本 App 通知", view -> openNotificationSettings()), wrap());
            content.addView(buttonWithAction("2 开启手机通知监听（可选）", view -> openNotificationListenerSettings()), wrap());
            content.addView(buttonWithAction("3 启动 LAN bridge", view -> {
                startBridge();
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(SETUP_COMPLETE_KEY, true).apply();
                setStatus("bridge 已启动：端口 " + BridgeService.PORT);
            }), wrap());
            content.addView(text("电脑端安装命令:\n" + desktopCommand(), 12, false), wrap());
            content.addView(buttonWithAction("复制电脑端安装命令", view -> copy("Codex hook command", desktopCommand())), wrap());
        }

        content.addView(text("LAN 地址", 13, true), wrap());
        TextView addresses = text(joinAddresses(), 14, false);
        addresses.setTypeface(Typeface.MONOSPACE);
        addresses.setPadding(0, dp(7), 0, dp(16));
        content.addView(addresses, wrap());

        content.addView(text("Bearer token", 13, true), wrap());
        tokenView = text(getToken(), 13, false);
        tokenView.setTypeface(Typeface.MONOSPACE);
        tokenView.setTextIsSelectable(true);
        tokenView.setPadding(0, dp(7), 0, dp(10));
        content.addView(tokenView, wrap());

        content.addView(buttonWithAction("复制 token", view -> copy("Band bridge token", getToken())), wrap());
        content.addView(buttonWithAction("启动 LAN bridge", view -> {
            startBridge();
            setStatus("bridge 已启动：端口 " + BridgeService.PORT);
        }), wrap());
        content.addView(buttonWithAction("停止 LAN bridge", view -> {
            stopService(new Intent(this, BridgeService.class));
            setStatus("bridge 已停止");
        }), wrap());
        content.addView(buttonWithAction("打开本 App 通知设置", view -> openNotificationSettings()), wrap());
        content.addView(buttonWithAction("打开手机通知监听设置", view -> openNotificationListenerSettings()), wrap());

        status = text("局域网模式：电脑和手机需要连接同一个 Wi-Fi。", 13, false);
        status.setPadding(0, dp(22), 0, dp(10));
        content.addView(status, wrap());

        TextView command = text("手动测试:\nnode bridge/bandctl.mjs notify --host <手机IP> --token <token> --title Codex --body '任务完成'", 12, false);
        command.setTypeface(Typeface.MONOSPACE);
        command.setPadding(0, dp(10), 0, dp(10));
        content.addView(command, wrap());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
    }

    private String desktopCommand() {
        String host = joinAddresses().split("\\n")[0].replace("http://", "").replace(":" + BridgeService.PORT, "");
        return "node codex/install-hook.mjs --host " + host + " --token " + getToken();
    }

    private void copy(String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
        setStatus("已复制到剪贴板");
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

    private void openNotificationListenerSettings() {
        try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
        catch (Exception ignored) { setStatus("系统不支持通知监听设置，请在系统设置中搜索“通知使用权”"); }
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

    private Button buttonWithAction(String label, View.OnClickListener action) {
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
