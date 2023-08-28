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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;

import androidx.fragment.app.Fragment;

import java.io.File;

public class AppInfo extends AppCompatActivity {

    private SharedPreferences appSettings;

    private SharedPreferences.Editor appSettingsEditor;

    private NestedScrollView licenseScroll;

    private Button licenseButton;

    protected RecyclerView contributorsView;

    protected ContributorsAdapter contributorsAdapter;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appSettings = getSharedPreferences(ScreenRecorder.prefsident, 0);

        appSettingsEditor = appSettings.edit();

        String darkTheme = appSettings.getString("darkthemeapplied", getResources().getString(R.string.dark_theme_option_auto));


        if (((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES && darkTheme.contentEquals(getResources().getString(R.string.dark_theme_option_auto))) || darkTheme.contentEquals("Dark")) {
            setTheme(R.style.Theme_AppCompat);
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
        }

        setContentView(R.layout.about);

        licenseScroll = (NestedScrollView) findViewById(R.id.mainscroll);

        licenseButton = (Button) findViewById(R.id.showlicense);

        licenseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Intent showlicense = new Intent(AppInfo.this, LicenseScreen.class);
                startActivity(showlicense);
            }
        });

        contributorsView = (RecyclerView) findViewById(R.id.contributorslist);

        contributorsAdapter = new ContributorsAdapter(getBaseContext());

        contributorsView.setAdapter(contributorsAdapter);

        contributorsView.post(new Runnable() {
            @Override
            public void run() {
                 licenseScroll.scrollTo(0,0);
            }
        });

        contributorsView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        try {
            PackageManager manager = getPackageManager();
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);

            TextView appVersion = (TextView) findViewById(R.id.appversionnum);
            appVersion.setText(info.versionName);
        } catch (PackageManager.NameNotFoundException e) {}

    }

    @Override
    public void onStart() {
        super.onStart();

    }

}
