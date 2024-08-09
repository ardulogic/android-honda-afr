package com.hondaafr.Libs.Devices.Spartan;

import android.content.Context;

import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Helpers.Debuggable;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpartanStudio extends Debuggable {
    private Context context;
    private SpartanStudioListener listener;
    public ArrayList<Double> mAfrHistory = new ArrayList<>();

    private boolean readingsOn = false;

    private ScheduledExecutorService scheduler;

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
        BluetoothService.send(context, SpartanCommands.getAFR());
    }

    public void requestSensorReadings() {
        BluetoothService.send(context, SpartanCommands.requestSensorReadings());
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
            listener.onSensorAfrReceived(afr);
            updateLastTrackingDataTimestamp();
        }

        if (SpartanCommands.dataIsSensorTemp(data)) {
            Double temp = SpartanCommands.parseSensorTemp(data);
            mAfrHistory.add(temp);
            listener.onSensorTempReceived(temp);
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
        BluetoothService.send(context, SpartanCommands.setAFR(targetAfr));

        listener.onTargetAfrUpdated(targetAfr);
    }
}
