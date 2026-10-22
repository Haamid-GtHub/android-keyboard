package org.futo.inputmethod.latin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class HaamidFormatter {

    private static final Set<String> CONTRACTIONS =
            new HashSet<>(Arrays.asList(
                    "s", "t", "m", "d", "ll", "re", "ve", "am"
            ));

    private static final char CURLY = '\u2019';

    private HaamidFormatter() {
    }

    /**
     * Automatic formatting preserves each entirely-uppercase line as the user typed it.
     * Other lines use the regular formatter. Manual formatting continues to use format().
     */
    public static String formatAutomatic(String text) {
        StringBuilder result = new StringBuilder();
        int start = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == '\n') {
                String line = text.substring(start, i);
                result.append(isEntireTextUppercase(line) ? line : format(line));
                if (i < text.length()) {
                    result.append('\n');
                }
                start = i + 1;
            }
        }
        return result.toString();
    }

    public static String format(String text) {
        boolean entireTextUppercase = isEntireTextUppercase(text);

        StringBuilder result = new StringBuilder();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (Character.isLetterOrDigit(ch) || ch == '\'' || ch == CURLY) {
                current.append(ch);
            } else {
                if (current.length() > 0) {
                    result.append(
                            processWord(
                                    current.toString(),
                                    entireTextUppercase
                            )
                    );
                    current.setLength(0);
                }

                result.append(ch);
            }
        }

        if (current.length() > 0) {
            result.append(
                    processWord(
                            current.toString(),
                            entireTextUppercase
                    )
            );
        }

        return result.toString();
    }

    private static boolean isEntireTextUppercase(String text) {
        boolean hasLetter = false;

        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;

                if (!Character.isUpperCase(c)) {
                    return false;
                }
            }
        }

        return hasLetter;
    }

    private static String processWord(
            String word,
            boolean entireTextUppercase
    ) {
        String apostrophe =
                word.indexOf(CURLY) >= 0
                        ? String.valueOf(CURLY)
                        : "'";

        String[] parts =
                word.replace(String.valueOf(CURLY), "'")
                        .split("'");

        if (parts.length == 1) {
            return formatWord(word, entireTextUppercase);
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (i == 0) {
                result.append(
                        formatWord(part, entireTextUppercase)
                );
            } else if (CONTRACTIONS.contains(part.toLowerCase())) {
                result.append(apostrophe).append(part.toLowerCase());
            } else {
                result.append(apostrophe)
                        .append(formatWord(part, entireTextUppercase));
            }
        }

        return result.toString();
    }

    private static String formatWord(
            String word,
            boolean entireTextUppercase
    ) {
        if (word.isEmpty()) {
            return word;
        }

        if (Character.isDigit(word.charAt(0))) {
            return word;
        }

        boolean hasUpper = false;
        boolean hasLower = false;

        for (char c : word.toCharArray()) {
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) hasUpper = true;
                if (Character.isLowerCase(c)) hasLower = true;
            }
        }

        if (hasUpper && hasLower) {
            return word;
        }

        boolean allUpper = true;
        boolean hasLetter = false;

        for (char c : word.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
                if (!Character.isUpperCase(c)) {
                    allUpper = false;
                    break;
                }
            }
        }

        if (hasLetter && allUpper) {
            if (!entireTextUppercase) {
                return word;
            }

            return Character.toUpperCase(word.charAt(0))
                    + word.substring(1).toLowerCase();
        }

        boolean hasDigit = false;

        for (char c : word.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
                break;
            }
        }

        if (hasDigit) {
            char[] chars = word.toCharArray();

            for (int i = 0; i < chars.length; i++) {
                if (Character.isLetter(chars[i])) {
                    chars[i] = Character.toUpperCase(chars[i]);
                    break;
                }
            }

            return new String(chars);
        }

        return Character.toUpperCase(word.charAt(0))
                + word.substring(1).toLowerCase();
    }
}
