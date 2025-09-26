package com.agustinpujol.notagus.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.agustinpujol.notagus.AppDatabase;
import com.agustinpujol.notagus.DatabaseClient;
import com.agustinpujol.notagus.R;
import com.agustinpujol.notagus.Task;
import com.agustinpujol.notagus.TaskDao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class NotagusWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    static class Factory implements RemoteViewsFactory {

        private static final int MAX_ITEMS = 8; // Cambiá si querés mostrar más/menos
        private final Context context;
        private final List<Task> tasks = new ArrayList<>();

        Factory(Context ctx) {
            this.context = ctx;
        }

        @Override public void onCreate() { /* no-op */ }

        @Override
        public void onDataSetChanged() {
            tasks.clear();
            try {
                AppDatabase db = DatabaseClient.getInstance(context).getAppDatabase();
                TaskDao dao = db.taskDao();

                int todayKey        = buildDayKey(System.currentTimeMillis());
                int REPEAT_ONE_DAY  = Task.REPEAT_ONE_DAY;
                int REPEAT_ALWAYS   = Task.REPEAT_ALWAYS;

                // 1) ÚNICO DÍA de hoy
                List<Task> singlesToday = dao.getOnlyOneDayForDay(todayKey, REPEAT_ONE_DAY);

                // 2) SIEMPRE + PINNED
                List<Task> pinnedAlways = dao.getPinnedAlways(REPEAT_ALWAYS);

                // 3) Merge evitando duplicados (clave = id en orden de inserción)
                LinkedHashMap<Long, Task> map = new LinkedHashMap<>();
                // Queremos que los pinned aparezcan primero si están en ambas listas
                for (Task t : pinnedAlways) map.put(t.getId(), t);
                for (Task t : singlesToday) map.put(t.getId(), t);

                // 4) Pasar a lista y ordenar como en la app: pinned DESC, id ASC
                List<Task> merged = new ArrayList<>(map.values());
                merged.sort((a, b) -> {
                    int p = Boolean.compare(b.isPinned(), a.isPinned()); // pinned primero
                    if (p != 0) return p;
                    return Long.compare(a.getId(), b.getId());           // id ASC
                });

                // 5) Limitar cantidad mostrada
                int max = Math.min(merged.size(), MAX_ITEMS);
                tasks.addAll(merged.subList(0, max));

            } catch (Throwable t) {
                // Si la DB aún no está lista, evitamos crashear
                tasks.clear();
            }
        }

        @Override public void onDestroy() { tasks.clear(); }
        @Override public int getCount() { return tasks.size(); }

        @Override
        public RemoteViews getViewAt(int position) {
            Task task = tasks.get(position);

            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_notagus_item);
            row.setTextViewText(R.id.tvTitle, task.getTitle());

            // Mostrar pin si corresponde (si no usás pin, podés quitar estas 2 líneas)
            row.setViewVisibility(R.id.ivPinned, task.isPinned() ? android.view.View.VISIBLE : android.view.View.GONE);

            // Click del ítem (usa el PendingIntentTemplate del Provider)
            Intent fillIn = new Intent();
            // Si querés pasar el id:
            // fillIn.putExtra("task_id", task.getId());
            row.setOnClickFillInIntent(R.id.tvTitle, fillIn);
            // Si querés que toda la fila sea clickeable, podés repetir para R.id.dot:
            // row.setOnClickFillInIntent(R.id.dot, fillIn);

            return row;
        }

        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public long getItemId(int position) { return tasks.get(position).getId(); }
        @Override public boolean hasStableIds() { return true; }

        private static int buildDayKey(long timeMillis) {
            Calendar cal = Calendar.getInstance(new Locale("es", "AR"));
            cal.setTimeInMillis(timeMillis);
            int y = cal.get(Calendar.YEAR);
            int m = cal.get(Calendar.MONTH) + 1;
            int d = cal.get(Calendar.DAY_OF_MONTH);
            return y * 10000 + m * 100 + d; // AAAAMMDD
        }
    }
}
