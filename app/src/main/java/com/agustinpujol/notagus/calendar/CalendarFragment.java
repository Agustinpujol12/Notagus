package com.agustinpujol.notagus.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agustinpujol.notagus.AppDatabase;
import com.agustinpujol.notagus.DatabaseClient;
import com.agustinpujol.notagus.R;
import com.agustinpujol.notagus.SettingsDialog;
import com.agustinpujol.notagus.SettingsManager;
import com.agustinpujol.notagus.Task;
import com.agustinpujol.notagus.TaskDao;
import com.agustinpujol.notagus.ThemePalettes;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarFragment extends Fragment {

    private View headerContainer;
    private View calendarContent;
    private TextView tvFecha, tvMonthTitle;
    private AppCompatImageButton btnAyer, btnManana, btnPrevMonth, btnNextMonth, btnSettings;

    private RecyclerView rvCalendar;
    private CalendarAdapter adapter;

    // Estado de calendario (grilla de mes)
    private int currentYear;
    private int currentMonth;

    // Estado del header (día mostrado arriba)
    private final Calendar headerDate = Calendar.getInstance();

    // DB utils para marcar días con tareas ÚNICO DÍA
    private AppDatabase db;
    private ExecutorService dbExec;

    public CalendarFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_calendar, container, false);

        // === Theming ===
        SettingsManager sm = SettingsManager.getInstance(requireContext());
        ThemePalettes.Colors cols = ThemePalettes.forMode(sm.getThemeMode());

        View header  = root.findViewById(R.id.headerContainer);
        calendarContent = root.findViewById(R.id.calendarContent);

        if (header != null)  header.setBackgroundColor(cols.header);
        if (calendarContent != null) ((View) calendarContent).setBackgroundColor(cols.content);
        root.setBackgroundColor(cols.content);

        int onHeader = (ColorUtils.calculateLuminance(cols.header) < 0.5) ? 0xFFFFFFFF : 0xFF000000;

        tvFecha     = root.findViewById(R.id.tvFecha);
        btnAyer     = root.findViewById(R.id.btnAyer);
        btnManana   = root.findViewById(R.id.btnManana);
        btnSettings = root.findViewById(R.id.btnSettings);

        if (tvFecha != null) tvFecha.setTextColor(onHeader);
        if (btnAyer != null)     btnAyer.setImageTintList(android.content.res.ColorStateList.valueOf(onHeader));
        if (btnManana != null)   btnManana.setImageTintList(android.content.res.ColorStateList.valueOf(onHeader));
        if (btnSettings != null) btnSettings.setImageTintList(android.content.res.ColorStateList.valueOf(onHeader));

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        headerContainer = root.findViewById(R.id.headerContainer);
        tvMonthTitle    = root.findViewById(R.id.tvMonthTitle);
        rvCalendar      = root.findViewById(R.id.rvCalendar);

        btnPrevMonth = root.findViewById(R.id.btnPrevMonth);
        btnNextMonth = root.findViewById(R.id.btnNextMonth);

        // >>> Aplicar color de texto/tint del área de contenido según paleta
        ThemePalettes.Colors cols = ThemePalettes.forMode(
                SettingsManager.getInstance(requireContext()).getThemeMode()
        );
        int onContent = cols.textOnContent;

        if (tvMonthTitle != null) tvMonthTitle.setTextColor(onContent);
        if (btnPrevMonth != null) btnPrevMonth.setImageTintList(android.content.res.ColorStateList.valueOf(onContent));
        if (btnNextMonth != null) btnNextMonth.setImageTintList(android.content.res.ColorStateList.valueOf(onContent));

        // Si agregás id al encabezado de semana (ver XML abajo), teñimos cada TextView
        View weekRow = root.findViewById(R.id.weekHeaderRow);
        if (weekRow instanceof LinearLayout) {
            LinearLayout row = (LinearLayout) weekRow;
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(onContent);
                }
            }
        }

        // DB init
        db = DatabaseClient.getInstance(requireContext()).getAppDatabase();
        dbExec = Executors.newSingleThreadExecutor();

        // Texto inicial del header
        if (tvFecha != null) tvFecha.setText(getHeaderDateText(headerDate.getTime()));

        // Settings dialog
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v ->
                    new SettingsDialog().show(getParentFragmentManager(), "SettingsDialog"));
        }

        // Insets para header y lista
        if (headerContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(headerContainer, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
        if (rvCalendar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rvCalendar, (v, insets) -> {
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                        Math.max(v.getPaddingBottom(), bottomInset + dp(8)));
                return insets;
            });
        }

        // Recycler (grilla 7x6)
        rvCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        adapter = new CalendarAdapter(requireContext());
        rvCalendar.setAdapter(adapter);

        // Click en día: devolver selección
        adapter.setOnDayClick(day -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR,  day.year);
            cal.set(Calendar.MONTH, day.month);
            cal.set(Calendar.DAY_OF_MONTH, day.dayOfMonth);
            cal.set(Calendar.HOUR_OF_DAY, 12);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            Bundle b = new Bundle();
            b.putLong("date", cal.getTimeInMillis());
            getParentFragmentManager().setFragmentResult("calendar_pick", b);
        });

        // Mes inicial = mes actual
        Calendar now = Calendar.getInstance();
        currentYear  = now.get(Calendar.YEAR);
        currentMonth = now.get(Calendar.MONTH);
        renderMonth(); // 🔥 también marca puntitos

        // Navegación de mes (barra del mes)
        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> {
                currentMonth--;
                if (currentMonth < 0) { currentMonth = 11; currentYear--; }
                renderMonth();
            });
        }
        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                currentMonth++;
                if (currentMonth > 11) { currentMonth = 0; currentYear++; }
                renderMonth();
            });
        }

        // Navegación de día (header)
        AppCompatImageButton btnAyer = root.findViewById(R.id.btnAyer);
        AppCompatImageButton btnManana = root.findViewById(R.id.btnManana);
        if (btnAyer != null) {
            btnAyer.setOnClickListener(v -> {
                headerDate.add(Calendar.DAY_OF_MONTH, -1);
                if (tvFecha != null) tvFecha.setText(getHeaderDateText(headerDate.getTime()));
            });
        }
        if (btnManana != null) {
            btnManana.setOnClickListener(v -> {
                headerDate.add(Calendar.DAY_OF_MONTH, 1);
                if (tvFecha != null) tvFecha.setText(getHeaderDateText(headerDate.getTime()));
            });
        }
    }

    /** Renderiza el mes en el adapter y marca días con tareas ÚNICO DÍA. */
    private void renderMonth() {
        final int FIRST_DAY_OF_WEEK = Calendar.MONDAY;

        adapter.setMonth(currentYear, currentMonth, FIRST_DAY_OF_WEEK);

        Calendar c = Calendar.getInstance();
        c.set(currentYear, currentMonth, 1);

        if (tvMonthTitle != null) {
            tvMonthTitle.setText(getMonthTitle(c.getTime()));
        }

        // === Rango de 42 celdas visibles (incluye arrastres) ===
        int dow = c.get(Calendar.DAY_OF_WEEK); // 1=Dom ... 7=Sab
        int offset = ((dow - FIRST_DAY_OF_WEEK) + 7) % 7;

        Calendar gridStart = (Calendar) c.clone(); // 1° del mes
        gridStart.add(Calendar.DAY_OF_MONTH, -offset);

        Calendar gridEnd = (Calendar) gridStart.clone();
        gridEnd.add(Calendar.DAY_OF_MONTH, 41); // 6 filas * 7 días - 1

        int startKey = toDayKey(gridStart.get(Calendar.YEAR),
                gridStart.get(Calendar.MONTH),
                gridStart.get(Calendar.DAY_OF_MONTH));
        int endKey   = toDayKey(gridEnd.get(Calendar.YEAR),
                gridEnd.get(Calendar.MONTH),
                gridEnd.get(Calendar.DAY_OF_MONTH));

        dbExec.execute(() -> {
            TaskDao dao = db.taskDao();
            List<Integer> oneDayKeys = dao.getOneDayKeysInRange(startKey, endKey, Task.REPEAT_ONE_DAY);
            List<Integer> completedAlways = dao.getCompletedAlwaysKeysInRange(startKey, endKey, Task.REPEAT_ALWAYS);

            Set<Integer> setOneDay = new HashSet<>(oneDayKeys);
            Set<Integer> setCompleted = new HashSet<>(completedAlways);

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setMarkedDayKeys(setOneDay);
                    adapter.setCompletedAlwaysKeys(setCompleted);
                });
            }
        });
    }

    // ---- Helpers ----
    private String getHeaderDateText(Date d) {
        Locale esAR = new Locale("es", "AR");
        SimpleDateFormat sdfDia   = new SimpleDateFormat("EEEE", esAR);
        SimpleDateFormat sdfFecha = new SimpleDateFormat("d 'de' MMMM", esAR);
        String dia   = capitalizeFirst(sdfDia.format(d));
        String fecha = sdfFecha.format(d);
        return dia + "\n" + fecha;
    }

    private String getMonthTitle(Date d) {
        Locale esAR = new Locale("es", "AR");
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", esAR);
        return capitalizeFirst(sdf.format(d));
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        int cp = s.codePointAt(0);
        String first = new String(Character.toChars(cp));
        String rest  = s.substring(first.length());
        return first.toUpperCase(new Locale("es", "AR")) + rest;
    }

    private int dp(int v) {
        return (int) (v * requireContext().getResources().getDisplayMetrics().density);
    }

    private static int toDayKey(int year, int monthZero, int day) {
        int m = monthZero + 1; // 1..12
        return year * 10000 + m * 100 + day;
    }
}
