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

import androidx.preference.ListPreference;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.hardware.display.DisplayManager;
import android.view.Surface;

import java.util.ArrayList;

public class ResolutionList extends ListPreference {

    private ArrayList<String> resolutionsList = new ArrayList<String>();

    @SuppressWarnings("deprecation")
    private void getResolutions(Context context) {
        Display display = ((DisplayManager)(context.getSystemService(Context.DISPLAY_SERVICE))).getDisplay(Display.DEFAULT_DISPLAY);

        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);

        int orientationOnStart = display.getRotation();

        float screenDensity = metrics.densityDpi;

        int screenWidthNormal = 0;

        int screenHeightNormal = 0;

        if (orientationOnStart == Surface.ROTATION_90 || orientationOnStart == Surface.ROTATION_270) {
            screenWidthNormal = metrics.heightPixels;
            screenHeightNormal = metrics.widthPixels;
        } else {
            screenWidthNormal = metrics.widthPixels;
            screenHeightNormal = metrics.heightPixels;
        }

        int measureResolution = screenHeightNormal;

        if (screenHeightNormal > screenWidthNormal) {
            measureResolution = screenWidthNormal;
        }

        if (measureResolution == 2160) {
            resolutionsList.add("2160p");
        }

        if (measureResolution >= 1080) {
            resolutionsList.add("1080p");
        }

        if (measureResolution >= 720) {
            resolutionsList.add("720p");
        }

        if (measureResolution >= 480) {
            resolutionsList.add("480p");
        }

        if (measureResolution >= 360) {
            resolutionsList.add("360p");
        }
    }

    public ResolutionList(Context context, AttributeSet attrs) {
        super(context, attrs);
        String autoEntry = context.getResources().getString(R.string.resolution_option_auto);
        String autoValue = context.getResources().getString(R.string.resolution_option_auto_value);

        getResolutions(context);
        String[] resolutionsAllEntries = new String[resolutionsList.size()+1];
        resolutionsAllEntries[0] = autoEntry;
        for (int i = 0; i < resolutionsList.size(); i++) {
            resolutionsAllEntries[i+1] = resolutionsList.get(i);
        }
        String[] resolutionsAllValues = new String[resolutionsList.size()+1];
        resolutionsAllValues[0] = autoValue;
        for (int i = 0; i < resolutionsList.size(); i++) {
            resolutionsAllValues[i+1] = resolutionsList.get(i);
        }
        setEntries(resolutionsAllEntries);
        setEntryValues(resolutionsAllValues);
        setDefaultValue(autoValue);
    }

}
