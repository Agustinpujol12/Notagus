package com.agustinpujol.notagus;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Diálogo de Configuración.
 * - Guarda: tema, fuente, "mostrar solo tareas del día" y "recibir notificaciones".
 * - Header = color "header", cuerpo = color "content".
 * - Incluye acción "Eliminar tareas pasadas" (Único día < hoy).
 */
public class SettingsDialog extends DialogFragment {

    // === Callback al Activity para acciones post-confirmación ===
    public interface SettingsActionListener {
        void onDeletePastTasksConfirmed();
    }

    @Nullable
    private SettingsActionListener actionListener;

    public void setSettingsActionListener(@Nullable SettingsActionListener l) {
        this.actionListener = l;
    }

    // Estado seleccionado
    private SettingsManager.ThemeMode themeSelected;
    private SettingsManager.FontFamily fontSelected;
    private boolean showOnlyDaySelected;
    private boolean notificationsSelected;

    // Colores para bordes de cards
    private int colorPrimary;
    private int colorOutline;

    // Vistas
    private View dialogCard;
    private View settingsHeaderBar;
    private TextView tvSettingsTitle;
    private TextView tvSectionTheme, tvSectionFont;
    private TextView tvBiometricLabel, tvAutoArchiveLabel, tvDeletePastTaskLabel;
    private MaterialSwitch switchBiometric, switchAutoArchive; // Biometric=Mostrar solo del día, AutoArchive=Recibir notificaciones
    private MaterialButton btnAplicar, btnCancelar;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_settings, null, false);

        // Material colors base
        colorPrimary = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, 0xFF6200EE);
        colorOutline = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOutline, 0xFFBDBDBD);

        // Refs UI
        dialogCard = content.findViewById(R.id.dialogCard);
        settingsHeaderBar = content.findViewById(R.id.settingsHeaderBar);
        tvSettingsTitle = content.findViewById(R.id.tvSettingsTitle);

        tvSectionTheme = content.findViewById(R.id.tvSectionTheme);
        tvSectionFont  = content.findViewById(R.id.tvSectionFont);

        tvBiometricLabel = content.findViewById(R.id.tvBiometricLabel);
        tvAutoArchiveLabel = content.findViewById(R.id.tvAutoArchiveLabel);
        tvDeletePastTaskLabel = content.findViewById(R.id.tvDeletePastTasksLabel);
        if (tvDeletePastTaskLabel == null) {
            int altId = getResources().getIdentifier("tvDeletePastTaskLabel", "id", requireContext().getPackageName());
            if (altId != 0) tvDeletePastTaskLabel = content.findViewById(altId);
        }

        switchBiometric = content.findViewById(R.id.switchBiometric);      // "Mostrar solo tareas del día"
        switchAutoArchive = content.findViewById(R.id.switchAutoArchive);  // "Recibir notificaciones"

        btnAplicar = content.findViewById(R.id.btnAplicar);
        btnCancelar = content.findViewById(R.id.btnCancelar);

        // Cards de selección
        MaterialCardView cardThemeSystem = content.findViewById(R.id.cardThemeSystem);
        MaterialCardView cardThemeDark   = content.findViewById(R.id.cardThemeDark);
        MaterialCardView cardThemeLight  = content.findViewById(R.id.cardThemePink); // Rosa = LIGHT
        List<MaterialCardView> themeGroup = Arrays.asList(cardThemeSystem, cardThemeDark, cardThemeLight);

        MaterialCardView cardFontA = content.findViewById(R.id.cardFontA);
        MaterialCardView cardFontB = content.findViewById(R.id.cardFontB);
        MaterialCardView cardFontC = content.findViewById(R.id.cardFontC);
        List<MaterialCardView> fontGroup = Arrays.asList(cardFontA, cardFontB, cardFontC);

        initGroup(themeGroup);
        initGroup(fontGroup);

        // Cargar selección persistida
        SettingsManager sm = SettingsManager.getInstance(requireContext());
        themeSelected = sm.getThemeMode();
        fontSelected  = sm.getFontFamily();
        showOnlyDaySelected = sm.isShowOnlyDayEnabled();
        notificationsSelected = sm.isNotificationsEnabled();

        // Marcar selección de tema
        switch (themeSelected) {
            case SYSTEM:
                selectInGroup(themeGroup, cardThemeSystem);
                break;
            case DARK:
                selectInGroup(themeGroup, cardThemeDark);
                break;
            case LIGHT:
                selectInGroup(themeGroup, cardThemeLight);
                break;
        }
        // Marcar selección de fuente
        switch (fontSelected) {
            case A:
                selectInGroup(fontGroup, cardFontA);
                break;
            case B:
                selectInGroup(fontGroup, cardFontB);
                break;
            case C:
                selectInGroup(fontGroup, cardFontC);
                break;
        }

        // Estados de switches
        if (switchBiometric != null) switchBiometric.setChecked(showOnlyDaySelected);
        if (switchAutoArchive != null) switchAutoArchive.setChecked(notificationsSelected);

        // Colorear preview
        applyPalettePreview(ThemePalettes.forMode(themeSelected));

        // Listeners de tema
        cardThemeSystem.setOnClickListener(v -> {
            selectInGroup(themeGroup, cardThemeSystem);
            themeSelected = SettingsManager.ThemeMode.SYSTEM;
            applyPalettePreview(ThemePalettes.forMode(themeSelected));
        });
        cardThemeDark.setOnClickListener(v -> {
            selectInGroup(themeGroup, cardThemeDark);
            themeSelected = SettingsManager.ThemeMode.DARK;
            applyPalettePreview(ThemePalettes.forMode(themeSelected));
        });
        cardThemeLight.setOnClickListener(v -> {
            selectInGroup(themeGroup, cardThemeLight);
            themeSelected = SettingsManager.ThemeMode.LIGHT;
            applyPalettePreview(ThemePalettes.forMode(themeSelected));
        });

        // Listeners de fuente
        cardFontA.setOnClickListener(v -> { selectInGroup(fontGroup, cardFontA); fontSelected = SettingsManager.FontFamily.A; });
        cardFontB.setOnClickListener(v -> { selectInGroup(fontGroup, cardFontB); fontSelected = SettingsManager.FontFamily.B; });
        cardFontC.setOnClickListener(v -> { selectInGroup(fontGroup, cardFontC); fontSelected = SettingsManager.FontFamily.C; });

        // --- Tacho "Eliminar tareas pasadas": confirm + delete ---
        View btnDeletePast = findAnyByIdOrName(
                content,
                R.id.btnDeletePastTasks,                   // id recomendado
                "btnDeletePastTasks", "btnDeletePastTask",
                "ivDeletePastTasks", "btnTrashDeletePast",
                "btnClearPastTasks"
        );
        if (btnDeletePast == null) {
            btnDeletePast = findByContentDescription(content, "Eliminar tareas pasadas");
        }
        if (btnDeletePast != null) {
            btnDeletePast.setClickable(true);
            btnDeletePast.setFocusable(true);
            btnDeletePast.setOnClickListener(v ->
                    new ConfirmDeleteDialog(
                            "Eliminar tareas pasadas",
                            "Se eliminarán:\n• todas las tareas de ÚNICO DÍA con fecha anterior a hoy,\n" +
                                    "• y las tareas PERMANENTES que ya fueron completadas en días anteriores.\n\n" +
                                    "Esta acción no se puede deshacer.",
                            () -> {
                                new Thread(() -> {
                                    int todayKey = getTodayKey();
                                    AppDatabase db = DatabaseClient.getInstance(requireContext()).getAppDatabase();

                                    // 1) ÚNICO DÍA < hoy
                                    int rowsOneDay = db.taskDao().deletePastOneDay(todayKey, Task.REPEAT_ONE_DAY);

                                    // 2) PERMANENTES completadas en días pasados
                                    int rowsAlways = db.taskDao().deletePastCompletedAlways(todayKey, Task.REPEAT_ALWAYS);

                                    int total = rowsOneDay + rowsAlways;

                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            android.widget.Toast.makeText(
                                                    requireContext(),
                                                    (total > 0
                                                            ? "Eliminadas " + total + " tareas pasadas"
                                                            : "No había tareas pasadas para eliminar"),
                                                    android.widget.Toast.LENGTH_SHORT
                                            ).show();

                                            dismiss(); // cerrar diálogo

                                            if (actionListener != null) {
                                                actionListener.onDeletePastTasksConfirmed();
                                            }
                                        });
                                    }
                                }).start();
                            }
                    ).show(getParentFragmentManager(), "ConfirmDeletePastTasks")
            );

        }
        // ---------------------------------------------------------

        // Botones
        btnCancelar.setOnClickListener(v -> dismiss());

        btnAplicar.setOnClickListener(v -> {
            // Persistir selecciones
            sm.setThemeMode(themeSelected);
            sm.setFontFamily(fontSelected);

            boolean newShowOnlyDay = (switchBiometric != null) && switchBiometric.isChecked();
            boolean newNotif = (switchAutoArchive != null) && switchAutoArchive.isChecked();
            sm.setShowOnlyDayEnabled(newShowOnlyDay);
            sm.setNotificationsEnabled(newNotif);

            // Aplicar modo claro/oscuro
            int mode;
            switch (themeSelected) {
                case LIGHT:
                    mode = AppCompatDelegate.MODE_NIGHT_NO;
                    break;
                case DARK:
                    mode = AppCompatDelegate.MODE_NIGHT_YES;
                    break;
                case SYSTEM:
                default:
                    mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    break;
            }
            AppCompatDelegate.setDefaultNightMode(mode);

            // Recrear para re-temear (solo aquí)
            if (getActivity() != null) getActivity().recreate();
            dismiss();
        });

        Dialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    // ==== helpers ====
    private void initGroup(List<MaterialCardView> group) {
        for (MaterialCardView c : group) {
            c.setClickable(true);
            c.setFocusable(true);
            c.setStrokeWidth(dp(1));
            c.setStrokeColor(colorOutline);
        }
    }

    private void selectInGroup(List<MaterialCardView> group, MaterialCardView selected) {
        for (MaterialCardView c : group) {
            boolean isSelected = (c == selected);
            c.setStrokeWidth(isSelected ? dp(3) : dp(1));
            c.setStrokeColor(isSelected ? colorPrimary : colorOutline);
        }
    }

    /** Aplica header + fondo del cuerpo con la paleta (preview local del diálogo). */
    private void applyPalettePreview(ThemePalettes.Colors cols) {
        if (dialogCard != null) ViewCompat.setBackgroundTintList(dialogCard, ColorStateList.valueOf(cols.content));
        if (settingsHeaderBar != null) settingsHeaderBar.setBackgroundColor(cols.header);

        int textOnHeader = ColorUtils.calculateLuminance(cols.header) < 0.5 ? 0xFFFFFFFF : 0xFF000000;
        int textOnContent = ColorUtils.calculateLuminance(cols.content) < 0.5 ? 0xFFFFFFFF : 0xFF000000;

        if (tvSettingsTitle != null) tvSettingsTitle.setTextColor(textOnHeader);
        if (tvSectionTheme  != null) tvSectionTheme.setTextColor(textOnContent);
        if (tvSectionFont   != null) tvSectionFont.setTextColor(textOnContent);
        if (tvBiometricLabel != null) tvBiometricLabel.setTextColor(textOnContent);
        if (tvAutoArchiveLabel != null) tvAutoArchiveLabel.setTextColor(textOnContent);
        if (tvDeletePastTaskLabel != null) tvDeletePastTaskLabel.setTextColor(textOnContent);

        if (switchBiometric != null) tintSwitch(switchBiometric, cols.header);
        if (switchAutoArchive != null) tintSwitch(switchAutoArchive, cols.header);

        if (btnAplicar != null) {
            btnAplicar.setBackgroundTintList(ColorStateList.valueOf(cols.header));
            btnAplicar.setTextColor(textOnHeader);
        }
        if (btnCancelar != null) btnCancelar.setTextColor(textOnContent);
    }

    /** Tinte personalizado para MaterialSwitch. */
    private void tintSwitch(MaterialSwitch sw, int onColor) {
        int offTrack = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOutline, 0xFFBDBDBD);
        int offThumb = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceVariant, 0xFFE0E0E0);
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] trackColors = new int[]{
                ColorUtils.setAlphaComponent(onColor, 150),
                ColorUtils.setAlphaComponent(offTrack, 120)
        };
        int[] thumbColors = new int[]{ onColor, offThumb };
        sw.setTrackTintList(new ColorStateList(states, trackColors));
        sw.setThumbTintList(new ColorStateList(states, thumbColors));
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }

    // ===== Localizadores robustos para el botón del tacho =====
    @Nullable
    private View findAnyByIdOrName(@NonNull View root, int knownId, @NonNull String... altNames) {
        try {
            View v = root.findViewById(knownId);
            if (v != null) return v;
        } catch (Exception ignored) {}

        for (String name : altNames) {
            int resId = getResources().getIdentifier(name, "id", requireContext().getPackageName());
            if (resId != 0) {
                View v = root.findViewById(resId);
                if (v != null) return v;
            }
        }
        return null;
    }

    @Nullable
    private View findByContentDescription(@NonNull View root, @NonNull String cd) {
        CharSequence cdesc = root.getContentDescription();
        if (cdesc != null && cd.contentEquals(cdesc)) return root;
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View r = findByContentDescription(vg.getChildAt(i), cd);
                if (r != null) return r;
            }
        }
        return null;
    }

    /** Devuelve hoy como AAAAMMDD para comparar con dayKey. */
    private int getTodayKey() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String s = sdf.format(new Date());
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0; // Fallback: no borrará nada si algo raro ocurre
        }
    }
}
