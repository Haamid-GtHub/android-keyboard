package org.futo.inputmethod.latin;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Applies Haamid Title Case from Android's text-selection menu, and supports
 * the system Share sheet as a fallback on apps that do not expose PROCESS_TEXT.
 */
public final class HaamidProcessTextActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent incoming = getIntent();

        if (Intent.ACTION_SEND.equals(incoming.getAction())) {
            CharSequence shared = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (shared == null) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            String formatted = HaamidFormatter.format(shared.toString());
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.action_haamid_formatter_title), formatted));
                Toast.makeText(this, R.string.action_haamid_formatter_done, Toast.LENGTH_SHORT).show();
            }
            finish();
            return;
        }

        CharSequence selected = incoming.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        if (selected == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        String formatted = HaamidFormatter.format(selected.toString());
        Intent result = new Intent();
        result.putExtra(Intent.EXTRA_PROCESS_TEXT, formatted);
        setResult(RESULT_OK, result);
        finish();
    }
}
