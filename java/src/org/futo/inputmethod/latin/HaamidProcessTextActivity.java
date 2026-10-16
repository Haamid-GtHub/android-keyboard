package org.futo.inputmethod.latin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Applies Haamid Title Case through Android's text-selection PROCESS_TEXT action. */
public final class HaamidProcessTextActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent incoming = getIntent();

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
