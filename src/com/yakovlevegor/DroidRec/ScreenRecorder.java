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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Build;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.Display;
import android.view.Surface;
import android.widget.Toast;
import android.provider.DocumentsContract;
import android.content.SharedPreferences;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.provider.Settings;
import android.media.MediaScannerConnection;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.lang.SecurityException;

import com.yakovlevegor.DroidRec.R;

public class ScreenRecorder extends Service {

    public boolean runningService = false;
    private Intent data;
    private int result;

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
    public static String ACTION_ACTIVITY_FINISHED_FILE = MainActivity.appName+".ACTIVITY_FINISHED_FILE";
    public static String ACTION_ACTIVITY_DELETE_FINISHED_FILE = MainActivity.appName+".ACTIVITY_DELETE_FINISHED_FILE";
    public static String ACTION_ACTIVITY_SHARE_FINISHED_FILE = MainActivity.appName+".ACTIVITY_SHARE_FINISHED_FILE";

    private Intent finishedFileIntent = null;
    private Intent shareFinishedFileIntent = null;

    private Uri deleteFinishedFileDocument;

    private static String NOTIFICATIONS_RECORDING_CHANNEL = "notifications";

    private static int NOTIFICATION_RECORDING_ID = 7023;
    private static int NOTIFICATION_RECORDING_FINISHED_ID = 7024;

    private long timeStart = 0;
    private long timeRecorded = 0;
    private boolean recordMicrophone = false;
    private boolean recordPlayback = false;
    private boolean isPaused = false;

    private FileDescriptor recordingFileDescriptor;

    private NotificationManager recordingNotificationManager;
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

    private int screenWidthNormal;

    private int screenHeightNormal;

