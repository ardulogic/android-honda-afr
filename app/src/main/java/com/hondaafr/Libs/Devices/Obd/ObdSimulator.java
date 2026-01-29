package com.hondaafr.Libs.Devices.Obd;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hondaafr.Libs.Bluetooth.BluetoothStates;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Clean, deterministic OBD simulator.
 * - Single-threaded (Handler)
 * - No locks
 * - Realistic sine-based engine dynamics
 * - Reactive (responds only to commands)
 */
public final class ObdSimulator {

    private static final String TAG = "ObdSimulator";

    private static final String PREFS_NAME = "ObdSimulatorPrefs";
    private static final String PREF_ENABLED = "enabled";

    private static final long INIT_DELAY_MS = 120;
    private static final long PID_DELAY_MS = 150;
    private static final long MIN_CMD_INTERVAL_MS = 50;

    private static final String DEVICE_ID = "obd";

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Deque<String> queue = new ArrayDeque<>();

    private boolean processing;
    private long lastCmdTime;
    private long engineStartMs;

    private static final String[] INIT_CMDS = {
            "ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0"
    };

    private int initIndex;
    private boolean initializing;

    public ObdSimulator(Context context) {
        this.context = context.getApplicationContext();
    }

    /* ===================== ENABLE ===================== */

    public static boolean isEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_ENABLED, false);
    }

    public static void setEnabled(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(PREF_ENABLED, enabled).apply();
    }

    /* ===================== CONNECTION ===================== */

    public void simulateConnection() {
        sendState(BluetoothStates.STATE_BT_CONNECTED);
        initializing = true;
        initIndex = 0;
        engineStartMs = System.currentTimeMillis();
        runInit();
    }

    public void simulateDisconnection() {
        sendState(BluetoothStates.STATE_BT_DISCONNECTED);
        handler.removeCallbacksAndMessages(null);
        queue.clear();
        processing = false;
        initializing = false;
    }

    private void sendState(int state) {
        Intent i = BluetoothStates.intentForBtStateChange(
                BluetoothService.ACTION_UI_UPDATE, state, DEVICE_ID);
        context.sendBroadcast(i);
    }

    /* ===================== INIT ===================== */

    private void runInit() {
        if (!initializing || initIndex >= INIT_CMDS.length) {
            initializing = false;
            return;
        }

        String cmd = INIT_CMDS[initIndex++];
        handler.postDelayed(() -> {
            simulateResponse(cmd.equals("ATZ") ? "ELM327 v1.5" : "OK");
            runInit();
        }, INIT_DELAY_MS);
    }

    /* ===================== COMMAND INPUT ===================== */

    public void handleCommand(String raw) {
        if (initializing || raw == null) return;

        String cmd = raw.trim().toUpperCase();
        if (cmd.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (now - lastCmdTime < MIN_CMD_INTERVAL_MS) return;
        lastCmdTime = now;

        queue.offer(cmd);
        if (!processing) processNext();
    }

    /* ===================== COMMAND PROCESSING ===================== */

    private void processNext() {
        String cmd = queue.poll();
        if (cmd == null) {
            processing = false;
            return;
        }

        processing = true;
        long start = System.currentTimeMillis();

        Runnable action;

        if (cmd.startsWith("01 ") && cmd.length() >= 5) {
            String pid = cmd.substring(3, 5);
            action = () -> simulatePid(pid);
        } else if (cmd.startsWith("AT")) {
            action = () -> simulateResponse(cmd.equals("ATZ") ? "ELM327 v1.5" : "OK");
        } else {
            action = () -> simulateResponse("NO DATA");
        }

        long delay = Math.max(0, PID_DELAY_MS - (System.currentTimeMillis() - start));
        handler.postDelayed(() -> {
            action.run();
            processing = false;
            processNext();
        }, delay);
    }

    /* ===================== ENGINE MODEL ===================== */

    private double phase() {
        return (System.currentTimeMillis() - engineStartMs) / 1000.0;
    }

    private int rpm() {
        return (int) (800 + 5200 * (0.5 + 0.5 * Math.sin(phase())));
    }

    private int map() {
        return (int) (30 + 70 * (0.5 + 0.5 * Math.sin(phase() + 0.8)));
    }

    private int speed() {
        return rpm() / 60;
    }

    /**
     * Fuel trims are represented in the app as
     * \((value - 128) * 100 / 128\) percent.
     * Here we generate a smooth, deterministic variation
     * around 0% using a sine wave.
     */
    private int fuelTrimRaw(double phaseOffset) {
        // ±12.5% swing around 0%
        double percent = 12.5 * Math.sin(phase() + phaseOffset);
        int raw = (int) Math.round(128 + percent * 128.0 / 100.0);
        return clampByte(raw);
    }

    private int stft() {
        // Short‑term reacts a bit faster
        return fuelTrimRaw(0.4);
    }

    private int ltft() {
        // Long‑term is slower / phase‑shifted
        return fuelTrimRaw(1.2);
    }

    /**
     * Upstream lambda voltage 0‑5 V encoded as:
     * A = voltage * 200, B = auxiliary byte (e.g. trim).
     * ObdUpstreamLambdaVoltage parses A / 200.0.
     */
    private int upstreamLambdaA() {
        // Oscillate between ~0.6‑0.9 V
        double volts = 0.75 + 0.15 * Math.sin(phase() + 0.7);
        int raw = (int) Math.round(volts * 200.0);
        return clampByte(raw);
    }

    private int upstreamLambdaB() {
        // Reuse a trim‑like value for the second byte
        return fuelTrimRaw(2.0);
    }

    private int clampByte(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    private int coolant() {
        // Slowly cycle ~80‑100°C around 90°C
        double slowPhase = phase() / 8.0; // much slower than RPM
        return (int) Math.round(90 + 10 * Math.sin(slowPhase));
    }

    private int intake() {
        // Slowly cycle ~20‑40°C around 30°C
        double slowPhase = phase() / 6.0;
        return (int) Math.round(30 + 10 * Math.sin(slowPhase + 0.5));
    }

    private int tps() {
        return (int) (10 + 80 * (0.5 + 0.5 * Math.sin(phase())));
    }

    /* ===================== PID RESPONSES ===================== */

    private void simulatePid(String pid) {
        String r;
        switch (pid) {
            case "0C": // RPM
                r = String.format("410C%04X", rpm() * 4);
                break;
            case "0B": // MAP
                r = String.format("410B%02X", map());
                break;
            case "06": // STFT (Bank 1)
                r = String.format("4106%02X", stft());
                break;
            case "07": // LTFT (Bank 1)
                r = String.format("4107%02X", ltft());
                break;
            case "0D": // SPEED
                r = String.format("410D%02X", speed());
                break;
            case "05": // COOLANT
                r = String.format("4105%02X", coolant() + 40);
                break;
            case "0F": // IAT
                r = String.format("410F%02X", intake() + 40);
                break;
            case "11": // TPS
                r = String.format("4111%02X", tps() * 255 / 100);
                break;
            case "14": // Upstream lambda voltage
                int a = upstreamLambdaA();
                int b = upstreamLambdaB();
                int raw = ((a & 0xFF) << 8) | (b & 0xFF);
                r = String.format("4114%04X", raw);
                break;
            default:
                r = "NO DATA";
        }
        simulateResponse(r);
    }

    /* ===================== OUTPUT ===================== */

    private void simulateResponse(String response) {
        Log.d(TAG, "TX: " + response);
        Intent i = BluetoothStates.intentForDataReceived(
                BluetoothService.ACTION_UI_UPDATE, response, DEVICE_ID);
        context.sendBroadcast(i);
    }
}
