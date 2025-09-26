package com.agustinpujol.notagus.calendar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.agustinpujol.notagus.R;
import com.agustinpujol.notagus.SettingsManager;
import com.agustinpujol.notagus.ThemePalettes;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.VH> {

    public interface OnDayClick { void onDayClick(CalendarDay day); }

    private final List<CalendarDay> data = new ArrayList<>();
    private OnDayClick listener;

    // Paleta
    private int headerColor;
    private int onHeaderText;
    private int onSurface;
    private int onSurfaceFaded;

    // 🔴 Días con ÚNICO DÍA
    private final Set<Integer> markedDayKeys = new HashSet<>();
    // ✅ Días en que se completaron PERMANENTES
    private final Set<Integer> completedAlwaysKeys = new HashSet<>();

    // Estado del mes actual que renderiza el adapter
    private int currentYear;
    private int currentMonthZero;

    // ➕ Fuente activa (CardFont A/B/C)
    private final SettingsManager.FontFamily fontFamily;

    public CalendarAdapter(@NonNull Context ctx) {
        SettingsManager sm = SettingsManager.getInstance(ctx);
        ThemePalettes.Colors cols = ThemePalettes.forMode(sm.getThemeMode());

        headerColor  = cols.header;
        onHeaderText = (ColorUtils.calculateLuminance(headerColor) < 0.5) ? 0xFFFFFFFF : 0xFF000000;
        onSurface    = cols.textOnContent;              // ✅ mantenemos tu color de números original
        onSurfaceFaded = ColorUtils.setAlphaComponent(onSurface, 90);

        fontFamily = sm.getFontFamily();                // A, B o C
    }

    public void setPalette(ThemePalettes.Colors cols) {
        headerColor  = cols.header;
        onHeaderText = (ColorUtils.calculateLuminance(headerColor) < 0.5) ? 0xFFFFFFFF : 0xFF000000;
        onSurface    = cols.textOnContent;
        onSurfaceFaded = ColorUtils.setAlphaComponent(onSurface, 90);
        notifyDataSetChanged();
    }

    public void setOnDayClick(OnDayClick l) { this.listener = l; }

    public void setMonth(int year, int monthZeroBased, int firstDayOfWeek) {
        currentYear = year;
        currentMonthZero = monthZeroBased;
        data.clear();
        data.addAll(buildMonth(year, monthZeroBased, firstDayOfWeek));
        notifyDataSetChanged();
    }

    /** Marca los días (dayKey AAAAMMDD) que tienen tareas ÚNICO DÍA. */
    public void setMarkedDayKeys(Set<Integer> keys) {
        markedDayKeys.clear();
        if (keys != null) markedDayKeys.addAll(keys);
        notifyDataSetChanged();
    }

    /** Marca los días con PERMANENTES completadas (solo un tilde por día). */
    public void setCompletedAlwaysKeys(Set<Integer> keys) {
        completedAlwaysKeys.clear();
        if (keys != null) completedAlwaysKeys.addAll(keys);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);

        // Hacer la celda cuadrada
        v.post(() -> {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = v.getWidth();
            v.setLayoutParams(lp);
        });
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CalendarDay d = data.get(pos);
        h.tv.setText(String.valueOf(d.dayOfMonth));

        // Aseguramos que el número no agregue padding de fuente
        h.tv.setIncludeFontPadding(false);

        if (d.isToday) {
            h.bgToday.setVisibility(View.VISIBLE);
            ViewCompat.setBackgroundTintList(h.bgToday, ColorStateList.valueOf(headerColor));
            h.tv.setTextColor(onSurface);
        } else {
            h.bgToday.setVisibility(View.GONE);
            h.tv.setTextColor(d.inCurrentMonth ? onSurface : onSurfaceFaded);
        }

        // --- Ajuste de separación punto/tilde según CardFont ---
        // Idea: B y C tienden a “subir” visualmente el número => bajamos indicadores.
        final int dotBottom;        // margen inferior del PUNTO
        final int tickNoDotBottom;  // margen inferior del TILDE cuando NO hay punto
        switch (fontFamily) {
            case B:
                dotBottom = dp(h.itemView.getContext(), 8);   // antes 14
                tickNoDotBottom = dp(h.itemView.getContext(), 6);
                break;
            case C:
                dotBottom = dp(h.itemView.getContext(), 8);    // un pelín más abajo aún
                tickNoDotBottom = dp(h.itemView.getContext(), 6);
                break;
            default: // A
                dotBottom = dp(h.itemView.getContext(), 12);   // tu valor original
                tickNoDotBottom = dp(h.itemView.getContext(), 8);
                break;
        }

        // Aplicamos el margen del punto (esté visible o no, dejamos listo el valor)
        FrameLayout.LayoutParams dotLp = (FrameLayout.LayoutParams) h.dot.getLayoutParams();
        dotLp.bottomMargin = dotBottom;
        h.dot.setLayoutParams(dotLp);

        int key = toDayKey(d.year, d.month, d.dayOfMonth);

        // Puntito Único Día
        boolean showDot = markedDayKeys.contains(key);
        if (showDot) {
            h.dot.setVisibility(View.VISIBLE);
            int dotColor = d.inCurrentMonth ? onSurface : onSurfaceFaded;
            ViewCompat.setBackgroundTintList(h.dot, ColorStateList.valueOf(dotColor));
            h.dot.setAlpha(1f);
        } else {
            h.dot.setVisibility(View.GONE);
        }

        // Mini-tilde Permanentes completadas (único)
        if (completedAlwaysKeys.contains(key)) {
            h.miniTick.setVisibility(View.VISIBLE);
            h.miniTick.setTextColor(d.inCurrentMonth ? onSurface : onSurfaceFaded);

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) h.miniTick.getLayoutParams();
            // Si hay puntito, el tilde va más pegado al borde; si no, usamos el margen según fuente
            lp.bottomMargin = showDot ? dp(h.itemView.getContext(), -3) : tickNoDotBottom;
            h.miniTick.setLayoutParams(lp);

        } else {
            h.miniTick.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(d);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tv;
        final View bgToday, dot;
        final TextView miniTick;
        final FrameLayout root;
        VH(@NonNull View itemView) {
            super(itemView);
            root   = itemView.findViewById(R.id.dayRoot);
            tv     = itemView.findViewById(R.id.tvDayNumber);
            bgToday= itemView.findViewById(R.id.bgToday);
            dot    = itemView.findViewById(R.id.dotTask);
            miniTick = itemView.findViewById(R.id.tvMiniTick);
        }
    }

    // -------- Helpers --------
    private List<CalendarDay> buildMonth(int year, int monthZero, int firstDayOfWeek) {
        List<CalendarDay> out = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(firstDayOfWeek);

        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, monthZero);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int dow = cal.get(Calendar.DAY_OF_WEEK); // 1=Dom ... 7=Sab
        int offset = ((dow - firstDayOfWeek) + 7) % 7;

        // Leading (mes anterior)
        Calendar prev = (Calendar) cal.clone();
        prev.add(Calendar.MONTH, -1);
        int prevDays = prev.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = offset - 1; i >= 0; i--) {
            int day = prevDays - i;
            out.add(new CalendarDay(
                    prev.get(Calendar.YEAR),
                    prev.get(Calendar.MONTH),
                    day,
                    false, false, false
            ));
        }

        // Mes actual
        Calendar today = Calendar.getInstance();
        boolean isThisMonth = (today.get(Calendar.YEAR) == year)
                && (today.get(Calendar.MONTH) == monthZero);
        for (int d = 1; d <= daysInMonth; d++) {
            boolean isToday = isThisMonth && (today.get(Calendar.DAY_OF_MONTH) == d);
            out.add(new CalendarDay(year, monthZero, d, true, isToday, false));
        }

        // Trailing (mes siguiente) hasta 42 celdas
        Calendar next = (Calendar) cal.clone();
        next.add(Calendar.MONTH, 1);
        int nd = 1;
        while (out.size() < 42) {
            out.add(new CalendarDay(
                    next.get(Calendar.YEAR),
                    next.get(Calendar.MONTH),
                    nd++,
                    false, false, false
            ));
        }

        return out;
    }

    private static int toDayKey(int year, int monthZero, int day) {
        int m = monthZero + 1; // 1..12
        return year * 10000 + m * 100 + day;
    }
    private static int dp(android.content.Context ctx, int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

}

