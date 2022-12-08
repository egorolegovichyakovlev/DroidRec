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

import android.util.DisplayMetrics;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.app.Service;
import android.view.WindowManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowMetrics;
import android.os.Build;
import android.annotation.TargetApi;
import android.os.Binder;
import android.os.IBinder;
import android.content.Intent;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.view.Gravity;
import android.view.MotionEvent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.widget.Chronometer;
import android.os.SystemClock;
import android.hardware.display.DisplayManager;

import com.yakovlevegor.DroidRec.R;

@TargetApi(Build.VERSION_CODES.O)
public class FloatingControls extends Service {

    private ViewGroup floatingPanel;

    private int layoutType;

    private WindowManager windowManager;

    private WindowManager.LayoutParams floatWindowLayoutParam;

    private boolean isHorizontal = true;

    private Display display;

    private int displayWidth;

    private int displayHeight;

    private SharedPreferences appSettings;

    private SharedPreferences.Editor appSettingsEditor;

    private ScreenRecorder.RecordingPanelBinder recordingPanelBinder;

    private ImageButton pauseButton;

    private ImageButton stopButton;

    private ImageButton resumeButton;

    private ImageView viewHandle;

    private Chronometer recordingProgress;

    private boolean recordingPaused = false;

    private boolean panelHidden = false;

    private int panelWidthNormal;

    private int panelWidth;

    private int panelWeightHidden;

    private int panelHeight;

    public static String ACTION_RECORD_PANEL = MainActivity.appName+".PANEL_RECORD";

    public static String ACTION_POSITION_PANEL = MainActivity.appName+".PANEL_POSITION";

    private String startAction;

    private int panelPositionX;

    private int panelPositionY;

    private String panelSize;

    private int orientationOnStart;

    private SensorManager sensor;

    private long timerStart;

    private boolean isStopped = true;

    private int widthNormal;

    private int heightNormal;

    private float densityNormal;

    private boolean isRestarting = false;

