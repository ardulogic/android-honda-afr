package com.hondaafr.Libs.Devices.Obd;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Helpers.Debuggable;
import com.hondaafr.MainActivity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Core orchestrator for OBD‑II communication through an ELM327 adapter.
 * <p>
 * ▸ Runs the full ELM327 initialisation handshake, verifying every <b>OK</b> reply.<br>
 * ▸ Maintains a continuous PID‑polling loop once initialised.<br>
 * ▸ Detects and recovers from <code>NO DATA</code> errors & link time‑outs.<br>
 * ▸ Persists user‑selected PIDs and restores them at start‑up.<br>
 * ▸ Surfaces connection state transitions through {@link ObdStudioListener}.
 */
public class ObdStudio extends Debuggable {

    // ────────────────────────────────────────────────────────────────────────────────
    // Configuration constants
    // ────────────────────────────────────────────────────────────────────────────────

    private static final long LINK_TIMEOUT_MS          = 3_000L;  // liveness threshold
    private static final long SUPERVISOR_PERIOD_MS     = 1_000L;  // watchdog tick
    private static final long INIT_RESPONSE_TIMEOUT_MS = 2_000L;  // wait for OK
    private static final long READING_DELAY = 50;

    // ────────────────────────────────────────────────────────────────────────────────
    // Types & state
    // ────────────────────────────────────────────────────────────────────────────────

    private enum Phase { INITIALISING, RUNNING }
    private Phase phase = Phase.INITIALISING;

    private final Context                context;
    private final ObdStudioListener      listener;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?>           supervisorTask;

    /** Holder for all available & active readings. */
    public ObdReadings readings;

    private long lastReadingTimestamp   = 0L;
    private long lastResponseTimestamp  = 0L;
    private long initCmdSentTimestamp   = 0L;

    private volatile boolean linkPreviouslyAlive = false;

    // Queue of remaining init‑commands. Pop once each “OK” is seen.
    private Deque<String> initQueue;

    public static final List<String> FUEL_CONS_OBD_READINGS = Arrays.asList("rpm", "map", "speed");

    // ────────────────────────────────────────────────────────────────────────────────
    // Constructors
    // ────────────────────────────────────────────────────────────────────────────────

    public ObdStudio(Context ctx, ArrayList<String> pidNames, ObdStudioListener listener) {
        this.context  = ctx;
        this.listener = listener;
        this.readings = new ObdReadings(context, pidNames);
    }

