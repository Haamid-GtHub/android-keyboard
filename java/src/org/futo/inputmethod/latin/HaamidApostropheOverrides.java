package org.futo.inputmethod.latin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Narrow, user-requested apostrophe spelling exceptions kept separate from HaamidFormatter.
 * The formatter's general rules remain unchanged.
 */
public final class HaamidApostropheOverrides {
    private static final Pattern MAAM =
            Pattern.compile("(?i)\\bma(['\\u2019])([a-z]{2})\\b");

    private HaamidApostropheOverrides() {
    }

    /**
     * Preserve the user's explicitly typed "am" versus "Am" in ma'am, while letting the
     * normal formatter determine every other character. Formatting changes case only, so
     * the original and formatted strings must have matching lengths before applying offsets.
     */
    public static String apply(String original, String formatted) {
        if (original == null || formatted == null
                || original.length() != formatted.length()
                || original.isEmpty()) {
            return formatted;
        }

        Matcher matcher = MAAM.matcher(original);
        if (!matcher.find()) {
            return formatted;
        }

        StringBuilder result = new StringBuilder(formatted);
        do {
            String typedSuffix = matcher.group(2);
            if ("am".equals(typedSuffix) || "Am".equals(typedSuffix)) {
                int start = matcher.start(2);
                result.replace(start, start + 2, typedSuffix);
            }
        } while (matcher.find());

        return result.toString();
    }
}
