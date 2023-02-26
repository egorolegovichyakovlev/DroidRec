package com.yakovlevegor.DroidRec.shake;

import static java.lang.Math.sqrt;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yakovlevegor.DroidRec.shake.event.OnShakePreferenceChangeEvent;
import com.yakovlevegor.DroidRec.ScreenRecorder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class OnShakeEventHelper {
    private SensorEventListener currentListener;
    private boolean hasListenerChanged;
    private Context context;
    private ScreenRecorder.RecordingBinder recordingBinder;
    private float acceleration = 10f;
    private float currentAcceleration = SensorManager.GRAVITY_EARTH;
    private float lastAcceleration = SensorManager.GRAVITY_EARTH;
    private SensorManager sensorManager;
    private Toast toast;
    private SensorEventListener emptyListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) { }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };

    public OnShakeEventHelper(ScreenRecorder.RecordingBinder recordingBinder, AppCompatActivity activity) {
        this.recordingBinder = recordingBinder;
        String initialValue = activity.getSharedPreferences(ScreenRecorder.prefsident, 0).getString("onshake", "Do nothing");
        currentListener = giveMeSensorListenerFor(initialValue);
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        context = activity.getApplicationContext();

        EventBus.getDefault().register(this);
    }

    public void registerListener() {
        sensorManager.registerListener(
                currentListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL
        );
        hasListenerChanged = false;
    }

    public void unregisterListener() {
        sensorManager.unregisterListener(currentListener);
    }

    @Subscribe
    public void onShakePreferenceChanged(OnShakePreferenceChangeEvent event) {
        currentListener = giveMeSensorListenerFor(event.getState());
        hasListenerChanged = true;
    }

    public boolean hasListenerChanged() {
        return hasListenerChanged;
    }

    private SensorEventListener giveMeSensorListenerFor(Runnable action, String msg) {
        return new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // Fetching x,y,z values
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                lastAcceleration = currentAcceleration;

                // Getting current accelerations
                // with the help of fetched x,y,z values
                currentAcceleration = (float) sqrt(x * x + y * y + z * z);
                float delta = currentAcceleration - lastAcceleration;
                acceleration = acceleration * 0.9f + delta;

                // Display a Toast message if
                // acceleration value is over 12
                if (acceleration > 12 && recordingBinder.isStarted()) {
                    action.run();
                    showToast(msg);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
    }
    private SensorEventListener giveMeSensorListenerFor(String state) {
        if("Do nothing".equals(state)) return emptyListener;

        if("Pause".equals(state)) return giveMeSensorListenerFor(() -> recordingBinder.recordingPause(), "Recording is paused");

        if("Stop".equals(state)) return giveMeSensorListenerFor(() -> recordingBinder.stopService(), "Recording is stopped");

        throw new IllegalArgumentException("Should never occur");
    }

    private void showToast(String msg) {
        if(toast != null) toast.cancel();
        toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        toast.show();
    }
}
