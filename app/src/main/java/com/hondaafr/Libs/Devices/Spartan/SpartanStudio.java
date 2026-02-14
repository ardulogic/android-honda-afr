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
    private static final long COMMUNICATION_CORE_TICK_MS = 10L;    // Poll interval
    private static final long MIN_TIME_AFTER_REQUEST_MS = 30L;     // Min gap between sends
    private static final long MIN_TIME_AFTER_RESPONSE_MS = 0L;    // No delay after response; send next command immediately
    private static final long SUPERVISOR_PERIOD_MS = 1000L;
    private static final String TAG = "SpartanStudio";

    private final Context context;
    private final SpartanStudioListener listener;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> supervisorTask;
    private ScheduledFuture<?> readingTask;

    private enum Phase {RUNNING, STOPPED}

    private Phase phase = Phase.STOPPED;

    public Double targetAfr = 14.7;
    public double lastSensorAfr = 0.0;
    public double lastSensorTemp = 0.0;

    private boolean targetAfrReceived = false;

    private long timeLastReadingReceived = 0L;
    private long timeLastDataReceived = 0L;
    private static long timeLastRequestSent = 0L;
    private boolean linkPreviouslyAlive = false;
    private static String pendingSetAfrCommand = null;
    private static String pendingGetAfrCommand = null;
    private static String pendingGetTargetAfrCommand = null;

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
                this::requestSensorReadingsWithThrottling,
                0, COMMUNICATION_CORE_TICK_MS,
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

    private void requestSensorReadingsWithThrottling() {
        boolean isConnected = BluetoothService.isConnected("spartan") || SpartanSimulator.isEnabled(context);
        boolean isDataReceivedAfterRequest = timeLastDataReceived >= timeLastRequestSent;
        boolean isTimeSinceDataReceivedPassed = timeSinceDataReceived() > MIN_TIME_AFTER_RESPONSE_MS;
        boolean isTimeSinceLastRequestPassed = timeSinceLastRequestSent() > MIN_TIME_AFTER_REQUEST_MS;
        boolean isSpamming = !(
                            isDataReceivedAfterRequest
                        && isTimeSinceDataReceivedPassed
                        && isTimeSinceLastRequestPassed
        );
        boolean isTimeout = timeSinceLastRequestSent() >= LINK_TIMEOUT_MS;


        if ((!isConnected || isSpamming) && !isTimeout) {
            return;
        }

        if (pendingSetAfrCommand != null) {
            sendRequest(context, pendingSetAfrCommand);
            return;
        }

        if (pendingGetTargetAfrCommand != null) {
            sendRequest(context, pendingGetTargetAfrCommand);
            return;
        }

        if (pendingGetAfrCommand != null) {
            sendRequest(context, pendingGetAfrCommand);
            return;
        }

        if (isRunning()) {
            if (!targetAfrReceived) {
                pendingGetTargetAfrCommand = SpartanCommands.getTargetAFR();
                sendRequest(context, pendingGetTargetAfrCommand);
            } else {
                pendingGetAfrCommand = SpartanCommands.getCurrentAfr();
                sendRequest(context, pendingGetAfrCommand);
            }
        }
    }

    private void requestSensorReadings() {
        if (!targetAfrReceived) {
            requestTargetAfr(context);
        } else {
            requestCurrentAfr(context);
        }
    }

    public static void requestTargetAfr(Context context) {
        sendRequest(context, SpartanCommands.getTargetAFR());
    }

    public static void requestCurrentAfr(Context context) {
        sendRequest(context, SpartanCommands.getCurrentAfr());
    }

    public void setAFR(double target) {
        targetAfr = target;
        pendingSetAfrCommand = SpartanCommands.setAFR(targetAfr);
        listener.onTargetAfrUpdated(targetAfr);
    }

    private static void sendRequest(Context context, String cmd) {
        AfrLogStore.logTx(cmd);
        BluetoothService.send(context, cmd, "spartan");
        timeLastRequestSent = System.currentTimeMillis();
    }

    public void adjustAFR(double delta) {
        setAFR(targetAfr + delta);
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Data handling
    // ────────────────────────────────────────────────────────────────────────────────

    public void onDataReceived(String data) {
        if (data != null && !data.trim().isEmpty()) {
            AfrLogStore.logRx(data);
        }
        updateDataReceivedTimestamp();

        if (SpartanCommands.dataIsSetAfrAck(data)) {
            // Real device responds with "OK Please Power Cycle Spartan 3" to SETNBSWLAM
            pendingSetAfrCommand = null;
        } else if (SpartanCommands.dataIsTargetLambda(data)) {
            targetAfr = SpartanCommands.parseTargetLambdaAndConvertToAfr(data);
            targetAfrReceived = true;
            listener.onTargetAfrUpdated(targetAfr);
            pendingGetTargetAfrCommand = null;
            pendingSetAfrCommand = null;
        } else if (SpartanCommands.dataIsSensorAfr(data)) {
            lastSensorAfr = SpartanCommands.parseSensorAfr(data);
            listener.onSensorAfrReceived(lastSensorAfr);
            updateReadingsReceivedTimestamp();
            pendingGetAfrCommand = null;
        } else if (SpartanCommands.dataIsSensorTemp(data)) {
            lastSensorTemp = SpartanCommands.parseSensorTemp(data);
            listener.onSensorTempReceived(lastSensorTemp);
            updateReadingsReceivedTimestamp();
        }
    }

    private void updateReadingsReceivedTimestamp() {
        timeLastReadingReceived = System.currentTimeMillis();
    }

    public long timeSinceLastSensorReadingReceived() {
        return System.currentTimeMillis() - timeLastReadingReceived;
    }

    private void updateDataReceivedTimestamp() {
        timeLastDataReceived = System.currentTimeMillis();
    }

    public long timeSinceDataReceived() {
        return System.currentTimeMillis() - timeLastDataReceived;
    }

    public static long timeSinceLastRequestSent() {
        return System.currentTimeMillis() - timeLastRequestSent;
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // State and metrics
    // ────────────────────────────────────────────────────────────────────────────────

    public boolean isAlive() {
        return timeSinceDataReceived() < LINK_TIMEOUT_MS;
    }

    public boolean isReading() {
        return timeSinceLastSensorReadingReceived() < LINK_TIMEOUT_MS
                && lastSensorAfr > 0;
    }

    public boolean isRunning() {
        return phase == Phase.RUNNING && !scheduler.isShutdown();
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
