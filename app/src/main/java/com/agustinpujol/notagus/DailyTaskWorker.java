package com.agustinpujol.notagus;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.agustinpujol.notagus.widget.NotagusWidgetProvider;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DailyTaskWorker extends Worker {

    public DailyTaskWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        // ✅ Siempre refrescamos el widget a medianoche, tenga o no notificaciones activadas
        try {
            NotagusWidgetProvider.requestRefresh(ctx);
        } catch (Throwable t) {
            // no interrumpir el flujo si falla el refresh
        }

        // Si el usuario desactivó notificaciones, no mostramos nada pero sí reprogramamos
        if (!SettingsManager.getInstance(ctx).isNotificationsEnabled()) {
            NotificationScheduler.scheduleDailyTaskCheck(ctx); // próxima medianoche
            return Result.success();
        }

        // ==== Lógica de notificación diaria ====
        // dayKey HOY (AAAAMMDD)
        Calendar cal = Calendar.getInstance(new Locale("es", "AR"));
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH) + 1;
        int d = cal.get(Calendar.DAY_OF_MONTH);
        int dayKey = y * 10000 + m * 100 + d;

        AppDatabase db = DatabaseClient.getInstance(ctx).getAppDatabase();
        List<Task> todaySingles = db.taskDao().getOnlyOneDayForDay(dayKey, Task.REPEAT_ONE_DAY);

        if (todaySingles != null && !todaySingles.isEmpty()) {
            String first = todaySingles.get(0).getTitle();
            NotificationHelper.showDailyTasksNotification(ctx, todaySingles.size(), first);
        }

        // 🔁 Reprogramar el siguiente disparo (próxima medianoche)
        NotificationScheduler.scheduleDailyTaskCheck(ctx);

        return Result.success();
    }
}
