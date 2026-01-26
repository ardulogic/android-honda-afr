package com.hondaafr.Libs.Helpers.Simulator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Configuration manager for the data simulator.
 * Allows easy enable/disable of simulator mode for testing without physical devices.
 */
public class SimulatorConfig {
    private static final String PREFS_NAME = "SimulatorPrefs";
    private static final String PREF_SIMULATOR_ENABLED = "simulator_enabled";
    private static final String PREF_AFR_SIMULATOR_ENABLED = "afr_simulator_enabled";
    private static final String PREF_OBD_SIMULATOR_ENABLED = "obd_simulator_enabled";
    
    private final Context context;
    
    public SimulatorConfig(Context context) {
        this.context = context;
    }
    
    /**
     * Check if simulator is enabled (either AFR or OBD)
     */
    public boolean isSimulatorEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_SIMULATOR_ENABLED, false);
    }
    
    /**
     * Enable or disable the entire simulator
     */
    public void setSimulatorEnabled(boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_SIMULATOR_ENABLED, enabled).apply();
    }
    
    /**
     * Check if AFR simulator is enabled
     */
    public boolean isAfrSimulatorEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AFR_SIMULATOR_ENABLED, false);
    }
    
    /**
     * Enable or disable AFR simulator
     */
    public void setAfrSimulatorEnabled(boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_AFR_SIMULATOR_ENABLED, enabled).apply();
    }
    
    /**
     * Check if OBD simulator is enabled
     */
    public boolean isObdSimulatorEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_OBD_SIMULATOR_ENABLED, false);
    }
    
    /**
     * Enable or disable OBD simulator
     */
    public void setObdSimulatorEnabled(boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_OBD_SIMULATOR_ENABLED, enabled).apply();
    }
    
    /**
     * Quick toggle: enable both AFR and OBD simulators
     */
    public void enableAll() {
        setSimulatorEnabled(true);
        setAfrSimulatorEnabled(true);
        setObdSimulatorEnabled(true);
    }
    
    /**
     * Quick toggle: disable all simulators
     */
    public void disableAll() {
        setSimulatorEnabled(false);
        setAfrSimulatorEnabled(false);
        setObdSimulatorEnabled(false);
    }
}

