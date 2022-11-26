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
import android.os.Build;
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

import com.yakovlevegor.DroidRec.R;

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
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_continue_color_action_big));
            } else if (panelSize.contentEquals("Normal") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_continue_color_action_normal));
            } else if (panelSize.contentEquals("Small") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_continue_color_action_small));
            } else if (panelSize.contentEquals("Little") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_continue_color_action_little));
            }
            setControlState(true);
        }

        void setResume(long time) {
            recordingProgress.setBase(time);
            recordingProgress.start();

            if (panelSize.contentEquals("Large") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_color_action_big));
            } else if (panelSize.contentEquals("Normal") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_color_action_normal));
            } else if (panelSize.contentEquals("Small") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_color_action_small));
            } else if (panelSize.contentEquals("Little") == true) {
                stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_color_action_little));
            }
            setControlState(false);
        }

        void setStop() {
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
                } else if (panelSize.contentEquals("Little") == true) {
                    appSettingsEditor.putInt("panelpositionhorizontalxlittle", floatWindowLayoutParam.x);
                    appSettingsEditor.putInt("panelpositionhorizontalylittle", floatWindowLayoutParam.y);

                    appSettingsEditor.putBoolean("panelpositionhorizontalhiddenlittle", panelHidden);
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
                } else if (panelSize.contentEquals("Little") == true) {
                    appSettingsEditor.putInt("panelpositionverticalxlittle", floatWindowLayoutParam.x);
                    appSettingsEditor.putInt("panelpositionverticalylittle", floatWindowLayoutParam.y);

                    appSettingsEditor.putBoolean("panelpositionverticalhiddenlittle", panelHidden);
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

        display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        Point screenSize = new Point();

        display.getSize(screenSize);

        displayWidth = screenSize.x;
        displayHeight = screenSize.y;

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
                floatWindowLayoutParam.width = (int)panelWidth;
            } else {
                y = y+((panelHeight/2)-(panelWeightHidden/2));
                floatWindowLayoutParam.height = (int)panelWidth;
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
                floatWindowLayoutParam.width = (int)panelWidth;
            } else {
                y = y-((panelHeight/2)-(panelWeightHidden/2));
                floatWindowLayoutParam.height = (int)panelHeight;
            }

        }

        floatWindowLayoutParam.x = x;
        floatWindowLayoutParam.y = y;

    }

    public void startRecord() {
        updateMetrics();

        if (displayWidth > displayHeight) {
            isHorizontal = true;
        } else {
            isHorizontal = false;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

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
            } else if (panelSize.contentEquals("Little") == true) {
                panelHidden = appSettings.getBoolean("panelpositionhorizontalhiddenlittle", false);
            }
        } else {
            if (panelSize.contentEquals("Large") == true) {
                panelHidden = appSettings.getBoolean("panelpositionverticalhiddenbig", false);
            } else if (panelSize.contentEquals("Normal") == true) {
                panelHidden = appSettings.getBoolean("panelpositionverticalhiddennormal", false);
            } else if (panelSize.contentEquals("Small") == true) {
                panelHidden = appSettings.getBoolean("panelpositionverticalhiddensmall", false);
            } else if (panelSize.contentEquals("Little") == true) {
                panelHidden = appSettings.getBoolean("panelpositionverticalhiddenlittle", false);
            }
        }

        String darkTheme = appSettings.getString("darktheme", getResources().getString(R.string.dark_theme_option_auto));

        boolean applyDarkTheme = (((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES && darkTheme.contentEquals(getResources().getString(R.string.dark_theme_option_auto))) || darkTheme.contentEquals("Dark"));

        if (applyDarkTheme == true) {
            getBaseContext().setTheme(android.R.style.Theme_Material);
        } else {
            getBaseContext().setTheme(android.R.style.Theme_Material_Light);
        }

        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);

        if (isHorizontal == true) {
            if (panelSize.contentEquals("Large") == true) {
                floatingPanel = (ViewGroup) inflater.inflate(R.layout.panel_float_big, null);
            } else if (panelSize.contentEquals("Normal") == true) {
                floatingPanel = (ViewGroup) inflater.inflate(R.layout.panel_float_normal, null);
            } else if (panelSize.contentEquals("Small") == true) {
                floatingPanel = (ViewGroup) inflater.inflate(R.layout.panel_float_small, null);
            } else if (panelSize.contentEquals("Little") == true) {
                floatingPanel = (ViewGroup) inflater.inflate(R.layout.panel_float_little, null);
            }
        } else {
            if (panelSize.contentEquals("Large") == true) {
                floatingPanel = (ViewGroup) inflater.inflate(R.layout.panel_float_vertical_big, null);
            } else if (panelSize.contentEquals("Normal") == true) {
                floatingPanel = (ViewGroup) inflater.inflate(R.layout.panel_float_vertical_normal, null);
            } else if (panelSize.contentEquals("Small") == true) {
                floatingPanel = (ViewGroup) inflater.inflate(R.layout.panel_float_vertical_small, null);
            } else if (panelSize.contentEquals("Little") == true) {
                floatingPanel = (ViewGroup) inflater.inflate(R.layout.panel_float_vertical_little, null);
            }
        }

        layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        viewHandle = (ImageView) floatingPanel.findViewById(R.id.floatingpanelhandle);

        LinearLayout viewBackground = (LinearLayout) floatingPanel.findViewById(R.id.panelwithbackground);

        if (applyDarkTheme == true) {
            viewBackground.setBackgroundDrawable(getResources().getDrawable(R.drawable.floatingpanel_shape_dark));
            viewHandle.setImageResource(R.drawable.floatingpanel_shape_dark);
        }

        int opacityLevel = appSettings.getInt("floatingcontrolsopacity", 9);
        float opacityValue = (float)((opacityLevel+1)*0.1f);
        viewBackground.setAlpha(opacityValue);

        LinearLayout viewSized = (LinearLayout) floatingPanel.findViewById(R.id.panelwrapped);

        DisplayMetrics metricsPanel = new DisplayMetrics();
        display.getRealMetrics(metricsPanel);

        viewBackground.measure(0, 0);

        panelWidthNormal = viewBackground.getMeasuredWidth();
        panelHeight = viewBackground.getMeasuredHeight();

        if (panelSize.contentEquals("Large") == true) {
            panelWeightHidden = (int)((50*metricsPanel.density)+0.5f);
        } else if (panelSize.contentEquals("Normal") == true) {
            panelWeightHidden = (int)((40*metricsPanel.density)+0.5f);
        } else if (panelSize.contentEquals("Small") == true) {
            panelWeightHidden = (int)((30*metricsPanel.density)+0.5f);
        } else if (panelSize.contentEquals("Little") == true) {
            panelWeightHidden = (int)((20*metricsPanel.density)+0.5f);
        }

        if (panelHidden == true) {
            if (isHorizontal == true) {
                floatWindowLayoutParam = new WindowManager.LayoutParams(panelWeightHidden, panelHeight, layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            } else {
                floatWindowLayoutParam = new WindowManager.LayoutParams(panelWidthNormal, panelWeightHidden, layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            }
        } else {
            if (isHorizontal == true) {
                floatWindowLayoutParam = new WindowManager.LayoutParams(panelWidthNormal, panelHeight, layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            } else {
                floatWindowLayoutParam = new WindowManager.LayoutParams(panelWidthNormal, panelHeight, layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            }
        }

        floatWindowLayoutParam.gravity = Gravity.CENTER;

        if (isHorizontal == true) {
            if (panelSize.contentEquals("Large") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionhorizontalxbig", (int)((displayWidth/2)-(panelWidth/2)));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionhorizontalybig", 0);
            } else if (panelSize.contentEquals("Normal") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionhorizontalxnormal", (int)((displayWidth/2)-(panelWidth/2)));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionhorizontalynormal", 0);
            } else if (panelSize.contentEquals("Small") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionhorizontalxsmall", (int)((displayWidth/2)-(panelWidth/2)));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionhorizontalysmall", 0);
            } else if (panelSize.contentEquals("Little") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionhorizontalxlittle", (int)((displayWidth/2)-(panelWidth/2)));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionhorizontalylittle", 0);
            }
        } else {
            if (panelSize.contentEquals("Large") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionverticalxbig", (int)((displayWidth/2)-(panelWidth/2)));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionverticalybig", 0);
            } else if (panelSize.contentEquals("Normal") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionverticalxnormal", (int)((displayWidth/2)-(panelWidth/2)));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionverticalynormal", 0);
            } else if (panelSize.contentEquals("Small") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionverticalxsmall", (int)((displayWidth/2)-(panelWidth/2)));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionverticalysmall", 0);
            } else if (panelSize.contentEquals("Little") == true) {
                floatWindowLayoutParam.x = appSettings.getInt("panelpositionverticalxlittle", (int)((displayWidth/2)-(panelWidth/2)));
                floatWindowLayoutParam.y = appSettings.getInt("panelpositionverticalylittle", 0);
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
             recordingProgress.setBase(recordingPanelBinder.getTimeStart());
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

                return false;
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() == ACTION_RECORD_PANEL) {
                startAction = ACTION_RECORD_PANEL;
            } else if (intent.getAction() == ACTION_POSITION_PANEL) {
                startAction = ACTION_POSITION_PANEL;
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
