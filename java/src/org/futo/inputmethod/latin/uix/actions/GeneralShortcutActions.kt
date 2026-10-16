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
        if (org.futo.inputmethod.latin.HaamidFormattingSettings.isAutomaticFormattingEnabled()) {
            val context = manager.getContext()
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            val clip = clipboard?.primaryClip
            val item = if (clip != null && clip.itemCount > 0) clip.getItemAt(0) else null
            val text = item?.coerceToText(context)?.toString()
            val connection = manager.getLatinIMEForDebug().currentInputConnection
            if (!text.isNullOrEmpty() && connection != null) {
                connection.commitText(org.futo.inputmethod.latin.HaamidFormatter.format(text), 1)
            } else {
                manager.pasteFromClipboard()
            }
        } else {
            manager.pasteFromClipboard()
        }
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
        val selected = connection?.getSelectedText(0)
        if (connection == null || selected.isNullOrEmpty()) {
            manager.announce(manager.getContext().getString(R.string.action_haamid_formatter_select_text))
        } else {
            connection.commitText(org.futo.inputmethod.latin.HaamidFormatter.format(selected.toString()), 1)
            manager.announce(manager.getContext().getString(R.string.action_haamid_formatter_done))
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
