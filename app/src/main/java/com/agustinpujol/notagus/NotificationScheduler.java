package com.agustinpujol.notagus;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    private static final String TAG_DAILY = "DAILY_TASK_CHECK";
    private static final String TAG_TEST  = "DAILY_TASK_CHECK_TEST";

    /** Programa el DailyTaskWorker para la PRÓXIMA medianoche local. */
    public static void scheduleDailyTaskCheck(@NonNull Context ctx) {
        // Evitamos acumulación de trabajos previos
        WorkManager.getInstance(ctx).cancelAllWorkByTag(TAG_DAILY);

        long delayMs = millisUntilNextMidnight();

        WorkRequest req = new OneTimeWorkRequest.Builder(DailyTaskWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .addTag(TAG_DAILY)
                .build();

        WorkManager.getInstance(ctx).enqueue(req);
    }

    /** Cancela cualquier programación diaria pendiente. */
    public static void cancelDailyTaskCheck(@NonNull Context ctx) {
        WorkManager.getInstance(ctx).cancelAllWorkByTag(TAG_DAILY);
    }

    /** Milisegundos hasta la próxima medianoche (00:00) en la zona local. */
    private static long millisUntilNextMidnight() {
        Calendar now = Calendar.getInstance(new Locale("es", "AR"));
        Calendar next = (Calendar) now.clone();
        next.add(Calendar.DAY_OF_MONTH, 1);
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        long diff = next.getTimeInMillis() - now.getTimeInMillis();
        return Math.max(1L, diff);
    }

    // =========================
    // ==== HELPERS DE TEST ====
    // =========================

    /**
     * Programa el DailyTaskWorker para HOY a la hora/minuto indicados.
     * Si la hora ya pasó, lo mueve a MAÑANA a la misma hora.
     * Útil para pruebas a una hora exacta (p.ej., 15:15).
     */
    public static void scheduleOneShotAt(@NonNull Context ctx, int hour24, int minute) {
        WorkManager.getInstance(ctx).cancelAllWorkByTag(TAG_TEST);

        long now = System.currentTimeMillis();
        Calendar target = Calendar.getInstance(new Locale("es", "AR"));
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        target.set(Calendar.HOUR_OF_DAY, hour24);
        target.set(Calendar.MINUTE, minute);

        long delayMs = target.getTimeInMillis() - now;
        if (delayMs <= 0) {
            target.add(Calendar.DAY_OF_MONTH, 1);
            delayMs = target.getTimeInMillis() - now;
        }

        WorkRequest req = new OneTimeWorkRequest.Builder(DailyTaskWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .addTag(TAG_TEST)
                .build();

        WorkManager.getInstance(ctx).enqueue(req);
    }

    /**
     * Programa el DailyTaskWorker para ejecutarse dentro de N minutos.
     * Útil para pruebas rápidas (p.ej., en 10 minutos).
     */
    public static void scheduleOneShotInMinutes(@NonNull Context ctx, int minutes) {
        WorkManager.getInstance(ctx).cancelAllWorkByTag(TAG_TEST);

        long delayMs = Math.max(1L, minutes * 60_000L);

        WorkRequest req = new OneTimeWorkRequest.Builder(DailyTaskWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .addTag(TAG_TEST)
                .build();

        WorkManager.getInstance(ctx).enqueue(req);
    }
}
