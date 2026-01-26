package com.hondaafr.Libs.Devices.Obd;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.hondaafr.Libs.Bluetooth.BluetoothStates;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import android.content.Intent;

import java.util.Random;

/**
 * Simulator for OBD device that generates random realistic values.
 * Useful for testing without a physical OBD adapter.
 */
public class ObdSimulator {
    private static final String PREFS_NAME = "ObdSimulatorPrefs";
    private static final String PREF_ENABLED = "enabled";
    private static final long INIT_RESPONSE_DELAY_MS = 100;
    private static final long READING_RESPONSE_DELAY_MS = 150;
    
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private boolean isInitializing = false;
    private int initCommandIndex = 0;
    private String lastRequestedPid = null;
    
    // Realistic value ranges
    private int rpm = 800 + random.nextInt(200); // 800-1000 idle
    private int map = 30 + random.nextInt(20); // 30-50 kPa idle
    private int speed = random.nextInt(10); // 0-10 km/h
    private int coolantTemp = 85 + random.nextInt(10); // 85-95°C
    private int intakeTemp = 20 + random.nextInt(20); // 20-40°C
    private int tps = random.nextInt(20); // 0-20% throttle
    
    private static final String[] INIT_COMMANDS = {
        "ATZ", "ATD", "ATD0", "ATE0", "ATM0", "ATL0", "ATS0", "ATH0", "ATAT1", "ATST96", "ATAL", "ATSP0"
    };
    
    public ObdSimulator(Context context) {
        this.context = context;
    }
    
    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_ENABLED, false);
    }
    
    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_ENABLED, enabled).apply();
    }
    
    public void simulateConnection() {
        // Simulate connection state
        sendBtStateChange(BluetoothStates.STATE_BT_CONNECTED, "obd");
        isInitializing = true;
        initCommandIndex = 0;
        simulateInitSequence();
    }
    
    public void simulateDisconnection() {
        sendBtStateChange(BluetoothStates.STATE_BT_DISCONNECTED, "obd");
        isInitializing = false;
        handler.removeCallbacksAndMessages(null);
    }
    
    private void sendBtStateChange(int state, String deviceId) {
        Intent intent = BluetoothStates.intentForBtStateChange(BluetoothService.ACTION_UI_UPDATE, state, deviceId);
        context.sendBroadcast(intent);
    }
    
    private void simulateInitSequence() {
        if (!isInitializing || initCommandIndex >= INIT_COMMANDS.length) {
            isInitializing = false;
            // Start periodic readings
            simulatePeriodicReadings();
            return;
        }
        
        String cmd = INIT_COMMANDS[initCommandIndex];
        if ("ATZ".equals(cmd)) {
            // ATZ returns ELM327 version
            handler.postDelayed(() -> {
                simulateResponse("ELM327 v1.5");
            }, INIT_RESPONSE_DELAY_MS);
        } else {
            // Other commands return OK
            handler.postDelayed(() -> {
                simulateResponse("OK");
            }, INIT_RESPONSE_DELAY_MS);
        }
        
        initCommandIndex++;
        handler.postDelayed(this::simulateInitSequence, INIT_RESPONSE_DELAY_MS + 50);
    }
    
    private void simulatePeriodicReadings() {
        // Simulate periodic PID requests by generating responses
        handler.postDelayed(() -> {
            if (lastRequestedPid != null) {
                simulatePidResponse(lastRequestedPid);
                lastRequestedPid = null;
            }
            simulatePeriodicReadings();
        }, 200 + random.nextInt(100)); // Random interval 200-300ms
    }
    
    public void handleCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        
        String cmd = command.trim().toUpperCase();
        
        // Handle init commands
        if (isInitializing) {
            // Already handled in init sequence
            return;
        }
        
        // Handle PID requests (format: "01 XX\r")
        if (cmd.startsWith("01 ")) {
            String pid = cmd.substring(3).trim();
            if (pid.length() >= 2) {
                final String pidFinal = pid.substring(0, 2);
                lastRequestedPid = pidFinal;
                handler.postDelayed(() -> simulatePidResponse(pidFinal), READING_RESPONSE_DELAY_MS);
            }
        } else if (cmd.contains("AT")) {
            // Other AT commands
            handler.postDelayed(() -> simulateResponse("OK"), INIT_RESPONSE_DELAY_MS);
        }
    }
    
    private void simulatePidResponse(String pid) {
        // Update values with slight random variation
        updateSimulatedValues();
        
        String response;
        switch (pid) {
            case "0C": // RPM
                int rpmHex = rpm * 4; // Convert back to hex value
                response = String.format("410C%04X", rpmHex);
                break;
            case "0B": // MAP
                response = String.format("410B%02X", map);
                break;
            case "0D": // Speed
                response = String.format("410D%02X", speed);
                break;
            case "05": // Coolant temp
                response = String.format("4105%02X", coolantTemp + 40); // Offset by 40
                break;
            case "0F": // Intake temp
                response = String.format("410F%02X", intakeTemp + 40); // Offset by 40
                break;
            case "11": // TPS
                response = String.format("4111%02X", (tps * 255) / 100);
                break;
            default:
                response = "4100" + pid + "00"; // Generic response
        }
        
        simulateResponse(response);
    }
    
    private void updateSimulatedValues() {
        // Gradually vary values to simulate realistic behavior
        rpm = Math.max(600, Math.min(7000, rpm + random.nextInt(200) - 100));
        map = Math.max(20, Math.min(100, map + random.nextInt(10) - 5));
        speed = Math.max(0, Math.min(200, speed + random.nextInt(5) - 2));
        coolantTemp = Math.max(70, Math.min(110, coolantTemp + random.nextInt(3) - 1));
        intakeTemp = Math.max(15, Math.min(60, intakeTemp + random.nextInt(5) - 2));
        tps = Math.max(0, Math.min(100, tps + random.nextInt(10) - 5));
    }
    
    private void simulateResponse(String response) {
        Intent intent = BluetoothStates.intentForDataReceived(BluetoothService.ACTION_UI_UPDATE, response, "obd");
        context.sendBroadcast(intent);
    }
}

