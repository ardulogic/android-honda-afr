package com.hondaafr.Libs.Devices.Phone;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class PhoneLightSensor implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor lightSensor;

    private float lightLevel = 0f;
    private final PhoneLightSensorListener listener;

    public PhoneLightSensor(Context context, PhoneLightSensorListener listener) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        this.listener = listener;

        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public float getLightLevel() {
        return lightLevel;
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            lightLevel = event.values[0];
        }

        listener.onLightIntensityUpdated((double) lightLevel);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
