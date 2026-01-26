package com.hondaafr.Libs.Helpers.Simulator;

import android.content.Context;
import android.util.Log;

import com.hondaafr.Libs.Devices.Obd.ObdReadings;
import com.hondaafr.Libs.Devices.Obd.ObdStudio;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simulates AFR and OBD data for testing without physical devices.
 * Generates random values that match the expected data formats.
 */
public class DataSimulator {
    private static final String TAG = "DataSimulator";
    
    // Update intervals (matching real device behavior)
    private static final long AFR_UPDATE_INTERVAL_MS = 50L;  // 20 Hz like Spartan
    private static final long OBD_UPDATE_INTERVAL_MS = 100L; // ~10 Hz
    
    private final Context context;
    private final TripComputer tripComputer;
    private final SimulatorConfig config;
    private final Random random = new Random();
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> afrSimulatorTask;
    private ScheduledFuture<?> obdSimulatorTask;
    
    // State for generating realistic varying values
    private double currentTargetLambda = 1.0;  // Start at stoichiometric
    private double currentSensorAfr = 14.7;
    private double currentSensorTemp = 600.0;
    
    // OBD state
    private int currentRpm = 800;
    private int currentMap = 30;
    private int currentTps = 10;
    private int currentSpeed = 0;
    private int currentCoolantTemp = 90;
    private int currentIntakeTemp = 25;
    
    public DataSimulator(Context context, TripComputer tripComputer) {
        this.context = context;
        this.tripComputer = tripComputer;
        this.config = new SimulatorConfig(context);
    }
    
    /**
     * Start the simulator if enabled in config
     */
    public void start() {
        if (!config.isSimulatorEnabled()) {
            return;
        }
        
        if (config.isAfrSimulatorEnabled()) {
            startAfrSimulator();
        }
        
        if (config.isObdSimulatorEnabled()) {
            startObdSimulator();
        }
        
        Log.d(TAG, "Data simulator started");
    }
    
    /**
     * Stop the simulator
     */
    public void stop() {
        if (afrSimulatorTask != null) {
            afrSimulatorTask.cancel(true);
            afrSimulatorTask = null;
        }
        
        if (obdSimulatorTask != null) {
            obdSimulatorTask.cancel(true);
            obdSimulatorTask = null;
        }
        
        Log.d(TAG, "Data simulator stopped");
    }
    
    /**
     * Restart the simulator (useful when config changes)
     */
    public void restart() {
        stop();
        start();
    }
    
