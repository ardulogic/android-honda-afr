package com.hondaafr.Libs.Devices.Spartan;

import android.content.Context;

import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Helpers.Studio;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SpartanStudio extends Studio {

    private static final long LINK_TIMEOUT_MS = 500L;     // Sensor considered dead after this
    private static final long READING_PERIOD_MS = 50L;    // Read every 50ms
    private static final long SUPERVISOR_PERIOD_MS = 1000L;

    private final Context context;
    private final SpartanStudioListener listener;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> supervisorTask;
    private ScheduledFuture<?> readingTask;

    public boolean isReading() {
        return isAlive() && lastSensorAfr > 0;
    }

    private enum Phase { RUNNING, STOPPED }
    private Phase phase = Phase.STOPPED;

    public double targetAfr = 14.7;
    public double lastSensorAfr = 0.0;
    public double lastSensorTemp = 0.0;

    private long lastSensorReadingsTimestamp = 0L;
    private boolean linkPreviouslyAlive = false;

    public SpartanStudio(Context context, SpartanStudioListener listener) {
        this.context = context;
        this.listener = listener;
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────────────────

    public void start() {
        if (phase == Phase.RUNNING) return;

        startReadingTask();
        startSupervisor();
        phase = Phase.RUNNING;
    }

    public void stop() {
        if (phase == Phase.STOPPED) return;

        if (readingTask != null) readingTask.cancel(true);
        if (supervisorTask != null) supervisorTask.cancel(true);
        scheduler.shutdownNow();
        phase = Phase.STOPPED;
    }

    private void startReadingTask() {
        readingTask = scheduler.scheduleAtFixedRate(
                this::requestSensorReadings,
                0, READING_PERIOD_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private void startSupervisor() {
        supervisorTask = scheduler.scheduleAtFixedRate(() -> {
            boolean alive = isAlive();
            if (alive && !linkPreviouslyAlive) {
                listener.onAfrConnectionPulse(true);
                linkPreviouslyAlive = true;
            } else if (!alive && linkPreviouslyAlive) {
                listener.onAfrConnectionPulse(false);
                linkPreviouslyAlive = false;
            }
        }, 0, SUPERVISOR_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Bluetooth interaction
    // ────────────────────────────────────────────────────────────────────────────────

    private void requestSensorReadings() {
        if (targetAfr == 0) {
            requestTargetAfr(context);
        } else {
            requestCurrentAfr(context);
        }
    }

    public static void requestTargetAfr(Context context) {
        BluetoothService.send(context, SpartanCommands.getTargetAFR(), "spartan");
    }

    public static void requestCurrentAfr(Context context) {
        BluetoothService.send(context, SpartanCommands.requestCurrentAfr(), "spartan");
    }

    public void setAFR(double target) {
        targetAfr = target;
        BluetoothService.send(context, SpartanCommands.setAFR(targetAfr), "spartan");

        listener.onTargetAfrUpdated(targetAfr);
    }

    public void adjustAFR(double delta) {
        setAFR(targetAfr + delta);
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Data handling
    // ────────────────────────────────────────────────────────────────────────────────

    public void onDataReceived(String data) {
        d(data, 1);

        if (SpartanCommands.dataIsTargetLambda(data)) {
            targetAfr = SpartanCommands.parseTargetLambdaAndConvertToAfr(data);
            listener.onTargetAfrUpdated(targetAfr);
        } else if (SpartanCommands.dataIsSensorAfr(data)) {
            lastSensorAfr = SpartanCommands.parseSensorAfr(data);
            listener.onSensorAfrReceived(lastSensorAfr);
            updateLastTrackingDataTimestamp();
        } else if (SpartanCommands.dataIsSensorTemp(data)) {
            lastSensorTemp = SpartanCommands.parseSensorTemp(data);
            listener.onSensorTempReceived(lastSensorTemp);
            updateLastTrackingDataTimestamp();
        }
    }

    private void updateLastTrackingDataTimestamp() {
        lastSensorReadingsTimestamp = System.currentTimeMillis();
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // State and metrics
    // ────────────────────────────────────────────────────────────────────────────────

    public boolean isAlive() {
        return timeSinceLastSensorReadings() < LINK_TIMEOUT_MS;
    }

    public boolean isRunning() {
        return phase == Phase.RUNNING && !scheduler.isShutdown();
    }

    public long timeSinceLastSensorReadings() {
        return System.currentTimeMillis() - lastSensorReadingsTimestamp;
    }

    public Map<String, String> getReadingsAsString() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("Target AFR", String.valueOf(targetAfr));
        map.put("AFR", String.valueOf(lastSensorAfr));
        map.put("O2 Temp", String.valueOf(lastSensorTemp));
        return map;
    }

    public void onResume(Context ctx) {
        if (!isRunning()) {
            start();
        }
    }
}
