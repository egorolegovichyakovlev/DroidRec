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
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.File;

import com.yakovlevegor.DroidRec.shake.event.OnShakePreferenceChangeEvent;

import org.greenrobot.eventbus.EventBus;

public class SettingsPanel extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private SharedPreferences appSettings;

    private SharedPreferences.Editor appSettingsEditor;

    private SettingsFragment settingsPanel;

    private Preference videoFolderPreference;

    private Preference audioFolderPreference;

    private AlertDialog dialog;

    private static final float BPP = 0.25f;

    @Override
    @SuppressWarnings("deprecation")
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        getSupportFragmentManager().beginTransaction().replace(R.id.settings, fragment).addToBackStack(null).commit();
        return true;
    }

    @Override
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

        setContentView(R.layout.settings_panel);
        if (savedInstanceState == null) {
            settingsPanel = new SettingsFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.settings, settingsPanel).commit();
        } else {
            settingsPanel = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings);
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        addEventListenerOnShakeEvent();

        videoFolderPreference = settingsPanel.findPreference("folderpathpref");

        audioFolderPreference = settingsPanel.findPreference("folderaudiopathpref");

        videoFolderPreference.setSummary(getRealPath(appSettings.getString("folderpath", "None")));

        audioFolderPreference.setSummary(getRealPath(appSettings.getString("folderaudiopath", "None")));

        Preference.OnPreferenceClickListener listenerVideoFolder = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                chooseDir(false);

                return true;
            }
        };

        videoFolderPreference.setOnPreferenceClickListener(listenerVideoFolder);

        Preference.OnPreferenceClickListener listenerAudioFolder = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                chooseDir(true);

                return true;
            }
        };

        audioFolderPreference.setOnPreferenceClickListener(listenerAudioFolder);

        Preference overlayPreference = settingsPanel.findPreference("floatingcontrols");

        Preference overlayPreferencePosition = settingsPanel.findPreference("floatingcontrolsposition");

        Preference overlayPreferenceSize = settingsPanel.findPreference("floatingcontrolssize");

        Preference overlayPreferenceOpacity = settingsPanel.findPreference("floatingcontrolsopacity");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            Preference.OnPreferenceChangeListener listenerPanel = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean newState = (boolean) newValue;

                    if (Settings.canDrawOverlays(getApplicationContext()) == true) {
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

            Preference.OnPreferenceClickListener listenerPanelClick = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    if (Settings.canDrawOverlays(SettingsPanel.this) == true) {
                        return false;
                    } else {
                        requestOverlayDisplayPermission();
                    }

                    return true;
                }
            };

            overlayPreferencePosition.setOnPreferenceClickListener(listenerPanelClick);

            overlayPreferenceSize.setOnPreferenceClickListener(listenerPanelClick);

            overlayPreferenceOpacity.setOnPreferenceClickListener(listenerPanelClick);

        }

        Preference bitrateCheckPreference = settingsPanel.findPreference("custombitrate");

        EditTextPreference bitrateValuePreference = (EditTextPreference) settingsPanel.findPreference("bitratevalue");

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



        Preference sampleRateCheckPreference = settingsPanel.findPreference("customsamplerate");

        EditTextPreference sampleRateValuePreference = (EditTextPreference) settingsPanel.findPreference("sampleratevalue");

        Preference.OnPreferenceChangeListener listenerSampleRate = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean newState = (boolean) newValue;

                if (newState == true) {
                    sampleRateValuePreference.setText("44100");
                }

                return true;
            }
        };

        sampleRateCheckPreference.setOnPreferenceChangeListener(listenerSampleRate);

        Preference themePreference = settingsPanel.findPreference("darktheme");

        Preference.OnPreferenceChangeListener listenerThemeChange = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Toast.makeText(SettingsPanel.this, R.string.dark_theme_change_notice, Toast.LENGTH_SHORT).show();
                return true;
            }
        };

        themePreference.setOnPreferenceChangeListener(listenerThemeChange);

    }

    private String getRealPath(String fullPath) {

        String filetreepattern = "^content://com\\.android\\.externalstorage\\.documents/tree/.*";

        String providertree = "^content://[^/]*/tree/";

        String documentspath = fullPath.replaceFirst(providertree, "");

        String outPath = fullPath;

        if (fullPath.matches(filetreepattern)) {

            if (documentspath.startsWith("primary%3A")) {
                outPath = "/storage/emulated/0/" + Uri.decode(documentspath.replaceFirst("primary%3A", "")) + "/";
            } else {
                outPath = "/storage/" + Uri.decode(documentspath.replaceFirst("%3A", "/")) + "/";

                Uri filefulluri = Uri.parse(outPath);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    File dirTest = new File(filefulluri.toString());
                    if (dirTest.isDirectory() == false) {
                        outPath = "/storage/sdcard" + Uri.decode(documentspath.replaceFirst(".*\\%3A", "/")) + "/";
                    }
                }
            }

        }

        return outPath;
    }

    void chooseDir(boolean isAudio) {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        if (isAudio == true) {
            requestAudioFolderPermission.launch(intent);
        } else {
            requestFolderPermission.launch(intent);
        }

    }

    private final ActivityResultLauncher<Intent> requestFolderPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result != null) {

                requestFolder(result.getResultCode(), result.getData().getData(), false);

            }
        }
    });

    private final ActivityResultLauncher<Intent> requestAudioFolderPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result != null) {

                requestFolder(result.getResultCode(), result.getData().getData(), true);

            }
        }
    });

    private void requestFolder(int resultCode, Uri extrauri, boolean isAudio) {
        if (resultCode == RESULT_OK) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

                getContentResolver().takePersistableUriPermission(extrauri, takeFlags);
            }

            if (isAudio == true) {
                appSettingsEditor.putString("folderaudiopath", extrauri.toString());
            } else {
                appSettingsEditor.putString("folderpath", extrauri.toString());
            }

            appSettingsEditor.commit();

            if (isAudio == true) {
                audioFolderPreference.setSummary(getRealPath(appSettings.getString("folderaudiopath", "None")));
            } else {
                videoFolderPreference.setSummary(getRealPath(appSettings.getString("folderpath", "None")));
            }

        } else {

            if (isAudio == true) {
                if (appSettings.getString("folderaudiopath", "NULL") == "NULL") {
                    Toast.makeText(this, R.string.error_storage_select_folder, Toast.LENGTH_SHORT).show();
                }
            } else {
                if (appSettings.getString("folderpath", "NULL") == "NULL") {
                    Toast.makeText(this, R.string.error_storage_select_folder, Toast.LENGTH_SHORT).show();
                }
            }

        }

    }

    private String getBitrateDefault() {

        float screenWidthDp = getResources().getConfiguration().screenWidthDp;

        float screenHeightDp = getResources().getConfiguration().screenHeightDp;

        float screenDensity = getResources().getConfiguration().densityDpi;


        int pixelWidth = (int) (screenWidthDp * screenDensity + 0.5f);
        int pixelHeight = (int) (screenHeightDp * screenDensity + 0.5f);

        boolean customQuality = appSettings.getBoolean("customquality", false);

        float qualityScale = 0.1f * (appSettings.getInt("qualityscale", 9)+1);


        boolean customFPS = appSettings.getBoolean("customfps", false);

        int fpsValue = Integer.parseInt(appSettings.getString("fpsvalue", "30"));


        Integer recordingBitrate = (int)(BPP*fpsValue*pixelWidth*pixelHeight);

        if (customQuality == true) {
            recordingBitrate = (int)(recordingBitrate*qualityScale);
        }

        return recordingBitrate.toString();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void requestOverlayDisplayPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(R.string.overlay_notice_title);
        builder.setMessage(R.string.overlay_notice_description);
        builder.setPositiveButton(R.string.overlay_notice_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + MainActivity.appName));
                startActivity(intent);
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    private void addEventListenerOnShakeEvent() {
        Preference onShake = settingsPanel.findPreference("onshake");
        onShake.setOnPreferenceChangeListener((preference, newValue) -> {
            EventBus.getDefault().post(new OnShakePreferenceChangeEvent(newValue.toString()));
            return true;
        });
    }
}
