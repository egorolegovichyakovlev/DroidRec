/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
*/

package com.yakovlevegor.DroidRec;

import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;

import com.yakovlevegor.DroidRec.R;

public class QualityDialog extends DialogPreference {

    private int qualityScale = 9;

    public QualityDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            qualityScale = this.getPersistedInt(9);
        } else {
            persistInt(qualityScale);
        }
    }

    private void updateHint(int value, TextView hint) {
        if (value > 6) {
            hint.setText(R.string.quality_normal);
        } else if (value < 6 && value > 3) {
            hint.setText(R.string.quality_low);
        } else if (value < 3) {
            hint.setText(R.string.quality_lowest);
        }
    }

    @Override
    public void onBindDialogView(View view) {
        TextView qualityHint = (TextView) view.findViewById(R.id.quality_title);
        SeekBar qualityBar = (SeekBar) view.findViewById(R.id.quality_seek);

        updateHint(qualityScale, qualityHint);
        qualityBar.setProgress(qualityScale);

        SeekBar.OnSeekBarChangeListener qualityValueListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                qualityScale = progress;
                updateHint(progress, qualityHint);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        qualityBar.setOnSeekBarChangeListener(qualityValueListener);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            persistInt(qualityScale);
        }
    }

}
