package org.futo.inputmethod.latin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class HaamidFormatter {

    private static final Set<String> CONTRACTIONS =
            new HashSet<>(Arrays.asList(
                    "s", "t", "m", "d", "ll", "re", "ve"
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

    /**
     * Detects deliberate mixed casing without treating hyphen-separated title-case
     * components (for example, E-mail) as a mixed-case name.
     */
    public static boolean hasIntentionalMixedCase(final String word) {
        if (word == null || word.isEmpty()) return false;
        final String[] hyphenParts = word.split("-", -1);
        for (String part : hyphenParts) {
            if (hasIntentionalMixedCasePart(part)) return true;
        }
        return false;
    }

    private static boolean hasIntentionalMixedCasePart(final String word) {
            boolean hasUppercase = false;
            boolean hasLowercase = false;
            for (int i = 0; i < word.length(); i++) {
                final char ch = word.charAt(i);
                if (Character.isUpperCase(ch)) {
                    hasUppercase = true;
                } else if (Character.isLowerCase(ch)) {
                    hasLowercase = true;
                }
            }
            if (!hasUppercase || !hasLowercase) {
                return false;
            }
    
            if (word.indexOf('\'') < 0 && word.indexOf('\u2019') < 0) {
                return true;
            }
    
            final String[] parts = word.replace('\u2019', '\'').split("\\'", -1);
    
            // A normal apostrophe word can be mixed-case as a whole while each segment is
            // consistently cased (for example Y'all or O'Clock). Do not mistake that for an
            // intentional mixed-case brand/name; the automatic formatter must normalize each
            // segment independently. Keep the deliberate contraction-suffix safeguard below.
            boolean hasMixedSegment = false;
            for (String part : parts) {
                boolean partUpper = false;
                boolean partLower = false;
                for (int j = 0; j < part.length(); j++) {
                    final char ch = part.charAt(j);
                    if (Character.isUpperCase(ch)) partUpper = true;
                    else if (Character.isLowerCase(ch)) partLower = true;
                }
                if (partUpper && partLower) {
                    final boolean titleCasedPart = Character.isUpperCase(part.charAt(0))
                            && part.substring(1).equals(part.substring(1).toLowerCase(Locale.ROOT));
                    if (!titleCasedPart) {
                        hasMixedSegment = true;
                        break;
                    }
                }
            }
            if (!hasMixedSegment) {
                for (int i = 1; i < parts.length; i++) {
                    final String part = parts[i];
                    if (("s".equalsIgnoreCase(part) || "t".equalsIgnoreCase(part)
                            || "m".equalsIgnoreCase(part) || "d".equalsIgnoreCase(part)
                            || "ll".equalsIgnoreCase(part) || "re".equalsIgnoreCase(part)
                            || "ve".equalsIgnoreCase(part))
                            && !part.equals(part.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                }
                return false;
            }
    
            for (int i = 0; i < parts.length; i++) {
                final String part = parts[i];
                boolean partUpper = false;
                boolean partLower = false;
                for (int j = 0; j < part.length(); j++) {
                    final char ch = part.charAt(j);
                    if (Character.isUpperCase(ch)) partUpper = true;
                    else if (Character.isLowerCase(ch)) partLower = true;
                }
                if (partUpper && partLower) {
                    // A normally Title-Cased apostrophe segment (for example "Clock" in
                    // "O'Clock") is not an intentional mixed-case spelling. Preserve only
                    // genuinely mixed internal casing such as "iPhone" or "eBay".
                    final boolean titleCasedPart = Character.isUpperCase(part.charAt(0))
                            && part.substring(1).equals(part.substring(1).toLowerCase(Locale.ROOT));
                    if (!titleCasedPart) {
                        return true;
                    }
                }
    
                // A contraction suffix is intentionally lowercase. Preserve an uppercase or
                // mixed-case suffix exactly as typed instead of normalizing "can'T" to "Can't".
                if (i > 0 && ("s".equalsIgnoreCase(part) || "t".equalsIgnoreCase(part)
                        || "m".equalsIgnoreCase(part) || "d".equalsIgnoreCase(part)
                        || "ll".equalsIgnoreCase(part) || "re".equalsIgnoreCase(part)
                        || "ve".equalsIgnoreCase(part)) && !part.equals(part.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
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

        // Preserve the established special title case for y'all, including curly apostrophes.
        String normalizedWord = word.replace(String.valueOf(CURLY), "'");
        if ("y'all".equalsIgnoreCase(normalizedWord)) {
            return "Y" + apostrophe + "All";
        }

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
