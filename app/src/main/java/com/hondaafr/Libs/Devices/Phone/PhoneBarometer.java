package com.hondaafr.Libs.Devices.Phone;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class PhoneBarometer implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor pressureSensor;

    private float currentPressure = 101.325f; // Default sea level pressure in kPa

    public PhoneBarometer(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor != null) {
            sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public float getPressureKPa() {
        return currentPressure;
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            currentPressure = event.values[0]; // hPa == mbar, equivalent to kPa
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

