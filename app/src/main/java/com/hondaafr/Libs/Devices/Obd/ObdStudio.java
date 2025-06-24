package com.hondaafr.Libs.Devices.Obd;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Helpers.Debuggable;
import com.hondaafr.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ObdStudio extends Debuggable {
    private final Context context;
    private final ObdStudioListener listener;

    public ObdReadings readings;

    private boolean readingsOn = false;

    private ScheduledExecutorService scheduler;

    private long lastReadingTimestamp = 0L;

    private Long lastResponseTimestamp = 0L;
    private boolean isBusy = false;
    private boolean ecuConnected = false;

    public static final List<String> FUEL_CONS_OBD_READINGS = Arrays.asList( "rpm", "map", "speed");

    public ObdStudio(Context mContext, ArrayList<String> pid_names, ObdStudioListener listener) {
        this.listener = listener;
        this.context = mContext;

        this.readings = new ObdReadings(context, pid_names);
    }

    public ObdStudio(MainActivity mContext,  ObdStudioListener listener) {
        this(mContext, new ArrayList<>(),  listener);

        loadAndSetActivePids();
    }

    public void loadAndSetActivePids() {
        this.readings = new ObdReadings(context, loadActivePids());

        listener.onObdReadingsToggled();
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

        // Extra commands
//        lines.add("ATE0\r");    // Echo off
//        lines.add("ATL0\r");    // Linefeeds off
//        lines.add("ATS0\r");    // Spaces off
//        lines.add("ATH0\r");    // Headers off
//        lines.add("ATSP0\r");   // Set protocol to auto

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
            for (ObdReading reading : readings.active.values()) {
                if (reading.incomingDataIsReply(data)) {  // assuming reading is passed correctly
                    reading.onData(data);  // Handle the data.
                    updateTimeSinceLastReading();

                    listener.onObdReadingUpdate(reading);  // Notify the listener of the update.
                }
            }

            if (readings.active.size() == 1) {
                this.readings.requestNextReading();
            } else {
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.schedule(() -> {
                    readings.requestNextReading();
                }, 100, TimeUnit.MILLISECONDS);
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

    public Map<String, String> getReadingsAsString() {
        LinkedHashMap<String, String> readings = new LinkedHashMap<>();

        for (ObdReading r : this.readings.available.values()) {
            readings.put(r.getDisplayName(), r.getValueAsString());
        }

        return readings;
    }

    public boolean readingsForFuelConsAvailable() {
        Map<String, ObdReading> readings = getActiveReadings();

        for (String key : FUEL_CONS_OBD_READINGS) {
            if (!readings.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    public Map<String, ObdReading> getAvailableReadings() {
        return this.readings.available;
    }

    public ObdReading getAvailableReading(String name) {
        return this.readings.getAvailable(name);
    }

    public Map<String, ObdReading> getActiveReadings() {
        return this.readings.active;
    }

    public void saveActivePids() {
        ArrayList<String> obdPids = readings.getActiveIds();

        // Convert ArrayList to a Set or JSON string and save it to SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("ObdPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Convert ArrayList to Set
        Set<String> obdPidsSet = new HashSet<>(obdPids);

        // Save the Set to SharedPreferences
        editor.putStringSet("obdPids", obdPidsSet);
        editor.apply();  // Commit changes asynchronously
    }

    private ArrayList<String> loadActivePids() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("ObdPrefs", MODE_PRIVATE);

        // Retrieve the saved Set, return a default empty Set if not found
        Set<String> obdPidsSet = sharedPreferences.getStringSet("obdPids", new HashSet<>());

        // Convert the Set back to ArrayList
        return new ArrayList<>(obdPidsSet);
    }

    public void setAsActiveOnly(ArrayList<String> pid_names) {
        readings.setAsActiveOnly(pid_names);

        listener.onObdReadingsToggled();
    }

    public void setAsActiveAdd(ArrayList<String> pid_names) {
        readings.setAsActiveAdd(pid_names);

        listener.onObdReadingsToggled();
    }
}
