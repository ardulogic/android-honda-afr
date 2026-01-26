package com.hondaafr.Libs.Devices.Spartan;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.hondaafr.Libs.Bluetooth.BluetoothStates;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Devices.Spartan.SpartanCommands;
import android.content.Intent;

import java.util.Random;

/**
 * Simulator for AFR (Spartan) device that generates random realistic values.
 * Useful for testing without a physical AFR sensor.
 */
public class AfrSimulator {
    private static final String PREFS_NAME = "AfrSimulatorPrefs";
    private static final String PREF_ENABLED = "enabled";
    private static final long RESPONSE_DELAY_MS = 100;
    private static final long PERIODIC_UPDATE_INTERVAL_MS = 500;
    
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    
    // Simulated values
    private double sensorAfr = 14.7 + (random.nextDouble() * 2.0 - 1.0); // 13.7-15.7
    private double targetLambda = 1.000; // Target lambda (will be converted to AFR)
    private double sensorTemp = 800.0 + random.nextDouble() * 100.0; // 800-900°C
    
    public AfrSimulator(Context context) {
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
        sendBtStateChange(BluetoothStates.STATE_BT_CONNECTED, "spartan");
        // Start periodic sensor updates
        simulatePeriodicUpdates();
    }
    
    public void simulateDisconnection() {
        sendBtStateChange(BluetoothStates.STATE_BT_DISCONNECTED, "spartan");
        handler.removeCallbacksAndMessages(null);
    }
    
    private void sendBtStateChange(int state, String deviceId) {
        Intent intent = BluetoothStates.intentForBtStateChange(BluetoothService.ACTION_UI_UPDATE, state, deviceId);
        context.sendBroadcast(intent);
    }
    
    private void sendDataReceived(String data) {
        Intent intent = BluetoothStates.intentForDataReceived(BluetoothService.ACTION_UI_UPDATE, data, "spartan");
        context.sendBroadcast(intent);
    }
    
    private void simulatePeriodicUpdates() {
        // Update sensor AFR with slight variation
        updateSimulatedValues();
        
        // Send sensor AFR update
        String sensorResponse = String.format("0:a:%.1f", sensorAfr);
        handler.postDelayed(() -> {
            sendDataReceived(sensorResponse);
        }, RESPONSE_DELAY_MS);
        
        // Send sensor temp update occasionally
        if (random.nextInt(5) == 0) {
            String tempResponse = String.format("1:a:%.0f", sensorTemp);
            handler.postDelayed(() -> {
                sendDataReceived(tempResponse);
            }, RESPONSE_DELAY_MS + 50);
        }
        
        handler.postDelayed(this::simulatePeriodicUpdates, PERIODIC_UPDATE_INTERVAL_MS);
    }
    
    public void handleCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        
        String cmd = command.trim();
        
        handler.postDelayed(() -> {
            if (cmd.equals("G\r\n") || cmd.equals("G")) {
                // Request current AFR - return sensor AFR
                String response = String.format("0:a:%.1f", sensorAfr);
                sendDataReceived(response);
            } else if (cmd.equals("GETNBSWLAMB\r\n") || cmd.equals("GETNBSWLAMB")) {
                // Get target lambda - return current target
                String response = String.format("%.3f", targetLambda);
                sendDataReceived(response);
            } else if (cmd.startsWith("SETNBSWLAM")) {
                // Set target lambda - parse and store
                try {
                    String lambdaStr = cmd.replace("SETNBSWLAM", "").replace("\r\n", "").replace("\r", "").replace("\n", "");
                    targetLambda = Double.parseDouble(lambdaStr);
                    // Confirm with the same value
                    String response = String.format("%.3f", targetLambda);
                    sendDataReceived(response);
                } catch (NumberFormatException e) {
                    // Invalid command, ignore
                }
            }
        }, RESPONSE_DELAY_MS);
    }
    
    private void updateSimulatedValues() {
        // Gradually vary sensor AFR around target (with some lag)
        double targetAfr = SpartanCommands.lambdaToAfr(targetLambda);
        double variation = (random.nextDouble() * 0.4) - 0.2; // ±0.2 AFR variation
        sensorAfr = Math.max(10.0, Math.min(20.0, targetAfr + variation));
        
        // Gradually vary sensor temp
        sensorTemp = Math.max(700.0, Math.min(950.0, sensorTemp + (random.nextDouble() * 20.0 - 10.0)));
    }
}

