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

    private ScreenRecorder.RecordingPanelBinder recordingPanelBinder;

    public static String ACTION_CONNECT_PANEL = MainActivity.appName+".ACTION_CONNECT_PANEL";

    private ImageButton pauseButton;

    private ImageButton stopButton;

    private ImageButton resumeButton;

    private Chronometer recordingProgress;

    private boolean recordingPaused = false;

    private boolean panelHidden = false;

    private int panelWidthNormal;

    private int panelWidth;

    private int panelWidthHidden;

    private int panelHeight;

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

            stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_continue_color_action_large));
            setControlState(true);
        }

        void setResume(long time) {
            recordingProgress.setBase(time);
            recordingProgress.start();

            stopButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_stop_color_action_large));
            setControlState(false);
        }

        void setStop() {
            closePanel();
        }
    }

    private final IBinder panelBinder = new PanelBinder();

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
                if ((int)(xval-(panelWidthHidden/2)) < -(displayWidth/2)) {
                    xval = (float)(-(displayWidth/2)+(panelWidthHidden/2));
                } else if ((int)(xval+(panelWidthHidden/2)) > (displayWidth/2)) {
                    xval = (float)((displayWidth/2)-(panelWidthHidden/2));
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
                if ((int)(yval-(panelWidthHidden/2)) < -(displayHeight/2)) {
                    yval = (float)(-(displayHeight/2)+(panelWidthHidden/2));
                } else if ((int)(yval+(panelWidthHidden/2)) > (displayHeight/2)) {
                    yval = (float)((displayHeight/2)-(panelWidthHidden/2));
                }
            }
        }

        floatWindowLayoutParam.x = (int)xval;
        floatWindowLayoutParam.y = (int)yval;

    }

    public void startRecord() {
        updateMetrics();

        panelHidden = false;

        if (displayWidth > displayHeight) {
            isHorizontal = true;
        } else {
            isHorizontal = false;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        appSettings = getSharedPreferences(ScreenRecorder.prefsident, 0);

        String darkTheme = appSettings.getString("darktheme", "Automatic");

        boolean applyDarkTheme = (((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES && darkTheme.contentEquals("Automatic")) || darkTheme.contentEquals("Dark"));

        if (applyDarkTheme == true) {
            getBaseContext().setTheme(android.R.style.Theme_Material);
        } else {
            getBaseContext().setTheme(android.R.style.Theme_Material_Light);
        }

        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);

        if (isHorizontal == true) {
            floatingPanel = (ViewGroup) inflater.inflate(R.layout.panel_float, null);
        } else {
            floatingPanel = (ViewGroup) inflater.inflate(R.layout.panel_float_vertical, null);
        }

        layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        ImageView viewHandle = (ImageView) floatingPanel.findViewById(R.id.floatingpanelhandle);

        LinearLayout viewBackground = (LinearLayout) floatingPanel.findViewById(R.id.panelwithbackground);

        if (applyDarkTheme == true) {
            viewBackground.setBackgroundDrawable(getResources().getDrawable(R.drawable.floatingpanel_shape_dark));
            viewHandle.setImageResource(R.drawable.floatingpanel_shape_dark);
        }

        LinearLayout viewSized = (LinearLayout) floatingPanel.findViewById(R.id.panelwrapped);

        DisplayMetrics metricsPanel = new DisplayMetrics();
        display.getRealMetrics(metricsPanel);

        panelWidthHidden = (int)((40*metricsPanel.density)+0.5f);

        viewBackground.measure(0, 0);

        panelWidthNormal = viewBackground.getMeasuredWidth();
        panelHeight = viewBackground.getMeasuredHeight();

        panelWidth = panelWidthNormal;

        floatWindowLayoutParam = new WindowManager.LayoutParams(panelWidth, panelHeight, layoutType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        floatWindowLayoutParam.gravity = Gravity.CENTER;

        int panelWidthSize = panelWidth;

        if (panelHidden == true) {
            panelWidthSize = panelWidthHidden;
        }

        if (isHorizontal == true) {
            floatWindowLayoutParam.x = (int)((displayWidth/2)-(panelWidthSize/2));
            floatWindowLayoutParam.y = 0;
        } else {
            floatWindowLayoutParam.x = 0;
            floatWindowLayoutParam.y = (int)((displayHeight/2)-(panelWidthSize/2));
        }

        windowManager.addView(floatingPanel, floatWindowLayoutParam);

        pauseButton = (ImageButton) floatingPanel.findViewById(R.id.recordpausebuttonfloating);
        stopButton = (ImageButton) floatingPanel.findViewById(R.id.recordstopbuttonfloating);
        resumeButton = (ImageButton) floatingPanel.findViewById(R.id.recordresumebuttonfloating);
        recordingProgress = (Chronometer) floatingPanel.findViewById(R.id.timerrecordfloating);

        resumeButton.setVisibility(View.GONE);

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

        final int panelWidthSizeListener = panelWidthSize;

        floatingPanel.setOnTouchListener(new View.OnTouchListener() {

            double x;
            double y;
            double px;
            double py;

            double touchX;
            double touchY;

            int panelWidthSizeInner = panelWidthSizeListener;

            int motionPrevX = 0;
            int motionPrevY = 0;

            int touchmotionX = 0;
            int touchmotionY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        updateMetrics();
                        checkBoundaries();
                        windowManager.updateViewLayout(floatingPanel, floatWindowLayoutParam);

                        panelWidthSizeInner = panelWidth;
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

                        touchmotionX += motionNewX;
                        touchmotionY += motionNewY;

                        motionPrevX = floatWindowLayoutParam.x;
                        motionPrevY = floatWindowLayoutParam.y;

                        windowManager.updateViewLayout(floatingPanel, floatWindowLayoutParam);

                        break;
                    case MotionEvent.ACTION_UP:

                        x = floatWindowLayoutParam.x;
                        y = floatWindowLayoutParam.y;

                        int threshold = 10;

                        if (touchmotionX < threshold && touchmotionY < threshold) {

                            x = touchX;
                            y = touchY;

                            if (panelHidden == false) {
                                panelHidden = true;
                                stopButton.setVisibility(View.GONE);
                                pauseButton.setVisibility(View.GONE);
                                resumeButton.setVisibility(View.GONE);
                                viewHandle.setVisibility(View.GONE);
                                recordingProgress.setVisibility(View.GONE);
                                panelWidthSizeInner = panelWidthHidden;

                                if (isHorizontal == true) {
                                    x = x+((panelWidthNormal/2)-(panelWidthHidden/2));
                                    floatWindowLayoutParam.width = (int)panelWidthSizeInner;
                                } else {
                                    y = y+((panelHeight/2)-(panelWidthHidden/2));
                                    floatWindowLayoutParam.height = (int)panelWidthSizeInner;
                                }

                            } else {
                                panelHidden = false;
                                stopButton.setVisibility(View.VISIBLE);
                                setControlState(recordingPaused);
                                recordingProgress.setVisibility(View.VISIBLE);
                                viewHandle.setVisibility(View.VISIBLE);

                                panelWidthSizeInner = panelWidthNormal;

                                if (isHorizontal == true) {
                                    x = x-((panelWidthNormal/2)-(panelWidthHidden/2));
                                    floatWindowLayoutParam.width = (int)panelWidthSizeInner;
                                } else {
                                    y = y-((panelHeight/2)-(panelWidthHidden/2));
                                    floatWindowLayoutParam.height = (int)panelHeight;
                                }

                            }
                        }

                        floatWindowLayoutParam.x = (int)x;
                        floatWindowLayoutParam.y = (int)y;

                        checkBoundaries();
                        windowManager.updateViewLayout(floatingPanel, floatWindowLayoutParam);

                        break;
                }

                return false;
            }
        });

        recordingProgress.setBase(recordingPanelBinder.getTimeStart());
        recordingProgress.start();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() == ACTION_CONNECT_PANEL) {
                startRecord();
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
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