    public ObdStudio(MainActivity ctx, ObdStudioListener listener) {
        this(ctx, new ArrayList<>(), listener);
        loadAndSetActivePids();
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Public lifecycle API
    // ────────────────────────────────────────────────────────────────────────────────

    /** Kick‑off ELM327 initialisation and start supervising. */
    public void start() {
        initElm327();
        startSupervisor();
    }

    /** Stop watchdog and executor to avoid leaks. */
    public void stop() {
        if (supervisorTask != null) supervisorTask.cancel(true);
        scheduler.shutdownNow();
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // ELM327 initialisation ▸ send‑and‑verify
    // ────────────────────────────────────────────────────────────────────────────────

    private static final List<String> BASE_INIT_CMDS = Arrays.asList(
            "ATE0",    // Echo off
            "ATSP3",   // ISO 15765‑4, CAN 11‑bit, 500 kbps
            "ATSTFF",  // Timeout 255 × 4 ms
            "ATAT2"    // Adaptive timing auto 2
    );

    private static final List<String> RECOVERY_CMDS = Arrays.asList(
            "ATSP3"   // ISO 15765‑4, CAN 11‑bit, 500 kbps
    );

    /** Resets the init queue and sends the very first command. */
    public void initElm327() {
        phase = Phase.INITIALISING;
        initQueue = new ArrayDeque<>(BASE_INIT_CMDS);
        sendNextInitCommand();
    }

    /** Pops and transmits the next command in {@link #initQueue}. */
    private void sendNextInitCommand() {
        if (initQueue.isEmpty()) {
            onInitialisationComplete();
            return;
        }

        String cmd = initQueue.peek() + "\r"; // keep head, wait for OK before pop
        BluetoothService.send(context, new ArrayList<>(Arrays.asList(cmd)), "obd");
        initCmdSentTimestamp = System.currentTimeMillis();
        d("• Sent init cmd: " + cmd.trim(), 2);
    }

    /** Transition from INIT → RUNNING. */
    private void onInitialisationComplete() {
        phase = Phase.RUNNING;
        linkPreviouslyAlive = false; // force a «connectionActive» on next supervisor tick
        readings.requestNextReading();
        listener.onObdConnectionActive();
        d("ELM327 initialisation finished — entering RUNNING phase", 1);
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Bluetooth inbound ⇢ central dispatch
    // ────────────────────────────────────────────────────────────────────────────────

    public void onDataReceived(String raw) {
        if (raw == null || raw.trim().isEmpty()) return;

        lastResponseTimestamp = System.currentTimeMillis();
        d(raw, 2);

        // ▸ During INIT we only look for «OK» / «NO DATA»
        if (phase == Phase.INITIALISING) {
            handleInitResponse(raw);
            return;
        }

        // ▸ RUNNING phase: normal sensor flow + error detection
        if (raw.contains("NO DATA")) {
            listener.onObdConnectionError("NO DATA response while RUNNING");
            recoverAfterError();
            return;
        }

        dispatchToReadings(raw);
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // INIT‑phase helpers
    // ────────────────────────────────────────────────────────────────────────────────

    private void handleInitResponse(String raw) {
        // Ignore echoed command lines (they equal the head of the queue)
        String head = initQueue.peek();
        if (head != null && raw.trim().equalsIgnoreCase(head)) return;

        if (raw.contains("OK")) {
            initQueue.pop();
            sendNextInitCommand();
            return;
        }

        listener.onObdConnectionError(raw  + " during initialisation (unexpected)");
        recoverDuringInit();
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // RUNNING‑phase helpers
    // ────────────────────────────────────────────────────────────────────────────────

    private void dispatchToReadings(String raw) {
        for (ObdReading reading : readings.active.values()) {
            if (reading.incomingDataIsReply(raw)) {
                reading.onData(raw);
                lastReadingTimestamp = System.currentTimeMillis();
                listener.onObdReadingUpdate(reading);

                if (readings.active.size() >= 3) {
                    scheduler.schedule(() -> readings.requestNextReading(), READING_DELAY, TimeUnit.MILLISECONDS);
                } else {
                    readings.requestNextReading();
                }

                break;
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Watchdog / supervisor
    // ────────────────────────────────────────────────────────────────────────────────

    private void startSupervisor() {
        if (supervisorTask != null && !supervisorTask.isCancelled()) return;

        supervisorTask = scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();

            if (phase == Phase.INITIALISING) {
                // Retry the current command if no OK within timeout
                if (now - initCmdSentTimestamp > INIT_RESPONSE_TIMEOUT_MS) {
                    listener.onObdConnectionError("Timeout waiting for OK during init — resending");
                    sendNextInitCommand();
                }
                return;
            }

            // RUNNING supervision
            boolean alive = isObdAlive();
            if (alive && !linkPreviouslyAlive) {
                listener.onObdConnectionActive();
                linkPreviouslyAlive = true;
            } else if (!alive && linkPreviouslyAlive) {
                listener.onObdConnectionLost();
                linkPreviouslyAlive = false;
            }

            if (!alive && timeSinceLastResponse() > 2 * LINK_TIMEOUT_MS) {
                recoverAfterError();
            }
        }, 0, SUPERVISOR_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Error‑recovery helpers
    // ────────────────────────────────────────────────────────────────────────────────

    private void recoverDuringInit() {
        d("Recovering during INIT: sending recovery commands", 1);
        initQueue = new ArrayDeque<>(RECOVERY_CMDS);
        sendNextInitCommand();
    }

    private void recoverAfterError() {
        d("Recovering during RUNNING: sending recovery commands", 1);
        phase = Phase.INITIALISING;
        initQueue = new ArrayDeque<>(RECOVERY_CMDS);
        sendNextInitCommand();
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // Misc public helpers & SharedPreferences I/O
    // ────────────────────────────────────────────────────────────────────────────────

    public long timeSinceLastResponse() { return System.currentTimeMillis() - lastResponseTimestamp; }
    public long timeSinceLastReading()  { return System.currentTimeMillis() - lastReadingTimestamp; }
    public boolean isObdAlive()         { return timeSinceLastResponse() < LINK_TIMEOUT_MS; }

    public Map<String, String> getReadingsAsString() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (ObdReading r : readings.available.values()) {
            map.put(r.getDisplayName(), r.getValueAsString());
        }
        return map;
    }

    public boolean readingsForFuelAreActive() {
        Map<String, ObdReading> active = readings.active;
        for (String key : FUEL_CONS_OBD_READINGS) if (!active.containsKey(key)) return false;
        return true;
    }

    public Map<String, ObdReading> getActiveReadings() { return readings.active; }
    public ObdReading getAvailableReading(String name) { return readings.getAvailable(name); }

    public void setActivePids(ArrayList<String> pidNames) {
        readings.setAsActiveOnly(pidNames);
        listener.onActivePidsChanged();
    }

    public void saveActivePids() {
        SharedPreferences prefs = context.getSharedPreferences("ObdPrefs", MODE_PRIVATE);
        prefs.edit().putStringSet("obdPids", new HashSet<>(readings.getActiveIds())).apply();
    }

    public ArrayList<String> loadActivePids() {
        SharedPreferences prefs = context.getSharedPreferences("ObdPrefs", MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("obdPids", new HashSet<>());
        return new ArrayList<>(set);
    }

    public void loadAndSetActivePids() {
        readings = new ObdReadings(context, loadActivePids());
        listener.onActivePidsChanged();
    }
}
