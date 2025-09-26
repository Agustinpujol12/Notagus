package com.agustinpujol.notagus.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.agustinpujol.notagus.NotificationScheduler;
import com.agustinpujol.notagus.R;

public class NotagusWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.agustinpujol.notagus.ACTION_REFRESH_WIDGET";

    @Override
    public void onEnabled(Context context) {
        // Primera vez que se añade un widget -> forzar una actualización
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, NotagusWidgetProvider.class));
        for (int id : ids) update(context, mgr, id, null);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            update(context, appWidgetManager, widgetId, null);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager appWidgetManager,
            int appWidgetId,
            Bundle newOptions) {
        update(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (action == null) return;

        if (action.equals(ACTION_REFRESH)
                || action.equals(Intent.ACTION_DATE_CHANGED)
                || action.equals(Intent.ACTION_TIME_CHANGED)
                || action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                || action.equals(Intent.ACTION_BOOT_COMPLETED)
                || action.equals(Intent.ACTION_MY_PACKAGE_REPLACED)
                || action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {

            // ✅ Si fue por BOOT o por actualización del paquete, rearmar el scheduler de medianoche
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)
                    || action.equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
                NotificationScheduler.scheduleDailyTaskCheck(context.getApplicationContext());
            }

            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(context, NotagusWidgetProvider.class));

            // 1) Invalidar la lista para que el host vuelva a pedir los datos
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.listTasks);

            // 2) Reatachar RemoteViewsService con Intent único y refrescar cada widget
            for (int id : ids) {
                update(context, mgr, id, null);
            }
        }
    }

    private void update(Context context, AppWidgetManager mgr, int appWidgetId, Bundle options) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_notagus);

        // Header con fecha (es-AR)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE d 'de' MMMM", new java.util.Locale("es", "AR"));
        String header = sdf.format(System.currentTimeMillis());
        header = header.substring(0, 1).toUpperCase() + header.substring(1);
        views.setTextViewText(R.id.tvHeader, header);

        // Conecta la ListView con el RemoteViewsService con Intent ÚNICO (rompe caché del host)
        Intent svcIntent = new Intent(context, NotagusWidgetService.class);
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        long ts = System.currentTimeMillis();
        android.net.Uri data = android.net.Uri.parse("notagus://widget/" + appWidgetId + "?t=" + ts);
        svcIntent.setData(data);
        views.setRemoteAdapter(R.id.listTasks, svcIntent);
        views.setEmptyView(R.id.listTasks, R.id.empty);

        // Tap en header abre la app
        Intent openApp = new Intent(context, com.agustinpujol.notagus.MainActivity.class);
        PendingIntent piOpen = PendingIntent.getActivity(
                context, 1001, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.tvHeader, piOpen);

        // Botón refrescar -> broadcast ACTION_REFRESH
        Intent refreshIntent = new Intent(context, NotagusWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent piRefresh = PendingIntent.getBroadcast(
                context, 2001, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btnRefresh, piRefresh);

        // Template de click por ítem (abre la app)
        PendingIntent piTemplate = PendingIntent.getActivity(
                context, 1002, new Intent(context, com.agustinpujol.notagus.MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setPendingIntentTemplate(R.id.listTasks, piTemplate);

        // Actualiza el widget
        mgr.updateAppWidget(appWidgetId, views);
        // Refresca dataset por las dudas
        mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.listTasks);
    }

    /** Llamá este helper desde tu app cuando cambien las tareas (insert/update/delete/pin/unpin/done). */
    public static void requestRefresh(Context context) {
        Intent i = new Intent(context, NotagusWidgetProvider.class);
        i.setAction(ACTION_REFRESH);
        context.sendBroadcast(i);
    }
}
