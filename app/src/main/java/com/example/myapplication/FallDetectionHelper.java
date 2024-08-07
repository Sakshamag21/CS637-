package com.example.myapplication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;
import android.util.Log;

public class FallDetectionHelper {

    private static final String TAG = "FallDetectionHelper";
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private SensorEventListener gyroscopeListener;
    private TextView angularVelocityTextView;
    private boolean isFallDetected = false;

    // Constructor to initialize SensorManager, gyroscopeSensor, and TextView
    public FallDetectionHelper(Context context, TextView textView) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        angularVelocityTextView = textView;

        if (gyroscopeSensor == null) {
            Log.e(TAG, "Gyroscope sensor not available on this device");
            // Handle case where gyroscope sensor is not available
        } else {
            Log.d(TAG, "Gyroscope sensor available");
        }
    }

    // Method to start listening for gyroscope events
    public void startListeningForFallDetection() {
        gyroscopeListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                // Calculate angular velocity magnitude
                float angularVelocityMagnitude = (float) Math.sqrt(x*x + y*y + z*z);

                // Update the TextView with the current angular velocity magnitude
                angularVelocityTextView.setText("Angular Velocity: " + angularVelocityMagnitude);
//                Log.d("Fall", String.valueOf(angularVelocityMagnitude));

                // Implement your fall detection algorithm
                if (isFallDetected(angularVelocityMagnitude)) {
                    handleFallDetected(angularVelocityMagnitude);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Handle accuracy changes if needed
            }
        };

        // Register gyroscope listener with SensorManager
        sensorManager.registerListener(gyroscopeListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    // Method to stop listening for gyroscope events
    public void stopListeningForFallDetection() {
        sensorManager.unregisterListener(gyroscopeListener);
    }

    // Example fall detection algorithm (replace with your own)
    private boolean isFallDetected(float angularVelocityMagnitude) {
        // Example: Threshold-based fall detection
        if (angularVelocityMagnitude > 5.0f) {
            return true;
        }
        return false;
    }

    // Example method to handle fall detected event
    private void handleFallDetected(float angularVelocityMagnitude) {
        Log.d(TAG, "Fall detected!");
    }
}
