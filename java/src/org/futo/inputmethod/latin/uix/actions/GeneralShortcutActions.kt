package org.futo.inputmethod.latin.uix.actions

import android.view.KeyEvent
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
        val selected = connection?.getSelectedText(0)
        if (selected.isNullOrEmpty()) {
            manager.announce(manager.getContext().getString(R.string.action_haamid_formatter_select_text))
        } else {
            connection.commitText(org.futo.inputmethod.latin.HaamidFormatter.format(selected.toString()), 1)
            manager.announce(manager.getContext().getString(R.string.action_haamid_formatter_done))
        }
    },
    windowImpl = null,
)
