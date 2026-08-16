package com.example.bandbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;

public final class NotificationPublisher {
    private static final String SERVICE_CHANNEL = "band_bridge_service";
    private static final String MESSAGE_CHANNEL = "band_bridge_messages";
    private static int nextId = 100;

    private NotificationPublisher() { }

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(SERVICE_CHANNEL, "Band bridge service", NotificationManager.IMPORTANCE_LOW));
        NotificationChannel messages = new NotificationChannel(MESSAGE_CHANNEL, "Band messages", NotificationManager.IMPORTANCE_HIGH);
        messages.setDescription("Messages forwarded to Xiaomi Smart Band 10 Pro through Mi Fitness");
        manager.createNotificationChannel(messages);
    }

    public static Notification serviceNotification(Context context) {
        ensureChannels(context);
        return new Notification.Builder(context, SERVICE_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Dylan小米手环")
                .setContentText("LAN bridge running on port " + BridgeService.PORT)
                .setOngoing(true)
                .build();
    }

    public static void publish(Context context, String title, String body, String imageBase64, String imageMime) {
        ensureChannels(context);
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(context, MESSAGE_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pending)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true);
        if (imageBase64 != null && imageBase64.length() > 0) {
            try {
                byte[] bytes = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) builder.setStyle(new Notification.BigPictureStyle().bigPicture(bitmap).setSummaryText(body));
            } catch (Exception ignored) { }
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(nextId++, builder.build());
    }
}
