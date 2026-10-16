package org.futo.inputmethod.latin;

import android.test.AndroidTestCase;

/**
 * Regression tests for Haamid's custom title-case rules.
 */
public final class HaamidFormatterTests extends AndroidTestCase {
    public void testTitleCasesOrdinaryText() {
        assertEquals("Hello World", HaamidFormatter.format("hello world"));
    }

    public void testPreservesMixedCaseNames() {
        assertEquals("iPhone ChatGPT YouTube macOS",
                HaamidFormatter.format("iPhone ChatGPT YouTube macOS"));
    }

    public void testFormatsContractionsAndApostropheSegments() {
        assertEquals("Don't Must've Rock'N'Roll",
                HaamidFormatter.format("don't must've rock'n'roll"));
    }

    public void testNormalizesEntirelyUppercaseInput() {
        assertEquals("Hello World", HaamidFormatter.format("HELLO WORLD"));
    }

    public void testPreservesAcronymsWhenInputAlsoContainsLowercase() {
        assertEquals("NASA And CPU", HaamidFormatter.format("NASA and CPU"));
    }

    public void testPreservesWordsBeginningWithDigits() {
        assertEquals("4g 5GHz 802.11ac", HaamidFormatter.format("4g 5GHz 802.11ac"));
    }
}
