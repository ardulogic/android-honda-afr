package com.hondaafr.Libs.Devices.Obd;

import android.content.Context;

import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Helpers.Debuggable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ObdStudio extends Debuggable {
    private final Context context;
    private final ObdStudioListener listener;

    public final ObdReadings readings;

    private boolean readingsOn = false;

    private ScheduledExecutorService scheduler;

    private long lastReadingTimestamp = 0L;

    private Long lastResponseTimestamp = 0L;
    private boolean isBusy = false;
    private boolean ecuConnected = false;


    public ObdStudio(Context mContext, ArrayList<String> pid_names, ObdStudioListener listener) {
        this.listener = listener;
        this.context = mContext;

        this.readings = new ObdReadings(context, pid_names);
    }

    public void startRequestingSensorReadings() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(2);
        }

        final Runnable requestTask = this::requestSensorReadings;

        scheduler.scheduleAtFixedRate(requestTask, 0, 100, TimeUnit.MILLISECONDS);
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
        if (!this.readings.active.isEmpty()) {
            if (ecuConnected) {
                if (timeSinceLastReading() > 3000) {
                    this.readings.requestNextReading();
                }
            } else {
                if (timeSinceLastResponse() > 3000) {
                    initBtConnection();
                }
            }
        }
    }


    private boolean deviceIsNotBusy() {
        return !isBusy || timeSinceLastResponse() > 10000;
    }

    public void initBtConnection() {
        isBusy = true;

        // Example usage of the send method
        ArrayList<String> lines = new ArrayList<>();
        lines.add(ObdCommands.resetObd());
        BluetoothService.send(context, lines, "obd");
    }

    public void onDataReceived(String data) {
        d(data, 1);
        isBusy = false;

        if (ObdCommands.dataIsBusy(data)) {
            isBusy = true;
        }

        if (ObdCommands.dataCantConnectToEcu(data)) {
            ecuConnected = false;
        }

        if (ObdCommands.dataEcuConnected(data)) {
            ecuConnected = true;
        }

        if (ecuConnected) {
            for (ObdReading reading : readings.active) {
                if (reading.incomingDataIsReply(data)) {  // assuming reading is passed correctly
                    reading.onData(data);  // Handle the data.
                    updateTimeSinceLastReading();

                    listener.onObdReadingUpdate(reading);  // Notify the listener of the update.
                    this.readings.requestNextReading();
                }
            }
        }

        updateTimeSinceLastResponse();
    }

    public void start() {
        initBtConnection();

        if (!readingsOn) {
            startRequestingSensorReadings();
        } else {
            stopRequestingSensorReadings();
        }
    }


    public void updateTimeSinceLastResponse() {
        lastResponseTimestamp = System.currentTimeMillis();
    }

    public long timeSinceLastResponse() {
        return System.currentTimeMillis() - lastResponseTimestamp;
    }

    public void updateTimeSinceLastReading() {
        lastReadingTimestamp = System.currentTimeMillis();
    }

    public long timeSinceLastReading() {
        return System.currentTimeMillis() - lastReadingTimestamp;
    }

    public Map<String, String> getReadings() {
        LinkedHashMap<String, String> readings = new LinkedHashMap<>();

        // Populate the map with some sample readings
        for (ObdReading r : this.readings.available) {
            readings.put(r.getDisplayName(), r.getValueAsString());
        }

        return readings;
    }
}
