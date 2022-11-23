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

import com.yakovlevegor.DroidRec.R;

public class SettingsPanel extends PreferenceActivity {

    private SharedPreferences appSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appSettings = getSharedPreferences(ScreenRecorder.prefsident, 0);

        String darkTheme = appSettings.getString("darktheme", "Automatic");

        if (((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES && darkTheme.contentEquals("Automatic")) || darkTheme.contentEquals("Dark")) {
            setTheme(android.R.style.Theme_Material);
            setDarkColoringForViewGroup((ViewGroup) getWindow().getDecorView(), getResources().getColor(R.color.colorDarkBackground), getResources().getColor(R.color.colorDarkText), true);
            getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorDarkBackground)));
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(ScreenRecorder.prefsident);

        addPreferencesFromResource(R.xml.settings);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Preference overlayPreference = (Preference) findPreference("floatingcontrols");
            PreferenceCategory overlayPreferenceCategory = (PreferenceCategory) findPreference("controlssettings");
            overlayPreferenceCategory.removePreference(overlayPreference);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Preference codecPreference = (Preference) findPreference("codecvalue");
            PreferenceCategory codecPreferenceCategory = (PreferenceCategory) findPreference("capturesettings");
            codecPreferenceCategory.removePreference(codecPreference);
        }

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
