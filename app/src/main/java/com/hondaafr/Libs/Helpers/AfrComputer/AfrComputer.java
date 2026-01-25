package com.hondaafr.Libs.Helpers.AfrComputer;

import android.content.Context;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.AdaptiveAfr.AdaptiveAfrState;

import java.util.LinkedHashMap;
import java.util.Map;

public class AfrComputer implements TripComputerListener {
    private final Context context;
    private final TripComputer tripComputer;
    private final AdaptiveAfrState state;
    private final Map<String, AfrComputerListener> listeners = new LinkedHashMap<>();

    public AfrComputer(Context context, TripComputer tripComputer) {
        this.context = context;
        this.tripComputer = tripComputer;
        this.state = new AdaptiveAfrState();
        this.state.setStartTimestamp(System.currentTimeMillis());
        
        // Load preferences
        loadPrefs();
        
        // Register as listener
        tripComputer.addListener("adaptive_afr_computer", this);
    }

    public void addListener(String key, AfrComputerListener listener) {
        listeners.put(key, listener);
    }

    public void removeListener(String key) {
        listeners.remove(key);
    }

    private void notifyDataUpdated() {
        for (AfrComputerListener listener : listeners.values()) {
            listener.onAdaptiveAfrDataUpdated(state);
        }
    }

    private void notifyTableDataChanged() {
        for (AfrComputerListener listener : listeners.values()) {
            listener.onTableDataChanged(state);
        }
    }

    private void notifyIsActivatedChanged() {
        for (AfrComputerListener listener : listeners.values()) {
            listener.onIsActivatedChanged(state);
        }
    }

    public AdaptiveAfrState getState() {
        return state;
    }

    public void setAdaptiveEnabled(boolean enabled) {
        state.setAdaptiveEnabled(enabled);
        saveEnabled();
        notifyIsActivatedChanged();
    }

    public void setActivePreset(String preset) {
        state.setActivePreset(preset);
        loadTableForPreset(preset);
        savePresetSelection();
        notifyTableDataChanged(); // Notify listeners that table data changed
    }

    public void saveCell(int r, int m, double value) {
        if (state.getTargetTable() != null) {
            state.getTargetTable()[r][m] = value;
            android.content.SharedPreferences prefs =
                    context.getSharedPreferences(AdaptiveAfrState.PREFS_NAME, Context.MODE_PRIVATE);
            String key = AdaptiveAfrState.PREF_CELL_PREFIX + state.getActivePreset() + "_" + r + "_" + m;
            prefs.edit().putFloat(key, (float) value).apply();
            notifyTableDataChanged(); // Notify listeners that table data changed
        }
    }

