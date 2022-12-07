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

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;

import androidx.fragment.app.DialogFragment;

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

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(ScreenRecorder.prefsident);

        setPreferencesFromResource(R.xml.settings, rootKey);

        Preference overlayPreference = findPreference("floatingcontrols");

        Preference overlayPreferencePosition = findPreference("floatingcontrolsposition");

        Preference overlayPreferenceSize = findPreference("floatingcontrolssize");

        Preference overlayPreferenceOpacity = findPreference("floatingcontrolsopacity");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            PreferenceCategory overlayPreferenceCategory = (PreferenceCategory) findPreference("controlssettings");
            overlayPreferenceCategory.removePreference(overlayPreference);
            overlayPreferenceCategory.removePreference(overlayPreferencePosition);
            overlayPreferenceCategory.removePreference(overlayPreferenceSize);
            overlayPreferenceCategory.removePreference(overlayPreferenceOpacity);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Preference codecPreference = findPreference("codecvalue");
            PreferenceCategory codecPreferenceCategory = (PreferenceCategory) findPreference("capturesettings");
            codecPreferenceCategory.removePreference(codecPreference);
        }

    }

    @Override
    @SuppressWarnings("deprecation")
/* Not yet implemented */
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference.getKey().contentEquals("qualityscale")) {
            QualityDialogFragment f = QualityDialogFragment.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.setKeyName("qualityscale");
            f.show(getFragmentManager(), null);
        } else if (preference.getKey().contentEquals("floatingcontrolsopacity")) {
            PanelOpacityDialogFragment f = PanelOpacityDialogFragment.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.setKeyName("floatingcontrolsopacity");
            f.show(getFragmentManager(), null);

        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

}
