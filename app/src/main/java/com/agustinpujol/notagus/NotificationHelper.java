package com.agustinpujol.notagus;

import android.Manifest;
import android.annotation.SuppressLint;          // ⬅️
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationHelper {

    public static final String CHANNEL_ID_DAILY = "daily_tasks";
    private static final String CHANNEL_NAME = "Recordatorios diarios";
    private static final String CHANNEL_DESC  = "Notificaciones de tareas únicas del día";

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID_DAILY) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID_DAILY, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
                );
                ch.setDescription(CHANNEL_DESC);
                nm.createNotificationChannel(ch);
            }
        }
    }

    @SuppressLint("MissingPermission")               // ⬅️ silencia el warning del notify()
    public static void showDailyTasksNotification(Context ctx, int count, String firstTitle) {
        ensureChannel(ctx);

        // Android 13+ requiere permiso en runtime
        if (Build.VERSION.SDK_INT >= 33) {
            int granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS);
            if (granted != PackageManager.PERMISSION_GRANTED) {
                return; // sin permiso, no notificamos
            }
        }

        String title = (count == 1) ? "1 tarea para hoy" : count + " tareas para hoy";
        String text  = (firstTitle != null && !firstTitle.trim().isEmpty())
                ? "• " + firstTitle
                : "Toca para verlas";

        Notification n = new NotificationCompat.Builder(ctx, CHANNEL_ID_DAILY)
                .setSmallIcon(R.drawable.ic_notification) // o R.mipmap.ic_launcher
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat.from(ctx).notify(1001, n);
    }
}
