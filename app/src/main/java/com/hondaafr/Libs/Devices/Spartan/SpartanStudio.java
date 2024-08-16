package com.hondaafr.Libs.Devices.Spartan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.LocationRequest;

import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Helpers.Debuggable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpartanStudio extends Debuggable {
    private final Context context;
    private SpartanStudioListener listener;
    public ArrayList<Double> mAfrHistory = new ArrayList<>();
    public ArrayList<Long> mTimestampHistory = new ArrayList<>();

    public ArrayList<Double> mTempHistory = new ArrayList<>();
    public ArrayList<Double> mSpeedHistory = new ArrayList<>();

    private boolean readingsOn = false;

    private ScheduledExecutorService scheduler;

    private static final long UPDATE_INTERVAL_MS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_MS = 500;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Double lastGpsSpeedKmh = 0.0D;
    private Double lastSensorTemp = 0.0D;

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        startLocationUpdates();
        Log.d("Speed", "Started");
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
            Double afr = SpartanCommands.parseSensorAfr(data);
            mAfrHistory.add(afr);
            mSpeedHistory.add((double) lastGpsSpeedKmh);
            mTempHistory.add(lastSensorTemp);
            mTimestampHistory.add(System.currentTimeMillis());
            listener.onSensorAfrReceived(afr);
            updateLastTrackingDataTimestamp();
        }

        if (SpartanCommands.dataIsSensorTemp(data)) {
            Double temp = SpartanCommands.parseSensorTemp(data);
            lastSensorTemp = temp;
            listener.onSensorTempReceived(temp);
            updateLastTrackingDataTimestamp();
        }
    }


    public List<String[]> getDataAsTable() {
        List<String[]> table = new ArrayList<>();
        table.add(new String[]{"Timestamp", "AfrValue", "Temperature", "Speed (Km/h)"});

        // Determine the number of rows based on the size of the lists
        int rowCount = mAfrHistory.size();
        if (mTimestampHistory.size() != rowCount || mTempHistory.size() != rowCount) {
            throw new IllegalStateException("The size of the lists do not match.");
        }

        for (int i = 0; i < rowCount; i++) {
            String[] row = new String[4];
            row[0] = mTimestampHistory.get(i).toString(); // Convert timestamp to String
            row[1] = mAfrHistory.get(i).toString(); // Convert AfrValue to String
            row[2] = mTempHistory.get(i).toString(); // Convert Temperature to String
            row[3] = mSpeedHistory.get(i).toString(); // Convert Temperature to String
            table.add(row);
        }

        return table;
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
        BluetoothService.send(context, SpartanCommands.setAFR(targetAfr));

        listener.onTargetAfrUpdated(targetAfr);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Log.d("Speed", "Building location request");
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL_MS)
                    .build();


            Log.d("Speed", "Building location callback");

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult == null) {
                        Log.d("Speed", "No locaiton result");
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        // Calculate speed in km/h
                        lastGpsSpeedKmh = (double) (location.getSpeed() * 3.6f);
                        listener.onSpeedUpdated(lastGpsSpeedKmh);
                    }
                }
            };

            Log.d("Speed", "Location updates requested");
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            Log.d("Speed", "Bad sdk");
        }
    }

}
