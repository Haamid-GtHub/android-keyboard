/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.futo.inputmethod.latin;

import android.text.TextUtils;

import org.futo.inputmethod.event.Event;
import org.futo.inputmethod.latin.common.InputPointers;
import org.futo.inputmethod.latin.define.DecoderSpecificConstants;

import java.util.ArrayList;

/**
 * This class encapsulates data about a word previously composed, but that has been
 * committed already. This is used for resuming suggestion, and cancel auto-correction.
 */
public final class LastComposedWord {
    // COMMIT_TYPE_USER_TYPED_WORD is used when the word committed is the exact typed word, with
    // no hinting from the IME. It happens when some external event happens (rotating the device,
    // for example) or when auto-correction is off by settings or editor attributes.
    public static final int COMMIT_TYPE_USER_TYPED_WORD = 0;
    // COMMIT_TYPE_MANUAL_PICK is used when the user pressed a field in the suggestion strip.
    public static final int COMMIT_TYPE_MANUAL_PICK = 1;
    // COMMIT_TYPE_DECIDED_WORD is used when the IME commits the word it decided was best
    // for the current user input. It may be different from what the user typed (true auto-correct)
    // or it may be exactly what the user typed if it's in the dictionary or the IME does not have
    // enough confidence in any suggestion to auto-correct (auto-correct to typed word).
    public static final int COMMIT_TYPE_DECIDED_WORD = 2;
    // COMMIT_TYPE_CANCEL_AUTO_CORRECT is used upon committing back the old word upon cancelling
    // an auto-correction.
    public static final int COMMIT_TYPE_CANCEL_AUTO_CORRECT = 3;

    public static final String NOT_A_SEPARATOR = "";

    public final ArrayList<Event> mEvents;
    public final String mTypedWord;
    public final CharSequence mCommittedWord;
    public String mSeparatorString;
    public final NgramContext mNgramContext;
    public final int mCapitalizedMode;
    public final InputPointers mInputPointers =
            new InputPointers(DecoderSpecificConstants.DICTIONARY_MAX_WORD_LENGTH);

    private boolean mActive;
    // True only when Haamid's automatic formatter changed the committed word's casing.
    private boolean mHaamidFormattingApplied;
    // True when automatic correction changed the exact text the user typed, even if the
    // difference is only case. Backspace should restore that exact original spelling.
    private boolean mAutoCorrectionChangedTypedWord;
    // A manually selected suggestion is a deliberate choice, not an auto-correction undo target.
    private boolean mManualSuggestionPick;

    public static final LastComposedWord NOT_A_COMPOSED_WORD =
            new LastComposedWord(new ArrayList<Event>(), null, "", "",
            NOT_A_SEPARATOR, null, WordComposer.CAPS_MODE_OFF);

    // Warning: this is using the passed objects as is and fully expects them to be
    // immutable. Do not fiddle with their contents after you passed them to this constructor.
    public LastComposedWord(final ArrayList<Event> events,
            final InputPointers inputPointers, final String typedWord,
            final CharSequence committedWord, final String separatorString,
            final NgramContext ngramContext, final int capitalizedMode) {
        if (inputPointers != null) {
            mInputPointers.copy(inputPointers);
        }
        mTypedWord = typedWord;
        mEvents = new ArrayList<>(events);
        mCommittedWord = committedWord;
        mSeparatorString = separatorString;
        mActive = true;
        mNgramContext = ngramContext;
        mCapitalizedMode = capitalizedMode;
    }

    public void deactivate() {
        mActive = false;
    }

    /**
     * Records that Haamid's automatic formatter changed the committed text.
     * Keep the commit active so a real lexical auto-correction can still be reverted,
     * while InputLogic can distinguish formatting-only case changes from auto-correction.
     */
    public void markHaamidFormattingApplied() {
        mHaamidFormattingApplied = true;
        mActive = true;
    }

    public boolean isHaamidFormattingApplied() {
        return mHaamidFormattingApplied;
    }

    public void markAutoCorrectionChangedTypedWord() {
        mAutoCorrectionChangedTypedWord = true;
    }

    public boolean isAutoCorrectionChangedTypedWord() {
        return mAutoCorrectionChangedTypedWord;
    }

    public void markManualSuggestionPick() {
        mManualSuggestionPick = true;
    }

    public boolean isManualSuggestionPick() {
        return mManualSuggestionPick;
    }

    public boolean canRevertCommit() {
        return mActive && !TextUtils.isEmpty(mCommittedWord) && !didCommitTypedWord();
    }

    private boolean didCommitTypedWord() {
        return TextUtils.equals(mTypedWord, mCommittedWord);
    }
}
