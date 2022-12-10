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

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.app.AlertDialog;
import android.Manifest;
import android.content.Context;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.os.Build;
import android.annotation.TargetApi;
import android.os.Bundle;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.RadioButton;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Color;
import android.view.WindowManager;
import android.graphics.Rect;
import android.view.Display;
import android.view.Surface;
import android.hardware.display.DisplayManager;
import android.view.WindowInsets;

import com.yakovlevegor.DroidRec.R;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_READ = 57206;

    private static final int REQUEST_READ_RECORD = 57226;

    private static final int REQUEST_RECORD = 59706;

    private static final int REQUEST_MICROPHONE = 56808;

    private static final int REQUEST_MICROPHONE_PLAYBACK = 59465;

    private static final int REQUEST_MICROPHONE_RECORD = 58467;

    private static final int REQUEST_STORAGE = 58593;

    private static final int REQUEST_MODE_CHANGE = 58857;

    private ScreenRecorder.RecordingBinder recordingBinder;

    boolean screenRecorderStarted = false;

    private MediaProjectionManager activityProjectionManager;

    private SharedPreferences appSettings;

    private SharedPreferences.Editor appSettingsEditor;

    Button startRecording;

    Button pauseRecording;

    Button resumeRecording;

    Button stopRecording;

    Button chooseFolder;

    Button showSettings;

    CheckBox recMicrophone;

    CheckBox recPlayback;

    RadioButton recordVideoOption;

    RadioButton recordAudioOption;

    Chronometer timeCounter;

    TextView audioPlaybackUnavailable;

    Intent serviceIntent;

    public static String appName = "com.yakovlevegor.DroidRec";

    public static String ACTION_ACTIVITY_START_RECORDING = appName+".ACTIVITY_START_RECORDING";

    private boolean stateActivated = false;

    private boolean serviceToRecording = false;

    private AlertDialog dialog;

    private boolean recordModeChosen;

    public class ActivityBinder extends Binder {
        void recordingStart() {
            timeCounter.stop();
            timeCounter.setBase(recordingBinder.getTimeStart());
            timeCounter.start();
            startRecording.setVisibility(View.GONE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                resumeRecording.setVisibility(View.GONE);
                pauseRecording.setVisibility(View.VISIBLE);
            }

            stopRecording.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_stop_color_action_normal, 0, 0, 0);
            stopRecording.setVisibility(View.VISIBLE);
        }

        void recordingStop() {
            timeCounter.stop();
            timeCounter.setBase(SystemClock.elapsedRealtime());
            startRecording.setVisibility(View.VISIBLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                resumeRecording.setVisibility(View.GONE);
                pauseRecording.setVisibility(View.GONE);
            }

            stopRecording.setVisibility(View.GONE);
        }

        void recordingPause(long time) {
            timeCounter.setBase(SystemClock.elapsedRealtime()-time);
            timeCounter.stop();
            startRecording.setVisibility(View.GONE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                resumeRecording.setVisibility(View.VISIBLE);
                pauseRecording.setVisibility(View.GONE);
            }

            stopRecording.setVisibility(View.VISIBLE);

            stopRecording.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_stop_continue_color_action_normal, 0, 0, 0);
        }

        void recordingResume(long time) {
            timeCounter.setBase(time);
            timeCounter.start();
            startRecording.setVisibility(View.GONE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                resumeRecording.setVisibility(View.GONE);
                pauseRecording.setVisibility(View.VISIBLE);
            }

            stopRecording.setVisibility(View.VISIBLE);
            stopRecording.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_stop_color_action_normal, 0, 0, 0);
        }

        void resetDir() {
            resetFolder();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            recordingBinder = (ScreenRecorder.RecordingBinder)service;
            screenRecorderStarted = recordingBinder.isStarted();

            recordingBinder.setConnect(new ActivityBinder());

            if (serviceToRecording == true) {
                serviceToRecording = false;
                recordingStart();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            recordingBinder.setDisconnect();
            screenRecorderStarted = false;
        }
    };


    private final ActivityResultLauncher<Intent> requestRecordingPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {

            int requestCode = result.getResultCode();

            if (requestCode == RESULT_OK && recordingBinder != null) {
                doStartService(requestCode, result.getData());
            }

        }
    });

    private final ActivityResultLauncher<Intent> requestFolderPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {

            requestFolder(result.getResultCode(), result.getData().getData(), false);

        }
    });

    private final ActivityResultLauncher<Intent> requestFolderPermissionAndProceed = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {

            requestFolder(result.getResultCode(), result.getData().getData(), false);

        }
    });



    void doStartService(int result, Intent data) {

        Display display = ((DisplayManager)(getBaseContext().getSystemService(Context.DISPLAY_SERVICE))).getDisplay(Display.DEFAULT_DISPLAY);

        int orientationOnStart = display.getRotation();

        Rect windowSize = new Rect();

        getWindow().getDecorView().getWindowVisibleDisplayFrame(windowSize);

        Rect rectgl = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectgl);


        int pixelWidth = windowSize.width();
        int pixelHeight = windowSize.height();


        int screenInsetsHoriz = 0;
        int screenInsetsLeftRight = 0;

        int screenWidthNormal = 0;

        int screenHeightNormal = 0;

        if (orientationOnStart == Surface.ROTATION_90 || orientationOnStart == Surface.ROTATION_270) {
            screenWidthNormal = pixelHeight;
            screenHeightNormal = pixelWidth;
        } else {
            screenWidthNormal = pixelWidth;
            screenHeightNormal = pixelHeight;
        }

        recordingBinder.setPreStart(result, data, screenWidthNormal, screenHeightNormal);

        if (appSettings.getBoolean("recordmode", false) == false) {
            serviceIntent.setAction(ScreenRecorder.ACTION_START);
        } else {
            serviceIntent.setAction(ScreenRecorder.ACTION_START_NOVIDEO);
        }
        startService(serviceIntent);
    }

    void doBindService() {
        serviceIntent = new Intent(MainActivity.this, ScreenRecorder.class);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        if (recordingBinder != null) {
            unbindService(mConnection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        doUnbindService();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appSettings = getSharedPreferences(ScreenRecorder.prefsident, 0);
        appSettingsEditor = appSettings.edit();

        String darkTheme = appSettings.getString("darktheme", getResources().getString(R.string.dark_theme_option_auto));

        if (appSettings.getString("darkthemeapplied", getResources().getString(R.string.dark_theme_option_auto)).contentEquals(darkTheme) == false) {
            appSettingsEditor.putString("darkthemeapplied", darkTheme);
            appSettingsEditor.commit();
        }

        if (((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES && darkTheme.contentEquals(getResources().getString(R.string.dark_theme_option_auto))) || darkTheme.contentEquals("Dark")) {
            setTheme(R.style.Theme_AppCompat_NoActionBar);
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
        }

        setContentView(R.layout.main);

        if (appSettings.getString("floatingcontrolssize", getResources().getString(R.string.floating_controls_size_option_auto_value)) == "Little") {
            appSettingsEditor.putString("floatingcontrolssize", "Tiny");
            appSettingsEditor.putBoolean("panelpositionhorizontalhiddentiny", appSettings.getBoolean("panelpositionhorizontalhiddenlittle", false));
            appSettingsEditor.putBoolean("panelpositionverticalhiddentiny", appSettings.getBoolean("panelpositionverticalhiddenlittle", false));
            appSettingsEditor.putInt("panelpositionhorizontalxtiny", appSettings.getInt("panelpositionhorizontalxlittle", 0));
            appSettingsEditor.putInt("panelpositionhorizontalytiny", appSettings.getInt("panelpositionhorizontalylittle", 0));
            appSettingsEditor.putInt("panelpositionverticalxtiny", appSettings.getInt("panelpositionverticalxlittle", 0));
            appSettingsEditor.putInt("panelpositionverticalytiny", appSettings.getInt("panelpositionverticalylittle", 0));
            appSettingsEditor.commit();
        }

        if (appSettings.getBoolean("checksoundplayback", false) == true && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            appSettingsEditor.putBoolean("checksoundplayback", false);
            appSettingsEditor.commit();
        }

        if ((appSettings.getBoolean("floatingcontrols", false) == true) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && (Settings.canDrawOverlays(this) == false)) {
            appSettingsEditor.putBoolean("floatingcontrols", false);
            appSettingsEditor.commit();
        }

        startRecording = (Button) findViewById(R.id.recordbutton);
        pauseRecording = (Button) findViewById(R.id.recordpausebutton);
        resumeRecording = (Button) findViewById(R.id.recordresumebutton);
        stopRecording = (Button) findViewById(R.id.recordstopbutton);
        chooseFolder = (Button) findViewById(R.id.recordfolder);
        showSettings = (Button) findViewById(R.id.recordsettings);
        recMicrophone = (CheckBox) findViewById(R.id.checksoundmic);
        recPlayback = (CheckBox) findViewById(R.id.checksoundplayback);
        timeCounter = (Chronometer) findViewById(R.id.timerrecord);
        audioPlaybackUnavailable = (TextView) findViewById(R.id.audioplaybackunavailable);
        recordAudioOption = (RadioButton) findViewById(R.id.record_audio);
        recordVideoOption = (RadioButton) findViewById(R.id.record_video);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            audioPlaybackUnavailable.setVisibility(View.VISIBLE);
            recPlayback.setVisibility(View.GONE);
        }

        if (appSettings.getBoolean("checksoundmic", false)) {
            recMicrophone.setChecked(true);
        }

        if (appSettings.getBoolean("checksoundplayback", false)) {
            recPlayback.setChecked(true);
        }

        setRecordMode(appSettings.getBoolean("recordmode", false));

        if (recordModeChosen == false) {
            recordVideoOption.toggle();
        } else {
            recordAudioOption.toggle();
        }

        activityProjectionManager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);

        recMicrophone.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CheckBox innerBox = (CheckBox) v;
                boolean checkedState = innerBox.isChecked();
                boolean audioPermissionDenied = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioPermissionDenied = (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED);
                }
                if (audioPermissionDenied && checkedState == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    innerBox.setChecked(false);
                    String accesspermission[] = {Manifest.permission.RECORD_AUDIO};
                    requestPermissions(accesspermission, REQUEST_MICROPHONE);
                } else {
                    if (recordModeChosen == true && checkedState == false && ((recPlayback.isChecked() == false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)) {
                        innerBox.setChecked(true);
                    } else {
                        appSettingsEditor.putBoolean("checksoundmic", checkedState);
                        appSettingsEditor.commit();
                    }
                }
            }
        });

        recPlayback.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CheckBox innerBox = (CheckBox) v;
                boolean checkedState = innerBox.isChecked();
                boolean audioPermissionDenied = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioPermissionDenied = (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED);
                }
                if (audioPermissionDenied && checkedState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    innerBox.setChecked(false);
                    String accesspermission[] = {Manifest.permission.RECORD_AUDIO};
                    requestPermissions(accesspermission, REQUEST_MICROPHONE_PLAYBACK);
                } else {
                    if (recordModeChosen == true && checkedState == false && recMicrophone.isChecked() == false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        innerBox.setChecked(true);
                    } else {
                        appSettingsEditor.putBoolean("checksoundplayback", ((CheckBox) v).isChecked());
                        appSettingsEditor.commit();
                    }
                }
            }
        });

        startRecording.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                recordingStart();
            }
        });

        pauseRecording.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                recordingBinder.recordingPause();
            }
        });

        resumeRecording.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                recordingBinder.recordingResume();
            }
        });

        stopRecording.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                recordingBinder.stopService();
            }
        });

        chooseFolder.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chooseDir(false);
            }
        });

        showSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent showsettings = new Intent(MainActivity.this, SettingsPanel.class);
                startActivity(showsettings);
            }
        });

    }

    public void setRecordMode(boolean mode) {
        appSettingsEditor.putBoolean("recordmode", mode);
        appSettingsEditor.commit();


        recordModeChosen = mode;

        if (mode == false) {

        } else if (mode == true) {
            if (!(appSettings.getBoolean("checksoundplayback", false) == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)) {
                 recMicrophone.setChecked(true);
                 appSettingsEditor.putBoolean("checksoundmic", true);
                 appSettingsEditor.commit();
            }
        }

    }

    public void onRecordingModeChecked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        if (checked == true) {
            if (view.getId() == R.id.record_video) {
                setRecordMode(false);
            } else if (view.getId() == R.id.record_audio) {
                boolean audioPermissionDenied = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioPermissionDenied = (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED);
                }
                if (audioPermissionDenied == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    recordVideoOption.toggle();
                    String accesspermission[] = {Manifest.permission.RECORD_AUDIO};
                    requestPermissions(accesspermission, REQUEST_MODE_CHANGE);
                } else {
                    recordAudioOption.toggle();
                    setRecordMode(true);
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        doBindService();
        Intent created_intent = getIntent();
        if (created_intent.getAction() == ACTION_ACTIVITY_START_RECORDING && stateActivated == false) {
            stateActivated = true;
            recordingStart();
        }
    }

    public void checkDirRecord() {
        if (appSettings.getString("folderpath", "NULL") == "NULL") {
            chooseDir(true);
        } else {
            proceedRecording();
        }
    }

    public void recordingStart() {
        if (recordingBinder == null) {
            serviceToRecording = true;
            doBindService();
        } else {
            if (recordingBinder.isStarted() == false) {
                boolean audioPermissionDenied = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioPermissionDenied = (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED);
                }

                boolean extStoragePermissionDenied = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    extStoragePermissionDenied = (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED);
                }

                if (audioPermissionDenied && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (appSettings.getBoolean("checksoundmic", false) == true || (appSettings.getBoolean("checksoundplayback", false) == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q))) {
                    String accesspermission[] = {Manifest.permission.RECORD_AUDIO};
                    requestPermissions(accesspermission, REQUEST_MICROPHONE_RECORD);
                } else if (extStoragePermissionDenied && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    String accesspermission[] = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    requestPermissions(accesspermission, REQUEST_STORAGE);
                } else if ((appSettings.getBoolean("floatingcontrols", false) == true) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && (Settings.canDrawOverlays(this) == false)) {
                    appSettingsEditor.putBoolean("floatingcontrols", false);
                    appSettingsEditor.commit();
                    requestOverlayDisplayPermission();
                } else {
                    checkDirRecord();
                }

            }
        }
    }

    private void requestFolder(int resultCode, Uri extrauri, boolean proceedToRecording) {
        if (resultCode == RESULT_OK) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

                getContentResolver().takePersistableUriPermission(extrauri, takeFlags);
            }

            appSettingsEditor.putString("folderpath", extrauri.toString());
            appSettingsEditor.commit();

            if (proceedToRecording == true) {
                proceedRecording();
            }

        } else {

            if (appSettings.getString("folderpath", "NULL") == "NULL") {
                Toast.makeText(this, R.string.error_storage_select_folder, Toast.LENGTH_SHORT).show();
            }

        }

    }

    void proceedRecording() {

        if (appSettings.getBoolean("recordmode", false) == true && (appSettings.getBoolean("checksoundplayback", false) == false || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) && recordingBinder != null) {
            doStartService(0, null);
        } else {
            requestRecordingPermission.launch(activityProjectionManager.createScreenCaptureIntent());
        }
    }

    void resetFolder() {
        appSettingsEditor.remove("folderpath");
        appSettingsEditor.commit();
        Toast.makeText(this, R.string.error_invalid_folder, Toast.LENGTH_SHORT).show();
        chooseDir(true);
    }

    void chooseDir(boolean toRecording) {
        int reqcode = REQUEST_READ;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        if (toRecording == true) {
            reqcode = REQUEST_READ_RECORD;
            requestFolderPermissionAndProceed.launch(intent);
        } else {
            requestFolderPermission.launch(intent);
        }

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
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + appName));
                startActivity(intent);
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appSettingsEditor.putBoolean("checksoundmic", true);
                appSettingsEditor.commit();
                recMicrophone.setChecked(true);
            } else {
                Toast.makeText(this, R.string.error_audio_required, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_MICROPHONE_PLAYBACK) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appSettingsEditor.putBoolean("checksoundplayback", true);
                appSettingsEditor.commit();
                recPlayback.setChecked(true);
            } else {
                Toast.makeText(this, R.string.error_audio_required, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_MICROPHONE_RECORD) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkDirRecord();
            } else {
                Toast.makeText(this, R.string.error_audio_required, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkDirRecord();
            } else {
                Toast.makeText(this, R.string.error_storage_required, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_MODE_CHANGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recordAudioOption.toggle();
                setRecordMode(true);
            } else {
                Toast.makeText(this, R.string.error_audio_required, Toast.LENGTH_SHORT).show();
            }
        }


    }

}
