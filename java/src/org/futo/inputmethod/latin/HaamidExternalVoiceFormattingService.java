package org.futo.inputmethod.latin;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
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
                    || eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
                    || isGoogleVoiceWindowEvent(event, eventPackage))) {
            // Preserve the last editable-field snapshot: Google often exposes its speech UI
            // separately from the app whose text is being edited.
            if (!waitingForGoogleVoiceText) {
                waitingForGoogleVoiceText = true;
                expectedText = null;
            }
            // Repeated speech-window events must not cancel a text-change debounce that is
            // already waiting to format a recognized result.
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

            String current = source.getText().toString();
            if (!waitingForGoogleVoiceText && eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                // Snapshot the focused field before a possible speech insertion.
                baselineText = current;
                targetPackage = eventPackage;
                expectedText = null;
                oldSelectionStart = source.getTextSelectionStart();
                oldSelectionEnd = source.getTextSelectionEnd();
                return;
            }

            if (!waitingForGoogleVoiceText) {
                // Do not infer voice typing from a multi-character insertion: that would also
                // trigger on paste or other app-driven edits. Only a recognized Google voice UI
                // event may arm this service for automatic formatting.
                baselineText = current;
                targetPackage = eventPackage;
                expectedText = null;
                oldSelectionStart = source.getTextSelectionStart();
                oldSelectionEnd = source.getTextSelectionEnd();
                return;
            } else if (baselineText == null) {
                CharSequence before = event.getBeforeText();
                if (before != null) {
                    baselineText = before.toString();
                } else {
                    int from = event.getFromIndex();
                    int added = event.getAddedCount();
                    if (event.getRemovedCount() != 0 || added <= 0 || from < 0
                            || from + added > current.length()) {
                        return;
                    }
                    baselineText = current.substring(0, from)
                            + current.substring(from + added);
                }
                targetPackage = eventPackage;
            }

            if (targetPackage == null || !eventPackage.equals(targetPackage)) return;
            expectedText = current;
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

        AccessibilityNodeInfo field = findTargetInputField();
        if (field == null) {
            resetSession();
            return;
        }

        try {
            String current = field.getText().toString();
            // Never overwrite text if the user or app changed it after the insertion event.
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
            field.recycle();
            resetSession();
        }
    }

    /**
     * Google voice UIs may occupy the active window while the real editable field remains in
     * another accessible window. Search all windows for the expected focused field.
     */
    private AccessibilityNodeInfo findTargetInputField() {
        AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
        if (activeRoot != null) {
            AccessibilityNodeInfo field = findTargetFieldInRoot(activeRoot);
            activeRoot.recycle();
            if (field != null) return field;
        }

        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return null;
        for (AccessibilityWindowInfo window : windows) {
            if (window == null) continue;
            AccessibilityNodeInfo root = window.getRoot();
            AccessibilityNodeInfo field = null;
            if (root != null) {
                field = findTargetFieldInRoot(root);
                root.recycle();
            }
            window.recycle();
            if (field != null) return field;
        }
        return null;
    }

    private AccessibilityNodeInfo findTargetFieldInRoot(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (isExpectedTargetField(focused)) return focused;
        if (focused != null) focused.recycle();
        return findFocusedEditableDescendant(root);
    }

    private AccessibilityNodeInfo findFocusedEditableDescendant(AccessibilityNodeInfo node) {
        if (isExpectedTargetField(node) && node.isFocused()) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            AccessibilityNodeInfo found = findFocusedEditableDescendant(child);
            child.recycle();
            if (found != null) return found;
        }
        return null;
    }

    private boolean isExpectedTargetField(AccessibilityNodeInfo node) {
        if (node == null || !node.isEditable() || node.isPassword() || node.getText() == null
                || node.getPackageName() == null || targetPackage == null) {
            return false;
        }
        return targetPackage.contentEquals(node.getPackageName())
                && expectedText != null && expectedText.contentEquals(node.getText());
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
