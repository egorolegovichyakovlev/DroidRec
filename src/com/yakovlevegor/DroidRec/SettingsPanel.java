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

import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.EditTextPreference;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toolbar;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.view.Window;
import android.content.SharedPreferences;
import android.view.WindowManager;
import android.provider.Settings;
import android.app.AlertDialog;
import android.net.Uri;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.content.Context;

import com.yakovlevegor.DroidRec.R;

public class SettingsPanel extends PreferenceActivity {

    private AlertDialog dialog;

    private SharedPreferences appSettings;

    private static final float BPP = 0.25f;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appSettings = getSharedPreferences(ScreenRecorder.prefsident, 0);

        String darkTheme = appSettings.getString("darktheme", "Automatic");

        if (((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES && darkTheme.contentEquals("Automatic")) || darkTheme.contentEquals("Dark")) {
            setTheme(android.R.style.Theme_Material);
            setDarkColoringForViewGroup((ViewGroup) getWindow().getDecorView(), getResources().getColor(R.color.colorDarkBackground), getResources().getColor(R.color.colorDarkText), true);
            getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorDarkBackground)));
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
        }

        PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(ScreenRecorder.prefsident);

        addPreferencesFromResource(R.xml.settings);

        Preference overlayPreference = (Preference) findPreference("floatingcontrols");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            PreferenceCategory overlayPreferenceCategory = (PreferenceCategory) findPreference("controlssettings");
            overlayPreferenceCategory.removePreference(overlayPreference);
        } else {
            Preference.OnPreferenceChangeListener listenerPanel = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean newState = (boolean) newValue;

                    if (Settings.canDrawOverlays(SettingsPanel.this) == true) {
                        return true;
                    } else {
                        requestOverlayDisplayPermission();
                        if (newState == false) {
                            return true;
                        }
                    }

                    return false;
                }
            };

            overlayPreference.setOnPreferenceChangeListener(listenerPanel);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Preference codecPreference = (Preference) findPreference("codecvalue");
            PreferenceCategory codecPreferenceCategory = (PreferenceCategory) findPreference("capturesettings");
            codecPreferenceCategory.removePreference(codecPreference);
        }

        Preference bitrateCheckPreference = (Preference) findPreference("custombitrate");

        EditTextPreference bitrateValuePreference = (EditTextPreference) findPreference("bitratevalue");

        Preference.OnPreferenceChangeListener listenerBitrate = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean newState = (boolean) newValue;

                if (newState == true) {
                    bitrateValuePreference.setDefaultValue(getBitrateDefault());

                    int bitrateValue = Integer.parseInt(appSettings.getString("bitratevalue", "0"));

                    bitrateValuePreference.setText(getBitrateDefault());
                }

                return true;
            }
        };

        bitrateCheckPreference.setOnPreferenceChangeListener(listenerBitrate);
    }

    private String getBitrateDefault() {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(metrics);

        boolean customQuality = appSettings.getBoolean("customquality", false);

        float qualityScale = 0.1f * (appSettings.getInt("qualityscale", 9)+1);


        boolean customFPS = appSettings.getBoolean("customfps", false);

        int fpsValue = Integer.parseInt(appSettings.getString("fpsvalue", "30"));


        Integer recordingBitrate = (int)(BPP*fpsValue*metrics.widthPixels*metrics.heightPixels);

        if (customQuality == true) {
            recordingBitrate = (int)(recordingBitrate*qualityScale);
        }

        return recordingBitrate.toString();
    }

    private void requestOverlayDisplayPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(R.string.overlay_notice_title);
        builder.setMessage(R.string.overlay_notice_description);
        builder.setPositiveButton(R.string.overlay_notice_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + MainActivity.appName));
                startActivityForResult(intent, RESULT_OK);
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    private static void setDarkColoringForViewGroup(ViewGroup viewGroup, int color, int colorLight, boolean parent) {
        for (int i = 0; i < viewGroup.getChildCount(); i++)
        {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup && !(child instanceof Toolbar)) {
                setDarkColoringForViewGroup((ViewGroup)child, color, colorLight, false);
            } else if (child instanceof Toolbar) {
                Toolbar childbar = (Toolbar)child;
                childbar.getNavigationIcon().setColorFilter(colorLight, PorterDuff.Mode.SRC_ATOP);
                childbar.getOverflowIcon().setColorFilter(colorLight, PorterDuff.Mode.SRC_ATOP);
                childbar.setTitleTextColor(colorLight);
            }
            child.setBackgroundColor(color);
        }
        viewGroup.setBackgroundColor(color);
    }

}
