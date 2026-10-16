package org.futo.inputmethod.latin;

/**
 * Runtime switch for Haamid automatic formatting.
 * Enabled by default for each app process; manual formatting is never disabled.
 */
public final class HaamidFormattingSettings {
    private static volatile boolean automaticFormattingEnabled = true;

    private HaamidFormattingSettings() {}

    public static boolean isAutomaticFormattingEnabled() {
        return automaticFormattingEnabled;
    }

    public static boolean toggleAutomaticFormatting() {
        automaticFormattingEnabled = !automaticFormattingEnabled;
        return automaticFormattingEnabled;
    }
}
