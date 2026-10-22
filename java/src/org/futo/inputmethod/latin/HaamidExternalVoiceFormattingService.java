package org.futo.inputmethod.latin;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Optional accessibility bridge for text committed by external speech IMEs such as Google Voice Typing.
 * It arms when accessibility reports a Google voice/search UI event, then watches for a subsequent
 * editable-field text change. Google versions vary in which window events they expose, so this
 * intentionally accepts more than one event type. It formats only the changed region.
 */
public final class HaamidExternalVoiceFormattingService extends AccessibilityService {
    private static final long DEBOUNCE_MS = 850L;
    private static final long SESSION_TIMEOUT_MS = 20000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean waitingForGoogleVoiceText = false;
    private String baselineText = null;
    private String expectedText = null;
    private String targetPackage = null;
    private int oldSelectionStart = -1;
    private int oldSelectionEnd = -1;
    private Runnable pendingFormat = null;
    private final Runnable clearSession = this::resetSession;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                | AccessibilityEvent.TYPE_VIEW_CLICKED
                | AccessibilityEvent.TYPE_VIEW_FOCUSED
                | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String eventPackage = event.getPackageName().toString();
        int eventType = event.getEventType();

        if (isGoogleVoicePackage(eventPackage)
                && (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    || eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    || eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                    || isGoogleVoiceWindowEvent(event, eventPackage))) {
            // Keep the most recent target-field snapshot captured from focus/click/text events.
            // Some Google speech versions do not expose the target field's focus change.
            waitingForGoogleVoiceText = true;
            expectedText = null;
            if (baselineText == null || targetPackage == null) {
                oldSelectionStart = -1;
                oldSelectionEnd = -1;
            }
            handler.removeCallbacks(clearSession);
            handler.postDelayed(clearSession, SESSION_TIMEOUT_MS);
            return;
        }

        if (isGoogleVoicePackage(eventPackage)) return;

        boolean isTargetSnapshotEvent =
                eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
                || eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                || eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        if (!isTargetSnapshotEvent) return;
        if (waitingForGoogleVoiceText && eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            return;
        }

