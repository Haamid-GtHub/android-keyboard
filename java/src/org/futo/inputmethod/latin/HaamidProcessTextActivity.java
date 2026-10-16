package org.futo.inputmethod.latin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Lets Android's standard text-selection menu apply Haamid Title Case in any app
 * that supports ACTION_PROCESS_TEXT.
 */
public final class HaamidProcessTextActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent incoming = getIntent();
        CharSequence selected = incoming.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        if (selected == null) {
            Toast.makeText(this, R.string.action_haamid_formatter_select_text, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        String original = selected.toString();
        String formatted = HaamidFormatter.format(original);
        Intent result = new Intent();
        result.putExtra(Intent.EXTRA_PROCESS_TEXT, formatted);
        setResult(RESULT_OK, result);
        finish();
    }
}
