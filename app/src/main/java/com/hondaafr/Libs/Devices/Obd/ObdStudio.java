package com.hondaafr.Libs.Devices.Obd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Devices.Spartan.SpartanCommands;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudioListener;
import com.hondaafr.Libs.Helpers.Debuggable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ObdStudio extends Debuggable {
    private final Context context;
    private ObdStudioListener listener;
    public ArrayList<Double> mTpsHistory = new ArrayList<>();

    private boolean readingsOn = false;

    private ScheduledExecutorService scheduler;
    private Long lastReadingTimestamp = 0L;

    public ObdStudio(Context mContext, ObdStudioListener listener) {
        this.listener = listener;
        this.context = mContext;
    }

    public void startRequestingSensorReadings() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(2);
        }

        final Runnable requestTask = this::requestSensorReadings;

        scheduler.scheduleAtFixedRate(requestTask, 0, 500, TimeUnit.MILLISECONDS);
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


    public void requestSensorReadings() {
        requestTps();
    }


    public void initBtConnection() {

    }

    public void requestTps() {
        BluetoothService.send(context, ObdCommands.requestTps(), "obd");
    }


    public void onDataReceived(String data) {
        d(data, 1);

        if (ObdCommands.dataIsTps(data)) {
            Double tps = ObdCommands.parseTpsData(data);
            mTpsHistory.add(tps);
            listener.onSensorTpsReceived(tps);
        }
    }

    public void start() {
        initBtConnection();

        if (!readingsOn) {
            startRequestingSensorReadings();
        } else {
            stopRequestingSensorReadings();
        }
    }


    public void updateLastTrackingDataTimestamp() {
        lastReadingTimestamp = System.currentTimeMillis();
    }

    public long timeSinceLastSensorReadings() {
        return System.currentTimeMillis() - lastReadingTimestamp;
    }


}
