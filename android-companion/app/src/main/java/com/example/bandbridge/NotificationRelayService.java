package com.example.bandbridge;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Locale;

public class NotificationRelayService extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        if (statusBarNotification == null || getPackageName().equals(statusBarNotification.getPackageName())) return;
        Notification notification = statusBarNotification.getNotification();
        if (notification == null || notification.extras == null) return;
        Bundle extras = notification.extras;
        String title = text(extras.getCharSequence(Notification.EXTRA_TITLE));
        String body = text(extras.getCharSequence(Notification.EXTRA_TEXT));
        String packageName = statusBarNotification.getPackageName().toLowerCase(Locale.US);
        String searchable = (packageName + " " + title + " " + body).toLowerCase(Locale.US);
        if (!(searchable.contains("codex") || searchable.contains("chatgpt") || packageName.contains("openai"))) return;
        if (title.length() == 0) title = "Codex";
        if (body.length() == 0) body = "手机收到一条 Codex 通知";
        NotificationPublisher.publish(this, title, body, null, null);
    }

    private String text(CharSequence value) { return value == null ? "" : value.toString().trim(); }
}
