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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.VectorDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Chronometer;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.AnimatorInflater;

import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;

import com.yakovlevegor.DroidRec.shake.OnShakeEventHelper;
import com.yakovlevegor.DroidRec.shake.event.ServiceConnectedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MICROPHONE = 56808;

    private static final int REQUEST_MICROPHONE_PLAYBACK = 59465;

    private static final int REQUEST_MICROPHONE_RECORD = 58467;

    private static final int REQUEST_STORAGE = 58593;

    private static final int REQUEST_STORAGE_AUDIO = 58563;

    private static final int REQUEST_MODE_CHANGE = 58857;

    private ScreenRecorder.RecordingBinder recordingBinder;

    boolean screenRecorderStarted = false;

    private MediaProjectionManager activityProjectionManager;

    private SharedPreferences appSettings;

    private SharedPreferences.Editor appSettingsEditor;

    Display display;

    ImageButton mainRecordingButton;

    ImageButton startRecordingButton;

    ImageButton recordScreenSetting;

    ImageButton recordMicrophoneSetting;

    ImageButton recordAudioSetting;

    ImageButton recordInfo;

    ImageButton recordSettings;

    ImageButton recordShare;

    ImageButton recordDelete;

    ImageButton recordOpen;

    ImageButton recordStop;

    LinearLayout timerPanel;

    LinearLayout modesPanel;

    LinearLayout finishedPanel;

    LinearLayout optionsPanel;

    Chronometer timeCounter;

    TextView audioPlaybackUnavailable;

    Intent serviceIntent;

    public static String appName = "com.yakovlevegor.DroidRec";

    public static String ACTION_ACTIVITY_START_RECORDING = appName+".ACTIVITY_START_RECORDING";

    private boolean stateActivated = false;

    private boolean serviceToRecording = false;

    private AlertDialog dialog;

    private boolean recordModeChosen;

    private OnShakeEventHelper onShakeEventHelper;

    private boolean isRecording = false;



    private boolean recordMicrophone = false;

    private boolean recordPlayback = false;

    private boolean recordOnlyAudio = false;




    private VectorDrawableCompat recordScreenState;

    private VectorDrawableCompat recordScreenStateDisabled;

    private VectorDrawableCompat recordMicrophoneState;

    private VectorDrawableCompat recordMicrophoneStateDisabled;

    private VectorDrawableCompat recordPlaybackState;

    private VectorDrawableCompat recordPlaybackStateDisabled;


    private VectorDrawableCompat recordInfoIcon;

    private VectorDrawableCompat recordSettingsIcon;


    private VectorDrawableCompat recordShareIcon;

    private VectorDrawableCompat recordDeleteIcon;

    private VectorDrawableCompat recordOpenIcon;


    private VectorDrawableCompat recordStopIcon;


    private VectorDrawableCompat recordingStartButtonNormal;

    private AnimatedVectorDrawableCompat recordingStartButtonHover;

    private AnimatedVectorDrawableCompat recordingStartButtonHoverReleased;

    private AnimatedVectorDrawableCompat recordingStartButtonTransitionToRecording;

    private VectorDrawableCompat recordingBackgroundNormal;

    private AnimatedVectorDrawableCompat recordingBackgroundHover;

    private AnimatedVectorDrawableCompat recordingBackgroundHoverReleased;

    private AnimatedVectorDrawableCompat recordingBackgroundTransition;

    private AnimatedVectorDrawableCompat recordingBackgroundTransitionBack;

    private VectorDrawableCompat recordingStartCameraLegs;

    private AnimatedVectorDrawableCompat recordingStartCameraLegsAppear;

    private AnimatedVectorDrawableCompat recordingStartCameraLegsDisappear;

    private VectorDrawableCompat recordingStartCameraMicrophone;

    private AnimatedVectorDrawableCompat recordingStartCameraMicrophoneAppear;

    private AnimatedVectorDrawableCompat recordingStartCameraMicrophoneDisappear;

    private AnimatedVectorDrawableCompat recordingStartCameraBodyWorking;

    private AnimatedVectorDrawableCompat recordingStartCameraBodyAppear;

    private AnimatedVectorDrawableCompat recordingStartCameraBodyDisappear;

    private VectorDrawableCompat recordingStartCameraHeadphones;

    private AnimatedVectorDrawableCompat recordingStartCameraHeadphonesAppear;

    private AnimatedVectorDrawableCompat recordingStartCameraHeadphonesDisappear;

    private AnimatedVectorDrawableCompat recordingStopCameraReelsFirstAppear;

    private AnimatedVectorDrawableCompat recordingStopCameraReelsFirstDisappear;

    private AnimatedVectorDrawableCompat recordingStopCameraReelsSecondAppear;

    private AnimatedVectorDrawableCompat recordingStopCameraReelsSecondDisappear;

    private AnimatedVectorDrawableCompat recordingStartAudioBodyWorking;

    private AnimatedVectorDrawableCompat recordingStartAudioBodyAppear;

    private AnimatedVectorDrawableCompat recordingStartAudioBodyDisappear;

    private AnimatedVectorDrawableCompat recordingStartAudioTapeWorking;

    private AnimatedVectorDrawableCompat recordingStartAudioTapeAppear;

    private AnimatedVectorDrawableCompat recordingStartAudioTapeDisappear;

    private VectorDrawableCompat recordingStartAudioHeadphones;

    private AnimatedVectorDrawableCompat recordingStartAudioHeadphonesAppear;

    private AnimatedVectorDrawableCompat recordingStartAudioHeadphonesDisappear;

    private VectorDrawableCompat recordingStartAudioMicrophone;

    private AnimatedVectorDrawableCompat recordingStartAudioMicrophoneAppear;

    private AnimatedVectorDrawableCompat recordingStartAudioMicrophoneDisappear;

    private AnimatedVectorDrawableCompat recordingStopAudioReelsFirstAppear;

    private AnimatedVectorDrawableCompat recordingStopAudioReelsFirstDisappear;

    private AnimatedVectorDrawableCompat recordingStopAudioReelsSecondAppear;

    private AnimatedVectorDrawableCompat recordingStopAudioReelsSecondDisappear;

    private AnimatedVectorDrawableCompat recordingStartButtonTransitionBack;


    private LayerDrawable mainButtonLayers;

    private AnimatedVectorDrawableCompat recordingStatusIconPause;

    private boolean stateToRestore = false;

    private boolean recordButtonHoverRelease = false;

    private boolean recordButtonHoverPressed = false;

    private boolean recordButtonLocked = false;

    private boolean recordButtonPressed = false;

    private enum DarkenState {
        TO_DARKEN,
        SET_DARKEN,
        UNDARKEN_RECORD,
        UNDARKEN_END,
    }

    private enum MainButtonState {
        BEFORE_RECORDING_NORMAL,
        BEFORE_RECORDING_HOVER,
        BEFORE_RECORDING_HOVER_RELEASED,
        TRANSITION_TO_RECORDING,
        START_RECORDING,
        CONTINUE_RECORDING,
        WHILE_RECORDING_NORMAL,
        WHILE_RECORDING_HOVER,
        WHILE_RECORDING_HOVER_RELEASED,
        TRANSITION_TO_RECORDING_PAUSE,
        WHILE_PAUSE_NORMAL,
        WHILE_PAUSE_HOVER,
        WHILE_PAUSE_HOVER_RELEASED,
        TRANSITION_FROM_PAUSE,
        TRANSITION_TO_RECORDING_END,
        END_SHOW_REELS,
        ENDED_RECORDING_NORMAL,
        ENDED_RECORDING_HOVER,
        ENDED_RECORDING_HOVER_RELEASED,
        TRANSITION_TO_RESTART,
    }

    private enum MainButtonActionState {
        RECORDING_STOPPED,
        RECORDING_IN_PROGRESS,
        RECORDING_PAUSED,
        RECORDING_ENDED,
    }

    private MainButtonState currentMainButtonState;

    private MainButtonState nextMainButtonState = null;

    private MainButtonActionState recordingState = MainButtonActionState.RECORDING_STOPPED;

    boolean hoverInProgress() {
        switch (currentMainButtonState) {
            case BEFORE_RECORDING_HOVER:
                return recordingStartButtonHover.isRunning();
            case WHILE_RECORDING_HOVER:
                return recordingBackgroundHover.isRunning();
            case WHILE_PAUSE_HOVER:
                return recordingBackgroundHover.isRunning();
            case ENDED_RECORDING_HOVER:
                return recordingBackgroundHover.isRunning();
        }
        return false;

    }

    void darkenLayers(ArrayList<Drawable> targetDrawables, float amount, int duration, boolean restore, DarkenState darkState, MainButtonState atState) {

        if (darkState == DarkenState.SET_DARKEN) {

            int drawablesList = targetDrawables.size();
            int drawablesCount = 0;

            while (drawablesCount < drawablesList) {

                targetDrawables.get(drawablesCount).setColorFilter(new PorterDuffColorFilter(Color.argb((int)(amount*100), 0, 0, 0), PorterDuff.Mode.SRC_ATOP));
                drawablesCount += 1;

            }

        } else {

            ValueAnimator colorAnim = ObjectAnimator.ofFloat(0f, amount);

            if (restore == true) {
                colorAnim = ObjectAnimator.ofFloat(amount, 0f);
            }

            colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float state = (float) animation.getAnimatedValue();

                    int drawablesList = targetDrawables.size();
                    int drawablesCount = 0;

                    while (drawablesCount < drawablesList) {
                        if (state == 0.0) {
                            targetDrawables.get(drawablesCount).setColorFilter(null);
                        } else {
                            targetDrawables.get(drawablesCount).setColorFilter(new PorterDuffColorFilter(Color.argb((int)(state*100), 0, 0, 0), PorterDuff.Mode.SRC_ATOP));
                        }
                        drawablesCount += 1;
                    }

                    if (((state == amount && restore == false) || (state == 0.0 && restore == true)) && currentMainButtonState == atState) {
                        if (darkState == DarkenState.TO_DARKEN) {
                            setMainButtonState(MainButtonState.WHILE_PAUSE_NORMAL);
                        } else if (darkState == DarkenState.UNDARKEN_RECORD) {
                            setMainButtonState(MainButtonState.WHILE_RECORDING_NORMAL);
                        } else if (darkState == DarkenState.UNDARKEN_END) {
                            setMainButtonState(MainButtonState.TRANSITION_TO_RECORDING_END);
                        }
                    }

                }
            });

            colorAnim.setDuration(duration);
            colorAnim.start();
        }

    }


    void showCounter(boolean displayCounter, MainButtonState beforeState) {
        if (displayCounter == true) {
            timeCounter.setScaleX(0.0f);
            timeCounter.setScaleY(0.0f);
            timerPanel.setVisibility(View.VISIBLE);
            ValueAnimator counterShowAnimX = ObjectAnimator.ofFloat(timeCounter, "scaleX", 0f, 1f);
            ValueAnimator counterShowAnimY = ObjectAnimator.ofFloat(timeCounter, "scaleY", 0f, 1f);
            counterShowAnimX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float state = (float) animation.getAnimatedValue();

                    if (state == 1f) {

                        if (beforeState != null) {
                            transitionToButtonState(beforeState);
                        }

                    }
                }
            });
            counterShowAnimX.setDuration(400);
            counterShowAnimY.setDuration(400);
            counterShowAnimX.start();
            counterShowAnimY.start();
        } else {
            timeCounter.setScaleX(1.0f);
            timeCounter.setScaleY(1.0f);
            timerPanel.setVisibility(View.VISIBLE);
            ValueAnimator counterShowAnimX = ObjectAnimator.ofFloat(timeCounter, "scaleX", 1f, 0f);
            ValueAnimator counterShowAnimY = ObjectAnimator.ofFloat(timeCounter, "scaleY", 1f, 0f);
            counterShowAnimX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float state = (float) animation.getAnimatedValue();

                    if (state == 0f) {
                        timerPanel.setVisibility(View.GONE);

                        if (beforeState != null) {
                            transitionToButtonState(beforeState);
                        }
                    }
                }
            });
            counterShowAnimX.setDuration(400);
            counterShowAnimY.setDuration(400);
            counterShowAnimX.start();
            counterShowAnimY.start();
        }

    }

    private void releaseMainButtonFocus() {
        if (currentMainButtonState == MainButtonState.BEFORE_RECORDING_HOVER && recordingStartButtonHover.isRunning() == false) {
            setMainButtonState(MainButtonState.BEFORE_RECORDING_HOVER_RELEASED);
        } else if (currentMainButtonState == MainButtonState.WHILE_RECORDING_HOVER && recordingBackgroundHover.isRunning() == false) {
            setMainButtonState(MainButtonState.WHILE_RECORDING_HOVER_RELEASED);
        } else if (currentMainButtonState == MainButtonState.WHILE_PAUSE_HOVER && recordingBackgroundHover.isRunning() == false) {
            setMainButtonState(MainButtonState.WHILE_PAUSE_HOVER_RELEASED);
        } else if (currentMainButtonState == MainButtonState.ENDED_RECORDING_HOVER && recordingBackgroundHover.isRunning() == false) {
            setMainButtonState(MainButtonState.ENDED_RECORDING_HOVER_RELEASED);
        }
    }

    private void updateRecordModeData() {
        recordMicrophone = appSettings.getBoolean("checksoundmic", false);

        recordPlayback = appSettings.getBoolean("checksoundplayback", false);

        if (recordPlayback == true && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            recordPlayback = false;
        }

        recordOnlyAudio = appSettings.getBoolean("recordmode", false);

        if (recordOnlyAudio == true && recordPlayback == false && recordMicrophone == false) {
            recordOnlyAudio = false;
        }
    }

    private void transitionToButtonState(MainButtonState toState) {
        if (hoverInProgress() == false) {
            setMainButtonState(toState);
        } else {
            nextMainButtonState = toState;
        }
    }

    private void setMainButtonState(MainButtonState state) {

        MainButtonState prevState = currentMainButtonState;

        currentMainButtonState = state;

        nextMainButtonState = null;

        switch (state) {

            case BEFORE_RECORDING_NORMAL:

                recordButtonLocked = false;

                mainRecordingButton.setContentDescription(getResources().getString(R.string.record_start));

                TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_start));

                recordingStartButtonNormal.setAlpha(255);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonTransitionToRecording.setAlpha(0);
                recordingBackgroundNormal.setAlpha(0);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(0);
                recordingBackgroundTransition.setAlpha(0);
                recordingBackgroundTransitionBack.setAlpha(0);
                recordingStartCameraLegs.setAlpha(0);
                recordingStartCameraLegsAppear.setAlpha(0);
                recordingStartCameraLegsDisappear.setAlpha(0);
                recordingStartCameraMicrophone.setAlpha(0);
                recordingStartCameraMicrophoneAppear.setAlpha(0);
                recordingStartCameraMicrophoneDisappear.setAlpha(0);
                recordingStartCameraBodyWorking.setAlpha(0);
                recordingStartCameraBodyAppear.setAlpha(0);
                recordingStartCameraBodyDisappear.setAlpha(0);
                recordingStartCameraHeadphones.setAlpha(0);
                recordingStartCameraHeadphonesAppear.setAlpha(0);
                recordingStartCameraHeadphonesDisappear.setAlpha(0);
                recordingStopCameraReelsFirstAppear.setAlpha(0);
                recordingStopCameraReelsFirstDisappear.setAlpha(0);
                recordingStopCameraReelsSecondAppear.setAlpha(0);
                recordingStopCameraReelsSecondDisappear.setAlpha(0);
                recordingStartAudioBodyWorking.setAlpha(0);
                recordingStartAudioBodyAppear.setAlpha(0);
                recordingStartAudioBodyDisappear.setAlpha(0);
                recordingStartAudioTapeWorking.setAlpha(0);
                recordingStartAudioTapeAppear.setAlpha(0);
                recordingStartAudioTapeDisappear.setAlpha(0);
                recordingStartAudioHeadphones.setAlpha(0);
                recordingStartAudioHeadphonesAppear.setAlpha(0);
                recordingStartAudioHeadphonesDisappear.setAlpha(0);
                recordingStartAudioMicrophone.setAlpha(0);
                recordingStartAudioMicrophoneAppear.setAlpha(0);
                recordingStartAudioMicrophoneDisappear.setAlpha(0);
                recordingStopAudioReelsFirstAppear.setAlpha(0);
                recordingStopAudioReelsFirstDisappear.setAlpha(0);
                recordingStopAudioReelsSecondAppear.setAlpha(0);
                recordingStopAudioReelsSecondDisappear.setAlpha(0);
                recordingStartButtonTransitionBack.setAlpha(0);

                break;

            case BEFORE_RECORDING_HOVER:

                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(255);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonHover.start();
                break;

            case BEFORE_RECORDING_HOVER_RELEASED:

                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(255);
                recordingStartButtonHoverReleased.start();
                break;

            case TRANSITION_TO_RECORDING:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mainRecordingButton.setContentDescription(getResources().getString(R.string.record_pause));

                    TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_pause));

                } else {
                    mainRecordingButton.setContentDescription(getResources().getString(R.string.record_stop));

                    TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_stop));

                }

                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonTransitionToRecording.setAlpha(255);
                recordingBackgroundNormal.setAlpha(0);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(0);
                recordingBackgroundTransition.setAlpha(0);
                recordingBackgroundTransitionBack.setAlpha(0);
                recordingStartCameraLegs.setAlpha(0);
                recordingStartCameraLegsAppear.setAlpha(0);
                recordingStartCameraLegsDisappear.setAlpha(0);
                recordingStartCameraMicrophone.setAlpha(0);
                recordingStartCameraMicrophoneAppear.setAlpha(0);
                recordingStartCameraMicrophoneDisappear.setAlpha(0);
                recordingStartCameraBodyWorking.setAlpha(0);
                recordingStartCameraBodyAppear.setAlpha(0);
                recordingStartCameraBodyDisappear.setAlpha(0);
                recordingStartCameraHeadphones.setAlpha(0);
                recordingStartCameraHeadphonesAppear.setAlpha(0);
                recordingStartCameraHeadphonesDisappear.setAlpha(0);
                recordingStopCameraReelsFirstAppear.setAlpha(0);
                recordingStopCameraReelsFirstDisappear.setAlpha(0);
                recordingStopCameraReelsSecondAppear.setAlpha(0);
                recordingStopCameraReelsSecondDisappear.setAlpha(0);
                recordingStartAudioBodyWorking.setAlpha(0);
                recordingStartAudioBodyAppear.setAlpha(0);
                recordingStartAudioBodyDisappear.setAlpha(0);
                recordingStartAudioTapeWorking.setAlpha(0);
                recordingStartAudioTapeAppear.setAlpha(0);
                recordingStartAudioTapeDisappear.setAlpha(0);
                recordingStartAudioHeadphones.setAlpha(0);
                recordingStartAudioHeadphonesAppear.setAlpha(0);
                recordingStartAudioHeadphonesDisappear.setAlpha(0);
                recordingStartAudioMicrophone.setAlpha(0);
                recordingStartAudioMicrophoneAppear.setAlpha(0);
                recordingStartAudioMicrophoneDisappear.setAlpha(0);
                recordingStopAudioReelsFirstAppear.setAlpha(0);
                recordingStopAudioReelsFirstDisappear.setAlpha(0);
                recordingStopAudioReelsSecondAppear.setAlpha(0);
                recordingStopAudioReelsSecondDisappear.setAlpha(0);
                recordingStartButtonTransitionBack.setAlpha(0);

                recordingStartButtonTransitionToRecording.start();

                break;
            case START_RECORDING:

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mainRecordingButton.setContentDescription(getResources().getString(R.string.record_pause));

                    TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_pause));

                } else {
                    mainRecordingButton.setContentDescription(getResources().getString(R.string.record_stop));

                    TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_stop));

                }

                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonTransitionToRecording.setAlpha(0);
                recordingBackgroundNormal.setAlpha(255);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(0);
                recordingBackgroundTransition.setAlpha(0);
                recordingBackgroundTransitionBack.setAlpha(0);
                recordingStartCameraLegs.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraLegsAppear.setAlpha(255);
                    recordingStartCameraLegsAppear.start();
                } else {
                    recordingStartCameraLegsAppear.setAlpha(0);
                }
                recordingStartCameraLegsDisappear.setAlpha(0);
                recordingStartCameraMicrophone.setAlpha(0);
                if (recordMicrophone == true && recordOnlyAudio == false) {
                    recordingStartCameraMicrophoneAppear.setAlpha(255);
                    recordingStartCameraMicrophoneAppear.start();
                } else {
                    recordingStartCameraMicrophoneAppear.setAlpha(0);
                }
                recordingStartCameraMicrophoneDisappear.setAlpha(0);
                recordingStartCameraBodyWorking.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraBodyAppear.setAlpha(255);
                    recordingStartCameraBodyAppear.start();
                } else {
                    recordingStartCameraBodyAppear.setAlpha(0);
                }
                recordingStartCameraBodyDisappear.setAlpha(0);
                recordingStartCameraHeadphones.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == false) {
                    recordingStartCameraHeadphonesAppear.setAlpha(255);
                    recordingStartCameraHeadphonesAppear.start();
                } else {
                    recordingStartCameraHeadphonesAppear.setAlpha(0);
                }
                recordingStartCameraHeadphonesDisappear.setAlpha(0);
                recordingStopCameraReelsFirstAppear.setAlpha(0);
                recordingStopCameraReelsFirstDisappear.setAlpha(0);
                recordingStopCameraReelsSecondAppear.setAlpha(0);
                recordingStopCameraReelsSecondDisappear.setAlpha(0);
                recordingStartAudioBodyWorking.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioBodyAppear.setAlpha(255);
                    recordingStartAudioBodyAppear.start();
                } else {
                    recordingStartAudioBodyAppear.setAlpha(0);
                }
                recordingStartAudioBodyDisappear.setAlpha(0);
                recordingStartAudioTapeWorking.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioTapeAppear.setAlpha(255);
                    recordingStartAudioTapeAppear.start();
                } else {
                    recordingStartAudioTapeAppear.setAlpha(0);
                }
                recordingStartAudioTapeDisappear.setAlpha(0);
                recordingStartAudioHeadphones.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == true) {
                    recordingStartAudioHeadphonesAppear.setAlpha(255);
                    recordingStartAudioHeadphonesAppear.start();
                } else {
                    recordingStartAudioHeadphonesAppear.setAlpha(0);
                }
                recordingStartAudioHeadphonesDisappear.setAlpha(0);
                recordingStartAudioMicrophone.setAlpha(0);
                if (recordMicrophone == true && recordOnlyAudio == true) {
                    recordingStartAudioMicrophoneAppear.setAlpha(255);
                    recordingStartAudioMicrophoneAppear.start();
                } else {
                    recordingStartAudioMicrophoneAppear.setAlpha(0);
                }
                recordingStartAudioMicrophoneDisappear.setAlpha(0);
                recordingStopAudioReelsFirstAppear.setAlpha(0);
                recordingStopAudioReelsFirstDisappear.setAlpha(0);
                recordingStopAudioReelsSecondAppear.setAlpha(0);
                recordingStopAudioReelsSecondDisappear.setAlpha(0);
                recordingStartButtonTransitionBack.setAlpha(0);

                break;

            case WHILE_RECORDING_NORMAL:

                recordButtonLocked = false;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mainRecordingButton.setContentDescription(getResources().getString(R.string.record_pause));

                    TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_pause));

                } else {
                    mainRecordingButton.setContentDescription(getResources().getString(R.string.record_stop));

                    TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_stop));

                }

                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonTransitionToRecording.setAlpha(0);
                recordingBackgroundNormal.setAlpha(255);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(0);
                recordingBackgroundTransition.setAlpha(0);
                recordingBackgroundTransitionBack.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraLegs.setAlpha(255);
                } else {
                    recordingStartCameraLegs.setAlpha(0);
                }
                recordingStartCameraLegsAppear.setAlpha(0);
                recordingStartCameraLegsDisappear.setAlpha(0);
                if (recordMicrophone == true && recordOnlyAudio == false) {
                    recordingStartCameraMicrophone.setAlpha(255);
                } else {
                    recordingStartCameraMicrophone.setAlpha(0);
                }
                recordingStartCameraMicrophoneAppear.setAlpha(0);
                recordingStartCameraMicrophoneDisappear.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraBodyWorking.setAlpha(255);
                    recordingStartCameraBodyWorking.start();
                } else {
                    recordingStartCameraBodyWorking.setAlpha(0);
                }
                recordingStartCameraBodyAppear.setAlpha(0);
                recordingStartCameraBodyDisappear.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == false) {
                    recordingStartCameraHeadphones.setAlpha(255);
                } else {
                    recordingStartCameraHeadphones.setAlpha(0);
                }
                recordingStartCameraHeadphonesAppear.setAlpha(0);
                recordingStartCameraHeadphonesDisappear.setAlpha(0);
                recordingStopCameraReelsFirstAppear.setAlpha(0);
                recordingStopCameraReelsFirstDisappear.setAlpha(0);
                recordingStopCameraReelsSecondAppear.setAlpha(0);
                recordingStopCameraReelsSecondDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioBodyWorking.setAlpha(255);
                    recordingStartAudioBodyWorking.start();
                } else {
                    recordingStartAudioBodyWorking.setAlpha(0);
                }
                recordingStartAudioBodyAppear.setAlpha(0);
                recordingStartAudioBodyDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioTapeWorking.setAlpha(255);
                    recordingStartAudioTapeWorking.start();
                } else {
                    recordingStartAudioTapeWorking.setAlpha(0);
                }
                recordingStartAudioTapeAppear.setAlpha(0);
                recordingStartAudioTapeDisappear.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == true) {
                    recordingStartAudioHeadphones.setAlpha(255);
                } else {
                    recordingStartAudioHeadphones.setAlpha(0);
                }
                recordingStartAudioHeadphonesAppear.setAlpha(0);
                recordingStartAudioHeadphonesDisappear.setAlpha(0);
                if (recordMicrophone == true && recordOnlyAudio == true) {
                    recordingStartAudioMicrophone.setAlpha(255);
                } else {
                    recordingStartAudioMicrophone.setAlpha(0);
                }
                recordingStartAudioMicrophoneAppear.setAlpha(0);
                recordingStartAudioMicrophoneDisappear.setAlpha(0);
                recordingStopAudioReelsFirstAppear.setAlpha(0);
                recordingStopAudioReelsFirstDisappear.setAlpha(0);
                recordingStopAudioReelsSecondAppear.setAlpha(0);
                recordingStopAudioReelsSecondDisappear.setAlpha(0);
                recordingStartButtonTransitionBack.setAlpha(0);
                break;

            case WHILE_RECORDING_HOVER:
                recordingBackgroundNormal.setAlpha(0);
                recordingBackgroundHover.setAlpha(255);
                recordingBackgroundHover.start();
                recordingBackgroundHoverReleased.setAlpha(0);

                break;

            case WHILE_RECORDING_HOVER_RELEASED:
                recordingBackgroundNormal.setAlpha(0);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(255);
                recordingBackgroundHoverReleased.start();

                break;
            case TRANSITION_TO_RECORDING_PAUSE:
                mainRecordingButton.setContentDescription(getResources().getString(R.string.record_resume));

                TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_resume));

                ArrayList<Drawable> layersToDarken = new ArrayList<Drawable>();

                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonTransitionToRecording.setAlpha(0);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(255);
                recordingBackgroundHoverReleased.start();
                recordingBackgroundTransition.setAlpha(0);
                recordingBackgroundTransitionBack.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraLegs.setAlpha(255);
                    layersToDarken.add(recordingStartCameraLegs);
                } else {
                    recordingStartCameraLegs.setAlpha(0);
                }
                recordingStartCameraLegsAppear.setAlpha(0);
                recordingStartCameraLegsDisappear.setAlpha(0);
                if (recordMicrophone == true && recordOnlyAudio == false) {
                    recordingStartCameraMicrophone.setAlpha(255);
                    layersToDarken.add(recordingStartCameraMicrophone);
                } else {
                    recordingStartCameraMicrophone.setAlpha(0);
                }
                recordingStartCameraMicrophoneAppear.setAlpha(0);
                recordingStartCameraMicrophoneDisappear.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraBodyWorking.setAlpha(255);
                    recordingStartCameraBodyWorking.stop();
                    layersToDarken.add(recordingStartCameraBodyWorking);
                } else {
                    recordingStartCameraBodyWorking.setAlpha(0);
                }
                recordingStartCameraBodyAppear.setAlpha(0);
                recordingStartCameraBodyDisappear.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == false) {
                    recordingStartCameraHeadphones.setAlpha(255);
                    layersToDarken.add(recordingStartCameraHeadphones);
                } else {
                    recordingStartCameraHeadphones.setAlpha(0);
                }
                recordingStartCameraHeadphonesAppear.setAlpha(0);
                recordingStartCameraHeadphonesDisappear.setAlpha(0);
                recordingStopCameraReelsFirstAppear.setAlpha(0);
                recordingStopCameraReelsFirstDisappear.setAlpha(0);
                recordingStopCameraReelsSecondAppear.setAlpha(0);
                recordingStopCameraReelsSecondDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioBodyWorking.setAlpha(255);
                    recordingStartAudioBodyWorking.stop();
                    layersToDarken.add(recordingStartAudioBodyWorking);
                } else {
                    recordingStartAudioBodyWorking.setAlpha(0);
                }
                recordingStartAudioBodyAppear.setAlpha(0);
                recordingStartAudioBodyDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioTapeWorking.setAlpha(255);
                    recordingStartAudioTapeWorking.stop();
                    layersToDarken.add(recordingStartAudioTapeWorking);
                } else {
                    recordingStartAudioTapeWorking.setAlpha(0);
                }
                recordingStartAudioTapeAppear.setAlpha(0);
                recordingStartAudioTapeDisappear.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == true) {
                    recordingStartAudioHeadphones.setAlpha(255);
                    layersToDarken.add(recordingStartAudioHeadphones);
                } else {
                    recordingStartAudioHeadphones.setAlpha(0);
                }
                recordingStartAudioHeadphonesAppear.setAlpha(0);
                recordingStartAudioHeadphonesDisappear.setAlpha(0);
                if (recordMicrophone == true && recordOnlyAudio == true) {
                    recordingStartAudioMicrophone.setAlpha(255);
                    layersToDarken.add(recordingStartAudioMicrophone);
                } else {
                    recordingStartAudioMicrophone.setAlpha(0);
                }
                recordingStartAudioMicrophoneAppear.setAlpha(0);
                recordingStartAudioMicrophoneDisappear.setAlpha(0);
                recordingStopAudioReelsFirstAppear.setAlpha(0);
                recordingStopAudioReelsFirstDisappear.setAlpha(0);
                recordingStopAudioReelsSecondAppear.setAlpha(0);
                recordingStopAudioReelsSecondDisappear.setAlpha(0);
                recordingStartButtonTransitionBack.setAlpha(0);
                darkenLayers(layersToDarken, 0.8f, 200, false, DarkenState.TO_DARKEN, state);
                break;
            case WHILE_PAUSE_NORMAL:
                recordingState = MainButtonActionState.RECORDING_PAUSED;

                recordButtonLocked = false;

                mainRecordingButton.setContentDescription(getResources().getString(R.string.record_resume));

                TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_resume));

                ArrayList<Drawable> layersSetDark = new ArrayList<Drawable>();

                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonTransitionToRecording.setAlpha(0);
                recordingBackgroundNormal.setAlpha(255);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(0);
                recordingBackgroundTransition.setAlpha(0);
                recordingBackgroundTransitionBack.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraLegs.setAlpha(255);
                } else {
                    recordingStartCameraLegs.setAlpha(0);
                }
                recordingStartCameraLegsAppear.setAlpha(0);
                recordingStartCameraLegsDisappear.setAlpha(0);
                if (recordMicrophone == true && recordOnlyAudio == false) {
                    recordingStartCameraMicrophone.setAlpha(255);
                } else {
                    recordingStartCameraMicrophone.setAlpha(0);
                }
                recordingStartCameraMicrophoneAppear.setAlpha(0);
                recordingStartCameraMicrophoneDisappear.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraBodyWorking.setAlpha(255);
                    recordingStartCameraBodyWorking.stop();
                } else {
                    recordingStartCameraBodyWorking.setAlpha(0);
                }
                recordingStartCameraBodyAppear.setAlpha(0);
                recordingStartCameraBodyDisappear.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == false) {
                    recordingStartCameraHeadphones.setAlpha(255);
                    layersSetDark.add(recordingStartCameraHeadphones);
                } else {
                    recordingStartCameraHeadphones.setAlpha(0);
                }
                recordingStartCameraHeadphonesAppear.setAlpha(0);
                recordingStartCameraHeadphonesDisappear.setAlpha(0);
                recordingStopCameraReelsFirstAppear.setAlpha(0);
                recordingStopCameraReelsFirstDisappear.setAlpha(0);
                recordingStopCameraReelsSecondAppear.setAlpha(0);
                recordingStopCameraReelsSecondDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioBodyWorking.setAlpha(255);
                    recordingStartAudioBodyWorking.stop();
                    layersSetDark.add(recordingStartAudioBodyWorking);
                } else {
                    recordingStartAudioBodyWorking.setAlpha(0);
                }
                recordingStartAudioBodyAppear.setAlpha(0);
                recordingStartAudioBodyDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioTapeWorking.setAlpha(255);
                    recordingStartAudioTapeWorking.stop();
                    layersSetDark.add(recordingStartAudioTapeWorking);
                } else {
                    recordingStartAudioTapeWorking.setAlpha(0);
                }
                recordingStartAudioTapeAppear.setAlpha(0);
                recordingStartAudioTapeDisappear.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == true) {
                    recordingStartAudioHeadphones.setAlpha(255);
                    layersSetDark.add(recordingStartAudioHeadphones);
                } else {
                    recordingStartAudioHeadphones.setAlpha(0);
                }
                recordingStartAudioHeadphonesAppear.setAlpha(0);
                recordingStartAudioHeadphonesDisappear.setAlpha(0);

                if (recordMicrophone == true && recordOnlyAudio == true) {
                    recordingStartAudioMicrophone.setAlpha(255);
                    layersSetDark.add(recordingStartAudioMicrophone);
                } else {
                    recordingStartAudioMicrophone.setAlpha(0);
                }

                recordingStartAudioMicrophoneAppear.setAlpha(0);
                recordingStartAudioMicrophoneDisappear.setAlpha(0);
                recordingStopAudioReelsFirstAppear.setAlpha(0);
                recordingStopAudioReelsFirstDisappear.setAlpha(0);
                recordingStopAudioReelsSecondAppear.setAlpha(0);
                recordingStopAudioReelsSecondDisappear.setAlpha(0);
                recordingStartButtonTransitionBack.setAlpha(0);

                darkenLayers(layersSetDark, 0.8f, 200, false, DarkenState.SET_DARKEN, state);

                break;
            case WHILE_PAUSE_HOVER:
                recordingBackgroundNormal.setAlpha(0);
                recordingBackgroundHover.setAlpha(255);
                recordingBackgroundHover.start();
                recordingBackgroundHoverReleased.setAlpha(0);

                break;
            case WHILE_PAUSE_HOVER_RELEASED:
                recordingBackgroundNormal.setAlpha(0);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(255);
                recordingBackgroundHoverReleased.start();

                break;
            case TRANSITION_FROM_PAUSE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mainRecordingButton.setContentDescription(getResources().getString(R.string.record_pause));

                    TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_pause));

                } else {
                    mainRecordingButton.setContentDescription(getResources().getString(R.string.record_stop));

                    TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_stop));

                }

                ArrayList<Drawable> layersToUndarken = new ArrayList<Drawable>();

                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonTransitionToRecording.setAlpha(0);
                recordingBackgroundNormal.setAlpha(0);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(255);
                recordingBackgroundHoverReleased.start();
                recordingBackgroundTransition.setAlpha(0);
                recordingBackgroundTransitionBack.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraLegs.setAlpha(255);
                    layersToUndarken.add(recordingStartCameraLegs);
                } else {
                    recordingStartCameraLegs.setAlpha(0);
                }
                recordingStartCameraLegsAppear.setAlpha(0);
                recordingStartCameraLegsDisappear.setAlpha(0);
                if (recordMicrophone == true && recordOnlyAudio == false) {
                    recordingStartCameraMicrophone.setAlpha(255);
                    layersToUndarken.add(recordingStartCameraMicrophone);
                } else {
                    recordingStartCameraMicrophone.setAlpha(0);
                }
                recordingStartCameraMicrophoneAppear.setAlpha(0);
                recordingStartCameraMicrophoneDisappear.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraBodyWorking.setAlpha(255);
                    recordingStartCameraBodyWorking.stop();
                    layersToUndarken.add(recordingStartCameraBodyWorking);
                } else {
                    recordingStartCameraBodyWorking.setAlpha(0);
                }
                recordingStartCameraBodyAppear.setAlpha(0);
                recordingStartCameraBodyDisappear.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == false) {
                    recordingStartCameraHeadphones.setAlpha(255);
                    layersToUndarken.add(recordingStartCameraHeadphones);
                } else {
                    recordingStartCameraHeadphones.setAlpha(0);
                }
                recordingStartCameraHeadphonesAppear.setAlpha(0);
                recordingStartCameraHeadphonesDisappear.setAlpha(0);
                recordingStopCameraReelsFirstAppear.setAlpha(0);
                recordingStopCameraReelsFirstDisappear.setAlpha(0);
                recordingStopCameraReelsSecondAppear.setAlpha(0);
                recordingStopCameraReelsSecondDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioBodyWorking.setAlpha(255);
                    recordingStartAudioBodyWorking.stop();
                    layersToUndarken.add(recordingStartAudioBodyWorking);
                } else {
                    recordingStartAudioBodyWorking.setAlpha(0);
                }
                recordingStartAudioBodyAppear.setAlpha(0);
                recordingStartAudioBodyDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioTapeWorking.setAlpha(255);
                    recordingStartAudioTapeWorking.stop();
                    layersToUndarken.add(recordingStartAudioTapeWorking);
                } else {
                    recordingStartAudioTapeWorking.setAlpha(0);
                }
                recordingStartAudioTapeAppear.setAlpha(0);
                recordingStartAudioTapeDisappear.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == true) {
                    recordingStartAudioHeadphones.setAlpha(255);
                    layersToUndarken.add(recordingStartAudioHeadphones);
                } else {
                    recordingStartAudioHeadphones.setAlpha(0);
                }
                recordingStartAudioHeadphonesAppear.setAlpha(0);
                recordingStartAudioHeadphonesDisappear.setAlpha(0);
                if (recordMicrophone == true && recordOnlyAudio == true) {
                    recordingStartAudioMicrophone.setAlpha(255);
                    layersToUndarken.add(recordingStartAudioMicrophone);
                } else {
                    recordingStartAudioMicrophone.setAlpha(0);
                }
                recordingStartAudioMicrophoneAppear.setAlpha(0);
                recordingStartAudioMicrophoneDisappear.setAlpha(0);
                recordingStopAudioReelsFirstAppear.setAlpha(0);
                recordingStopAudioReelsFirstDisappear.setAlpha(0);
                recordingStopAudioReelsSecondAppear.setAlpha(0);
                recordingStopAudioReelsSecondDisappear.setAlpha(0);
                recordingStartButtonTransitionBack.setAlpha(0);

                darkenLayers(layersToUndarken, 0.8f, 200, true, DarkenState.UNDARKEN_RECORD, state);
                break;
            case TRANSITION_TO_RECORDING_END:
                mainRecordingButton.setContentDescription(getResources().getString(R.string.recording_finished_title));

                TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.recording_finished_title));

                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonTransitionToRecording.setAlpha(0);
                recordingBackgroundNormal.setAlpha(255);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(0);
                recordingBackgroundTransition.setAlpha(0);
                recordingBackgroundTransitionBack.setAlpha(0);
                recordingStartCameraLegs.setAlpha(0);
                recordingStartCameraLegsAppear.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraLegsDisappear.setAlpha(255);
                    recordingStartCameraLegsDisappear.start();
                } else {
                    recordingStartCameraLegsDisappear.setAlpha(0);
                }
                recordingStartCameraMicrophone.setAlpha(0);
                recordingStartCameraMicrophoneAppear.setAlpha(0);
                if (recordMicrophone == true && recordOnlyAudio == false) {
                    recordingStartCameraMicrophoneDisappear.setAlpha(255);
                    recordingStartCameraMicrophoneDisappear.start();
                } else {
                    recordingStartCameraMicrophoneDisappear.setAlpha(0);
                }
                recordingStartCameraBodyWorking.setAlpha(0);
                recordingStartCameraBodyAppear.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStartCameraBodyDisappear.setAlpha(255);
                    recordingStartCameraBodyDisappear.start();
                } else {
                    recordingStartCameraBodyDisappear.setAlpha(0);
                }
                recordingStartCameraHeadphones.setAlpha(0);
                recordingStartCameraHeadphonesAppear.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == false) {
                    recordingStartCameraHeadphonesDisappear.setAlpha(255);
                    recordingStartCameraHeadphonesDisappear.start();
                } else {
                    recordingStartCameraHeadphonesDisappear.setAlpha(0);
                }
                recordingStopCameraReelsFirstAppear.setAlpha(0);
                recordingStopCameraReelsFirstDisappear.setAlpha(0);
                recordingStopCameraReelsSecondAppear.setAlpha(0);
                recordingStopCameraReelsSecondDisappear.setAlpha(0);
                recordingStartAudioBodyWorking.setAlpha(0);
                recordingStartAudioBodyAppear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioBodyDisappear.setAlpha(255);
                    recordingStartAudioBodyDisappear.start();
                } else {
                    recordingStartAudioBodyDisappear.setAlpha(0);
                }
                recordingStartAudioTapeWorking.setAlpha(0);
                recordingStartAudioTapeAppear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStartAudioTapeDisappear.setAlpha(255);
                    recordingStartAudioTapeDisappear.start();
                } else {
                    recordingStartAudioTapeDisappear.setAlpha(0);
                }
                recordingStartAudioTapeDisappear.setAlpha(0);
                recordingStartAudioHeadphones.setAlpha(0);
                recordingStartAudioHeadphonesAppear.setAlpha(0);
                if (recordPlayback == true && recordOnlyAudio == true) {
                    recordingStartAudioHeadphonesDisappear.setAlpha(255);
                    recordingStartAudioHeadphonesDisappear.start();
                } else {
                    recordingStartAudioHeadphonesDisappear.setAlpha(0);
                }
                recordingStartAudioMicrophone.setAlpha(0);
                recordingStartAudioMicrophoneAppear.setAlpha(0);
                if (recordMicrophone == true && recordOnlyAudio == true) {
                    recordingStartAudioMicrophoneDisappear.setAlpha(255);
                    recordingStartAudioMicrophoneDisappear.start();
                } else {
                    recordingStartAudioMicrophoneDisappear.setAlpha(0);
                }
                recordingStopAudioReelsFirstAppear.setAlpha(0);
                recordingStopAudioReelsFirstDisappear.setAlpha(0);
                recordingStopAudioReelsSecondAppear.setAlpha(0);
                recordingStopAudioReelsSecondDisappear.setAlpha(0);
                recordingStartButtonTransitionBack.setAlpha(0);

                break;

            case END_SHOW_REELS:
                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonTransitionToRecording.setAlpha(0);
                recordingBackgroundNormal.setAlpha(255);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(0);
                recordingBackgroundTransition.setAlpha(0);
                recordingBackgroundTransitionBack.setAlpha(0);
                recordingStartCameraLegs.setAlpha(0);
                recordingStartCameraLegs.setColorFilter(null);
                recordingStartCameraLegsAppear.setAlpha(0);
                recordingStartCameraLegsDisappear.setAlpha(0);
                recordingStartCameraMicrophone.setAlpha(0);
                recordingStartCameraMicrophone.setColorFilter(null);
                recordingStartCameraMicrophoneAppear.setAlpha(0);
                recordingStartCameraMicrophoneDisappear.setAlpha(0);
                recordingStartCameraBodyWorking.setAlpha(0);
                recordingStartCameraBodyWorking.setColorFilter(null);
                recordingStartCameraBodyAppear.setAlpha(0);
                recordingStartCameraBodyDisappear.setAlpha(0);
                recordingStartCameraHeadphones.setAlpha(0);
                recordingStartCameraHeadphones.setColorFilter(null);
                recordingStartAudioHeadphones.setColorFilter(null);
                recordingStartCameraHeadphonesAppear.setAlpha(0);
                recordingStartCameraHeadphonesDisappear.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStopCameraReelsFirstAppear.setAlpha(255);
                    recordingStopCameraReelsFirstAppear.start();
                } else {
                    recordingStopCameraReelsFirstAppear.setAlpha(0);
                }
                recordingStopCameraReelsFirstDisappear.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStopCameraReelsSecondAppear.setAlpha(255);
                    recordingStopCameraReelsSecondAppear.start();
                } else {
                    recordingStopCameraReelsSecondAppear.setAlpha(0);
                }
                recordingStopCameraReelsSecondDisappear.setAlpha(0);
                recordingStartAudioBodyWorking.setAlpha(0);
                recordingStartAudioBodyWorking.setColorFilter(null);
                recordingStartAudioBodyAppear.setAlpha(0);
                recordingStartAudioBodyDisappear.setAlpha(0);
                recordingStartAudioTapeWorking.setAlpha(0);
                recordingStartAudioTapeWorking.setColorFilter(null);
                recordingStartAudioTapeAppear.setAlpha(0);
                recordingStartAudioTapeDisappear.setAlpha(0);
                recordingStartAudioHeadphones.setAlpha(0);
                recordingStartAudioHeadphonesAppear.setAlpha(0);
                recordingStartAudioHeadphonesDisappear.setAlpha(0);
                recordingStartAudioMicrophone.setAlpha(0);
                recordingStartAudioMicrophone.setColorFilter(null);
                recordingStartAudioMicrophoneAppear.setAlpha(0);
                recordingStartAudioMicrophoneDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStopAudioReelsFirstAppear.setAlpha(255);
                    recordingStopAudioReelsFirstAppear.start();
                } else {
                    recordingStopAudioReelsFirstAppear.setAlpha(0);
                }
                recordingStopAudioReelsFirstDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStopAudioReelsSecondAppear.setAlpha(255);
                    recordingStopAudioReelsSecondAppear.start();
                } else {
                    recordingStopAudioReelsSecondAppear.setAlpha(0);
                }
                recordingStopAudioReelsSecondDisappear.setAlpha(0);
                recordingStartButtonTransitionBack.setAlpha(0);

                break;

            case ENDED_RECORDING_NORMAL:
                recordingState = MainButtonActionState.RECORDING_ENDED;

                recordButtonLocked = false;

                mainRecordingButton.setContentDescription(getResources().getString(R.string.recording_finished_title));

                TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.recording_finished_title));

                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonTransitionToRecording.setAlpha(0);
                recordingBackgroundNormal.setAlpha(255);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(0);
                recordingBackgroundTransition.setAlpha(0);
                recordingBackgroundTransitionBack.setAlpha(0);
                recordingStartCameraLegs.setAlpha(0);
                recordingStartCameraLegs.setColorFilter(null);
                recordingStartCameraLegsAppear.setAlpha(0);
                recordingStartCameraLegsDisappear.setAlpha(0);
                recordingStartCameraMicrophone.setAlpha(0);
                recordingStartCameraMicrophone.setColorFilter(null);
                recordingStartCameraMicrophoneAppear.setAlpha(0);
                recordingStartCameraMicrophoneDisappear.setAlpha(0);
                recordingStartCameraBodyWorking.setAlpha(0);
                recordingStartCameraBodyWorking.setColorFilter(null);
                recordingStartCameraBodyAppear.setAlpha(0);
                recordingStartCameraBodyDisappear.setAlpha(0);
                recordingStartCameraHeadphones.setAlpha(0);
                recordingStartCameraHeadphones.setColorFilter(null);
                recordingStartAudioHeadphones.setColorFilter(null);
                recordingStartCameraHeadphonesAppear.setAlpha(0);
                recordingStartCameraHeadphonesDisappear.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStopCameraReelsFirstAppear.setAlpha(255);
                } else {
                    recordingStopCameraReelsFirstAppear.setAlpha(0);
                }
                recordingStopCameraReelsFirstDisappear.setAlpha(0);
                if (recordOnlyAudio == false) {
                    recordingStopCameraReelsSecondAppear.setAlpha(255);
                } else {
                    recordingStopCameraReelsSecondAppear.setAlpha(0);
                }
                recordingStopCameraReelsSecondDisappear.setAlpha(0);
                recordingStartAudioBodyWorking.setAlpha(0);
                recordingStartAudioBodyWorking.setColorFilter(null);
                recordingStartAudioBodyAppear.setAlpha(0);
                recordingStartAudioBodyDisappear.setAlpha(0);
                recordingStartAudioTapeWorking.setAlpha(0);
                recordingStartAudioTapeWorking.setColorFilter(null);
                recordingStartAudioTapeAppear.setAlpha(0);
                recordingStartAudioTapeDisappear.setAlpha(0);
                recordingStartAudioHeadphones.setAlpha(0);
                recordingStartAudioHeadphonesAppear.setAlpha(0);
                recordingStartAudioHeadphonesDisappear.setAlpha(0);
                recordingStartAudioMicrophone.setAlpha(0);
                recordingStartAudioMicrophone.setColorFilter(null);
                recordingStartAudioMicrophoneAppear.setAlpha(0);
                recordingStartAudioMicrophoneDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStopAudioReelsFirstAppear.setAlpha(255);
                } else {
                    recordingStopAudioReelsFirstAppear.setAlpha(0);
                }
                recordingStopAudioReelsFirstDisappear.setAlpha(0);
                if (recordOnlyAudio == true) {
                    recordingStopAudioReelsSecondAppear.setAlpha(255);
                } else {
                    recordingStopAudioReelsSecondAppear.setAlpha(0);
                }
                recordingStopAudioReelsSecondDisappear.setAlpha(0);
                recordingStartButtonTransitionBack.setAlpha(0);

                break;
            case ENDED_RECORDING_HOVER:
                recordingBackgroundNormal.setAlpha(0);
                recordingBackgroundHover.setAlpha(255);
                recordingBackgroundHover.start();
                recordingBackgroundHoverReleased.setAlpha(0);

                break;
            case ENDED_RECORDING_HOVER_RELEASED:
                recordingBackgroundNormal.setAlpha(0);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(255);
                recordingBackgroundHoverReleased.start();

                break;
            case TRANSITION_TO_RESTART:
                recordingState = MainButtonActionState.RECORDING_STOPPED;

                mainRecordingButton.setContentDescription(getResources().getString(R.string.record_start));

                TooltipCompat.setTooltipText(mainRecordingButton, getResources().getString(R.string.record_start));

                recordingStartButtonNormal.setAlpha(0);
                recordingStartButtonHover.setAlpha(0);
                recordingStartButtonHoverReleased.setAlpha(0);
                recordingStartButtonTransitionToRecording.setAlpha(0);
                recordingBackgroundNormal.setAlpha(0);
                recordingBackgroundHover.setAlpha(0);
                recordingBackgroundHoverReleased.setAlpha(0);
                recordingBackgroundTransition.setAlpha(0);
                recordingBackgroundTransitionBack.setAlpha(0);
                recordingStartCameraLegs.setAlpha(0);
                recordingStartCameraLegsAppear.setAlpha(0);
                recordingStartCameraLegsDisappear.setAlpha(0);
                recordingStartCameraMicrophone.setAlpha(0);
                recordingStartCameraMicrophoneAppear.setAlpha(0);
                recordingStartCameraMicrophoneDisappear.setAlpha(0);
                recordingStartCameraBodyWorking.setAlpha(0);
                recordingStartCameraBodyAppear.setAlpha(0);
                recordingStartCameraBodyDisappear.setAlpha(0);
                recordingStartCameraHeadphones.setAlpha(0);
                recordingStartCameraHeadphonesAppear.setAlpha(0);
                recordingStartCameraHeadphonesDisappear.setAlpha(0);
                recordingStopCameraReelsFirstAppear.setAlpha(0);

                recordingStopCameraReelsSecondAppear.setAlpha(0);

                recordingStartAudioBodyWorking.setAlpha(0);
                recordingStartAudioBodyAppear.setAlpha(0);
                recordingStartAudioBodyDisappear.setAlpha(0);
                recordingStartAudioTapeWorking.setAlpha(0);
                recordingStartAudioTapeAppear.setAlpha(0);
                recordingStartAudioTapeDisappear.setAlpha(0);
                recordingStartAudioHeadphones.setAlpha(0);
                recordingStartAudioHeadphonesAppear.setAlpha(0);
                recordingStartAudioHeadphonesDisappear.setAlpha(0);
                recordingStartAudioMicrophone.setAlpha(0);
                recordingStartAudioMicrophoneAppear.setAlpha(0);
                recordingStartAudioMicrophoneDisappear.setAlpha(0);
                recordingStopAudioReelsFirstAppear.setAlpha(0);

                recordingStopAudioReelsSecondAppear.setAlpha(0);

                recordingStartButtonTransitionBack.setAlpha(255);

                recordingStartButtonTransitionBack.start();

                break;

        }

    }

    public class ActivityBinder extends Binder {
        void recordingStart() {
            timeCounter.stop();
            timeCounter.setBase(recordingBinder.getTimeStart());
            timeCounter.start();

            audioPlaybackUnavailable.setVisibility(View.GONE);

            modesPanel.setVisibility(View.GONE);
            optionsPanel.setVisibility(View.GONE);

            recordingState = MainButtonActionState.RECORDING_IN_PROGRESS;

            if (stateToRestore == true) {
                showCounter(true, MainButtonState.TRANSITION_TO_RECORDING);
            } else {
                timeCounter.setScaleX(1.0f);
                timeCounter.setScaleY(1.0f);
                timerPanel.setVisibility(View.VISIBLE);

                transitionToButtonState(MainButtonState.WHILE_RECORDING_NORMAL);
            }
        }

        void recordingStop() {
            timeCounter.stop();
            timeCounter.setBase(SystemClock.elapsedRealtime());

            audioPlaybackUnavailable.setVisibility(View.GONE);

            modesPanel.setVisibility(View.GONE);
            optionsPanel.setVisibility(View.GONE);

            finishedPanel.setVisibility(View.VISIBLE);

            recordStop.setVisibility(View.GONE);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                audioPlaybackUnavailable.setVisibility(View.VISIBLE);
            }

            recordingState = MainButtonActionState.RECORDING_ENDED;

            if (stateToRestore == true) {
                showCounter(false, MainButtonState.TRANSITION_TO_RECORDING_END);
            } else {
                timerPanel.setVisibility(View.GONE);

                transitionToButtonState(MainButtonState.ENDED_RECORDING_NORMAL);
            }
        }

        void recordingPause(long time) {
            timeCounter.setBase(SystemClock.elapsedRealtime()-time);
            timeCounter.stop();

            modesPanel.setVisibility(View.GONE);
            optionsPanel.setVisibility(View.GONE);

            timeCounter.setScaleX(1.0f);
            timeCounter.setScaleY(1.0f);
            timerPanel.setVisibility(View.VISIBLE);

            recordStop.setVisibility(View.VISIBLE);

            recordingState = MainButtonActionState.RECORDING_PAUSED;

            if (stateToRestore == true) {
                transitionToButtonState(MainButtonState.TRANSITION_TO_RECORDING_PAUSE);
            } else {
                transitionToButtonState(MainButtonState.WHILE_PAUSE_NORMAL);
            }
        }

        void recordingResume(long time) {
            timeCounter.setBase(time);
            timeCounter.start();

            recordStop.setVisibility(View.GONE);

            recordingState = MainButtonActionState.RECORDING_IN_PROGRESS;
        }

        void recordingReset() {
            finishedPanel.setVisibility(View.GONE);
            optionsPanel.setVisibility(View.VISIBLE);
            modesPanel.setVisibility(View.VISIBLE);

            transitionToButtonState(MainButtonState.TRANSITION_TO_RESTART);
        }

        void resetDir(boolean isAudio) {
            resetFolder(isAudio);
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

            EventBus.getDefault().post(new ServiceConnectedEvent(true));
        }

        public void onServiceDisconnected(ComponentName className) {
            recordingBinder.setDisconnect();
            screenRecorderStarted = false;
            EventBus.getDefault().post(new ServiceConnectedEvent(false));
        }
    };


    private final ActivityResultLauncher<Intent> requestRecordingPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result != null) {

                int requestCode = result.getResultCode();

                if (requestCode == RESULT_OK && recordingBinder != null) {
                    doStartService(requestCode, result.getData());
                }

            }

        }
    });

    private final ActivityResultLauncher<Intent> requestFolderPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result != null) {

                requestFolder(result.getResultCode(), result.getData().getData(), false, false);

            }
        }
    });

    private final ActivityResultLauncher<Intent> requestAudioFolderPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result != null) {

                requestFolder(result.getResultCode(), result.getData().getData(), false, true);

            }
        }
    });


    private final ActivityResultLauncher<Intent> requestFolderPermissionAndProceed = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result != null) {

                requestFolder(result.getResultCode(), result.getData().getData(), true, false);

            }
        }
    });

    private final ActivityResultLauncher<Intent> requestAudioFolderPermissionAndProceed = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result != null) {

                requestFolder(result.getResultCode(), result.getData().getData(), true, true);

            }
        }
    });


    void doStartService(int result, Intent data) {

        display = ((DisplayManager)(getBaseContext().getSystemService(Context.DISPLAY_SERVICE))).getDisplay(Display.DEFAULT_DISPLAY);

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

        updateRecordModeData();

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
        EventBus.getDefault().unregister(this);
        onShakeEventHelper.unregisterListener();
        super.onDestroy();
        doUnbindService();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        display = ((DisplayManager)(getBaseContext().getSystemService(Context.DISPLAY_SERVICE))).getDisplay(Display.DEFAULT_DISPLAY);
        int orientationOnStart = display.getRotation();

        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);

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

        View mainRecordingLayout = findViewById(R.id.recordpanel);

        RelativeLayout.LayoutParams rightPanelLayoutParams = (RelativeLayout.LayoutParams) mainRecordingLayout.getLayoutParams();

        if ((orientationOnStart == Surface.ROTATION_90 || orientationOnStart == Surface.ROTATION_270) && metrics.widthPixels > metrics.heightPixels) {

            rightPanelLayoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.recordingmainbutton);
            mainRecordingLayout.setLayoutParams(rightPanelLayoutParams);

        } else {

            rightPanelLayoutParams.addRule(RelativeLayout.BELOW, R.id.recordingmainbutton);
            mainRecordingLayout.setLayoutParams(rightPanelLayoutParams);

        }

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


        updateRecordModeData();


        mainRecordingButton = (ImageButton) findViewById(R.id.recordingmainbutton);

        String darkThemeApplied = appSettings.getString("darkthemeapplied", getResources().getString(R.string.dark_theme_option_auto));

        if (((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES && darkTheme.contentEquals(getResources().getString(R.string.dark_theme_option_auto))) || darkThemeApplied.contentEquals("Dark")) {


            recordScreenState = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_screen_dark, null);

            recordScreenStateDisabled = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_screen_disabled_dark, null);

            recordMicrophoneState = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_mic_dark, null);

            recordMicrophoneStateDisabled = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_mic_disabled_dark, null);

            recordPlaybackState = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_audio_dark, null);

            recordPlaybackStateDisabled = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_audio_disabled_dark, null);


            recordInfoIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_info_dark, null);

            recordSettingsIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_settings_dark, null);


            recordShareIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_share_dark, null);

            recordDeleteIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_delete_dark, null);

            recordOpenIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_play_dark, null);

            recordStopIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_stop_dark, null);


            recordingStartButtonNormal = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_stopped_dark, null);

            recordingStartButtonHover = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_button_pressed_dark);

            recordingStartButtonHoverReleased = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_button_released_dark);

            recordingStartButtonTransitionToRecording = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_button_activate_dark);

            recordingBackgroundNormal = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_in_progress_background_dark, null);

            recordingBackgroundHover = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_background_pressed_dark);

            recordingBackgroundHoverReleased = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_background_released_dark);

            recordingBackgroundTransition = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_background_transition_dark);

            recordingBackgroundTransitionBack = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_background_transition_back_dark);

            recordingStartCameraLegs = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_in_progress_camlegs_dark, null);

            recordingStartCameraMicrophone = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_in_progress_with_mic_dark, null);

            recordingStartCameraHeadphones = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_in_progress_with_headphones_dark, null);

            recordingStartCameraLegsAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_legs_appear_dark);

            recordingStartCameraLegsDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_legs_disappear_dark);

            recordingStartCameraMicrophoneAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_microphone_appear_dark);

            recordingStartCameraMicrophoneDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_microphone_disappear_dark);

            recordingStartCameraBodyWorking = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_body_working_dark);

            recordingStartCameraBodyAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_body_appear_dark);

            recordingStartCameraBodyDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_body_disappear_dark);

            recordingStartCameraHeadphonesAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_headphones_appear_dark);

            recordingStartCameraHeadphonesDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_headphones_disappear_dark);

            recordingStopCameraReelsFirstAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_camera_reels_first_appear_dark);

            recordingStopCameraReelsFirstDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_camera_reels_first_disappear_dark);

            recordingStopCameraReelsSecondAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_camera_reels_second_appear_dark);

            recordingStopCameraReelsSecondDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_camera_reels_second_disappear_dark);

            recordingStartAudioHeadphones = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_audio_in_progress_with_headphones_dark, null);

            recordingStartAudioMicrophone = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_audio_in_progress_with_mic_dark, null);

            recordingStartAudioBodyWorking = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_body_working_dark);

            recordingStartAudioBodyAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_body_appear_dark);

            recordingStartAudioBodyDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_body_disappear_dark);

            recordingStartAudioTapeWorking = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_tape_working_dark);

            recordingStartAudioTapeAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_tape_appear_dark);

            recordingStartAudioTapeDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_tape_disappear_dark);

            recordingStartAudioHeadphonesAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_headphones_appear_dark);

            recordingStartAudioHeadphonesDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_headphones_disappear_dark);

            recordingStartAudioMicrophoneAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_microphone_appear_dark);

            recordingStartAudioMicrophoneDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_microphone_disappear_dark);

            recordingStopAudioReelsFirstAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_audio_reels_first_appear_dark);

            recordingStopAudioReelsFirstDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_audio_reels_first_disappear_dark);

            recordingStopAudioReelsSecondAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_audio_reels_second_appear_dark);

            recordingStopAudioReelsSecondDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_audio_reels_second_disappear_dark);

            recordingStartButtonTransitionBack = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_button_restore_dark);

        } else {

            recordScreenState = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_screen, null);

            recordScreenStateDisabled = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_screen_disabled, null);

            recordMicrophoneState = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_mic, null);

            recordMicrophoneStateDisabled = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_mic_disabled, null);

            recordPlaybackState = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_audio, null);

            recordPlaybackStateDisabled = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_audio_disabled, null);


            recordInfoIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_info, null);

            recordSettingsIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_settings, null);


            recordShareIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_share, null);

            recordDeleteIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_delete, null);

            recordOpenIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_play, null);

            recordStopIcon = VectorDrawableCompat.create(getResources(), R.drawable.icon_record_stop, null);


            recordingStartButtonNormal = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_stopped, null);

            recordingStartButtonHover = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_button_pressed);

            recordingStartButtonHoverReleased = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_button_released);

            recordingStartButtonTransitionToRecording = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_button_activate);

            recordingBackgroundNormal = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_in_progress_background, null);

            recordingBackgroundHover = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_background_pressed);

            recordingBackgroundHoverReleased = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_background_released);

            recordingBackgroundTransition = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_background_transition);

            recordingBackgroundTransitionBack = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_background_transition_back);

            recordingStartCameraLegs = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_in_progress_camlegs, null);

            recordingStartCameraMicrophone = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_in_progress_with_mic, null);

            recordingStartCameraHeadphones = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_in_progress_with_headphones, null);

            recordingStartCameraLegsAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_legs_appear);

            recordingStartCameraLegsDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_legs_disappear);

            recordingStartCameraMicrophoneAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_microphone_appear);

            recordingStartCameraMicrophoneDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_microphone_disappear);

            recordingStartCameraBodyWorking = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_body_working);

            recordingStartCameraBodyAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_body_appear);

            recordingStartCameraBodyDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_body_disappear);

            recordingStartCameraHeadphonesAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_headphones_appear);

            recordingStartCameraHeadphonesDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_camera_headphones_disappear);

            recordingStopCameraReelsFirstAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_camera_reels_first_appear);

            recordingStopCameraReelsFirstDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_camera_reels_first_disappear);

            recordingStopCameraReelsSecondAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_camera_reels_second_appear);

            recordingStopCameraReelsSecondDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_camera_reels_second_disappear);

            recordingStartAudioHeadphones = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_audio_in_progress_with_headphones, null);

            recordingStartAudioMicrophone = VectorDrawableCompat.create(getResources(), R.drawable.icon_recording_audio_in_progress_with_mic, null);

            recordingStartAudioBodyWorking = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_body_working);

            recordingStartAudioBodyAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_body_appear);

            recordingStartAudioBodyDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_body_disappear);

            recordingStartAudioTapeWorking = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_tape_working);

            recordingStartAudioTapeAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_tape_appear);

            recordingStartAudioTapeDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_tape_disappear);

            recordingStartAudioHeadphonesAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_headphones_appear);

            recordingStartAudioHeadphonesDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_headphones_disappear);

            recordingStartAudioMicrophoneAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_microphone_appear);

            recordingStartAudioMicrophoneDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_start_audio_microphone_disappear);

            recordingStopAudioReelsFirstAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_audio_reels_first_appear);

            recordingStopAudioReelsFirstDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_audio_reels_first_disappear);

            recordingStopAudioReelsSecondAppear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_audio_reels_second_appear);

            recordingStopAudioReelsSecondDisappear = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_stop_audio_reels_second_disappear);

            recordingStartButtonTransitionBack = AnimatedVectorDrawableCompat.create(getBaseContext(), R.drawable.anim_recording_button_restore);

        }

        Animatable2Compat.AnimationCallback callbackBeforeRecordingHover = new Animatable2Compat.AnimationCallback() {
            public void onAnimationEnd(Drawable drawable) {
                if (recordButtonPressed == false) {
                    if (currentMainButtonState == MainButtonState.BEFORE_RECORDING_HOVER) {
                        if (nextMainButtonState != null) {
                            setMainButtonState(nextMainButtonState);
                        } else {
                            setMainButtonState(MainButtonState.BEFORE_RECORDING_HOVER_RELEASED);
                        }
                    }
                }
            }
        };

        recordingStartButtonHover.registerAnimationCallback(callbackBeforeRecordingHover);



        Animatable2Compat.AnimationCallback callbackBeforeRecordingHoverReleased = new Animatable2Compat.AnimationCallback() {
            public void onAnimationEnd(Drawable drawable) {
                if (currentMainButtonState == MainButtonState.BEFORE_RECORDING_HOVER_RELEASED) {
                    setMainButtonState(MainButtonState.BEFORE_RECORDING_NORMAL);
                }
            }
        };

        recordingStartButtonHoverReleased.registerAnimationCallback(callbackBeforeRecordingHoverReleased);



        Animatable2Compat.AnimationCallback callbackRecordingStartButtonTransitionToRecording = new Animatable2Compat.AnimationCallback() {
            public void onAnimationEnd(Drawable drawable) {
                if (currentMainButtonState == MainButtonState.TRANSITION_TO_RECORDING) {
                    setMainButtonState(MainButtonState.START_RECORDING);
                }
            }
        };

        recordingStartButtonTransitionToRecording.registerAnimationCallback(callbackRecordingStartButtonTransitionToRecording);



        Animatable2Compat.AnimationCallback callbackWhileRecordingHover = new Animatable2Compat.AnimationCallback() {
            public void onAnimationEnd(Drawable drawable) {
                if (recordButtonPressed == false) {
                    if (nextMainButtonState != null) {
                        setMainButtonState(nextMainButtonState);
                    } else {
                        if (currentMainButtonState == MainButtonState.WHILE_RECORDING_HOVER) {
                            setMainButtonState(MainButtonState.WHILE_RECORDING_HOVER_RELEASED);
                        } else if (currentMainButtonState == MainButtonState.WHILE_PAUSE_HOVER) {
                            setMainButtonState(MainButtonState.WHILE_PAUSE_HOVER_RELEASED);
                        } else if (currentMainButtonState == MainButtonState.ENDED_RECORDING_HOVER) {
                            setMainButtonState(MainButtonState.ENDED_RECORDING_HOVER_RELEASED);
                        }
                    }
                }
            }
        };

        recordingBackgroundHover.registerAnimationCallback(callbackWhileRecordingHover);



        Animatable2Compat.AnimationCallback callbackWhileRecordingHoverReleased = new Animatable2Compat.AnimationCallback() {
            public void onAnimationEnd(Drawable drawable) {
                if (currentMainButtonState == MainButtonState.WHILE_RECORDING_HOVER_RELEASED) {
                    setMainButtonState(MainButtonState.WHILE_RECORDING_NORMAL);
                } else if (currentMainButtonState == MainButtonState.WHILE_PAUSE_HOVER_RELEASED) {
                    setMainButtonState(MainButtonState.WHILE_PAUSE_NORMAL);
                } else if (currentMainButtonState == MainButtonState.ENDED_RECORDING_HOVER_RELEASED) {
                    setMainButtonState(MainButtonState.ENDED_RECORDING_NORMAL);
                }
            }
        };

        recordingBackgroundHoverReleased.registerAnimationCallback(callbackWhileRecordingHoverReleased);

        Animatable2Compat.AnimationCallback callbackRecordingStartButtonToRecordingMode = new Animatable2Compat.AnimationCallback() {
            public void onAnimationEnd(Drawable drawable) {
                if (currentMainButtonState == MainButtonState.START_RECORDING) {
                    setMainButtonState(MainButtonState.WHILE_RECORDING_NORMAL);
                }
            }
        };

        recordingStartCameraLegsAppear.registerAnimationCallback(callbackRecordingStartButtonToRecordingMode);

        recordingStartAudioBodyAppear.registerAnimationCallback(callbackRecordingStartButtonToRecordingMode);



        Animatable2Compat.AnimationCallback callbackRecordingStartButtonToShowReels = new Animatable2Compat.AnimationCallback() {
            public void onAnimationEnd(Drawable drawable) {
                if (currentMainButtonState == MainButtonState.TRANSITION_TO_RECORDING_END) {
                    setMainButtonState(MainButtonState.END_SHOW_REELS);
                }
            }
        };

        recordingStartAudioTapeDisappear.registerAnimationCallback(callbackRecordingStartButtonToShowReels);

        recordingStartCameraBodyDisappear.registerAnimationCallback(callbackRecordingStartButtonToShowReels);


        Animatable2Compat.AnimationCallback callbackRecordingStartButtonToFinished = new Animatable2Compat.AnimationCallback() {
            public void onAnimationEnd(Drawable drawable) {
                if (currentMainButtonState == MainButtonState.END_SHOW_REELS) {
                    setMainButtonState(MainButtonState.ENDED_RECORDING_NORMAL);
                }
            }
        };

        recordingStopCameraReelsFirstAppear.registerAnimationCallback(callbackRecordingStartButtonToFinished);

        recordingStopAudioReelsFirstAppear.registerAnimationCallback(callbackRecordingStartButtonToFinished);

        Animatable2Compat.AnimationCallback callbackRecordingStartButtonToNew = new Animatable2Compat.AnimationCallback() {
            public void onAnimationEnd(Drawable drawable) {
                if (currentMainButtonState == MainButtonState.TRANSITION_TO_RESTART) {

                    finishedPanel.setVisibility(View.GONE);
                    optionsPanel.setVisibility(View.VISIBLE);
                    modesPanel.setVisibility(View.VISIBLE);

                    setMainButtonState(MainButtonState.BEFORE_RECORDING_NORMAL);
                }
            }
        };

        recordingStartButtonTransitionBack.registerAnimationCallback(callbackRecordingStartButtonToNew);

        Drawable[] mainButtonLayersList = {recordingStartButtonNormal, recordingStartButtonHover, recordingStartButtonHoverReleased, recordingStartButtonTransitionToRecording, recordingBackgroundNormal, recordingBackgroundHover, recordingBackgroundHoverReleased, recordingBackgroundTransition, recordingBackgroundTransitionBack, recordingStartCameraLegs, recordingStartCameraLegsAppear, recordingStartCameraLegsDisappear, recordingStartCameraMicrophone, recordingStartCameraMicrophoneAppear, recordingStartCameraMicrophoneDisappear, recordingStartCameraBodyWorking, recordingStartCameraBodyAppear, recordingStartCameraBodyDisappear, recordingStartCameraHeadphones, recordingStartCameraHeadphonesAppear, recordingStartCameraHeadphonesDisappear, recordingStopCameraReelsFirstAppear, recordingStopCameraReelsFirstDisappear, recordingStopCameraReelsSecondAppear, recordingStopCameraReelsSecondDisappear, recordingStartAudioBodyWorking, recordingStartAudioBodyAppear, recordingStartAudioBodyDisappear, recordingStartAudioTapeWorking, recordingStartAudioTapeAppear, recordingStartAudioTapeDisappear, recordingStartAudioHeadphones, recordingStartAudioHeadphonesAppear, recordingStartAudioHeadphonesDisappear, recordingStartAudioMicrophone, recordingStartAudioMicrophoneAppear, recordingStartAudioMicrophoneDisappear, recordingStopAudioReelsFirstAppear, recordingStopAudioReelsFirstDisappear, recordingStopAudioReelsSecondAppear, recordingStopAudioReelsSecondDisappear, recordingStartButtonTransitionBack};

        mainButtonLayers = new LayerDrawable(mainButtonLayersList);

        mainRecordingButton.setBackground(mainButtonLayers);

        setMainButtonState(MainButtonState.BEFORE_RECORDING_NORMAL);

        mainRecordingButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        if (recordButtonLocked == false) {

                            recordButtonPressed = true;

                            if (currentMainButtonState == MainButtonState.BEFORE_RECORDING_NORMAL) {
                                setMainButtonState(MainButtonState.BEFORE_RECORDING_HOVER);
                            } else if (currentMainButtonState == MainButtonState.WHILE_RECORDING_NORMAL) {
                                setMainButtonState(MainButtonState.WHILE_RECORDING_HOVER);
                            } else if (currentMainButtonState == MainButtonState.WHILE_PAUSE_NORMAL) {
                                setMainButtonState(MainButtonState.WHILE_PAUSE_HOVER);
                            } else if (currentMainButtonState == MainButtonState.ENDED_RECORDING_NORMAL) {
                                setMainButtonState(MainButtonState.ENDED_RECORDING_HOVER);
                            }

                        } else {
                            return true;
                        }


                        break;

                    case MotionEvent.ACTION_MOVE:

                        if (recordButtonPressed == true) {

                            int[] eventCoords = new int[2];

                            eventCoords[0] = (int)event.getRawX();

                            eventCoords[1] = (int)event.getRawY();



                            int[] buttonCoords = new int[2];

                            v.getLocationOnScreen(buttonCoords);



                            int[] buttonSize = new int[2];

                            buttonSize[0] = v.getWidth();

                            buttonSize[1] = v.getHeight();


                            if (buttonCoords[0] > eventCoords[0] || (buttonCoords[0]+buttonSize[0]) < eventCoords[0] || buttonCoords[1] > eventCoords[1] || (buttonCoords[1]+buttonSize[1]) < eventCoords[1]) {

                                recordButtonPressed = false;

                                recordButtonLocked = true;

                                if (currentMainButtonState == MainButtonState.BEFORE_RECORDING_HOVER && recordingStartButtonHover.isRunning() == false) {
                                    setMainButtonState(MainButtonState.BEFORE_RECORDING_HOVER_RELEASED);
                                } else if (currentMainButtonState == MainButtonState.WHILE_RECORDING_HOVER && recordingBackgroundHover.isRunning() == false) {
                                    setMainButtonState(MainButtonState.WHILE_RECORDING_HOVER_RELEASED);
                                } else if (currentMainButtonState == MainButtonState.WHILE_PAUSE_HOVER && recordingBackgroundHover.isRunning() == false) {
                                    setMainButtonState(MainButtonState.WHILE_PAUSE_HOVER_RELEASED);
                                } else if (currentMainButtonState == MainButtonState.ENDED_RECORDING_HOVER && recordingBackgroundHover.isRunning() == false) {
                                    setMainButtonState(MainButtonState.ENDED_RECORDING_HOVER_RELEASED);
                                }

                            }

                        }

                        break;

                    case MotionEvent.ACTION_UP:

                        recordButtonPressed = false;

                        break;



                }
                return false;
            }
        });


        mainRecordingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (recordButtonLocked == false) {
                    recordButtonLocked = true;

                    stateToRestore = true;

                    if (recordingState == MainButtonActionState.RECORDING_STOPPED) {
                        recordingStart();
                    } else if (recordingState == MainButtonActionState.RECORDING_IN_PROGRESS) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            recordingBinder.recordingPause();
                        } else {
                            recordingBinder.stopService();
                        }
                    } else if (recordingState == MainButtonActionState.RECORDING_PAUSED) {
                        transitionToButtonState(MainButtonState.TRANSITION_FROM_PAUSE);
                        recordingBinder.recordingResume();
                    } else if (recordingState == MainButtonActionState.RECORDING_ENDED) {
                        recordingBinder.recordingReset();
                        transitionToButtonState(MainButtonState.TRANSITION_TO_RESTART);
                    }
                }

            }
        });

        View mainAppView = findViewById(R.id.mainlayout);

        View.OnClickListener mainButtonStopHover = new View.OnClickListener() {
            public void onClick(View v) {
                releaseMainButtonFocus();
            }
        };

        mainAppView.setOnClickListener(mainButtonStopHover);

        recordScreenSetting = (ImageButton) findViewById(R.id.recordscreen);
        recordMicrophoneSetting = (ImageButton) findViewById(R.id.recordmicrohone);
        recordAudioSetting = (ImageButton) findViewById(R.id.recordaudio);

        recordScreenSetting.setContentDescription(getResources().getString(R.string.setting_record_screen) + ": " + getResources().getString(R.string.option_deactivated));
        recordMicrophoneSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_microphone_sound) + ": " + getResources().getString(R.string.option_deactivated));
        recordAudioSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_playback_sound) + ": " + getResources().getString(R.string.option_deactivated));

        recordInfo = (ImageButton) findViewById(R.id.openinfo);
        recordSettings = (ImageButton) findViewById(R.id.opensettings);

        recordShare = (ImageButton) findViewById(R.id.sharerecord);
        recordDelete = (ImageButton) findViewById(R.id.deleterecord);
        recordOpen = (ImageButton) findViewById(R.id.openrecord);

        recordStop = (ImageButton) findViewById(R.id.recordstop);

        timerPanel = (LinearLayout) findViewById(R.id.recordtimerpanel);
        modesPanel = (LinearLayout) findViewById(R.id.recordmodepanel);
        finishedPanel = (LinearLayout) findViewById(R.id.recordfinishedpanel);
        optionsPanel = (LinearLayout) findViewById(R.id.optionspanel);

        recordScreenSetting.setImageDrawable(recordScreenStateDisabled);
        recordMicrophoneSetting.setImageDrawable(recordMicrophoneStateDisabled);
        recordAudioSetting.setImageDrawable(recordPlaybackStateDisabled);

        recordInfo.setImageDrawable(recordInfoIcon);
        recordSettings.setImageDrawable(recordSettingsIcon);

        recordShare.setImageDrawable(recordShareIcon);
        recordDelete.setImageDrawable(recordDeleteIcon);
        recordOpen.setImageDrawable(recordOpenIcon);

        recordStop.setImageDrawable(recordStopIcon);

        TooltipCompat.setTooltipText(recordScreenSetting, getResources().getString(R.string.setting_record_screen));
        TooltipCompat.setTooltipText(recordMicrophoneSetting, getResources().getString(R.string.setting_audio_record_microphone_sound));
        TooltipCompat.setTooltipText(recordAudioSetting, getResources().getString(R.string.setting_audio_record_playback_sound));

        TooltipCompat.setTooltipText(recordInfo, getResources().getString(R.string.info_title));
        TooltipCompat.setTooltipText(recordSettings, getResources().getString(R.string.settings_title));

        TooltipCompat.setTooltipText(recordShare, getResources().getString(R.string.record_share));
        TooltipCompat.setTooltipText(recordDelete, getResources().getString(R.string.record_delete));
        TooltipCompat.setTooltipText(recordOpen, getResources().getString(R.string.record_open));

        TooltipCompat.setTooltipText(recordStop, getResources().getString(R.string.record_stop));

        timeCounter = (Chronometer) findViewById(R.id.timerrecord);
        audioPlaybackUnavailable = (TextView) findViewById(R.id.audioplaybackunavailable);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            audioPlaybackUnavailable.setVisibility(View.VISIBLE);
            recordAudioSetting.setVisibility(View.GONE);
        }

        if (!recordOnlyAudio) {
            recordScreenSetting.setImageDrawable(recordScreenState);
            recordScreenSetting.setContentDescription(getResources().getString(R.string.setting_record_screen) + ": " + getResources().getString(R.string.option_activated));
        }

        if (recordMicrophone) {
            recordMicrophoneSetting.setImageDrawable(recordMicrophoneState);
            recordMicrophoneSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_microphone_sound) + ": " + getResources().getString(R.string.option_activated));
        }

        if (recordPlayback) {
            recordAudioSetting.setImageDrawable(recordPlaybackState);
            recordAudioSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_playback_sound) + ": " + getResources().getString(R.string.option_activated));
        }

        setRecordMode(appSettings.getBoolean("recordmode", false));

        activityProjectionManager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);

        recordScreenSetting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                releaseMainButtonFocus();

                boolean checkedState = !recordOnlyAudio;

                if (checkedState == false) {
                    recordScreenSetting.setImageDrawable(recordScreenState);
                    recordOnlyAudio = false;
                    recordScreenSetting.setContentDescription(getResources().getString(R.string.setting_record_screen) + ": " + getResources().getString(R.string.option_activated));
                    setRecordMode(false);
                } else if (checkedState == true) {
                    if (recordMicrophone == true || recordPlayback == true) {
                        boolean audioPermissionDenied = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            audioPermissionDenied = (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED);
                        }
                        if (audioPermissionDenied == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            recordScreenSetting.setImageDrawable(recordScreenState);
                            recordOnlyAudio = false;
                            recordScreenSetting.setContentDescription(getResources().getString(R.string.setting_record_screen) + ": " + getResources().getString(R.string.option_activated));

                            String accesspermission[] = {Manifest.permission.RECORD_AUDIO};
                            requestPermissions(accesspermission, REQUEST_MODE_CHANGE);
                        } else {
                            recordScreenSetting.setImageDrawable(recordScreenStateDisabled);
                            recordOnlyAudio = true;
                            recordScreenSetting.setContentDescription(getResources().getString(R.string.setting_record_screen) + ": " + getResources().getString(R.string.option_deactivated));
                            setRecordMode(true);
                        }
                    }
                }


            }
        });

        recordMicrophoneSetting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                releaseMainButtonFocus();

                boolean checkedState = !recordMicrophone;
                boolean audioPermissionDenied = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioPermissionDenied = (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED);
                }
                if (audioPermissionDenied && checkedState == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    recordMicrophoneSetting.setImageDrawable(recordMicrophoneStateDisabled);
                    recordMicrophone = false;
                    recordMicrophoneSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_microphone_sound) + ": " + getResources().getString(R.string.option_deactivated));

                    String accesspermission[] = {Manifest.permission.RECORD_AUDIO};
                    requestPermissions(accesspermission, REQUEST_MICROPHONE);
                } else {
                    if (recordModeChosen == true && checkedState == false && (recordOnlyAudio == false || recordPlayback == true) && ((recordMicrophone == false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)) {
                        recordMicrophoneSetting.setImageDrawable(recordMicrophoneState);
                        recordMicrophone = true;
                        recordMicrophoneSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_microphone_sound) + ": " + getResources().getString(R.string.option_activated));

                    } else {
                        if (checkedState == true || (recordOnlyAudio == false || recordPlayback == true)) {
                            recordMicrophone = checkedState;
                            appSettingsEditor.putBoolean("checksoundmic", checkedState);
                            appSettingsEditor.commit();
                            if (checkedState == true) {
                                recordMicrophoneSetting.setImageDrawable(recordMicrophoneState);
                                recordMicrophoneSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_microphone_sound) + ": " + getResources().getString(R.string.option_activated));
                            } else {
                                recordMicrophoneSetting.setImageDrawable(recordMicrophoneStateDisabled);
                                recordMicrophoneSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_microphone_sound) + ": " + getResources().getString(R.string.option_deactivated));
                            }
                        }
                    }
                }
            }
        });

        recordAudioSetting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                releaseMainButtonFocus();

                boolean checkedState = !recordPlayback;
                boolean audioPermissionDenied = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioPermissionDenied = (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED);
                }
                if (audioPermissionDenied && checkedState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (recordOnlyAudio == false || recordMicrophone == true)) {
                    recordAudioSetting.setImageDrawable(recordPlaybackStateDisabled);
                    recordPlayback = false;
                    recordAudioSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_playback_sound) + ": " + getResources().getString(R.string.option_deactivated));

                    String accesspermission[] = {Manifest.permission.RECORD_AUDIO};
                    requestPermissions(accesspermission, REQUEST_MICROPHONE_PLAYBACK);
                } else {
                    if (recordModeChosen == true && checkedState == false && (recordOnlyAudio == false || recordMicrophone == true) && recordPlayback == false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        recordAudioSetting.setImageDrawable(recordPlaybackState);
                        recordPlayback = true;
                        recordAudioSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_playback_sound) + ": " + getResources().getString(R.string.option_activated));
                    } else {
                        if (checkedState == true || (recordOnlyAudio == false || recordMicrophone == true)) {
                            recordPlayback = checkedState;
                            appSettingsEditor.putBoolean("checksoundplayback", checkedState);
                            appSettingsEditor.commit();
                            if (checkedState == true) {
                                recordAudioSetting.setImageDrawable(recordPlaybackState);
                                recordAudioSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_playback_sound) + ": " + getResources().getString(R.string.option_activated));
                            } else {
                                recordAudioSetting.setImageDrawable(recordPlaybackStateDisabled);
                                recordAudioSetting.setContentDescription(getResources().getString(R.string.setting_audio_record_playback_sound) + ": " + getResources().getString(R.string.option_deactivated));
                            }
                        }
                    }
                }
            }
        });

        recordShare.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                releaseMainButtonFocus();

                recordingBinder.recordingShare();
            }
        });

        recordDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                releaseMainButtonFocus();

                recordingBinder.recordingDelete();

                finishedPanel.setVisibility(View.GONE);
                optionsPanel.setVisibility(View.VISIBLE);
                modesPanel.setVisibility(View.VISIBLE);

                transitionToButtonState(MainButtonState.TRANSITION_TO_RESTART);
            }
        });

        recordOpen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                releaseMainButtonFocus();

                recordingBinder.recordingOpen();
            }
        });

        recordStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                releaseMainButtonFocus();

                recordingBinder.stopService();
            }
        });

        recordInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                releaseMainButtonFocus();

                Intent showinfo = new Intent(MainActivity.this, AppInfo.class);
                startActivity(showinfo);
            }
        });

        recordSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                releaseMainButtonFocus();

                Intent showsettings = new Intent(MainActivity.this, SettingsPanel.class);
                startActivity(showsettings);
            }
        });

        EventBus.getDefault().register(this);
    }

    public void setRecordMode(boolean mode) {
        appSettingsEditor.putBoolean("recordmode", mode);
        appSettingsEditor.commit();

        recordModeChosen = mode;

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

    public void checkDirRecord(boolean isAudio) {
        String audioPathPrefix = appSettings.getString("folderpath", "NULL");

        if (isAudio == true) {
            audioPathPrefix = appSettings.getString("folderaudiopath", "NULL");
        }

        if (audioPathPrefix == "NULL") {
            chooseDir(true, isAudio);
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
                    if (appSettings.getBoolean("recordmode", false) == false) {
                        requestPermissions(accesspermission, REQUEST_STORAGE);
                    } else {
                        requestPermissions(accesspermission, REQUEST_STORAGE_AUDIO);
                    }
                } else if ((appSettings.getBoolean("floatingcontrols", false) == true) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && (Settings.canDrawOverlays(this) == false)) {
                    appSettingsEditor.putBoolean("floatingcontrols", false);
                    appSettingsEditor.commit();
                    requestOverlayDisplayPermission();
                } else {
                    checkDirRecord(appSettings.getBoolean("recordmode", false));
                }

            }
        }
    }

    private void requestFolder(int resultCode, Uri extrauri, boolean proceedToRecording, boolean isAudio) {
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

            if (proceedToRecording == true) {
                proceedRecording();
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

    void proceedRecording() {

        if (appSettings.getBoolean("recordmode", false) == true && (appSettings.getBoolean("checksoundplayback", false) == false || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) && recordingBinder != null) {
            doStartService(0, null);
        } else {
            requestRecordingPermission.launch(activityProjectionManager.createScreenCaptureIntent());
        }
    }

    void resetFolder(boolean isAudio) {
        if (isAudio == true) {
            appSettingsEditor.remove("folderaudiopath");
        } else {
            appSettingsEditor.remove("folderpath");
        }

        appSettingsEditor.commit();
        Toast.makeText(this, R.string.error_invalid_folder, Toast.LENGTH_SHORT).show();
        chooseDir(true, isAudio);
    }

    void chooseDir(boolean toRecording, boolean isAudio) {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        if (toRecording == true) {
            if (isAudio == true) {
                requestAudioFolderPermissionAndProceed.launch(intent);
            } else {
                requestFolderPermissionAndProceed.launch(intent);
            }
        } else {
            if (isAudio == true) {
                requestAudioFolderPermission.launch(intent);
            } else {
                requestFolderPermission.launch(intent);
            }
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
            } else {
                Toast.makeText(this, R.string.error_audio_required, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_MICROPHONE_PLAYBACK) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appSettingsEditor.putBoolean("checksoundplayback", true);
                appSettingsEditor.commit();
            } else {
                Toast.makeText(this, R.string.error_audio_required, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_MICROPHONE_RECORD) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkDirRecord(appSettings.getBoolean("recordmode", false));
            } else {
                Toast.makeText(this, R.string.error_audio_required, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkDirRecord(false);
            } else {
                Toast.makeText(this, R.string.error_storage_required, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_AUDIO) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkDirRecord(true);
            } else {
                Toast.makeText(this, R.string.error_storage_required, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_MODE_CHANGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setRecordMode(true);
            } else {
                Toast.makeText(this, R.string.error_audio_required, Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onResume() {
        if (onShakeEventHelper != null && onShakeEventHelper.hasListenerChanged()) {
            onShakeEventHelper.unregisterListener();
            onShakeEventHelper.registerListener();
        }
        super.onResume();
    }

    @Subscribe
    public void onServiceConnected(ServiceConnectedEvent event) {
        if (event.isServiceConnected()) {
            onShakeEventHelper = new OnShakeEventHelper(recordingBinder, this);
            onShakeEventHelper.registerListener();
        }
        else {
            onShakeEventHelper.unregisterListener();
        }
    }
}
