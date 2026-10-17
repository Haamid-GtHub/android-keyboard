package org.futo.inputmethod.latin;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;

/**
 * Persistent switch for automatic Haamid formatting.
 * Manual formatting remains available regardless of this setting.
 */
public final class HaamidFormattingSettings {
    private static final String PREFS_NAME = "haamid_formatting_settings";
    private static final String KEY_AUTOMATIC_FORMATTING = "automatic_formatting_enabled";
    private static final String KEY_PROCESS_TEXT = "process_text_enabled";
    private static volatile boolean automaticFormattingEnabled = true;
    private static volatile boolean initialized = false;

    private HaamidFormattingSettings() {}

    public static synchronized void initialize(Context context) {
        if (initialized) return;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        automaticFormattingEnabled = prefs.getBoolean(KEY_AUTOMATIC_FORMATTING, true);
        initialized = true;
    }

    public static boolean isAutomaticFormattingEnabled() {
        return automaticFormattingEnabled;
    }

    public static synchronized void setAutomaticFormattingEnabled(Context context, boolean enabled) {
        initialize(context);
        automaticFormattingEnabled = enabled;
        context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_AUTOMATIC_FORMATTING, enabled).apply();
    }

    public static boolean isProcessTextEnabled(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PROCESS_TEXT, true);
    }

    public static synchronized void setProcessTextEnabled(Context context, boolean enabled) {
        Context app = context.getApplicationContext();
        app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_PROCESS_TEXT, enabled).apply();
        ComponentName component = new ComponentName(app, HaamidProcessTextActivity.class);
        app.getPackageManager().setComponentEnabledSetting(component,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static synchronized boolean toggleAutomaticFormatting(Context context) {
        initialize(context);
        automaticFormattingEnabled = !automaticFormattingEnabled;
        context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AUTOMATIC_FORMATTING, automaticFormattingEnabled)
                .apply();
        return automaticFormattingEnabled;
    }
}