    private void startAfrSimulator() {
        if (afrSimulatorTask != null && !afrSimulatorTask.isCancelled()) {
            return;
        }
        
        // Send initial target lambda
        sendTargetLambda();
        
        afrSimulatorTask = scheduler.scheduleAtFixedRate(
                this::simulateAfrData,
                100, // Initial delay
                AFR_UPDATE_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }
    
    private void startObdSimulator() {
        if (obdSimulatorTask != null && !obdSimulatorTask.isCancelled()) {
            return;
        }
        
        // Simulate OBD initialization responses first
        simulateObdInit();
        
        obdSimulatorTask = scheduler.scheduleAtFixedRate(
                this::simulateObdData,
                500, // Initial delay to allow init
                OBD_UPDATE_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }
    
    private void simulateAfrData() {
        if (tripComputer == null || tripComputer.mSpartanStudio == null) {
            return;
        }
        
        // Occasionally send target lambda (less frequent)
        if (random.nextInt(100) < 5) { // 5% chance
            sendTargetLambda();
        }
        
        // Always send sensor readings
        sendSensorAfr();
        sendSensorTemp();
    }
    
    private void sendTargetLambda() {
        // Target lambda varies between 0.8 (rich) and 1.2 (lean)
        currentTargetLambda = 0.8 + (random.nextDouble() * 0.4);
        // Format: "1.234" (3 decimal places)
        String data = String.format("%.3f", currentTargetLambda);
        tripComputer.mSpartanStudio.onDataReceived(data);
    }
    
    private void sendSensorAfr() {
        // Sensor AFR varies around target, with some noise
        double targetAfr = currentTargetLambda * 14.7;
        double noise = (random.nextDouble() - 0.5) * 0.5; // ±0.25 AFR
        currentSensorAfr = Math.max(10.0, Math.min(20.0, targetAfr + noise));
        
        // Format: "0:a:14.7"
        String data = String.format("0:a:%.1f", currentSensorAfr);
        tripComputer.mSpartanStudio.onDataReceived(data);
    }
    
    private void sendSensorTemp() {
        // Sensor temp varies between 400-800°C (typical wideband range)
        double change = (random.nextDouble() - 0.5) * 10.0; // ±5°C per update
        currentSensorTemp = Math.max(400.0, Math.min(800.0, currentSensorTemp + change));
        
        // Format: "1:a:600.0"
        String data = String.format("1:a:%.1f", currentSensorTemp);
        tripComputer.mSpartanStudio.onDataReceived(data);
    }
    
    private void simulateObdInit() {
        if (tripComputer == null || tripComputer.mObdStudio == null) {
            return;
        }
        
        // Simulate ELM327 initialization responses
        scheduler.schedule(() -> tripComputer.mObdStudio.onDataReceived("ELM327 v1.5"), 50, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> tripComputer.mObdStudio.onDataReceived("OK"), 100, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> tripComputer.mObdStudio.onDataReceived("OK"), 150, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> tripComputer.mObdStudio.onDataReceived("OK"), 200, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> tripComputer.mObdStudio.onDataReceived("OK"), 250, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> tripComputer.mObdStudio.onDataReceived("OK"), 300, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> tripComputer.mObdStudio.onDataReceived("OK"), 350, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> tripComputer.mObdStudio.onDataReceived("OK"), 400, TimeUnit.MILLISECONDS);
    }
    
    private void simulateObdData() {
        if (tripComputer == null || tripComputer.mObdStudio == null) {
            return;
        }
        
        ObdReadings readings = tripComputer.mObdStudio.readings;
        if (readings == null || readings.active.isEmpty()) {
            return;
        }
        
        // Cycle through active readings and send simulated responses
        for (ObdReading reading : readings.active.values()) {
            String pid = reading.getPid();
            String response = generateObdResponse(pid, reading);
            if (response != null) {
                tripComputer.mObdStudio.onDataReceived(response);
                break; // Send one reading per cycle
            }
        }
    }
    
    private String generateObdResponse(String pid, ObdReading reading) {
        int dataBytes = reading.getDataByteCount();
        int rawValue = 0;
        
        // Generate realistic values based on PID
        switch (reading.getName().toLowerCase()) {
            case "rpm":
                // RPM: 800-6000, stored as raw * 4
                currentRpm = Math.max(800, Math.min(6000, currentRpm + random.nextInt(200) - 100));
                rawValue = currentRpm * 4;
                break;
                
            case "map":
                // MAP: 20-100 kPa
                currentMap = Math.max(20, Math.min(100, currentMap + random.nextInt(10) - 5));
                rawValue = currentMap;
                break;
                
            case "tps":
                // TPS: 0-100%
                currentTps = Math.max(0, Math.min(100, currentTps + random.nextInt(5) - 2));
                rawValue = (int) (currentTps * 2.55); // 0-255 range
                break;
                
            case "speed":
                // Speed: 0-200 km/h
                currentSpeed = Math.max(0, Math.min(200, currentSpeed + random.nextInt(10) - 5));
                rawValue = currentSpeed;
                break;
                
            case "coolanttemp":
                // Coolant temp: 70-110°C (raw: -40 offset)
                currentCoolantTemp = Math.max(70, Math.min(110, currentCoolantTemp + random.nextInt(2) - 1));
                rawValue = currentCoolantTemp + 40;
                break;
                
            case "intaketemp":
                // Intake temp: 10-50°C (raw: -40 offset)
                currentIntakeTemp = Math.max(10, Math.min(50, currentIntakeTemp + random.nextInt(2) - 1));
                rawValue = currentIntakeTemp + 40;
                break;
                
            case "ltfttrim":
            case "stfttrim":
                // Fuel trim: -25% to +25% (raw: 0-255, 128 = 0%)
                rawValue = 128 + random.nextInt(64) - 32; // ±12.5%
                break;
                
            case "upstreamlambdavoltage":
                // Lambda voltage: 0-5V (raw: 0-255, /200)
                rawValue = 100 + random.nextInt(100); // 0.5-1.0V range
                break;
                
            default:
                // Generic random value
                rawValue = random.nextInt(255);
                break;
        }
        
        // Format response: "41" + PID + hex data bytes (no spaces, ATS0 format)
        StringBuilder response = new StringBuilder("41");
        response.append(pid);
        
        if (dataBytes == 1) {
            response.append(String.format("%02X", rawValue & 0xFF));
        } else if (dataBytes == 2) {
            response.append(String.format("%02X", (rawValue >> 8) & 0xFF));
            response.append(String.format("%02X", rawValue & 0xFF));
        }
        
        return response.toString();
    }
}

