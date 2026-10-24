package org.futo.inputmethod.latin;

import android.test.AndroidTestCase;

import org.futo.inputmethod.latin.inputlogic.InputLogic;

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

    public void testMaamOverrideIsSeparateFromFormatter() {
        assertEquals("Ma'Am", HaamidFormatter.format("ma'am"));
        assertEquals("Ma'Am", HaamidFormatter.format("Ma'Am"));
        assertEquals("Ma'Am", HaamidFormatter.formatAutomatic("ma'am"));
        assertEquals("Ma'am", HaamidApostropheOverrides.apply(
                "ma'am", HaamidFormatter.formatAutomatic("ma'am")));
        assertEquals("Ma'Am", HaamidApostropheOverrides.apply(
                "Ma'Am", HaamidFormatter.formatAutomatic("Ma'Am")));
    }

    public void testOtherApostropheWordsKeepTitleCase() {
        assertEquals("O'Clock Y'All", HaamidFormatter.format("o'clock y'all"));
        assertEquals("O'Clock", HaamidFormatter.formatAutomatic("O'clock"));
        assertEquals("Y'All", HaamidFormatter.formatAutomatic("Y'all"));
        assertEquals("Y'All", HaamidFormatter.formatAutomatic("y'all"));
        assertEquals("Y’All", HaamidFormatter.formatAutomatic("y’all"));
        assertEquals("O’Clock", HaamidFormatter.formatAutomatic("o’clock"));
    }

    public void testAutomaticFormattingPreservesAcronymSms() {
        assertEquals("SMS", HaamidFormatter.formatAutomatic("SMS"));
        assertEquals("SMS And NASA", HaamidFormatter.formatAutomatic("SMS and NASA"));
    }

    public void testNormalizesEntirelyUppercaseInput() {
        assertEquals("Hello World", HaamidFormatter.format("HELLO WORLD"));
    }

    public void testPreservesAcronymsWhenInputAlsoContainsLowercase() {
        assertEquals("NASA And CPU", HaamidFormatter.format("NASA and CPU"));
    }

    public void testNumberLeadingTokenDetectionDoesNotLeakToFollowingSuggestion() {
        // The number and its suffix remain protected, but the next word after a space
        // must not inherit that protection when selected from the suggestion bar.
        assertTrue(InputLogic.isNumberLeadingTypedToken("2nd", ' ', false, false));
        assertTrue(InputLogic.isNumberLeadingTypedToken("nd", '2', true, false));
        // A preceding letter plus attached-to-non-word state is not a numeric token.
        assertFalse(InputLogic.isNumberLeadingTypedToken("word", 'x', true, false));
        assertFalse(InputLogic.isNumberLeadingTypedToken("hello", ' ', true, true));
        assertFalse(InputLogic.isNumberLeadingTypedToken("hello", ' ', false, true));
        // A stale digit context must not suppress formatting once a space separates words.
        assertFalse(InputLogic.isNumberLeadingTypedToken("hello", '2', true, true));
    }

    public void testPreservesWordsBeginningWithDigits() {
        assertEquals("4g 5GHz 802.11ac", HaamidFormatter.format("4g 5GHz 802.11ac"));
    }

    public void testAutomaticFormattingTitleCasesOrdinaryText() {
        assertEquals("Hello World", HaamidFormatter.formatAutomatic("hello world"));
    }

    public void testAutomaticFormattingPreservesIntentionalUppercase() {
        assertEquals("HELLO WORLD", HaamidFormatter.formatAutomatic("HELLO WORLD"));
        assertEquals("Hello WORLD", HaamidFormatter.formatAutomatic("hello WORLD"));
    }

    public void testAutomaticFormattingPreservesAcronymsAndMixedCaseNames() {
        assertEquals("NASA And CPU iPhone ChatGPT",
                HaamidFormatter.formatAutomatic("NASA and CPU iPhone ChatGPT"));
    }

    public void testAutomaticFormattingPreservesUppercaseLinesOnly() {
        assertEquals("HELLO WORLD\nHello World",
                HaamidFormatter.formatAutomatic("HELLO WORLD\nhello world"));
    }

    public void testHaamidFormattingCommitRemainsUndoableAfterDeactivation() {
        LastComposedWord commit = new LastComposedWord(
                new java.util.ArrayList<org.futo.inputmethod.event.Event>(),
                null, "hello", "Hello", " ", null, WordComposer.CAPS_MODE_OFF);

        commit.deactivate();
        assertFalse(commit.canRevertCommit());

        commit.markHaamidFormattingApplied();
        assertTrue(commit.isHaamidFormattingApplied());
        assertTrue(commit.canRevertCommit());
    }

    public void testHaamidFormattingUndoRequiresAnActualTextChange() {
        LastComposedWord commit = new LastComposedWord(
                new java.util.ArrayList<org.futo.inputmethod.event.Event>(),
                null, "Hello", "Hello", " ", null, WordComposer.CAPS_MODE_OFF);

        commit.markHaamidFormattingApplied();
        assertFalse(commit.canRevertCommit());
    }

    public void testCaseOnlyAutoCorrectionRemainsUndoableAfterTitleCase() {
        LastComposedWord commit = new LastComposedWord(
                new java.util.ArrayList<org.futo.inputmethod.event.Event>(),
                null, "can'T", "Can't", " ", null, WordComposer.CAPS_MODE_OFF);

        commit.markAutoCorrectionChangedTypedWord();
        commit.markHaamidFormattingApplied();

        assertTrue(commit.isAutoCorrectionChangedTypedWord());
        assertTrue(commit.canRevertCommit());
    }
}