    private SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent e) {
            if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if (orientationOnStart != display.getRotation() && isActive == true) {
                    orientationOnStart = display.getRotation();
                    if (recordOnlyAudio == false) {
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
        boolean isStarted() {
            return ScreenRecorder.this.runningService;
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

        void setPreStart(int resultcode, Intent resultdata) {
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
        if (intent.getAction() == QuickTile.ACTION_CONNECT_TILE) {
            return recordingTileBinder;
        }

        return recordingBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

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
            } else if (intent.getAction() == ACTION_ACTIVITY_FINISHED_FILE) {
                startActivity(finishedFileIntent);
            } else if (intent.getAction() == ACTION_ACTIVITY_DELETE_FINISHED_FILE) {
                try {
                    DocumentsContract.deleteDocument(getContentResolver(), deleteFinishedFileDocument);
                } catch (FileNotFoundException e) {} catch (SecurityException e) {}
                recordingNotificationManager.cancel(NOTIFICATION_RECORDING_FINISHED_ID);
            } else if (intent.getAction() == ACTION_ACTIVITY_SHARE_FINISHED_FILE) {
                startActivity(shareFinishedFileIntent);
            }
        } else {
            if (runningService == false) {
                Toast.makeText(this, R.string.error_recorder_failed, Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        }

        return START_STICKY;
    } 

    public void actionStart() {

        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);

        orientationOnStart = display.getRotation();

        if (orientationOnStart == Surface.ROTATION_90 || orientationOnStart == Surface.ROTATION_270) {
            screenWidthNormal = metrics.heightPixels;
            screenHeightNormal = metrics.widthPixels;
        } else {
            screenWidthNormal = metrics.widthPixels;
            screenHeightNormal = metrics.heightPixels;
        }

        appSettings = getSharedPreferences(prefsident, 0);

        recordingNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (recordingNotificationManager.getNotificationChannel(NOTIFICATIONS_RECORDING_CHANNEL) == null) {
                NotificationChannel recordingNotifications = new NotificationChannel(NOTIFICATIONS_RECORDING_CHANNEL, getString(R.string.notifications_channel), NotificationManager.IMPORTANCE_HIGH);
                recordingNotifications.enableLights(true);
                recordingNotifications.setLightColor(Color.RED);
                recordingNotifications.setShowBadge(true);
                recordingNotifications.enableVibration(true);

                recordingNotificationManager.createNotificationChannel(recordingNotifications);
            }
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

    private void screenRecordingStart() {

        showFloatingControls = ((appSettings.getBoolean("floatingcontrols", false) == true) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && (Settings.canDrawOverlays(this) == true));

        if (showFloatingControls == true && isRestarting == false) {
            Intent panelIntent = new Intent(ScreenRecorder.this, FloatingControls.class);
            panelIntent.setAction(FloatingControls.ACTION_RECORD_PANEL);
            startService(panelIntent);
        }

        recordMicrophone = appSettings.getBoolean("checksoundmic", false);

        recordPlayback = appSettings.getBoolean("checksoundplayback", false);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");

        String timeString = formatter.format(Calendar.getInstance().getTime());

        String fullFileName = "ScreenRecording_" + timeString;

        String providertree = "^content://[^/]*/tree/";

        String filetreepattern = "^content://com\\.android\\.externalstorage\\.documents/tree/.*";

        Uri filefulluri = null;

        String documentspath = appSettings.getString("folderpath", "").replaceFirst(providertree, "");

        String docExtension = ".mp4";

        String docMime = "video/mp4";

        Uri docParent = Uri.parse(appSettings.getString("folderpath", "") + "/document/" + documentspath);

        if (recordOnlyAudio == true) {
            fullFileName = "AudioRecording_" + timeString;
            docExtension = ".m4a";
            docMime = "audio/mp4";
        }

        if (appSettings.getString("folderpath", "").matches(filetreepattern)) {
            if (documentspath.startsWith("primary%3A")) {
                filefulluri = Uri.parse("/storage/emulated/0/" + Uri.decode(documentspath.replaceFirst("primary%3A", "")) + "/" + fullFileName + docExtension);
            } else {
                filefulluri = Uri.parse("/storage/" + Uri.decode(documentspath.replaceFirst("%3A", "/")) + "/" + fullFileName + docExtension);
            }
        }

        try {
            Uri outdocpath = DocumentsContract.createDocument(getContentResolver(), docParent, docMime, fullFileName);
            if (!outdocpath.toString().endsWith(".m4a") && recordOnlyAudio == true) {
                outdocpath = DocumentsContract.renameDocument(getContentResolver(), outdocpath, fullFileName + ".m4a");
            }

            if (outdocpath == null) {
                recordingError();
                if (activityBinder != null) {
                    activityBinder.resetDir();
                }
                stopSelf();
                return;
            } else {
                recordFilePath = outdocpath;
                recordFileMime = docMime;
                recordFilePathParent = docParent;
                recordFileFullPath = filefulluri;
            }
        } catch (FileNotFoundException e) {
            if (activityBinder != null) {
                recordingError();

                activityBinder.resetDir();
                stopSelf();
                return;
            }

        } catch (SecurityException e) {
            if (activityBinder != null) {
                recordingError();

                activityBinder.resetDir();
                stopSelf();
                return;
            }
        }

        timeStart = SystemClock.elapsedRealtime();

        Icon stopIcon = Icon.createWithResource(this, R.drawable.icon_stop_color_action);

        Icon recordingIcon = Icon.createWithResource(this, R.drawable.icon_record_status);

        Icon recordingIconLarge = Icon.createWithResource(this, R.drawable.icon_record_color_action_normal);

        Intent stopRecordIntent = new Intent(this, ScreenRecorder.class);

        stopRecordIntent.setAction(ACTION_STOP);

        PendingIntent stopRecordActionIntent = PendingIntent.getService(this, 0, stopRecordIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Action.Builder stopRecordAction = new Notification.Action.Builder(stopIcon, getString(R.string.notifications_stop), stopRecordActionIntent);


        Icon pauseIcon = Icon.createWithResource(this, R.drawable.icon_pause_color_action);

        Intent pauseRecordIntent = new Intent(this, ScreenRecorder.class);

        pauseRecordIntent.setAction(ACTION_PAUSE);

        PendingIntent pauseRecordActionIntent = PendingIntent.getService(this, 0, pauseRecordIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Action.Builder pauseRecordAction = new Notification.Action.Builder(pauseIcon, getString(R.string.notifications_pause), pauseRecordActionIntent);

        Notification.Builder notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL);
        } else {
            notification = new Notification.Builder(this);
        }

        notification = notification
            .setContentTitle(getString(R.string.recording_started_title))
            .setContentText(getString(R.string.recording_started_text))
            .setTicker(getString(R.string.recording_started_text))
            .setSmallIcon(recordingIcon)
            .setLargeIcon(recordingIconLarge)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis()-(SystemClock.elapsedRealtime()-timeStart))
            .setOngoing(true)
            .addAction(stopRecordAction.build());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notification.setPriority(Notification.PRIORITY_LOW);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.addAction(pauseRecordAction.build());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_RECORDING_ID, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_RECORDING_ID, notification.build());
        }



        if (activityBinder != null) {
            activityBinder.recordingStart();
        }

        DisplayMetrics metrics = new DisplayMetrics();

        ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(metrics);

        int width = 0;
        int height = 0;

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

        try {
            recordingFileDescriptor = getContentResolver().openFileDescriptor(recordFilePath, "rw").getFileDescriptor();
        } catch (Exception e) {
            recordingError();
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
            recordingVirtualDisplay = recordingMediaProjection.createVirtualDisplay("DroidRec", width, height, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, null, null, null);
        }

        isRestarting = false;

        int frameRate = (int)(((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRefreshRate());


        boolean customQuality = appSettings.getBoolean("customquality", false);

        float qualityScale = 0.1f * (appSettings.getInt("qualityscale", 9)+1);


        boolean customFPS = appSettings.getBoolean("customfps", false);

        int fpsValue = Integer.parseInt(appSettings.getString("fpsvalue", "30"));


        boolean customBitrate = appSettings.getBoolean("custombitrate", false);

        int bitrateValue = Integer.parseInt(appSettings.getString("bitratevalue", "0"));


        String customCodec = appSettings.getString("customcodec", getResources().getString(R.string.codec_option_auto_value));


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
                    recordingMediaRecorder.setAudioEncodingBitRate(44100*32*2);
                    recordingMediaRecorder.setAudioSamplingRate(44100);
                }

                if (recordOnlyAudio == false) {
                    recordingMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                    recordingMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                } else {
                    recordingMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
                }


                recordingMediaRecorder.setOutputFile(recordingFileDescriptor);

                if (recordOnlyAudio == false) {
                    recordingMediaRecorder.setVideoSize(width, height);

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
            recorderPlayback = new PlaybackRecorder(getApplicationContext(), recordOnlyAudio, recordingVirtualDisplay, recordingFileDescriptor, recordingMediaProjection, width, height, frameRate, recordMicrophone, recordPlayback, customQuality, qualityScale, customFPS, fpsValue, customBitrate, bitrateValue, (!customCodec.contentEquals(getResources().getString(R.string.codec_option_auto_value))), customCodec);

            recorderPlayback.start();
        }

        isActive = true;
    }

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
            }
        }

        finishedFileIntent = new Intent(Intent.ACTION_VIEW);
        finishedFileIntent.setDataAndType(Uri.parse("file://" + recordFileFullPath.toString()), recordFileMime);
        finishedFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent openFolderIntent = new Intent(this, ScreenRecorder.class);

        openFolderIntent.setAction(ACTION_ACTIVITY_FINISHED_FILE);

        PendingIntent openFolderActionIntent = PendingIntent.getService(this, 0, openFolderIntent, PendingIntent.FLAG_IMMUTABLE);

        deleteFinishedFileDocument = recordFilePath;

        Intent deleteRecordIntent = new Intent(this, ScreenRecorder.class);

        deleteRecordIntent.setAction(ACTION_ACTIVITY_DELETE_FINISHED_FILE);

        Icon deleteIcon = Icon.createWithResource(this, R.drawable.icon_record_delete_color_action);

        PendingIntent deleteRecordActionIntent = PendingIntent.getService(this, 0, deleteRecordIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Action.Builder deleteRecordAction = new Notification.Action.Builder(deleteIcon, getString(R.string.notifications_delete), deleteRecordActionIntent);

        shareFinishedFileIntent = new Intent(Intent.ACTION_SEND);
        shareFinishedFileIntent.setType(recordFileMime);
        shareFinishedFileIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + recordFileFullPath.toString()));
        shareFinishedFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        Intent shareRecordIntent = new Intent(this, ScreenRecorder.class);

        shareRecordIntent.setAction(ACTION_ACTIVITY_SHARE_FINISHED_FILE);

        Icon shareIcon = Icon.createWithResource(this, R.drawable.icon_record_share_color_action);

        PendingIntent shareRecordActionIntent = PendingIntent.getService(this, 0, shareRecordIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Action.Builder shareRecordAction = new Notification.Action.Builder(shareIcon, getString(R.string.notifications_share), shareRecordActionIntent);


        if (recordFileFullPath != null) {
            MediaScannerConnection.scanFile(ScreenRecorder.this, new String[] { recordFileFullPath.toString() }, null, null);
        }

        if (isRestarting == false) {
            Icon finishedIcon = Icon.createWithResource(this, R.drawable.icon_record_finished_status);

            Icon finishedIconLarge = Icon.createWithResource(this, R.drawable.icon_record_finished_color_action_normal);

            Notification.Builder finishedNotification;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                finishedNotification = new Notification.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL);
            } else {
                finishedNotification = new Notification.Builder(this);
            }

            finishedNotification = finishedNotification
                .setContentTitle(getString(R.string.recording_finished_title))
                .setContentText(getString(R.string.recording_finished_text))
                .setContentIntent(openFolderActionIntent)
                .setSmallIcon(finishedIcon)
                .setLargeIcon(finishedIconLarge)
                .addAction(shareRecordAction.build())
                .addAction(deleteRecordAction.build())
                .setAutoCancel(true);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                finishedNotification.setPriority(Notification.PRIORITY_LOW);
            }

            recordingNotificationManager.notify(NOTIFICATION_RECORDING_FINISHED_ID, finishedNotification.build());
        } else {
            Icon restartIcon = Icon.createWithResource(this, R.drawable.icon_rotate_status);

            Icon restartIconLarge = Icon.createWithResource(this, R.drawable.icon_rotate_color_action_normal);

            Notification.Builder restartNotification;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                restartNotification = new Notification.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL);
            } else {
                restartNotification = new Notification.Builder(this);
            }

            restartNotification = restartNotification
                .setContentTitle(getString(R.string.recording_rotated_title))
                .setContentText(getString(R.string.recording_rotated_text))
                .setContentIntent(openFolderActionIntent)
                .setSmallIcon(restartIcon)
                .setLargeIcon(restartIconLarge)
                .setAutoCancel(true);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                restartNotification.setPriority(Notification.PRIORITY_LOW);
            }

            recordingNotificationManager.notify(NOTIFICATION_RECORDING_FINISHED_ID, restartNotification.build());

        }

        stopForeground(Service.STOP_FOREGROUND_REMOVE);

        if (isRestarting == false) {
            stopSelf();
        }
    }

    private void screenRecordingPause() {
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

        Icon stopIcon = Icon.createWithResource(this, R.drawable.icon_stop_continue_color_action);

        Icon pausedIcon = Icon.createWithResource(this, R.drawable.icon_pause_status);

        Icon pausedIconLarge = Icon.createWithResource(this, R.drawable.icon_pause_color_action_normal);

        Intent stopRecordIntent = new Intent(this, ScreenRecorder.class);

        stopRecordIntent.setAction(ACTION_STOP);

        PendingIntent stopRecordActionIntent = PendingIntent.getService(this, 0, stopRecordIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Action.Builder stopRecordAction = new Notification.Action.Builder(stopIcon, getString(R.string.notifications_stop), stopRecordActionIntent);

        Icon continueIcon = Icon.createWithResource(this, R.drawable.icon_record_continue_color_action);

        Intent continueRecordIntent = new Intent(this, ScreenRecorder.class);

        continueRecordIntent.setAction(ACTION_CONTINUE);

        PendingIntent continueRecordActionIntent = PendingIntent.getService(this, 0, continueRecordIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Action.Builder continueRecordAction = new Notification.Action.Builder(continueIcon, getString(R.string.notifications_resume), continueRecordActionIntent);

        Notification.Builder notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL);
        } else {
            notification = new Notification.Builder(this);
        }

        notification = notification
            .setContentTitle(getString(R.string.recording_paused_title))    
            .setContentText(getString(R.string.recording_paused_text))
            .setSmallIcon(pausedIcon)
            .setLargeIcon(pausedIconLarge)
            .setOngoing(true)
            .addAction(stopRecordAction.build())
            .addAction(continueRecordAction.build());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notification.setPriority(Notification.PRIORITY_LOW);
        }

        recordingNotificationManager.notify(NOTIFICATION_RECORDING_ID, notification.build());
    }

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

        Icon stopIcon = Icon.createWithResource(this, R.drawable.icon_stop_color_action);

        Icon recordingIcon = Icon.createWithResource(this, R.drawable.icon_record_status);

        Icon recordingIconLarge = Icon.createWithResource(this, R.drawable.icon_record_color_action_normal);

        Intent stopRecordIntent = new Intent(this, ScreenRecorder.class);

        stopRecordIntent.setAction(ACTION_STOP);

        PendingIntent stopRecordActionIntent = PendingIntent.getService(this, 0, stopRecordIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Action.Builder stopRecordAction = new Notification.Action.Builder(stopIcon, getString(R.string.notifications_stop), stopRecordActionIntent);


        Icon pauseIcon = Icon.createWithResource(this, R.drawable.icon_pause_color_action);

        Intent pauseRecordIntent = new Intent(this, ScreenRecorder.class);

        pauseRecordIntent.setAction(ACTION_PAUSE);

        PendingIntent pauseRecordActionIntent = PendingIntent.getService(this, 0, pauseRecordIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Action.Builder pauseRecordAction = new Notification.Action.Builder(pauseIcon, getString(R.string.notifications_pause), pauseRecordActionIntent);

        Notification.Builder notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL);
        } else {
            notification = new Notification.Builder(this);
        }

        notification = notification
            .setContentTitle(getString(R.string.recording_started_title))
            .setContentText(getString(R.string.recording_started_text))
            .setTicker(getString(R.string.recording_started_text))
            .setSmallIcon(recordingIcon)
            .setLargeIcon(recordingIconLarge)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis()-(SystemClock.elapsedRealtime()-timeStart))
            .setOngoing(true)
            .addAction(stopRecordAction.build())
            .addAction(pauseRecordAction.build());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notification.setPriority(Notification.PRIORITY_LOW);
        }

        recordingNotificationManager.notify(NOTIFICATION_RECORDING_ID, notification.build());
    }

}
