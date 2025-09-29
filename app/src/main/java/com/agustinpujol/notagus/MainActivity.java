package com.agustinpujol.notagus;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agustinpujol.notagus.calendar.CalendarFragment;
import com.agustinpujol.notagus.widget.NotagusWidgetProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_TAB = "selected_tab";
    private static final int REQ_POST_NOTIF = 501;

    private TextView tvFecha;
    private AppCompatImageButton btnSettings;
    private AppCompatImageButton btnAyer, btnManana;
    private LinearLayout emptyStateContainer;
    private RecyclerView recyclerViewTasks;

    private LinearLayout rootColumn;
    private View fragmentHost;
    private FloatingActionButton fab;
    private BottomNavigationView bottomNav;

    private java.util.Calendar selectedDate;

    private boolean pendingSwitchFromCalendar = false;
    private boolean isBooting = true;

    private final List<Task> tasks = new ArrayList<>();
    private TaskAdapter adapter;

    private AppDatabase db;
    private ExecutorService dbExecutor;

    // --- ItemDecoration: espacio solo bajo el último ítem ---
    static final class LastItemBottomOffset extends RecyclerView.ItemDecoration {
        private int bottomPx;
        LastItemBottomOffset(int px) { bottomPx = px; }
        void setBottomPx(int px) { bottomPx = px; }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            RecyclerView.Adapter<?> ad = parent.getAdapter();
            if (ad == null) return;
            int pos = parent.getChildAdapterPosition(view);
            if (pos == RecyclerView.NO_POSITION) return;
            if (pos == ad.getItemCount() - 1) {
                outRect.bottom += bottomPx; // solo al ÚLTIMO
            }
        }
    }

    private void applyEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat wic =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void applyFontThemeFromSettings() {
        SettingsManager.FontFamily ff = SettingsManager.getInstance(this).getFontFamily();
        switch (ff) {
            case A: setTheme(R.style.Theme_Notagus_FontA); break;
            case B: setTheme(R.style.Theme_Notagus_FontB); break;
            case C: setTheme(R.style.Theme_Notagus_FontC); break;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SettingsManager sm = SettingsManager.getInstance(this);
        SettingsManager.ThemeMode themeMode = sm.getThemeMode();
        int nightMode;
        switch (themeMode) {
            case LIGHT: nightMode = AppCompatDelegate.MODE_NIGHT_NO; break;
            case DARK: nightMode = AppCompatDelegate.MODE_NIGHT_YES; break;
            case SYSTEM:
            default: nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);

        applyFontThemeFromSettings();
        setContentView(R.layout.activity_main);
        applyEdgeToEdge();

        NotificationHelper.ensureChannel(this);
        if (SettingsManager.getInstance(this).isNotificationsEnabled()) {
            requestPostNotificationsIfNeeded();
            NotificationScheduler.scheduleDailyTaskCheck(this);
        } else {
            NotificationScheduler.cancelDailyTaskCheck(this);
        }

        ThemePalettes.Colors cols = ThemePalettes.forMode(themeMode);

        rootColumn   = findViewById(R.id.rootColumn);
        fragmentHost = findViewById(R.id.fragmentHost);
        fab          = findViewById(R.id.fabAgregar);
        bottomNav    = findViewById(R.id.bottomNav);
        View header  = findViewById(R.id.headerContainer);
        View content = findViewById(R.id.contentArea);

        if (header  != null) header.setBackgroundColor(cols.header);
        if (content != null) content.setBackgroundColor(cols.content);

        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        int textOnHeader = ColorUtils.calculateLuminance(cols.header) < 0.5 ? 0xFFFFFFFF : 0xFF000000;
        tvFecha     = findViewById(R.id.tvFecha);
        btnAyer     = findViewById(R.id.btnAyer);
        btnManana   = findViewById(R.id.btnManana);
        btnSettings = findViewById(R.id.btnSettings);

        if (tvFecha != null) tvFecha.setTextColor(textOnHeader);
        if (btnAyer   != null) btnAyer.setImageTintList(android.content.res.ColorStateList.valueOf(textOnHeader));
        if (btnManana != null) btnManana.setImageTintList(android.content.res.ColorStateList.valueOf(textOnHeader));
        if (btnSettings != null) {
            btnSettings.setImageTintList(android.content.res.ColorStateList.valueOf(textOnHeader));
            btnSettings.setOnClickListener(v -> {
                SettingsDialog dlg = new SettingsDialog();
                dlg.setSettingsActionListener(() -> {
                    if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_tareas);
                    goToToday();
                    refreshHeaderDate();
                    refreshTasksFor(selectedDate);
                    NotagusWidgetProvider.requestRefresh(getApplicationContext());
                });
                dlg.show(getSupportFragmentManager(), "SettingsDialog");
            });
        }

        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        recyclerViewTasks   = findViewById(R.id.recyclerViewTasks);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));

        TextView emptyTitle    = findViewById(R.id.emptyTitle);
        TextView emptySubtitle = findViewById(R.id.emptySubtitle);
        if (emptyTitle    != null) emptyTitle.setTextColor(cols.textOnContent);
        if (emptySubtitle != null) emptySubtitle.setTextColor(ColorUtils.setAlphaComponent(cols.textOnContent, 160));

        TextView exTitle = findViewById(R.id.emptyExampleTitle);
        TextView exSub   = findViewById(R.id.emptyExampleSubtitle);
        if (exTitle != null) exTitle.setTextColor(0xFF444444);
        if (exSub   != null) exSub.setTextColor(0xFF777777);

        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        android.graphics.drawable.Drawable d = ContextCompat.getDrawable(this, R.drawable.divider);
        if (d != null) divider.setDrawable(d);
        recyclerViewTasks.addItemDecoration(divider);

        // ===== ItemDecoration para espacio del último ítem (sin “saltos”) =====
        final LastItemBottomOffset bottomOffset = new LastItemBottomOffset(dp(56));
        recyclerViewTasks.addItemDecoration(bottomOffset);

        // Recalcular una vez medidos FAB/BNV y ante futuros relayouts
        View.OnLayoutChangeListener recalc = (v, a,b,c,d2, e,f,g,h) -> recomputeBottomSpace(bottomOffset);
        if (bottomNav != null) bottomNav.addOnLayoutChangeListener(recalc);
        if (fab != null)       fab.addOnLayoutChangeListener(recalc);
        recyclerViewTasks.post(() -> recomputeBottomSpace(bottomOffset));

        adapter = new TaskAdapter(
                tasks,
                isEmpty -> {
                    if (isEmpty) {
                        emptyStateContainer.setVisibility(View.VISIBLE);
                        recyclerViewTasks.setVisibility(View.GONE);
                    }
                },
                task -> {
                    if (dbExecutor != null && db != null) {
                        dbExecutor.execute(() -> {
                            db.taskDao().delete(task);
                            NotagusWidgetProvider.requestRefresh(getApplicationContext());
                        });
                    }
                }
        );
        recyclerViewTasks.setAdapter(adapter);
        adapter.attachRecyclerView(recyclerViewTasks);

        // Proveer dayKey al adapter
        adapter.setDayKeyProvider(() -> (selectedDate != null) ? dateToDayKey(selectedDate) : null);

        dbExecutor = Executors.newSingleThreadExecutor();
        db = DatabaseClient.getInstance(getApplicationContext()).getAppDatabase();

        goToToday();
        refreshHeaderDate();
        refreshTasksFor(selectedDate);

        if (fab != null) {
            fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(cols.header));
            int fabIcon = (ColorUtils.calculateLuminance(cols.header) < 0.5) ? 0xFFFFFFFF : 0xFF000000;
            fab.setImageTintList(android.content.res.ColorStateList.valueOf(fabIcon));

            ViewCompat.setOnApplyWindowInsetsListener(fab, (v, insets) -> {
                int nav = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom;
                ViewGroup.LayoutParams base = v.getLayoutParams();
                if (!(base instanceof ViewGroup.MarginLayoutParams)) return insets;
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) base;
                int baseMarginPx = dp(16);
                lp.bottomMargin = baseMarginPx + nav;
                lp.rightMargin  = baseMarginPx;
                v.setLayoutParams(lp);
                return insets;
            });

            fab.setOnClickListener(v -> {
                final Integer dayKey = (selectedDate != null) ? dateToDayKey(selectedDate) : null;
                NewTaskDialog dialog = new NewTaskDialog((title, repeatMode, dk) -> {
                    if (title == null || title.trim().isEmpty()) return;

                    if (emptyStateContainer.getVisibility() == View.VISIBLE) {
                        emptyStateContainer.setVisibility(View.GONE);
                        recyclerViewTasks.setVisibility(View.VISIBLE);
                    }

                    final Task newTask = (repeatMode == Task.REPEAT_ONE_DAY)
                            ? new Task(title, repeatMode, dk)
                            : new Task(title);

                    if (repeatMode == Task.REPEAT_ALWAYS) {
                        newTask.setRepeatMode(Task.REPEAT_ALWAYS);
                        newTask.setDayKey(null);
                    }

                    if (dbExecutor == null || db == null) return;

                    dbExecutor.execute(() -> {
                        long id = db.taskDao().insert(newTask);
                        newTask.setId(id);

                        NotagusWidgetProvider.requestRefresh(getApplicationContext());

                        boolean shouldAppear =
                                newTask.getRepeatMode() == Task.REPEAT_ALWAYS
                                        || (newTask.getRepeatMode() == Task.REPEAT_ONE_DAY
                                        && newTask.getDayKey() != null
                                        && newTask.getDayKey().equals(dateToDayKey(selectedDate)));

                        runOnUiThread(() -> {
                            if (shouldAppear) {
                                adapter.addTaskAndSort(newTask);
                                recyclerViewTasks.scrollToPosition(0);
                                showEmptyStateIfNeeded();
                            }
                        });
                    });
                }, dayKey);
                dialog.show(getSupportFragmentManager(), "NewTaskDialog");
            });
        }

        if (btnAyer != null) {
            btnAyer.setOnClickListener(v -> {
                selectedDate.add(java.util.Calendar.DAY_OF_MONTH, -1);
                refreshHeaderDate();
                refreshTasksFor(selectedDate);
            });
        }
        if (btnManana != null) {
            btnManana.setOnClickListener(v -> {
                selectedDate.add(java.util.Calendar.DAY_OF_MONTH, +1);
                refreshHeaderDate();
                refreshTasksFor(selectedDate);
            });
        }

        getSupportFragmentManager().setFragmentResultListener(
                "calendar_pick",
                this,
                (requestKey, result) -> {
                    long millis = result.getLong("date", -1L);
                    if (millis > 0) {
                        selectedDate = java.util.Calendar.getInstance(new Locale("es", "AR"));
                        selectedDate.setTimeInMillis(millis);
                        refreshHeaderDate();
                        pendingSwitchFromCalendar = true;
                        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_tareas);
                        refreshTasksFor(selectedDate);
                    }
                }
        );

        if (bottomNav != null) {
            bottomNav.setBackgroundColor(cols.header);
            int onFooter = (ColorUtils.calculateLuminance(cols.header) < 0.5) ? 0xFFFFFFFF : 0xFF000000;
            bottomNav.setItemIconTintList(android.content.res.ColorStateList.valueOf(onFooter));
            bottomNav.setItemTextColor(android.content.res.ColorStateList.valueOf(onFooter));
            bottomNav.setItemRippleColor(ContextCompat.getColorStateList(this, R.color.bnv_ripple));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                for (int i = 0; i < bottomNav.getMenu().size(); i++) {
                    bottomNav.getMenu().getItem(i).setContentDescription(null);
                    bottomNav.getMenu().getItem(i).setTooltipText(null);
                }
            }

            View child0 = bottomNav.getChildAt(0);
            if (child0 instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) child0;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View item = group.getChildAt(i);
                    item.setOnLongClickListener(v -> true);
                    item.setLongClickable(false);
                    ViewCompat.setTooltipText(item, null);
                }
            }

            bottomNav.setOnItemSelectedListener(item -> {
                if (isBooting) return true;

                View itemView = bottomNav.findViewById(item.getItemId());
                if (itemView != null) {
                    itemView.animate().cancel();
                    itemView.setScaleX(0.9f);
                    itemView.setScaleY(0.9f);
                    itemView.animate()
                            .scaleX(1.05f).scaleY(1.05f).setDuration(90)
                            .withEndAction(() ->
                                    itemView.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                            .start();
                }

                int id = item.getItemId();
                if (id == R.id.nav_tareas) {
                    if (pendingSwitchFromCalendar) {
                        pendingSwitchFromCalendar = false;
                    } else {
                        goToToday();
                        refreshHeaderDate();
                    }
                    showClassicContent();
                    refreshTasksFor(selectedDate);
                    return true;
                } else if (id == R.id.nav_calendar) {
                    showCalendarFragment();
                    return true;
                } else if (id == R.id.nav_notas) {
                    showNotasFragment();
                    return true;
                }
                return true;
            });

            bottomNav.setOnItemReselectedListener(item -> {
                View itemView = bottomNav.findViewById(item.getItemId());
                if (itemView != null) {
                    itemView.animate().cancel();
                    itemView.animate()
                            .rotation(5f).setDuration(60)
                            .withEndAction(() ->
                                    itemView.animate().rotation(0f).setDuration(60).start())
                            .start();
                }
                if (item.getItemId() == R.id.nav_tareas) {
                    pendingSwitchFromCalendar = false;
                    goToToday();
                    refreshHeaderDate();
                    refreshTasksFor(selectedDate);
                }
            });

            int initialTab;
            if (savedInstanceState != null) {
                initialTab = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_tareas);
            } else {
                int current = bottomNav.getSelectedItemId();
                initialTab = (current != 0) ? current : R.id.nav_tareas;
            }
            isBooting = false;
            bottomNav.setSelectedItemId(initialTab);
        }

        // ===== Back =====
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (adapter != null && adapter.handleBack()) return;
                if (adapter != null && adapter.isEditing()) {
                    adapter.commitActiveEdit();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        tintSystemBars(cols.header, true, true);
        NotagusWidgetProvider.requestRefresh(getApplicationContext());
    }

    private void requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIF);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null && adapter.isEditing()) {
            adapter.commitActiveEdit();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNav != null) {
            outState.putInt(KEY_SELECTED_TAB, bottomNav.getSelectedItemId());
        }
    }

    private void showClassicContent() {
        if (rootColumn   != null) rootColumn.setVisibility(View.VISIBLE);
        if (fragmentHost != null) fragmentHost.setVisibility(View.GONE);
        if (fab          != null) fab.setVisibility(View.VISIBLE);

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentHost, new androidx.fragment.app.Fragment())
                .commitAllowingStateLoss();
    }

    private void showCalendarFragment() {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentHost, new CalendarFragment())
                .commitNowAllowingStateLoss();

        if (fragmentHost != null) fragmentHost.setVisibility(View.VISIBLE);
        if (rootColumn   != null) rootColumn.setVisibility(View.GONE);
        if (fab          != null) fab.setVisibility(View.GONE);
    }

    private void showNotasFragment() {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentHost, new NotasFragment())
                .commitNowAllowingStateLoss();

        if (fragmentHost != null) fragmentHost.setVisibility(View.VISIBLE);
        if (rootColumn   != null) rootColumn.setVisibility(View.GONE);
        if (fab          != null) fab.setVisibility(View.GONE);
    }

    private String getHeaderDateText(Date d) {
        Locale esAR = new Locale("es", "AR");
        SimpleDateFormat sdfDia   = new SimpleDateFormat("EEEE", esAR);
        SimpleDateFormat sdfFecha = new SimpleDateFormat("d 'de' MMMM", esAR);
        String dia   = capitalizeFirst(sdfDia.format(d));
        String fecha = sdfFecha.format(d);
        return dia + "\n" + fecha;
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        int cp = s.codePointAt(0);
        String first = new String(Character.toChars(cp));
        String rest  = s.substring(first.length());
        return first.toUpperCase(new Locale("es", "AR")) + rest;
    }

    private void showEmptyStateIfNeeded() {
        if (tasks.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            recyclerViewTasks.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            recyclerViewTasks.setVisibility(View.VISIBLE);
        }
    }

    private void tintSystemBars(int headerColor, boolean tintStatus, boolean tintNav) {
        Window w = getWindow();
        if (tintStatus) w.setStatusBarColor(headerColor);
        if (tintNav)    w.setNavigationBarColor(headerColor);
        boolean light = ColorUtils.calculateLuminance(headerColor) > 0.5;
        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(w, w.getDecorView());
        wic.setAppearanceLightStatusBars(light);
        wic.setAppearanceLightNavigationBars(light);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void refreshHeaderDate() {
        if (tvFecha != null && selectedDate != null) {
            tvFecha.setText(getHeaderDateText(selectedDate.getTime()));
        }
    }

    private void goToToday() {
        selectedDate = java.util.Calendar.getInstance(new Locale("es", "AR"));
        refreshHeaderDate();
    }

    private static int dateToDayKey(@NonNull java.util.Calendar cal) {
        int y = cal.get(java.util.Calendar.YEAR);
        int m = cal.get(java.util.Calendar.MONTH) + 1;
        int d = cal.get(java.util.Calendar.DAY_OF_MONTH);
        return y * 10000 + m * 100 + d;
    }

    private void refreshTasksFor(@Nullable java.util.Calendar cal) {
        if (cal == null || dbExecutor == null || db == null || adapter == null) return;

        final int dayKey = dateToDayKey(cal);
        final boolean onlyDay = SettingsManager.getInstance(this).isShowOnlyDayEnabled();

        dbExecutor.execute(() -> {
            List<Task> persisted;
            if (onlyDay) {
                persisted = db.taskDao().getOnlyOneDayForDay(dayKey, Task.REPEAT_ONE_DAY);
            } else {
                persisted = db.taskDao().getForDay(dayKey, Task.REPEAT_ONE_DAY, Task.REPEAT_ALWAYS);
            }

            runOnUiThread(() -> {
                adapter.setTasksAndSort(persisted);
                showEmptyStateIfNeeded();
                if (!persisted.isEmpty()) recyclerViewTasks.scrollToPosition(0);
            });
        });
    }

    public void setBottomNavVisible(boolean visible) {
        View bnv = findViewById(R.id.bottomNav);
        if (bnv != null) bnv.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // Recalcula el espacio seguro del último ítem (BNV/FAB + insets + padding base)
    private void recomputeBottomSpace(LastItemBottomOffset deco) {
        int basePad = dp(36);

        int bnvH = (bottomNav != null) ? bottomNav.getHeight() : 0;

        int fabH = (fab != null && fab.getHeight() > 0) ? fab.getHeight() : dp(62);
        int fabRad = fabH / 2;

        int fabMb = dp(16);
        if (fab != null && fab.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            fabMb = ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin;
        }
        int fabOverlap = fabRad + fabMb;

        int extra = Math.max(bnvH, fabOverlap);

        int nav = 0;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(recyclerViewTasks);
        if (insets != null) {
            nav = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom;
        }
        deco.setBottomPx(basePad + nav + extra);
        recyclerViewTasks.invalidateItemDecorations();
    }
}
