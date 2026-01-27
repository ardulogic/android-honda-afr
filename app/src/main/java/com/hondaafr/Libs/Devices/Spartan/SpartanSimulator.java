package com.hondaafr.Libs.Devices.Spartan;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.hondaafr.Libs.Bluetooth.BluetoothStates;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Devices.Spartan.SpartanCommands;
import android.content.Intent;
import android.util.Log;

/**
 * Simulator for AFR (Spartan) device that generates sine wave values.
 * Useful for testing without a physical AFR sensor.
 */
public class SpartanSimulator {
    private static final String PREFS_NAME = "SpartanSimulatorPrefs";
    private static final String PREF_ENABLED = "enabled";
    private static final long RESPONSE_DELAY_MS = 20;
    private static final long MIN_COMMAND_INTERVAL_MS = 20;
    private static final String TAG = "SpartanSimulator";
    
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // Track last command time to detect spam
    private long lastCommandTime = 0;
    // Track if a command is currently being handled
    private boolean isHandlingCommand = false;
    
    // Sine wave parameters for AFR (12 to 15 range)
    private double sineAngle = 0.0; // Current angle for sine calculation
    private static final double SINE_INCREMENT = 0.3; // Increment per update
    private static final double AFR_CENTER = 13.5; // Center of 12-15 range
    private static final double AFR_AMPLITUDE = 1.5; // Half the range (15-12)/2
    
    // Simulated values
    private double sensorAfr = 13.5; // Initial value at center
    private double targetLambda = 1.000; // Target lambda (will be converted to AFR)
    private double sensorTemp = 850.0; // Fixed temperature
    
    public SpartanSimulator(Context context) {
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
    }
    
    public void simulateDisconnection() {
        sendBtStateChange(BluetoothStates.STATE_BT_DISCONNECTED, "spartan");
        handler.removeCallbacksAndMessages(null);
        lastCommandTime = 0; // Reset command tracking on disconnect
        isHandlingCommand = false; // Reset handling flag on disconnect
    }
    
    private void sendBtStateChange(int state, String deviceId) {
        Intent intent = BluetoothStates.intentForBtStateChange(BluetoothService.ACTION_UI_UPDATE, state, deviceId);
        context.sendBroadcast(intent);
    }
    
    private void sendDataReceived(String data) {
        Intent intent = BluetoothStates.intentForDataReceived(BluetoothService.ACTION_UI_UPDATE, data, "spartan");
        context.sendBroadcast(intent);
    }
    
    public void handleCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            Log.w(TAG, "Empty command received!");

            return;
        }
        
        // Check if command is being called too frequently
        long currentTime = System.currentTimeMillis();
        if (lastCommandTime > 0) {
            long timeSinceLastCommand = currentTime - lastCommandTime;
            if (timeSinceLastCommand < MIN_COMMAND_INTERVAL_MS) {
                Log.w(TAG, "handleCommand called too frequently! Only " + timeSinceLastCommand + "ms since last call (minimum: " + MIN_COMMAND_INTERVAL_MS + "ms). Command: " + command.trim());
                return; // Ignore commands that come too quickly
            }
        }
        
        lastCommandTime = currentTime;
        String cmd = command.trim();

        Log.d(TAG, "Handling command: " + cmd);

        handler.postDelayed(() -> {
            // Check if already handling a command (detect concurrent execution - shouldn't happen on single thread)
            if (isHandlingCommand) {
                Log.e(TAG, "Command handler executed while another command is being processed! Command: " + cmd);
            }
            isHandlingCommand = true;
            
            try {
                updateSimulatedValues();

            if (cmd.equals("G\r\n") || cmd.equals("G")) {
                // Request current AFR - return current value (updated by periodic task)
                String response = String.format("0:a:%.1f", sensorAfr);
                sendDataReceived(response);
            } else if (cmd.equals("GETNBSWLAMB\r\n") || cmd.equals("GETNBSWLAMB")) {
                // Get target lambda - return current target
                String response = String.format("%.3f", targetLambda);
                sendDataReceived(response);
            } else if (cmd.startsWith("SETNBSWLAM")) {
                // Set target lambda - parse and store
                // Command format: SETNBSWLAMx.xxx (exactly 5 characters after SETNBSWLAM)
                try {
                    // Extract the lambda value (everything after "SETNBSWLAM")
                    String lambdaStr = cmd.substring("SETNBSWLAM".length());
                    // Remove any line endings
                    lambdaStr = lambdaStr.replace("\r\n", "").replace("\r", "").replace("\n", "");
                    targetLambda = Double.parseDouble(lambdaStr);
                    // Respond with exactly 5 characters: x.xxx format
                    String response = String.format("%.3f", targetLambda);
                    sendDataReceived(response);
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                    // Invalid command format, ignore
                    Log.w(TAG, "Invalid SETNBSWLAM command format: " + cmd);
                }
            }
            } finally {
                // Mark that we're done handling the command
                isHandlingCommand = false;
            }
        }, RESPONSE_DELAY_MS);
    }
    
    private void updateSimulatedValues() {
        // Increment sine angle for next calculation
        sineAngle += SINE_INCREMENT;
        
        // Calculate AFR using sine wave: ranges from 12 to 15
        // sin(x) ranges from -1 to 1, so: center + amplitude * sin(x) = 13.5 + 1.5 * sin(x)
        // This gives: 13.5 - 1.5 = 12 (min) and 13.5 + 1.5 = 15 (max)
        sensorAfr = AFR_CENTER + AFR_AMPLITUDE * Math.sin(sineAngle);

        Log.d(TAG, "SineAfr: " + sensorAfr);
        // Keep sensor temp fixed (or you can also use sine if needed)
        // sensorTemp remains at 850.0
    }
}