    private SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent e) {

            if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER && isStopped == false) {
            int newRot = display.getRotation();
                if (orientationOnStart != newRot) {
                    orientationOnStart = newRot;
                    timerStart = recordingProgress.getBase();
                    if (isStopped == false) {
                        closePanel();
                        startRecord();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    public class PanelBinder extends Binder {
        void setConnectPanel(ScreenRecorder.RecordingPanelBinder lbinder) {
            FloatingControls.this.actionConnectPanel(lbinder);
        }

        void setDisconnectPanel() {
            FloatingControls.this.actionDisconnectPanel();
        }

        void setPause(long time) {
            recordingProgress.setBase(SystemClock.elapsedRealtime()-time);
            recordingProgress.stop();

            if (panelSize.contentEquals("Large") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_continue_color_action_big, getTheme()));
            } else if (panelSize.contentEquals("Normal") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_continue_color_action_normal, getTheme()));
            } else if (panelSize.contentEquals("Small") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_continue_color_action_small, getTheme()));
            } else if (panelSize.contentEquals("Tiny") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_continue_color_action_tiny, getTheme()));
            }
            setControlState(true);
        }

        void setResume(long time) {
            recordingProgress.setBase(time);
            recordingProgress.start();

            if (panelSize.contentEquals("Large") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_color_action_big, getTheme()));
            } else if (panelSize.contentEquals("Normal") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_color_action_normal, getTheme()));
            } else if (panelSize.contentEquals("Small") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_color_action_small, getTheme()));
            } else if (panelSize.contentEquals("Tiny") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_color_action_tiny, getTheme()));
            }
            setControlState(false);
        }

        void setRestart(int orient) {
            isRestarting = true;
            orientationOnStart = orient;
            timerStart = SystemClock.elapsedRealtime();
            closePanel();
            startRecord();
        }

        void setStop() {
            isStopped = true;
            closePanel();
        }

    }

    private final IBinder panelBinder = new PanelBinder();


    public class PanelPositionBinder extends Binder {
        void setStop() {
            if (isHorizontal == true) {
                if (panelSize.contentEquals("Large") == true) {
                    appSettingsEditor.putInt("panelpositionhorizontalxbig", floatWindowLayoutParam.x);
                    appSettingsEditor.putInt("panelpositionhorizontalybig", floatWindowLayoutParam.y);

                    appSettingsEditor.putBoolean("panelpositionhorizontalhiddenbig", panelHidden);
                } else if (panelSize.contentEquals("Normal") == true) {
                    appSettingsEditor.putInt("panelpositionhorizontalxnormal", floatWindowLayoutParam.x);
                    appSettingsEditor.putInt("panelpositionhorizontalynormal", floatWindowLayoutParam.y);

                    appSettingsEditor.putBoolean("panelpositionhorizontalhiddennormal", panelHidden);
                } else if (panelSize.contentEquals("Small") == true) {
                    appSettingsEditor.putInt("panelpositionhorizontalxsmall", floatWindowLayoutParam.x);
                    appSettingsEditor.putInt("panelpositionhorizontalysmall", floatWindowLayoutParam.y);

                    appSettingsEditor.putBoolean("panelpositionhorizontalhiddensmall", panelHidden);
                } else if (panelSize.contentEquals("Tiny") == true) {
                    appSettingsEditor.putInt("panelpositionhorizontalxtiny", floatWindowLayoutParam.x);
                    appSettingsEditor.putInt("panelpositionhorizontalytiny", floatWindowLayoutParam.y);

                    appSettingsEditor.putBoolean("panelpositionhorizontalhiddentiny", panelHidden);
                }
                appSettingsEditor.commit();
            } else {
                if (panelSize.contentEquals("Large") == true) {
                    appSettingsEditor.putInt("panelpositionverticalxbig", floatWindowLayoutParam.x);
                    appSettingsEditor.putInt("panelpositionverticalybig", floatWindowLayoutParam.y);

                    appSettingsEditor.putBoolean("panelpositionverticalhiddenbig", panelHidden);
                } else if (panelSize.contentEquals("Normal") == true) {
                    appSettingsEditor.putInt("panelpositionverticalxnormal", floatWindowLayoutParam.x);
                    appSettingsEditor.putInt("panelpositionverticalynormal", floatWindowLayoutParam.y);

                    appSettingsEditor.putBoolean("panelpositionverticalhiddennormal", panelHidden);
                } else if (panelSize.contentEquals("Small") == true) {
                    appSettingsEditor.putInt("panelpositionverticalxsmall", floatWindowLayoutParam.x);
                    appSettingsEditor.putInt("panelpositionverticalysmall", floatWindowLayoutParam.y);

                    appSettingsEditor.putBoolean("panelpositionverticalhiddensmall", panelHidden);
                } else if (panelSize.contentEquals("Tiny") == true) {
                    appSettingsEditor.putInt("panelpositionverticalxtiny", floatWindowLayoutParam.x);
                    appSettingsEditor.putInt("panelpositionverticalytiny", floatWindowLayoutParam.y);

                    appSettingsEditor.putBoolean("panelpositionverticalhiddentiny", panelHidden);
                }
                appSettingsEditor.commit();
            }

            closePanel();
        }

    }

    private final IBinder panelPositionBinder = new PanelPositionBinder();


    public void actionConnectPanel(ScreenRecorder.RecordingPanelBinder service) {
        recordingPanelBinder = service;
    }

    public void actionDisconnectPanel() {
        recordingPanelBinder = null;
    }

    private void setControlState(boolean paused) {
        recordingPaused = paused;

        if (panelHidden == false) {
            if (paused == true) {
                pauseButton.setVisibility(View.GONE);
                resumeButton.setVisibility(View.VISIBLE);
            } else {
                pauseButton.setVisibility(View.VISIBLE);
                resumeButton.setVisibility(View.GONE);
            }
        }
    }

    private void updateMetrics() {

        if (orientationOnStart == Surface.ROTATION_90 || orientationOnStart == Surface.ROTATION_270) {
            displayWidth = heightNormal;
            displayHeight = widthNormal;
        } else {
            displayWidth = widthNormal;
            displayHeight = heightNormal;
        }

    }

    private void checkBoundaries() {
        double xval = floatWindowLayoutParam.x;
        double yval = floatWindowLayoutParam.y;

        if (panelHidden == false) {
            if (isHorizontal == true) {
                if ((int)(xval-(panelWidth/2)) < -(displayWidth/2)) {
                    xval = (float)(-(displayWidth/2)+(panelWidth/2));
                } else if ((int)(xval+(panelWidth/2)) > (displayWidth/2)) {
                    xval = (float)((displayWidth/2)-(panelWidth/2));
                }
                if ((int)(yval-(panelHeight/2)) < -(displayHeight/2)) {
                    yval = (float)(-(displayHeight/2)+(panelHeight/2));
                } else if ((int)(yval+(panelHeight/2)) > (displayHeight/2)) {
                    yval = (float)((displayHeight/2)-(panelHeight/2));
                }
            } else {
                if ((int)(xval-(panelWidth/2)) < -(displayWidth/2)) {
                    xval = (float)(-(displayWidth/2)+(panelWidth/2));
                } else if ((int)(xval+(panelWidth/2)) > (displayWidth/2)) {
                    xval = (float)((displayWidth/2)-(panelWidth/2));
                }
                if ((int)(yval-(panelHeight/2)) < -(displayHeight/2)) {
                    yval = (float)(-(displayHeight/2)+(panelHeight/2));
                } else if ((int)(yval+(panelHeight/2)) > (displayHeight/2)) {
                    yval = (float)((displayHeight/2)-(panelHeight/2));
                }
            }
        } else {
            if (isHorizontal == true) {
                if ((int)(xval-(panelWeightHidden/2)) < -(displayWidth/2)) {
                    xval = (float)(-(displayWidth/2)+(panelWeightHidden/2));
                } else if ((int)(xval+(panelWeightHidden/2)) > (displayWidth/2)) {
                    xval = (float)((displayWidth/2)-(panelWeightHidden/2));
                }
                if ((int)(yval-(panelHeight/2)) < -(displayHeight/2)) {
                    yval = (float)(-(displayHeight/2)+(panelHeight/2));
                } else if ((int)(yval+(panelHeight/2)) > (displayHeight/2)) {
                    yval = (float)((displayHeight/2)-(panelHeight/2));
                }
            } else {
                if ((int)(xval-(panelWidth/2)) < -(displayWidth/2)) {
                    xval = (float)(-(displayWidth/2)+(panelWidth/2));
                } else if ((int)(xval+(panelWidth/2)) > (displayWidth/2)) {
                    xval = (float)((displayWidth/2)-(panelWidth/2));
                }
                if ((int)(yval-(panelWeightHidden/2)) < -(displayHeight/2)) {
                    yval = (float)(-(displayHeight/2)+(panelWeightHidden/2));
                } else if ((int)(yval+(panelWeightHidden/2)) > (displayHeight/2)) {
                    yval = (float)((displayHeight/2)-(panelWeightHidden/2));
                }
            }
        }

        floatWindowLayoutParam.x = (int)xval;
        floatWindowLayoutParam.y = (int)yval;

    }

    private void togglePanel() {

        int x = floatWindowLayoutParam.x;
        int y = floatWindowLayoutParam.y;

        if (panelHidden == false) {
            panelHidden = true;
            stopButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.GONE);
            resumeButton.setVisibility(View.GONE);
            viewHandle.setVisibility(View.GONE);
            recordingProgress.setVisibility(View.GONE);
            panelWidth = panelWeightHidden;

            if (isHorizontal == true) {
                x = x+((panelWidthNormal/2)-(panelWeightHidden/2));
                floatWindowLayoutParam.width = panelWidth;
            } else {
                y = y+((panelHeight/2)-(panelWeightHidden/2));
                floatWindowLayoutParam.height = panelWidth;
            }

        } else {
            panelHidden = false;
            stopButton.setVisibility(View.VISIBLE);
            setControlState(recordingPaused);
            recordingProgress.setVisibility(View.VISIBLE);
            viewHandle.setVisibility(View.VISIBLE);

            panelWidth = panelWidthNormal;

            if (isHorizontal == true) {
                x = x-((panelWidthNormal/2)-(panelWeightHidden/2));
                floatWindowLayoutParam.width = panelWidth;
            } else {
                y = y-((panelHeight/2)-(panelWeightHidden/2));
                floatWindowLayoutParam.height = panelHeight;
            }

        }

        floatWindowLayoutParam.x = x;
        floatWindowLayoutParam.y = y;

    }

    @Override
    public void onCreate() {
        super.onCreate();

        display = ((DisplayManager)(getBaseContext().getSystemService(Context.DISPLAY_SERVICE))).getDisplay(Display.DEFAULT_DISPLAY);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        sensor = (SensorManager)getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        sensor.registerListener(sensorListener, sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
    }

    public void startRecord() {

        updateMetrics();

        if (orientationOnStart == Surface.ROTATION_90 || orientationOnStart == Surface.ROTATION_270) {
            isHorizontal = true;
        } else {
            isHorizontal = false;
        }

        appSettings = getSharedPreferences(ScreenRecorder.prefsident, 0);

        appSettingsEditor = appSettings.edit();

        panelSize = appSettings.getString("floatingcontrolssize", getResources().getString(R.string.floating_controls_size_option_auto_value));

        if (isHorizontal == true) {
            if (panelSize.contentEquals("Large") == true) {
                panelHidden = appSettings.getBoolean("panelpositionhorizontalhiddenbig", false);
            } else if (panelSize.contentEquals("Normal") == true) {
                panelHidden = appSettings.getBoolean("panelpositionhorizontalhiddennormal", false);
            } else if (panelSize.contentEquals("Small") == true) {
                panelHidden = appSettings.getBoolean("panelpositionhorizontalhiddensmall", false);
            } else if (panelSize.contentEquals("Tiny") == true) {
                panelHidden = appSettings.getBoolean("panelpositionhorizontalhiddentiny", false);
            }
        } else {
            if (panelSize.contentEquals("Large") == true) {
                panelHidden = appSettings.getBoolean("panelpositionverticalhiddenbig", false);
            } else if (panelSize.contentEquals("Normal") == true) {
                panelHidden = appSettings.getBoolean("panelpositionverticalhiddennormal", false);
            } else if (panelSize.contentEquals("Small") == true) {
                panelHidden = appSettings.getBoolean("panelpositionverticalhiddensmall", false);
            } else if (panelSize.contentEquals("Tiny") == true) {
                panelHidden = appSettings.getBoolean("panelpositionverticalhiddentiny", false);
            }
        }

        String darkTheme = appSettings.getString("darkthemeapplied", getResources().getString(R.string.dark_theme_option_auto));

        boolean applyDarkTheme = (((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES && darkTheme.contentEquals(getResources().getString(R.string.dark_theme_option_auto))) || darkTheme.contentEquals("Dark"));

        if (applyDarkTheme == true) {
            getBaseContext().setTheme(R.style.Theme_AppCompat);
        } else {
            getBaseContext().setTheme(R.style.Theme_AppCompat_Light);
        }

        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);

        if (isHorizontal == true) {
            if (panelSize.contentEquals("Large") == true) {
                floatingPanel = (ViewGroup) View.inflate(getBaseContext(), R.layout.panel_float_big, null);
            } else if (panelSize.contentEquals("Normal") == true) {
                floatingPanel = (ViewGroup) View.inflate(getBaseContext(), R.layout.panel_float_normal, null);
            } else if (panelSize.contentEquals("Small") == true) {
                floatingPanel = (ViewGroup) View.inflate(getBaseContext(), R.layout.panel_float_small, null);
            } else if (panelSize.contentEquals("Tiny") == true) {
                floatingPanel = (ViewGroup) View.inflate(getBaseContext(), R.layout.panel_float_tiny, null);
            }
        } else {
            if (panelSize.contentEquals("Large") == true) {
                floatingPanel = (ViewGroup) View.inflate(getBaseContext(), R.layout.panel_float_vertical_big, null);
            } else if (panelSize.contentEquals("Normal") == true) {
                floatingPanel = (ViewGroup) View.inflate(getBaseContext(), R.layout.panel_float_vertical_normal, null);
            } else if (panelSize.contentEquals("Small") == true) {
                floatingPanel = (ViewGroup) View.inflate(getBaseContext(), R.layout.panel_float_vertical_small, null);
            } else if (panelSize.contentEquals("Tiny") == true) {
                floatingPanel = (ViewGroup) View.inflate(getBaseContext(), R.layout.panel_float_vertical_tiny, null);
            }
        }

        layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        viewHandle = (ImageView) floatingPanel.findViewById(R.id.floatingpanelhandle);

        LinearLayout viewBackground = (LinearLayout) floatingPanel.findViewById(R.id.panelwithbackground);

        if (applyDarkTheme == true) {
            viewBackground.setBackground(getResources().getDrawable(R.drawable.floatingpanel_shape_dark, getTheme()));
            viewHandle.setImageResource(R.drawable.floatingpanel_shape_dark);
        }

        int opacityLevel = appSettings.getInt("floatingcontrolsopacity", 9);
        float opacityValue = (opacityLevel+1)*0.1f;
        viewBackground.setAlpha(opacityValue);

        LinearLayout viewSized = (LinearLayout) floatingPanel.findViewById(R.id.panelwrapped);

        viewBackground.measure(0, 0);

        panelWidthNormal = viewBackground.getMeasuredWidth();
        panelHeight = viewBackground.getMeasuredHeight();

        if (panelSize.contentEquals("Large") == true) {
            panelWeightHidden = (int)((50*densityNormal)+0.5f);
        } else if (panelSize.contentEquals("Normal") == true) {
            panelWeightHidden = (int)((40*densityNormal)+0.5f);
        } else if (panelSize.contentEquals("Small") == true) {
            panelWeightHidden = (int)((30*densityNormal)+0.5f);
        } else if (panelSize.contentEquals("Tiny") == true) {
            panelWeightHidden = (int)((20*densityNormal)+0.5f);
        }

        if (panelHidden == true) {
            panelWidth = panelWeightHidden;
            if (isHorizontal == true) {
                floatWindowLayoutParam = new WindowManager.LayoutParams(panelWeightHidden, panelHeight, layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            } else {
                floatWindowLayoutParam = new WindowManager.LayoutParams(panelWidthNormal, panelWeightHidden, layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            }
        } else {
            panelWidth = panelWidthNormal;
            if (isHorizontal == true) {
                floatWindowLayoutParam = new WindowManager.LayoutParams(panelWidthNormal, panelHeight, layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            } else {
                floatWindowLayoutParam = new WindowManager.LayoutParams(panelWidthNormal, panelHeight, layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            }
        }

        floatWindowLayoutParam.gravity = Gravity.CENTER;

        if (isHorizontal == true) {
            if (panelSize.contentEquals("Large") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionhorizontalxbig", (displayWidth/2)-(panelWidth/2));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionhorizontalybig", 0);
            } else if (panelSize.contentEquals("Normal") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionhorizontalxnormal", (displayWidth/2)-(panelWidth/2));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionhorizontalynormal", 0);
            } else if (panelSize.contentEquals("Small") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionhorizontalxsmall", (displayWidth/2)-(panelWidth/2));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionhorizontalysmall", 0);
            } else if (panelSize.contentEquals("Tiny") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionhorizontalxtiny", (displayWidth/2)-(panelWidth/2));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionhorizontalytiny", 0);
            }
        } else {
            if (panelSize.contentEquals("Large") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionverticalxbig", (displayWidth/2)-(panelWidth/2));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionverticalybig", 0);
            } else if (panelSize.contentEquals("Normal") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionverticalxnormal", (displayWidth/2)-(panelWidth/2));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionverticalynormal", 0);
            } else if (panelSize.contentEquals("Small") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionverticalxsmall", (displayWidth/2)-(panelWidth/2));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionverticalysmall", 0);
            } else if (panelSize.contentEquals("Tiny") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionverticalxtiny", (displayWidth/2)-(panelWidth/2));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionverticalytiny", 0);
            }
        }

        checkBoundaries();

        windowManager.addView(floatingPanel, floatWindowLayoutParam);

        pauseButton = (ImageButton) floatingPanel.findViewById(R.id.recordpausebuttonfloating);
        stopButton = (ImageButton) floatingPanel.findViewById(R.id.recordstopbuttonfloating);
        resumeButton = (ImageButton) floatingPanel.findViewById(R.id.recordresumebuttonfloating);
        recordingProgress = (Chronometer) floatingPanel.findViewById(R.id.timerrecordfloating);

        resumeButton.setVisibility(View.GONE);

        if (panelHidden == true) {

            stopButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.GONE);
            resumeButton.setVisibility(View.GONE);
            viewHandle.setVisibility(View.GONE);
            recordingProgress.setVisibility(View.GONE);

        } else {

            stopButton.setVisibility(View.VISIBLE);
            setControlState(recordingPaused);
            recordingProgress.setVisibility(View.VISIBLE);
            viewHandle.setVisibility(View.VISIBLE);

        }

        if (startAction == ACTION_RECORD_PANEL) {
            stopButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (recordingPanelBinder != null) {
                        recordingPanelBinder.stopService();
                    }
                    closePanel();
                }
            });

            pauseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (recordingPanelBinder != null) {
                        recordingPanelBinder.recordingPause();
                    }
                    setControlState(true);
                }
            });

            resumeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (recordingPanelBinder != null) {
                        recordingPanelBinder.recordingResume();
                    }
                    setControlState(false);
                }
            });
            if (isRestarting == false) {
                recordingProgress.setBase(timerStart);
            }
            recordingProgress.start();

        }

        floatingPanel.setOnTouchListener(new View.OnTouchListener() {

            double x;
            double y;
            double px;
            double py;

            double touchX;
            double touchY;

            int motionPrevX = 0;
            int motionPrevY = 0;

            int touchmotionX = 0;
            int touchmotionY = 0;

            int threshold = 10;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        updateMetrics();
                        checkBoundaries();
                        windowManager.updateViewLayout(floatingPanel, floatWindowLayoutParam);

                        x = floatWindowLayoutParam.x;
                        y = floatWindowLayoutParam.y;

                        px = event.getRawX();
                        py = event.getRawY();

                        touchX = x;
                        touchY = y;

                        motionPrevX = (int)(x);
                        motionPrevY = (int)(y);

                        touchmotionX = 0;
                        touchmotionY = 0;

                        break;
                    case MotionEvent.ACTION_MOVE:
                        floatWindowLayoutParam.x = (int) ((x + event.getRawX()) - px);
                        floatWindowLayoutParam.y = (int) ((y + event.getRawY()) - py);

                        int motionNewX = floatWindowLayoutParam.x - motionPrevX;
                        int motionNewY = floatWindowLayoutParam.y - motionPrevY;

                        if (motionNewX < 0) {
                            motionNewX = -motionNewX;
                        }
                        if (motionNewY < 0) {
                            motionNewY = -motionNewY;
                        }

                        if (touchmotionX < threshold) {
                            touchmotionX += motionNewX;
                        }

                        if (touchmotionY < threshold) {
                            touchmotionY += motionNewY;
                        }

                        motionPrevX = floatWindowLayoutParam.x;
                        motionPrevY = floatWindowLayoutParam.y;

                        windowManager.updateViewLayout(floatingPanel, floatWindowLayoutParam);

                        break;
                    case MotionEvent.ACTION_UP:

                        if (touchmotionX < threshold && touchmotionY < threshold) {

                            floatWindowLayoutParam.x = (int)(touchX);
                            floatWindowLayoutParam.y = (int)(touchY);

                            togglePanel();

                        }

                        checkBoundaries();
                        windowManager.updateViewLayout(floatingPanel, floatWindowLayoutParam);

                        break;
                }
                v.performClick();

                return false;
            }
        });

    }

    @Override
    @SuppressWarnings("deprecation")
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            isRestarting = false;
            if (intent.getAction() == ACTION_RECORD_PANEL) {
                isStopped = false;
                startAction = ACTION_RECORD_PANEL;
            } else if (intent.getAction() == ACTION_POSITION_PANEL) {
                isStopped = true;
                startAction = ACTION_POSITION_PANEL;
            }

            Point screenSize = new Point();

            display.getSize(screenSize);

            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);

            densityNormal = metrics.density;

            orientationOnStart = display.getRotation();

            if (orientationOnStart == Surface.ROTATION_90 || orientationOnStart == Surface.ROTATION_270) {
                widthNormal = screenSize.y;
                heightNormal = screenSize.x;
            } else {
                widthNormal = screenSize.x;
                heightNormal = screenSize.y;
            }

            if (recordingPanelBinder != null) {
                timerStart = recordingPanelBinder.getTimeStart();
            } else {
                timerStart = 0;
            }
            startRecord();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getAction() == ACTION_POSITION_PANEL) {
            return panelPositionBinder;
        }

        return panelBinder;
    }

    public void closePanel() {
        windowManager.removeView(floatingPanel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
    }

}
