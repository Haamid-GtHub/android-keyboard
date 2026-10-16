package org.futo.inputmethod.latin;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persistent switch for automatic Haamid formatting.
 * Manual formatting remains available regardless of this setting.
 */
public final class HaamidFormattingSettings {
    private static final String PREFS_NAME = "haamid_formatting_settings";
    private static final String KEY_AUTOMATIC_FORMATTING = "automatic_formatting_enabled";
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