    private void saveEnabled() {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(AdaptiveAfrState.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(AdaptiveAfrState.PREF_ENABLED, state.isAdaptiveEnabled()).apply();
    }

    private void savePresetSelection() {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(AdaptiveAfrState.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(AdaptiveAfrState.PREF_PRESET, state.getActivePreset()).apply();
    }

    private void loadPrefs() {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(AdaptiveAfrState.PREFS_NAME, Context.MODE_PRIVATE);
        state.setAdaptiveEnabled(prefs.getBoolean(AdaptiveAfrState.PREF_ENABLED, true));
        state.setActivePreset(prefs.getString(AdaptiveAfrState.PREF_PRESET, "eco"));
        loadTableForPreset(state.getActivePreset());
    }

    private void loadTableForPreset(String preset) {
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(AdaptiveAfrState.PREFS_NAME, Context.MODE_PRIVATE);
        double[][] targetTable = new double[AdaptiveAfrState.RPM_BINS.length][AdaptiveAfrState.MAP_BINS.length];
        for (int r = 0; r < AdaptiveAfrState.RPM_BINS.length; r++) {
            for (int m = 0; m < AdaptiveAfrState.MAP_BINS.length; m++) {
                String key = AdaptiveAfrState.PREF_CELL_PREFIX + preset + "_" + r + "_" + m;
                float stored = prefs.getFloat(key, Float.NaN);
                if (Float.isNaN(stored)) {
                    targetTable[r][m] = defaultPresetValue(preset, AdaptiveAfrState.MAP_BINS[m]);
                } else {
                    targetTable[r][m] = stored;
                }
            }
        }
        state.setTargetTable(targetTable);
    }

    private double defaultPresetValue(String preset, double map) {
        if ("sport".equals(preset)) {
            return sportPreset(map);
        }
        if ("eco_plus".equals(preset)) {
            return ecoPlusPreset(map);
        }
        if ("eco".equals(preset)) {
            return ecoPreset(map);
        }
        return computeTargetAfr(0, map);
    }

    private double computeTargetAfr(double rpm, double map) {
        double rpmFactor = rpm / AdaptiveAfrState.RPM_BINS[AdaptiveAfrState.RPM_BINS.length - 1];
        double mapFactor = map / AdaptiveAfrState.MAP_BINS[AdaptiveAfrState.MAP_BINS.length - 1];
        double target = 14.7 - (mapFactor * 2.0) - (rpmFactor * 1.0);
        return Math.max(10.5, Math.min(15.5, target));
    }

    private double ecoPlusPreset(double map) {
        if (map <= 30.0) {
            return lerp(15.0, 15.5, map / 30.0);
        }
        if (map <= 50.0) {
            return lerp(15.5, 16.7, (map - 30.0) / 20.0);
        }
        return lerp(16.7, 14.7, Math.min(1.0, (map - 50.0) / 50.0));
    }

    private double ecoPreset(double map) {
        if (map <= 30.0) {
            return lerp(15.0, 15.0, map / 30.0);
        }
        if (map <= 50.0) {
            return lerp(15.0, 15.7, (map - 30.0) / 20.0);
        }
        return lerp(15.7, 14.7, Math.min(1.0, (map - 50.0) / 50.0));
    }

    private double sportPreset(double map) {
        if (map <= 30.0) {
            return lerp(13.6, 13.2, map / 30.0);
        }
        if (map <= 50.0) {
            return lerp(13.2, 12.8, (map - 30.0) / 20.0);
        }
        return lerp(12.8, 12.3, Math.min(1.0, (map - 50.0) / 50.0));
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private double lookupTargetAfr(double rpm, double map) {
        if (state.getTargetTable() == null) {
            return Double.NaN;
        }
        
        int rIndex = nearestIndex(AdaptiveAfrState.RPM_BINS, rpm);
        int mIndex = nearestIndex(AdaptiveAfrState.MAP_BINS, map);
        return state.getTargetTable()[rIndex][mIndex];
    }

    private int nearestIndex(double[] bins, double value) {
        int bestIndex = 0;
        double bestDelta = Double.MAX_VALUE;
        for (int i = 0; i < bins.length; i++) {
            double delta = Math.abs(bins[i] - value);
            if (delta < bestDelta) {
                bestDelta = delta;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private void updateAfronDevice() {
        // Set AFR on device if adaptive is enabled
        if (state.isAdaptiveEnabled() && state.getTargetTable() != null) {
            Integer rpmValue = state.getLastRpm();
            Integer mapValue = state.getLastMap();
            double targetAfr = (rpmValue != null && mapValue != null) ? lookupTargetAfr(rpmValue, mapValue) : Double.NaN;
            // Always update lastTargetAfr for display purposes
            state.setLastTargetAfr(targetAfr);
            
            if (!Double.isNaN(targetAfr)) {
                if (Double.isNaN(state.getLastSetTargetAfr()) ||
                    Math.abs(targetAfr - state.getLastSetTargetAfr()) >= AdaptiveAfrState.AFR_CHANGE_THRESHOLD) {
                    if (tripComputer != null && tripComputer.mSpartanStudio != null) {
                        tripComputer.mSpartanStudio.setAFR(targetAfr);
                        state.setLastSetTargetAfr(targetAfr);
                    }
                }
            }
        } else {
            // Clear target AFR if adaptive is disabled
            state.setLastTargetAfr(Double.NaN);
        }
    }

    @Override
    public void onGpsUpdate(Double speed, double distanceIncrement) {
        // Not needed for adaptive AFR
    }

    @Override
    public void onGpsPulse(com.hondaafr.Libs.Devices.Phone.PhoneGps gps) {
        // Not needed for adaptive AFR
    }

    @Override
    public void onAfrPulse(boolean isActive) {
        // Not needed for adaptive AFR
    }

    @Override
    public void onAfrTargetValue(double targetAfr) {
        // Not needed for adaptive AFR
    }

    @Override
    public void onAfrValue(Double afr) {
        if (afr != null) {
            state.setLastAfr(afr);
        }
        updateAfronDevice();
        notifyDataUpdated();
    }

    @Override
    public void onObdPulse(boolean isActive) {
        // Not needed for adaptive AFR
    }

    @Override
    public void onObdActivePidsChanged() {
        // Not needed for adaptive AFR
    }

    @Override
    public void onObdValue(ObdReading reading) {
        boolean dataUpdated = false;
        String name = reading.getMachineName();
        if ("rpm".equals(name)) {
            state.setLastRpm((Integer)  reading.getValue());
            dataUpdated = true;
        } else if ("map".equals(name)) {
            Object val = reading.getValue();
            state.setLastMap((Integer) reading.getValue());
            dataUpdated = true;
        }
        
        if (dataUpdated) {
            updateAfronDevice();
            notifyDataUpdated();
        }
    }

    @Override
    public void onCalculationsUpdated() {
        // Not needed for adaptive AFR
    }

    @Override
    public void onNightModeUpdated(boolean isNight) {
        // Not needed for adaptive AFR
    }

    public void onResume(Context context) {
        loadPrefs();
        // Notify listeners that state was reloaded
        notifyIsActivatedChanged();
        notifyTableDataChanged();
    }

    public void onPause(Context context) {
        // Save preferences if needed
    }

    public void onDestroy(Context context) {
        tripComputer.removeListener("adaptive_afr_computer");
    }
}

