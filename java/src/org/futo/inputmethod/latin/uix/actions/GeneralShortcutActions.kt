package org.futo.inputmethod.latin.uix.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.view.KeyEvent
import android.widget.Toast
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.Action

val SelectAllAction = Action(
    icon = R.drawable.maximize,
    name = R.string.action_select_all_title,
    simplePressImpl = { manager, _ ->
        manager.sendKeyEvent(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON)
    },
    windowImpl = null,
)

val CutAction = Action(
    icon = R.drawable.scissors,
    name = R.string.action_cut_title,
    simplePressImpl = { manager, _ ->
        manager.copyToClipboard(cut = true)
    },
    windowImpl = null,
)

val CopyAction = Action(
    icon = R.drawable.copy,
    name = R.string.action_copy_title,
    simplePressImpl = { manager, _ ->
        manager.copyToClipboard(cut = false)
    },
    windowImpl = null,
)

val PasteAction = Action(
    icon = R.drawable.clipboard,
    name = R.string.action_paste_title,
    simplePressImpl = { manager, _ ->
        // Pasting should not format text immediately. Automatic Title Case runs only
        // when the user finishes through a supported editor action.
        manager.pasteFromClipboard()
    },
    windowImpl = null,
)

/**
 * Applies Haamid's strict title-case formatter to the text selected in the active editor.
 * This action intentionally uses the active InputConnection so it works with the user's
 * selection without changing FUTO's voice-input implementation.
 */
val HaamidFormatterAction = Action(
    icon = R.drawable.edit_text,
    name = R.string.action_haamid_formatter_title,
    simplePressImpl = { manager, _ ->
        val connection = manager.getLatinIMEForDebug().currentInputConnection
        val context = manager.getContext()
        if (connection == null) {
            manager.announce(context.getString(R.string.action_haamid_formatter_select_text))
        } else {
            val extracted = connection.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
            val original = extracted?.text?.toString()
            // Refuse partial editor snapshots: replacing one would risk deleting unseen text.
            if (original.isNullOrEmpty() || extracted.startOffset != 0 ||
                extracted.partialStartOffset != -1 || extracted.partialEndOffset != -1) {
                manager.announce(context.getString(R.string.action_haamid_formatter_select_text))
            } else {
                val formatted = org.futo.inputmethod.latin.HaamidFormatter.format(original)
                if (formatted != original) {
                    connection.beginBatchEdit()
                    try {
                        connection.setSelection(0, original.length)
                        connection.commitText(formatted, 1)
                    } finally {
                        connection.endBatchEdit()
                    }
                }
                manager.announce(context.getString(R.string.action_haamid_formatter_done))
            }
        }
    },
    windowImpl = null,
)

/** Toggles automatic formatting for this app session; manual formatting remains available. */
val HaamidAutoFormatToggleAction = Action(
    icon = R.drawable.edit_text,
    name = R.string.action_haamid_auto_format_toggle,
    simplePressImpl = { manager, _ ->
        val enabled = org.futo.inputmethod.latin.HaamidFormattingSettings.toggleAutomaticFormatting(manager.getContext())
        val context = manager.getContext()
        val message = if (enabled) R.string.action_haamid_auto_format_enabled else R.string.action_haamid_auto_format_disabled
        val statusText = context.getString(message)
        manager.announce(statusText)
        Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
    },
    windowImpl = null,
)
