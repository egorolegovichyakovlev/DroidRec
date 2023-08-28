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

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Window;
import android.content.SharedPreferences;
import android.view.WindowManager;
import android.provider.Settings;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.view.Surface;
import android.view.Display;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;

import java.io.InputStream;
import java.io.IOException;

import com.yakovlevegor.DroidRec.R;

public class LicenseScreen extends AppCompatActivity {

    private SharedPreferences appSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appSettings = getSharedPreferences(ScreenRecorder.prefsident, 0);

        String darkTheme = appSettings.getString("darkthemeapplied", getResources().getString(R.string.dark_theme_option_auto));

        if (((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES && darkTheme.contentEquals(getResources().getString(R.string.dark_theme_option_auto))) || darkTheme.contentEquals("Dark")) {
            setTheme(R.style.Theme_AppCompat);
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
        }

        setContentView(R.layout.license);

        TextView positionScreen = (TextView) findViewById(R.id.licensetext);

        try {

            InputStream licenseStream = getApplicationContext().getAssets().open("unlicense.txt");

            byte[] bytesRead = new byte[licenseStream.available()];

            licenseStream.read(bytesRead);

            String licenseText = new String(bytesRead);

            positionScreen.setText(licenseText);

            licenseStream.close();

        } catch (IOException e) {}

    }

}
