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
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.IconCompat;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ScreenRecorder extends Service {

    private WindowManager windowManager;

    public boolean runningService = false;
    private Intent data;
    private int result;

    private File recordFile;
    private Uri recordFilePath;
    private Uri recordFilePathParent;
    private String recordFileMime;
    private Uri recordFileFullPath;

    public static final int RECORDING_START = 100;
    public static final int RECORDING_STOP = 101;
    public static final int RECORDING_PAUSE = 102;
    public static final int RECORDING_RESUME = 103;

    public static String ACTION_START = MainActivity.appName+".START_RECORDING";
    public static String ACTION_START_NOVIDEO = MainActivity.appName+".START_RECORDING_NOVIDEO";
    public static String ACTION_PAUSE = MainActivity.appName+".PAUSE_RECORDING";
    public static String ACTION_CONTINUE = MainActivity.appName+".CONTINUE_RECORDING";
    public static String ACTION_STOP = MainActivity.appName+".STOP_RECORDING";
    public static String ACTION_ACTIVITY_CONNECT = MainActivity.appName+".ACTIVITY_CONNECT";
    public static String ACTION_ACTIVITY_DISCONNECT = MainActivity.appName+".ACTIVITY_DISCONNECT";
    public static String ACTION_ACTIVITY_DELETE_FINISHED_FILE = MainActivity.appName+".ACTIVITY_DELETE_FINISHED_FILE";

    private Intent finishedFileIntent = null;
    private Intent shareFinishedFileIntent = null;

    private File finishedFile;
    private Uri finishedFileDocument;
    private Uri finishedFullFileDocument;
    private String finishedDocumentMime;

    private static String NOTIFICATIONS_RECORDING_CHANNEL = "notifications";

    private static int NOTIFICATION_RECORDING_ID = 7023;
    private static int NOTIFICATION_RECORDING_FINISHED_ID = 7024;

    private long timeStart = 0;
    private long timeRecorded = 0;
    private boolean recordMicrophone = false;
    private boolean recordPlayback = false;
    private boolean isPaused = false;

    private ParcelFileDescriptor recordingOpenFileDescriptor;
    private FileDescriptor recordingFileDescriptor;
    private String recordingFilePath;

    private NotificationManagerCompat recordingNotificationManager;
    private MediaProjection recordingMediaProjection;
    private VirtualDisplay recordingVirtualDisplay;
    private MediaRecorder recordingMediaRecorder;

    private MainActivity.ActivityBinder activityBinder = null;

    private QuickTile.TileBinder tileBinder = null;

    private FloatingControls.PanelBinder panelBinder = null;

    private PlaybackRecorder recorderPlayback;

    private boolean isRestarting = false;

    private int orientationOnStart;

    private SharedPreferences appSettings;

    public static final String prefsident = "DroidRecPreferences";

    private static final float BPP = 0.25f;

    private SensorManager sensor;

    private boolean showFloatingControls = false;

    private boolean recordOnlyAudio = false;

    private boolean isActive = false;

    private boolean ignoreRotate = false;

    private boolean dontNotifyOnRotate = false;

    private boolean dontNotifyOnFinish = false;

    private boolean mediaAudioSource = true;

    private boolean gameAudioSource = false;

    private boolean unknownAudioSource = false;

    private boolean minimizeOnStart = false;

    private int screenWidthNormal;

    private int screenHeightNormal;

    private int customSampleRate;

    private int customChannelsCount;

    private static float screenDensity;

    private int screenWindowWidth;

    private int screenWindowHeight;

    private int intentFlag;

    private SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent e) {
            if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if (orientationOnStart != display.getRotation() && isActive == true) {
                    orientationOnStart = display.getRotation();
                    if (recordOnlyAudio == false && ignoreRotate == false) {
                        isActive = false;
                        isRestarting = true;
                        screenRecordingStop();
                        screenRecordingStart();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private Display display;

    public class RecordingBinder extends Binder {
        public boolean isStarted() {
            return ScreenRecorder.this.runningService;
        }

        public void recordingPause() {
            ScreenRecorder.this.screenRecordingPause();
        }

        public void stopService() {
            ScreenRecorder.this.screenRecordingStop();
        }

        void recordingResume() {
            ScreenRecorder.this.screenRecordingResume();
        }

        long getTimeStart() {
            return ScreenRecorder.this.timeStart;
        }

        long getTimeRecorded() {
            return ScreenRecorder.this.timeRecorded;
        }

        void setConnect(MainActivity.ActivityBinder lbinder) {
            ScreenRecorder.this.actionConnect(lbinder);
        }

        void setDisconnect() {
            ScreenRecorder.this.actionDisconnect();
        }

        void setPreStart(int resultcode, Intent resultdata, int windowWidth, int windowHeight) {
            ScreenRecorder.this.result = resultcode;
            ScreenRecorder.this.data = resultdata;
        }
    }

    private ServiceConnection mPanelConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            panelBinder = (FloatingControls.PanelBinder)service;
            panelBinder.setConnectPanel(new RecordingPanelBinder());
        }

        public void onServiceDisconnected(ComponentName className) {
            panelBinder.setDisconnectPanel();
        }
    };

    private final IBinder recordingBinder = new RecordingBinder();

    public class RecordingTileBinder extends Binder {
        void setConnectTile(QuickTile.TileBinder lbinder) {
            ScreenRecorder.this.actionConnectTile(lbinder);
        }

        void setDisconnectTile() {
            ScreenRecorder.this.actionDisconnectTile();
        }

        boolean isStarted() {
            return ScreenRecorder.this.runningService;
        }

        void stopService() {
            ScreenRecorder.this.screenRecordingStop();
        }
    }

    private final IBinder recordingTileBinder = new RecordingTileBinder();

    public class RecordingPanelBinder extends Binder {

        long getTimeStart() {
            return ScreenRecorder.this.timeStart;
        }

        boolean isStarted() {
            return ScreenRecorder.this.runningService;
        }

        void registerListener() {
            sensor.registerListener(sensorListener, sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        }

        void recordingPause() {
            ScreenRecorder.this.screenRecordingPause();
        }

        void recordingResume() {
            ScreenRecorder.this.screenRecordingResume();
        }

        void stopService() {
            ScreenRecorder.this.screenRecordingStop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (sensor != null) {
            sensor.unregisterListener(sensorListener);
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (intent.getAction() == QuickTile.ACTION_CONNECT_TILE) {
                return recordingTileBinder;
            }
        }

        return recordingBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        display = ((DisplayManager)(getBaseContext().getSystemService(Context.DISPLAY_SERVICE))).getDisplay(Display.DEFAULT_DISPLAY);

        sensor = (SensorManager)getApplicationContext().getSystemService(Context.SENSOR_SERVICE);

        sensor.registerListener(sensorListener, sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);

        Intent serviceIntent = new Intent(ScreenRecorder.this, FloatingControls.class);
        serviceIntent.setAction(FloatingControls.ACTION_RECORD_PANEL);
        bindService(serviceIntent, mPanelConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            if (intent.getAction() == ACTION_START) {
                recordOnlyAudio = false;
                actionStart();
            } else if (intent.getAction() == ACTION_START_NOVIDEO) {
                recordOnlyAudio = true;
                actionStart();
            } else if (intent.getAction() == ACTION_STOP) {
                screenRecordingStop();
            } else if (intent.getAction() == ACTION_PAUSE) {
                screenRecordingPause();
            } else if (intent.getAction() == ACTION_CONTINUE) {
                screenRecordingResume();
            } else if (intent.getAction() == ACTION_ACTIVITY_DELETE_FINISHED_FILE) {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    try {
                        finishedFile.delete();
                    } catch (Exception e) {}
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        DocumentsContract.deleteDocument(getContentResolver(), finishedFileDocument);
                    } catch (Exception e) {}
                }

                if (recordingNotificationManager != null) {
                    recordingNotificationManager.cancel(NOTIFICATION_RECORDING_FINISHED_ID);
                }
            }
        }

        return START_STICKY;
    }

    @SuppressWarnings("deprecation")
    public void actionStart() {
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);

        orientationOnStart = display.getRotation();

        screenDensity = metrics.densityDpi;

        if (orientationOnStart == Surface.ROTATION_90 || orientationOnStart == Surface.ROTATION_270) {
            screenWidthNormal = metrics.heightPixels;
            screenHeightNormal = metrics.widthPixels;
        } else {
            screenWidthNormal = metrics.widthPixels;
            screenHeightNormal = metrics.heightPixels;
        }

        appSettings = getSharedPreferences(prefsident, 0);

        ignoreRotate = appSettings.getBoolean("norotate", false);

        dontNotifyOnRotate = appSettings.getBoolean("dontnotifyonrotate", false);

        dontNotifyOnFinish = appSettings.getBoolean("dontnotifyonfinish", false);

        mediaAudioSource = appSettings.getBoolean("audsourcemedia", true);

        gameAudioSource = appSettings.getBoolean("audsourcegame", false);

        unknownAudioSource = appSettings.getBoolean("audsourceunknown", false);

        minimizeOnStart = appSettings.getBoolean("minimizeonstart", false);

        intentFlag = PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intentFlag = PendingIntent.FLAG_IMMUTABLE;
        }


        if (appSettings.getBoolean("customsamplerate", false) == true) {

            String customSampleRateValueString = appSettings.getString("sampleratevalue", "44100");

            if (customSampleRateValueString.length() < 10) {
                try {
                    customSampleRate = Integer.parseInt(customSampleRateValueString);
                } catch (NumberFormatException exception) {
                    customSampleRate = 44100;
                }
            }

        } else {
            customSampleRate = 44100;

            String sampleRateValue = ((AudioManager)getSystemService(Context.AUDIO_SERVICE)).getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);

            if (sampleRateValue != null) {
                if (sampleRateValue.length() < 10) {
                    try {
                        int newSampleRate = Integer.parseInt(sampleRateValue);
                        if (newSampleRate > 44100 && newSampleRate >= 48000) {
                            customSampleRate = 48000;
                        }
                    } catch (NumberFormatException exception) {
                        customSampleRate = 44100;
                    }
                }
            }

        }

        if (appSettings.getString("audiochannels", "Stereo").contentEquals("Mono") == true) {
            customChannelsCount = 1;
        } else {
            customChannelsCount = 2;
        }

        recordingNotificationManager = NotificationManagerCompat.from(ScreenRecorder.this);

        if (recordingNotificationManager.getNotificationChannel(NOTIFICATIONS_RECORDING_CHANNEL) == null) {
            NotificationChannelCompat recordingNotifications = new NotificationChannelCompat.Builder(NOTIFICATIONS_RECORDING_CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(getString(R.string.notifications_channel))
                .setLightsEnabled(true)
                .setLightColor(Color.RED)
                .setShowBadge(true)
                .setVibrationEnabled(true)
                .build();

            recordingNotificationManager.createNotificationChannel(recordingNotifications);
        }

        runningService = true;

        if (tileBinder != null) {
            tileBinder.recordingState(true);
        }

        screenRecordingStart();
    }

    public void actionConnect(MainActivity.ActivityBinder service) {
        activityBinder = service;

        if (runningService == true) {
            if (isPaused == false) {
                if (activityBinder != null) {
                    activityBinder.recordingStart();
                }
            } else if (isPaused == true) {
                if (activityBinder != null) {
                    activityBinder.recordingPause(timeRecorded);
                }
            }
        }
    }

    public void actionConnectTile(QuickTile.TileBinder service) {
        tileBinder = service;
    }

    public void actionDisconnect() {
        activityBinder = null;
    }

    public void actionDisconnectTile() {
        tileBinder = null;
    }

    private void recordingError() {
        Toast.makeText(this, R.string.error_recorder_failed, Toast.LENGTH_SHORT).show();

        screenRecordingStop();
    }

    /* Old devices don't support many resolutions */
    private int[] getScreenResolution() {
        int[] resolution = new int[2];


        boolean landscape = false;

        if (orientationOnStart == Surface.ROTATION_90 || orientationOnStart == Surface.ROTATION_270) {
            landscape = true;
        }

        if ((landscape == true && screenWidthNormal < screenHeightNormal) || (landscape == false && screenWidthNormal > screenHeightNormal)) {
            resolution[0] = 1920;
            resolution[1] = 1080;

            if (screenHeightNormal == 3840) {
                resolution[0] = 3840;
                resolution[1] = 2160;
            } else if (screenHeightNormal < 3840 && screenHeightNormal >= 1920) {
                resolution[0] = 1920;
                resolution[1] = 1080;
            } else if (screenHeightNormal < 1920 && screenHeightNormal >= 1280) {
                resolution[0] = 1280;
                resolution[1] = 720;
            } else if (screenHeightNormal < 1280 && screenHeightNormal >= 720) {
                resolution[0] = 720;
                resolution[1] = 480;
            } else if (screenHeightNormal < 720 && screenHeightNormal >= 480) {
                resolution[0] = 480;
                resolution[1] = 360;
            } else if (screenHeightNormal < 480 && screenHeightNormal >= 320) {
                resolution[0] = 360;
                resolution[1] = 240;
            }
        } else if ((landscape == false && screenWidthNormal < screenHeightNormal) || (landscape == true && screenWidthNormal > screenHeightNormal)) {
            resolution[0] = 1080;
            resolution[1] = 1920;

            if (screenWidthNormal == 3840) {
                resolution[0] = 2160;
                resolution[1] = 3840;
            } else if (screenWidthNormal < 3840 && screenWidthNormal >= 1920) {
                resolution[0] = 1080;
                resolution[1] = 1920;
            } else if (screenWidthNormal < 1920 && screenWidthNormal >= 1280) {
                resolution[0] = 720;
                resolution[1] = 1280;
            } else if (screenWidthNormal < 1280 && screenWidthNormal >= 720) {
                resolution[0] = 480;
                resolution[1] = 720;
            } else if (screenWidthNormal < 720 && screenWidthNormal >= 480) {
                resolution[0] = 360;
                resolution[1] = 480;
            } else if (screenWidthNormal < 480 && screenWidthNormal >= 320) {
                resolution[0] = 240;
                resolution[1] = 360;
            }
        }

        return resolution;
    }

    @SuppressWarnings("deprecation")
    private void screenRecordingStart() {

        if (minimizeOnStart == true) {
            Intent minimizeIntent = new Intent(Intent.ACTION_MAIN);
            minimizeIntent.addCategory(Intent.CATEGORY_HOME);
            minimizeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(minimizeIntent);
        }

        showFloatingControls = ((appSettings.getBoolean("floatingcontrols", false) == true) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && (Settings.canDrawOverlays(this) == true));

        if (showFloatingControls == true && isRestarting == false) {
            Intent panelIntent = new Intent(ScreenRecorder.this, FloatingControls.class);
            panelIntent.setAction(FloatingControls.ACTION_RECORD_PANEL);
            startService(panelIntent);
        }

        recordMicrophone = appSettings.getBoolean("checksoundmic", false);

        recordPlayback = appSettings.getBoolean("checksoundplayback", false);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);

        String timeString = formatter.format(Calendar.getInstance().getTime());

        String fullFileName = "ScreenRecording_" + timeString;

        String providertree = "^content://[^/]*/tree/";

        String filetreepattern = "^content://com\\.android\\.externalstorage\\.documents/tree/.*";

        Uri filefulluri = null;

        String folderPathPrefix = appSettings.getString("folderpath", "");

        if (recordOnlyAudio == true) {
            folderPathPrefix = appSettings.getString("folderaudiopath", "");
        }

        String documentspath = folderPathPrefix.replaceFirst(providertree, "");

        String docExtension = ".mp4";

        String docMime = "video/mp4";

        Uri docParent = Uri.parse(folderPathPrefix + "/document/" + documentspath);

        if (recordOnlyAudio == true) {
            fullFileName = "AudioRecording_" + timeString;
            docExtension = ".m4a";
            docMime = "audio/mp4";
        }

        if (folderPathPrefix.matches(filetreepattern)) {
            if (documentspath.startsWith("primary%3A")) {
                filefulluri = Uri.parse("/storage/emulated/0/" + Uri.decode(documentspath.replaceFirst("primary%3A", "")) + "/" + fullFileName + docExtension);
            } else {
                filefulluri = Uri.parse("/storage/" + Uri.decode(documentspath.replaceFirst("%3A", "/")) + "/" + fullFileName + docExtension);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    File dirTest = new File(filefulluri.toString());
                    if (dirTest.isDirectory() == false) {
                        filefulluri = Uri.parse("/storage/sdcard" + Uri.decode(documentspath.replaceFirst(".*\\%3A", "/")) + "/" + fullFileName + docExtension);
                    }
                }
            }
        }

        Uri outdocpath = null;

        File docFile = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            try {
                outdocpath = DocumentsContract.createDocument(getContentResolver(), docParent, docMime, fullFileName);
            } catch (Exception e) {

                recordingError();
                if (activityBinder != null) {
                    activityBinder.resetDir(recordOnlyAudio);
                }
                stopSelf();
                return;

            }

            if (!outdocpath.toString().endsWith(".m4a") && recordOnlyAudio == true) {
                Uri outdocpathnew = null;

                try {
                    outdocpathnew = DocumentsContract.renameDocument(getContentResolver(), outdocpath, fullFileName + ".m4a");
                } catch (Exception e) {}

                if (outdocpathnew == null) {
                    outdocpath = Uri.parse(outdocpath.toString() + ".m4a");
                } else {
                    outdocpath = outdocpathnew;
                }
            }

        } else {
            try {
                docFile = new File(filefulluri.toString());
                docFile.createNewFile();
            } catch (Exception e) {
                docFile = null;
            }
        }

        if ((docFile == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) || (outdocpath == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)) {
            recordingError();
            if (activityBinder != null) {
                activityBinder.resetDir(recordOnlyAudio);
            }
            stopSelf();
            return;
        } else {
            recordFile = docFile;
            recordFilePath = outdocpath;
            recordFileMime = docMime;
            recordFilePathParent = docParent;
            recordFileFullPath = filefulluri;
        }

        timeStart = SystemClock.elapsedRealtime();

        IconCompat stopIcon = IconCompat.createWithResource(this, R.drawable.icon_stop_color_action);

        Intent stopRecordIntent = new Intent(this, ScreenRecorder.class);

        stopRecordIntent.setAction(ACTION_STOP);

        PendingIntent stopRecordActionIntent = PendingIntent.getService(this, 0, stopRecordIntent, intentFlag);

        NotificationCompat.Action.Builder stopRecordAction = new NotificationCompat.Action.Builder(stopIcon, getString(R.string.notifications_stop), stopRecordActionIntent);


        IconCompat pauseIcon = IconCompat.createWithResource(this, R.drawable.icon_pause_color_action);

        Intent pauseRecordIntent = new Intent(this, ScreenRecorder.class);

        pauseRecordIntent.setAction(ACTION_PAUSE);

        PendingIntent pauseRecordActionIntent = PendingIntent.getService(this, 0, pauseRecordIntent, intentFlag);

        NotificationCompat.Action.Builder pauseRecordAction = new NotificationCompat.Action.Builder(pauseIcon, getString(R.string.notifications_pause), pauseRecordActionIntent);

        NotificationCompat.Builder notification;

        notification = new NotificationCompat.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL);

        if (recordOnlyAudio == true) {
            notification = notification
                .setContentTitle(getString(R.string.recording_audio_started_title))
                .setContentText(getString(R.string.recording_audio_started_text))
                .setTicker(getString(R.string.recording_audio_started_text));

        } else {
            notification = notification
                .setContentTitle(getString(R.string.recording_started_title))
                .setContentText(getString(R.string.recording_started_text))
                .setTicker(getString(R.string.recording_started_text));

        }

        notification = notification
            .setSmallIcon(R.drawable.icon_record_status)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_record_color_action_normal))
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis()-(SystemClock.elapsedRealtime()-timeStart))
            .setOngoing(true)
            .addAction(stopRecordAction.build())
            .setPriority(NotificationCompat.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.addAction(pauseRecordAction.build());
        }

        if (isRestarting == false) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_RECORDING_ID, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(NOTIFICATION_RECORDING_ID, notification.build());
            }
        }

        if (activityBinder != null) {
            activityBinder.recordingStart();
        }

        int width = 0;
        int height = 0;

        float scaleRatio = 1.0f;

        if (orientationOnStart == Surface.ROTATION_90 || orientationOnStart == Surface.ROTATION_270) {
            width = screenHeightNormal;
            height = screenWidthNormal;
        } else {
            width = screenWidthNormal;
            height = screenHeightNormal;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            int[] resolutions = getScreenResolution();
            width = resolutions[0];
            height = resolutions[1];
        }

        if (appSettings.getString("resolutionvalue", "Native").contentEquals("Native") == false) {
            String resolutionSetting = appSettings.getString("resolutionvalue", "Native");
            int measureResolution = height;

            if (height > width) {
                measureResolution = width;
            }

            if (resolutionSetting.contentEquals("2160p") == true && measureResolution >= 2160) {
                scaleRatio = (float)2160/(float)measureResolution;
            } else if (resolutionSetting.contentEquals("1080p") == true && measureResolution >= 1080) {
                scaleRatio = (float)1080/(float)measureResolution;
            } else if (resolutionSetting.contentEquals("720p") == true && measureResolution >= 720) {
                scaleRatio = (float)720/(float)measureResolution;
            } else if (resolutionSetting.contentEquals("480p") == true && measureResolution >= 480) {
                scaleRatio = (float)480/(float)measureResolution;
            } else if (resolutionSetting.contentEquals("360p") == true && measureResolution >= 360) {
                scaleRatio = (float)360/(float)measureResolution;
            }
        }

        MediaProjectionManager recordingMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (recordingMediaProjection != null) {
            recordingMediaProjection.stop();
        }
        if (recordOnlyAudio == true && (recordPlayback == false || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)) {
            recordingMediaProjection = null;
        } else {
            recordingMediaProjection = recordingMediaProjectionManager.getMediaProjection(result, data);
        }

        if (recordOnlyAudio == false) {
            recordingVirtualDisplay = recordingMediaProjection.createVirtualDisplay("DroidRec", width, height, (int)screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, null, null, null);
        }

        isRestarting = false;

        int frameRate = (int)(display.getRefreshRate());


        boolean customQuality = appSettings.getBoolean("customquality", false);

        float qualityScale = 0.1f * (appSettings.getInt("qualityscale", 9)+1);


        boolean customFPS = appSettings.getBoolean("customfps", false);

        int fpsValue = Integer.parseInt(appSettings.getString("fpsvalue", "30"));


        boolean customBitrate = appSettings.getBoolean("custombitrate", false);

        int bitrateValue = Integer.parseInt(appSettings.getString("bitratevalue", "0"));


        String customCodec = appSettings.getString("codecvalue", getResources().getString(R.string.codec_option_auto_value));

        String customAudioCodec = appSettings.getString("audiocodecvalue", getResources().getString(R.string.audio_codec_option_auto_value));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            recordingMediaRecorder = new MediaRecorder();

            recordingMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                @Override
                public void onError(MediaRecorder mr, int what, int extra) {
                    recordingError();
                }
            });

            try {

                if (recordMicrophone == true) {
                    recordingMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    recordingMediaRecorder.setAudioEncodingBitRate(customSampleRate*32*2);
                    recordingMediaRecorder.setAudioSamplingRate(customSampleRate);
                    recordingMediaRecorder.setAudioChannels(customChannelsCount);
                }

                if (recordOnlyAudio == false) {
                    recordingMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                }

                recordingMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);


                recordingMediaRecorder.setOutputFile(recordFileFullPath.toString());


                if (recordOnlyAudio == false) {
                    recordingMediaRecorder.setVideoSize((int)((float)width*scaleRatio), (int)((float)height*scaleRatio));

                    recordingMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                }

                if (recordMicrophone == true) {
                    recordingMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                }

                if (recordOnlyAudio == false) {

                    if (customFPS == true) {
                        frameRate = fpsValue;
                    }

                    int recordingBitrate = (int)(BPP*frameRate*width*height);

                    if (customQuality == true) {
                        recordingBitrate = (int)(recordingBitrate*qualityScale);
                    }

                    if (customBitrate == true) {
                        recordingBitrate = bitrateValue;
                    }

                    recordingMediaRecorder.setVideoEncodingBitRate(recordingBitrate);

                    recordingMediaRecorder.setVideoFrameRate(frameRate);
                }
                recordingMediaRecorder.prepare();
            } catch (IOException e) {
                recordingError();
            }
            try {
                recordingMediaRecorder.start();
            } catch (IllegalStateException e) {
                if (recordingMediaProjection != null) {
                    recordingMediaProjection.stop();
                }
                recordingError();
            }
            if (recordOnlyAudio == false) {
                recordingVirtualDisplay.setSurface(recordingMediaRecorder.getSurface());
            }
        } else {

            try {
                recordingOpenFileDescriptor = getContentResolver().openFileDescriptor(recordFilePath, "rw");
                recordingFileDescriptor = recordingOpenFileDescriptor.getFileDescriptor();
            } catch (Exception e) {
                recordingError();
            }

            recorderPlayback = new PlaybackRecorder(getApplicationContext(), recordOnlyAudio, recordingVirtualDisplay, recordingFileDescriptor, recordingMediaProjection, width, height, scaleRatio, frameRate, recordMicrophone, recordPlayback, customQuality, qualityScale, customFPS, fpsValue, customBitrate, bitrateValue, (!customCodec.contentEquals(getResources().getString(R.string.codec_option_auto_value))), customCodec, (!customAudioCodec.contentEquals(getResources().getString(R.string.audio_codec_option_auto_value))), customAudioCodec, customSampleRate, customChannelsCount, mediaAudioSource, gameAudioSource, unknownAudioSource);

            recorderPlayback.start();
        }

        isActive = true;

    }

    @SuppressWarnings("deprecation")
    private void screenRecordingStop() {
        isActive = false;

        timeStart = 0;
        timeRecorded = 0;
        isPaused = false;

        if (isRestarting == false) {
            runningService = false;

            if (tileBinder != null) {
                tileBinder.recordingState(false);
            }

        }

        if (activityBinder != null) {
            activityBinder.recordingStop();
        }

        if (panelBinder != null && showFloatingControls == true) {
            if (isRestarting == false) {
                panelBinder.setStop();
            } else {
                panelBinder.setRestart(orientationOnStart);
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (recordingMediaRecorder != null) {
                try {
                    recordingMediaRecorder.stop();
                    recordingMediaRecorder.reset();
                    recordingMediaRecorder.release();
                    if (recordOnlyAudio == false) {
                        recordingVirtualDisplay.release();
                    }
                } catch (RuntimeException e) {
                    Toast.makeText(this, R.string.error_recorder_failed, Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            if (recorderPlayback != null) {
                recorderPlayback.quit();
                try {
                    recordingOpenFileDescriptor.close();
                } catch (IOException e) {
                    Toast.makeText(this, R.string.error_recorder_failed, Toast.LENGTH_SHORT).show();
                }

            }
        }

        finishedFile = recordFile;

        finishedFileDocument = recordFilePath;

        finishedFullFileDocument = recordFileFullPath;

        finishedDocumentMime = recordFileMime;

        finishedFileIntent = new Intent(Intent.ACTION_VIEW);
        finishedFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        finishedFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (finishedFile != null) {

                Uri data = FileProvider.getUriForFile(getApplicationContext(), MainActivity.appName + ".DocProvider", finishedFile);

                finishedFileIntent.setDataAndType(data, finishedDocumentMime);

            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             if (finishedFileDocument != null) {

                finishedFileIntent.setDataAndType(finishedFileDocument, finishedDocumentMime);

            }

        }


        PendingIntent openFolderActionIntent = PendingIntent.getActivity(this, 0, finishedFileIntent, intentFlag);

        Intent deleteRecordIntent = new Intent(this, ScreenRecorder.class);

        deleteRecordIntent.setAction(ACTION_ACTIVITY_DELETE_FINISHED_FILE);

        IconCompat deleteIcon = IconCompat.createWithResource(this, R.drawable.icon_record_delete_color_action);

        PendingIntent deleteRecordActionIntent = PendingIntent.getService(this, 0, deleteRecordIntent, intentFlag);

        NotificationCompat.Action.Builder deleteRecordAction = new NotificationCompat.Action.Builder(deleteIcon, getString(R.string.notifications_delete), deleteRecordActionIntent);

        IconCompat shareIcon = IconCompat.createWithResource(this, R.drawable.icon_record_share_color_action);

        shareFinishedFileIntent = new Intent(Intent.ACTION_SEND);

        if (finishedFullFileDocument != null) {
            shareFinishedFileIntent.setType(finishedDocumentMime);
            shareFinishedFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareFinishedFileIntent.putExtra(Intent.EXTRA_STREAM, recordFilePath);
            shareFinishedFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        PendingIntent shareRecordActionIntent = PendingIntent.getActivity(this, 0, shareFinishedFileIntent, intentFlag);

        NotificationCompat.Action.Builder shareRecordAction = new NotificationCompat.Action.Builder(shareIcon, getString(R.string.notifications_share), shareRecordActionIntent);


        if (recordFileFullPath != null) {
            MediaScannerConnection.scanFile(ScreenRecorder.this, new String[] { recordFileFullPath.toString() }, null, null);
        }

        if (isRestarting == false) {
            IconCompat finishedIcon = IconCompat.createWithResource(this, R.drawable.icon_record_finished_status);

            NotificationCompat.Builder finishedNotification;

            finishedNotification = new NotificationCompat.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL);

            if (recordOnlyAudio == true) {
                finishedNotification = finishedNotification
                    .setContentTitle(getString(R.string.recording_audio_finished_title))
                    .setContentText(getString(R.string.recording_audio_finished_text));

            } else {
                finishedNotification = finishedNotification
                    .setContentTitle(getString(R.string.recording_finished_title))
                    .setContentText(getString(R.string.recording_finished_text));

            }
            finishedNotification = finishedNotification
                .setContentIntent(openFolderActionIntent)
                .setSmallIcon(R.drawable.icon_record_finished_status)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_record_finished_color_action_normal))
                .addAction(shareRecordAction.build())
                .addAction(deleteRecordAction.build())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

            if (dontNotifyOnFinish == false) {
                recordingNotificationManager.notify(NOTIFICATION_RECORDING_FINISHED_ID, finishedNotification.build());
            }
        } else {
            IconCompat restartIcon = IconCompat.createWithResource(this, R.drawable.icon_rotate_status);

            NotificationCompat.Builder restartNotification;

            restartNotification = new NotificationCompat.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL);

            restartNotification = restartNotification
                .setContentTitle(getString(R.string.recording_rotated_title))
                .setContentText(getString(R.string.recording_rotated_text))
                .setContentIntent(openFolderActionIntent)
                .setSmallIcon(R.drawable.icon_rotate_status)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_rotate_color_action_normal))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

            if (dontNotifyOnRotate == false) {
                recordingNotificationManager.notify(NOTIFICATION_RECORDING_FINISHED_ID, restartNotification.build());
            }

        }

        if (isRestarting == false) {
            stopForeground(true);
            stopSelf();
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void screenRecordingPause() {
        if (isPaused) return;

        isPaused = true;
        timeRecorded += SystemClock.elapsedRealtime() - timeStart;
        timeStart = 0;

        if (activityBinder != null) {
            activityBinder.recordingPause(timeRecorded);
        }

        if (panelBinder != null && showFloatingControls == true) {
            panelBinder.setPause(timeRecorded);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            recordingMediaRecorder.pause();
        } else {
            recorderPlayback.pause();
        }

        IconCompat stopIcon = IconCompat.createWithResource(this, R.drawable.icon_stop_continue_color_action);

        Intent stopRecordIntent = new Intent(this, ScreenRecorder.class);

        stopRecordIntent.setAction(ACTION_STOP);

        PendingIntent stopRecordActionIntent = PendingIntent.getService(this, 0, stopRecordIntent, intentFlag);

        NotificationCompat.Action.Builder stopRecordAction = new NotificationCompat.Action.Builder(stopIcon, getString(R.string.notifications_stop), stopRecordActionIntent);

        IconCompat continueIcon = IconCompat.createWithResource(this, R.drawable.icon_record_continue_color_action);

        Intent continueRecordIntent = new Intent(this, ScreenRecorder.class);

        continueRecordIntent.setAction(ACTION_CONTINUE);

        PendingIntent continueRecordActionIntent = PendingIntent.getService(this, 0, continueRecordIntent, intentFlag);

        NotificationCompat.Action.Builder continueRecordAction = new NotificationCompat.Action.Builder(continueIcon, getString(R.string.notifications_resume), continueRecordActionIntent);

        NotificationCompat.Builder notification;

        notification = new NotificationCompat.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL);

        if (recordOnlyAudio == true) {
            notification = notification
                .setContentTitle(getString(R.string.recording_audio_paused_title))
                .setContentText(getString(R.string.recording_audio_paused_text));

        } else {
            notification = notification
                .setContentTitle(getString(R.string.recording_paused_title))
                .setContentText(getString(R.string.recording_paused_text));

        }

        notification = notification
            .setSmallIcon(R.drawable.icon_pause_status)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_pause_color_action_normal))
            .setOngoing(true)
            .addAction(stopRecordAction.build())
            .addAction(continueRecordAction.build())
            .setPriority(NotificationCompat.PRIORITY_LOW);

        recordingNotificationManager.notify(NOTIFICATION_RECORDING_ID, notification.build());
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void screenRecordingResume() {
        isPaused = false;
        timeStart = SystemClock.elapsedRealtime() - timeRecorded;
        timeRecorded = 0;

        if (activityBinder != null) {
            activityBinder.recordingResume(timeStart);
        }

        if (panelBinder != null && showFloatingControls == true) {
            panelBinder.setResume(timeStart);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            recordingMediaRecorder.resume();
        } else {
            recorderPlayback.resume();
        }

        IconCompat stopIcon = IconCompat.createWithResource(this, R.drawable.icon_stop_color_action);

        Intent stopRecordIntent = new Intent(this, ScreenRecorder.class);

        stopRecordIntent.setAction(ACTION_STOP);

        PendingIntent stopRecordActionIntent = PendingIntent.getService(this, 0, stopRecordIntent, intentFlag);

        NotificationCompat.Action.Builder stopRecordAction = new NotificationCompat.Action.Builder(stopIcon, getString(R.string.notifications_stop), stopRecordActionIntent);


        IconCompat pauseIcon = IconCompat.createWithResource(this, R.drawable.icon_pause_color_action);

        Intent pauseRecordIntent = new Intent(this, ScreenRecorder.class);

        pauseRecordIntent.setAction(ACTION_PAUSE);

        PendingIntent pauseRecordActionIntent = PendingIntent.getService(this, 0, pauseRecordIntent, intentFlag);

        NotificationCompat.Action.Builder pauseRecordAction = new NotificationCompat.Action.Builder(pauseIcon, getString(R.string.notifications_pause), pauseRecordActionIntent);

        NotificationCompat.Builder notification;

        notification = new NotificationCompat.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL);

        notification = notification
            .setContentTitle(getString(R.string.recording_started_title))
            .setContentText(getString(R.string.recording_started_text))
            .setTicker(getString(R.string.recording_started_text))
            .setSmallIcon(R.drawable.icon_record_status)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_record_color_action_normal))
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis()-(SystemClock.elapsedRealtime()-timeStart))
            .setOngoing(true)
            .addAction(stopRecordAction.build())
            .addAction(pauseRecordAction.build())
            .setPriority(NotificationCompat.PRIORITY_LOW);

        recordingNotificationManager.notify(NOTIFICATION_RECORDING_ID, notification.build());
    }

}
