package com.hondaafr.Libs.Devices.Spartan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.LocationRequest;

import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Devices.Obd.ObdStudio;
import com.hondaafr.Libs.Helpers.Debuggable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpartanStudio extends Debuggable {
    private final Context context;
    private SpartanStudioListener listener;

    private boolean readingsOn = false;

    private ScheduledExecutorService scheduler;

    public Double lastSensorTemp = 0.0D;
    public Double lastSensorAfr = 0.0D;

    public void startRequestingSensorReadings() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(2);
        }

        final Runnable requestTask = this::requestSensorReadings;

        scheduler.scheduleAtFixedRate(requestTask, 0, 50, TimeUnit.MILLISECONDS);
        readingsOn = true;
    }

    public void stopRequestingSensorReadings() {

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        readingsOn = false;
        scheduler = null;
    }

    public double targetAfr = 14.7;

    private Long lastSensorReadingsTimestamp = 0L;


    public SpartanStudio(Context mContext, SpartanStudioListener listener) {
        this.listener = listener;
        this.context = mContext;
    }

    public static void requestCurrentAFR(Context context) {
        BluetoothService.send(context, SpartanCommands.getAFR(), "spartan");
    }

    public void requestSensorReadings() {
        BluetoothService.send(context, SpartanCommands.requestSensorReadings(), "spartan");
    }


    public void onDataReceived(String data) {
        d(data, 1);

        if (SpartanCommands.dataIsTargetLambda(data)) {
            targetAfr = SpartanCommands.parseTargetLambdaAndConvertToAfr(data);
            listener.onTargetAfrUpdated(targetAfr);
        }

        if (SpartanCommands.dataIsSensorAfr(data)) {
            lastSensorAfr = SpartanCommands.parseSensorAfr(data);
            listener.onSensorAfrReceived(lastSensorAfr);
            updateLastTrackingDataTimestamp();
        }

        if (SpartanCommands.dataIsSensorTemp(data)) {
            lastSensorTemp = SpartanCommands.parseSensorTemp(data);
            listener.onSensorTempReceived(lastSensorTemp);
            updateLastTrackingDataTimestamp();
        }
    }

    public void start() {
        requestCurrentAFR(context);

        if (!readingsOn) {
            startRequestingSensorReadings();
        } else {
            stopRequestingSensorReadings();
        }
    }


    public void updateLastTrackingDataTimestamp() {
        lastSensorReadingsTimestamp = System.currentTimeMillis();
    }

    public long timeSinceLastSensorReadings() {
        return System.currentTimeMillis() - lastSensorReadingsTimestamp;
    }

    public void adjustAFR(double adjustment) {
        targetAfr += adjustment;
        setAFR(targetAfr);
    }

    public void setAFR(double target) {
        targetAfr = target;
        BluetoothService.send(context, SpartanCommands.setAFR(targetAfr), "spartan");

        listener.onTargetAfrUpdated(targetAfr);
    }

    public Map<String, String> getReadings() {
        LinkedHashMap<String, String> readings = new LinkedHashMap<>();

        // Populate the map with some sample readings
        readings.put("Target AFR", String.valueOf(targetAfr));
        readings.put("AFR", String.valueOf(lastSensorAfr));
        readings.put("o2 Temp", String.valueOf(lastSensorTemp));

        // Return the populated map
        return readings;
    }
}
