package com.gome.launcher;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.gome.launcher.util.DLog;


/**
 * Created by huangshuai on 2016/5/13.
 * move from launcher6.0 to mtk7.0 by rongwenzhao 2017-7-3
 */
public class ShakeListener implements SensorEventListener {
    private static final String TAG = "ShakeListener";
    private static final boolean LOG = false;
    // The velocity threshold value is generated
    // when the shaking speed is reached.
    private static final int SPEED_SHRESHOLD = 3000;
    // Two time interval of detection
    private static final int UPTATE_INTERVAL_TIME = 70;
    // Sensor Manager
    private SensorManager sensorManager;
    // sensor
    private Sensor sensor;
    // Gravity sensing monitor
    private OnShakeListener onShakeListener;
    // Context
    private Context mContext;
    // The position of the mobile phone
    // when the gravity sensor coordinates
    private float lastX;
    private float lastY;
    private float lastZ;
    // Last detection time
    private long lastUpdateTime;

    public static boolean isRegister = false;

    public ShakeListener(Context c) {
        // Get listener object
        mContext = c;
        // get Sensor Manager
        sensorManager = (SensorManager) mContext
                .getSystemService(Context.SENSOR_SERVICE);
    }


    /**
     * start listener
     */
    public void start() {
        if(isRegister){
            return;
        }
        if (sensorManager == null) {
            sensorManager = (SensorManager) mContext
                    .getSystemService(Context.SENSOR_SERVICE);
        }
        // Get the gravity sensor
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // register
        if (sensor != null) {
            if(sensorManager.registerListener(this, sensor,
                    SensorManager.SENSOR_DELAY_GAME)){
                isRegister = true;
            } else {
                DLog.e(TAG,"ShakeListener --> start() error register fail");
            }
        }
    }

    /**
     * Stop listener
     */
    public void stop() {
        if(!isRegister){
            return;
        }
        if(sensorManager != null){
            sensorManager.unregisterListener(this);
            isRegister = false;
        }
    }

    // Set the gravity sensing monitor
    public void setOnShakeListener(OnShakeListener listener) {
        onShakeListener = listener;
    }

    /**
     * Gravity sensor induction to obtain change data
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Current detection time
        long currentUpdateTime = System.currentTimeMillis();
        // Two time interval of detection
        long timeInterval = currentUpdateTime - lastUpdateTime;
        // To determine whether to achieve the detection time interval
        if (timeInterval < UPTATE_INTERVAL_TIME){
            return;
        }
        // Save last time
        lastUpdateTime = currentUpdateTime;
        // Get x, y, z coordinates
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        // Get the change value of X, Y, Z
        float deltaX = x - lastX;
        float deltaY = y - lastY;
        float deltaZ = z - lastZ;
        // Save present the coordinates into last coordinates
        lastX = x;
        lastY = y;
        lastZ = z;
        double speed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ
                * deltaZ)
                / timeInterval * 10000;
        DLog.e(TAG, "===========log===================");
        // Reach a speed threshold, prompting
        if (speed >= SPEED_SHRESHOLD) {
            onShakeListener.onShake();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * Shake monitor interface
     */
    public interface OnShakeListener {
         void onShake();
    }
}
