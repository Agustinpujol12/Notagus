package com.agustinpujol.notagus;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persistencia simple de configuración usando SharedPreferences.
 * - Guarda/lee: tema, fuente, tamaño, "mostrar solo tareas del día" y "recibir notificaciones".
 */
public class SettingsManager {

    // ==== Enums visibles ====
    public enum ThemeMode { SYSTEM, DARK, LIGHT }
    public enum FontFamily { A, B, C }
    public enum FontSize  { SMALL, MEDIUM, LARGE }

    private static final String PREFS_NAME = "settings";

    private static final String K_THEME = "theme_mode";
    private static final String K_FONT  = "font_family";
    private static final String K_SIZE  = "font_size";

    // Ya existente: filtro de lista
    private static final String K_SHOW_ONLY_DAY = "show_only_day";

    // Nuevo: notificaciones diarias
    private static final String K_NOTIF_ENABLED = "notif_enabled";

    // Defaults
    private static final String  DEF_THEME = ThemeMode.SYSTEM.name();
    private static final String  DEF_FONT  = FontFamily.A.name();
    private static final String  DEF_SIZE  = FontSize.MEDIUM.name();
    private static final boolean DEF_SHOW_ONLY_DAY = false;
    private static final boolean DEF_NOTIF_ENABLED = false;

    private static SettingsManager instance;
    private final SharedPreferences prefs;

    private SettingsManager(Context ctx) {
        this.prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SettingsManager getInstance(Context ctx) {
        if (instance == null) instance = new SettingsManager(ctx);
        return instance;
    }

    // ===== THEME =====
    public ThemeMode getThemeMode() {
        String raw = prefs.getString(K_THEME, DEF_THEME);
        try { return ThemeMode.valueOf(raw); } catch (Exception e) { return ThemeMode.SYSTEM; }
    }
    public void setThemeMode(ThemeMode mode) {
        prefs.edit().putString(K_THEME, mode.name()).apply();
    }

    // ===== FONT =====
    public FontFamily getFontFamily() {
        String raw = prefs.getString(K_FONT, DEF_FONT);
        try { return FontFamily.valueOf(raw); } catch (Exception e) { return FontFamily.A; }
    }
    public void setFontFamily(FontFamily ff) {
        prefs.edit().putString(K_FONT, ff.name()).apply();
    }

    // ===== FONT SIZE =====
    public FontSize getFontSize() {
        String raw = prefs.getString(K_SIZE, DEF_SIZE);
        try { return FontSize.valueOf(raw); } catch (Exception e) { return FontSize.MEDIUM; }
    }
    public void setFontSize(FontSize fs) {
        prefs.edit().putString(K_SIZE, fs.name()).apply();
    }

    // ===== SHOW ONLY DAY =====
    public boolean isShowOnlyDayEnabled() {
        return prefs.getBoolean(K_SHOW_ONLY_DAY, DEF_SHOW_ONLY_DAY);
    }
    public void setShowOnlyDayEnabled(boolean enabled) {
        prefs.edit().putBoolean(K_SHOW_ONLY_DAY, enabled).apply();
    }

    // ===== NOTIFICATIONS =====
    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(K_NOTIF_ENABLED, DEF_NOTIF_ENABLED);
    }
    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(K_NOTIF_ENABLED, enabled).apply();
    }
}