        AccessibilityNodeInfo source = event.getSource();
        if (source == null) return;
        try {
            if (!source.isEditable() || source.isPassword() || source.getText() == null) return;

            if (!waitingForGoogleVoiceText) {
                // Track the latest focused editable field and its current text. This provides a
                // baseline even when the speech UI never emits a useful focus event of its own.
                baselineText = source.getText().toString();
                targetPackage = eventPackage;
                expectedText = null;
                oldSelectionStart = source.getTextSelectionStart();
                oldSelectionEnd = source.getTextSelectionEnd();
                return;
            }

            if (baselineText == null) {
                CharSequence before = event.getBeforeText();
                if (before != null) {
                    baselineText = before.toString();
                } else {
                    // Some Android/editor combinations omit beforeText. For a pure insertion,
                    // reconstruct the prior value from the current text and event indices.
                    // Do not guess when existing text was replaced or removed.
                    CharSequence currentSequence = source.getText();
                    int from = event.getFromIndex();
                    int added = event.getAddedCount();
                    if (currentSequence == null || event.getRemovedCount() != 0
                            || added <= 0 || from < 0
                            || from + added > currentSequence.length()) {
                        return;
                    }
                    String currentValue = currentSequence.toString();
                    baselineText = currentValue.substring(0, from)
                            + currentValue.substring(from + added);
                }
                targetPackage = eventPackage;
            }

            if (!eventPackage.equals(targetPackage)) return;
            expectedText = source.getText().toString();
            oldSelectionStart = source.getTextSelectionStart();
            oldSelectionEnd = source.getTextSelectionEnd();

            if (pendingFormat != null) handler.removeCallbacks(pendingFormat);
            pendingFormat = this::applyPendingFormat;
            handler.postDelayed(pendingFormat, DEBOUNCE_MS);
        } finally {
            source.recycle();
        }
    }

    private void applyPendingFormat() {
        pendingFormat = null;
        if (!waitingForGoogleVoiceText || baselineText == null
                || expectedText == null || targetPackage == null) {
            resetSession();
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            resetSession();
            return;
        }

        AccessibilityNodeInfo field = null;
        try {
            field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (field == null || !field.isEditable()) {
                resetSession();
                return;
            }
            if (field.isPassword() || field.getText() == null
                    || field.getPackageName() == null
                    || !targetPackage.contentEquals(field.getPackageName())) {
                resetSession();
                return;
            }

            String current = field.getText().toString();
            // Never overwrite text if the user or app changed it after the voice result.
            if (!current.equals(expectedText)) {
                resetSession();
                return;
            }

            int prefix = 0;
            int maxPrefix = Math.min(baselineText.length(), current.length());
            while (prefix < maxPrefix && baselineText.charAt(prefix) == current.charAt(prefix)) {
                prefix++;
            }

            int suffix = 0;
            int maxSuffix = Math.min(baselineText.length() - prefix, current.length() - prefix);
            while (suffix < maxSuffix
                    && baselineText.charAt(baselineText.length() - 1 - suffix)
                    == current.charAt(current.length() - 1 - suffix)) {
                suffix++;
            }

            int changedEnd = current.length() - suffix;
            if (prefix > changedEnd) {
                resetSession();
                return;
            }

            String changed = current.substring(prefix, changedEnd);
            String formatted = HaamidApostropheOverrides.apply(
                    changed, HaamidFormatter.formatAutomatic(changed));
            if (!formatted.equals(changed)) {
                String replacement = current.substring(0, prefix) + formatted
                        + current.substring(changedEnd);

                Bundle setTextArgs = new Bundle();
                setTextArgs.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replacement);
                boolean changedSuccessfully = field.performAction(
                        AccessibilityNodeInfo.ACTION_SET_TEXT, setTextArgs);

                if (changedSuccessfully) {
                    int delta = formatted.length() - changed.length();
                    int start = mapSelection(oldSelectionStart, prefix, changedEnd, formatted.length(), delta);
                    int end = mapSelection(oldSelectionEnd, prefix, changedEnd, formatted.length(), delta);
                    if (start >= 0 && end >= 0) {
                        Bundle selectionArgs = new Bundle();
                        selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                                Math.min(start, replacement.length()));
                        selectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                                Math.min(end, replacement.length()));
                        field.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs);
                    }
                }
            }
        } finally {
            if (field != null) field.recycle();
            root.recycle();
            resetSession();
        }
    }

    private static int mapSelection(int position, int changedStart, int changedEnd,
                                    int formattedLength, int delta) {
        if (position < 0) return -1;
        if (position <= changedStart) return position;
        if (position >= changedEnd) return position + delta;
        return changedStart + formattedLength;
    }

    private static boolean isGoogleVoicePackage(String packageName) {
        return "com.google.android.googlequicksearchbox".equals(packageName)
                || "com.google.android.voicesearch".equals(packageName)
                || packageName.startsWith("com.google.android.apps.gsa");
    }

    private static boolean isGoogleVoiceWindowEvent(AccessibilityEvent event, String packageName) {
        if (!isGoogleVoicePackage(packageName)) return false;

        String className = event.getClassName() == null
                ? "" : event.getClassName().toString().toLowerCase(java.util.Locale.ROOT);
        if (className.contains("voice") || className.contains("speech")
                || className.contains("recognition") || className.contains("dictation")) {
            return true;
        }

        // Some Google builds expose the voice prompt as event text rather than a useful class name.
        for (CharSequence item : event.getText()) {
            if (item == null) continue;
            String text = item.toString().toLowerCase(java.util.Locale.ROOT);
            if (text.contains("listening") || text.contains("speak now")
                    || text.contains("try saying") || text.contains("voice typing")) {
                return true;
            }
        }
        return false;
    }

    private void resetSession() {
        handler.removeCallbacks(clearSession);
        if (pendingFormat != null) handler.removeCallbacks(pendingFormat);
        pendingFormat = null;
        waitingForGoogleVoiceText = false;
        baselineText = null;
        expectedText = null;
        targetPackage = null;
        oldSelectionStart = -1;
        oldSelectionEnd = -1;
    }

    @Override
    public void onInterrupt() {
        resetSession();
    }

    @Override
    public void onDestroy() {
        resetSession();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
