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

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceDialogFragmentCompat;

import android.widget.SeekBar;
import android.widget.ImageView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.yakovlevegor.DroidRec.R;

public class PanelOpacityDialogFragment extends PreferenceDialogFragmentCompat {

    public static final String SAVE_STATE_VALUE = "PanelOpacityDialogFragment.value";

    private String keyName;

    private SharedPreferences appSettings;

    private SharedPreferences.Editor appSettingsEditor;

    private int opacityScale = 9;

    public static PanelOpacityDialogFragment newInstance(String key) {
        final PanelOpacityDialogFragment fragment = new PanelOpacityDialogFragment();
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    public void setKeyName(String key) {
        keyName = key;
    }

    private void updateHint(int value, ImageView hint) {
        float opacityValue = (value+1)*0.1f;
        hint.setAlpha(opacityValue);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appSettings = getContext().getSharedPreferences(ScreenRecorder.prefsident, 0);
        appSettingsEditor = appSettings.edit();
    }

    @Override
    public void onBindDialogView(View view) {
        opacityScale = appSettings.getInt("floatingcontrolsopacity", 9);
        ImageView opacityHint = (ImageView) view.findViewById(R.id.opacity_handle);

        String darkTheme = appSettings.getString("darkthemeapplied", getContext().getResources().getString(R.string.dark_theme_option_auto));

        if (((getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES && darkTheme.contentEquals(getContext().getResources().getString(R.string.dark_theme_option_auto))) || darkTheme.contentEquals("Dark")) {
            opacityHint.setImageDrawable(getContext().getResources().getDrawable(R.drawable.floatingpanel_shape_dark, getContext().getTheme()));
        }

        SeekBar opacityBar = (SeekBar) view.findViewById(R.id.opacity_seek);

        updateHint(opacityScale, opacityHint);
        opacityBar.setProgress(opacityScale);

        SeekBar.OnSeekBarChangeListener opacityValueListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                opacityScale = progress;
                updateHint(progress, opacityHint);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        opacityBar.setOnSeekBarChangeListener(opacityValueListener);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            appSettingsEditor.putInt("floatingcontrolsopacity", opacityScale);
            appSettingsEditor.commit();
        }
    }
}
