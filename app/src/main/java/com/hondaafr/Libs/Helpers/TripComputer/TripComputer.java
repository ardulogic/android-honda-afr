package com.hondaafr.Libs.Helpers.TripComputer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.hondaafr.Libs.Devices.Obd.ObdStudio;
import com.hondaafr.Libs.Devices.Obd.ObdStudioListener;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudioListener;
import com.hondaafr.Libs.Helpers.DataLog;
import com.hondaafr.Libs.Helpers.DataLogEntry;
import com.hondaafr.Libs.Helpers.ReadingHistory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class TripComputer implements ObdStudioListener, SpartanStudioListener {
    public final ObdStudio mObdStudio;
    public final SpartanStudio mSpartanStudio;
    public final PhoneGps gps;
    public final ReadingHistory afrHistory = new ReadingHistory();
    public final TotalStats totalStats = new TotalStats("TotalStatsPrefs");
    public final TripStats tripStats = new TripStats("TripStatsPrefs");
    public final InstantStats instStats;
    private final Context context;
    private long timeDistanceLogged = 0L;

    private final Handler supervisorHandler = new Handler(Looper.getMainLooper());
    private final Map<String, TripComputerListener> listeners = new LinkedHashMap<>();

    public boolean isRecording = false;
    private DataLog mDataLog;
    public TripComputer(Context context) {
        this.context = context;
        this.instStats = new InstantStats(context);
        this.mObdStudio = new ObdStudio(context, this);
        this.mSpartanStudio = new SpartanStudio(context, this);
        this.mDataLog = new DataLog(context);

        // Modify GPS listener
        this.gps = new PhoneGps(context, (speedKmh, deltaKm, accuracy) -> {
            if (mObdStudio.isAlive() && mSpartanStudio.isAlive()) {
                if (deltaKm > 0) {
                    tripStats.addDistance(deltaKm);
                    totalStats.addDistance(deltaKm);

                    timeDistanceLogged = System.currentTimeMillis();
                }
            }

            for (TripComputerListener l : listeners.values()) {
                l.onGpsUpdate(speedKmh, deltaKm);
                onDataUpdated();
            }
        });

        this.gps.setMinDistanceDeltaInMeters(25);
    }

    public void addListener(String key, TripComputerListener listener) {
        listeners.put(key, listener);
    }

    public void removeListener(String key) {
        listeners.remove(key);
    }

    public void onDataUpdated() {
        afrHistory.add(mSpartanStudio.lastSensorAfr);

        if (mObdStudio.readingsForFuelAreActive()) {
            Double afr = mSpartanStudio.lastSensorAfr;
            Integer iatObd = (Integer) mObdStudio.getAvailableReading("iat").getValue();
            Integer rpmObd = (Integer) mObdStudio.getAvailableReading("rpm").getValue();
            Integer mapObd = (Integer) mObdStudio.getAvailableReading("map").getValue();

            instStats.onReadingsReceived(afr, iatObd, rpmObd, mapObd, 1.590, getSpeed());

            double litersIncrement = instStats.getLitersIncrement();

            tripStats.addLiters(litersIncrement);
            totalStats.addLiters(litersIncrement);
        }

        for (TripComputerListener l : listeners.values()) {
            l.onCalculationsUpdated();
        }

        if (isRecording) {
            logReadings();
        }
    }

    private void startSupervisor() {
        supervisorHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ensureObdAndAfrAreAlive();

                for (TripComputerListener l : listeners.values()) {
                    l.onGpsPulse(gps);
                    l.onAfrPulse(mSpartanStudio.isAlive());
                    l.onObdPulse(mObdStudio.isAlive());
                }

                supervisorHandler.postDelayed(this, 1000); // Reschedule
            }
        }, 1000);
    }

    public void startRecording() {
        mDataLog.clearAllEntries();
        isRecording = true;
    }

    public void stopRecording() {
        mDataLog.saveAsCsv();
        isRecording = false;
    }

    public void stopSupervisor() {
        supervisorHandler.removeCallbacksAndMessages(null);
    }


    public boolean isGpsLogging() {
        return System.currentTimeMillis() - timeDistanceLogged < 2000;
    }

    public boolean isGpsSpeedUsed() {
        Double speedGps = gps.getSpeed();
        ObdReading speedObd = mObdStudio.getAvailableReading("speed");

        if (speedObd.getTimeSinceLastUpdate() > 3000) {
            return true;
        } else {
            if (gps.isAlive()) {

                // Has recent readings
                boolean isGpsValid = Math.abs(speedGps - (Double) speedObd.getValue()) <= 20;
                return isGpsValid && gps.getSpeed() > 30 && (Integer) speedObd.getValue() > 30;
            }
        }

        return true;
    }

    public double getSpeed() {
        if (isGpsSpeedUsed()) {
            return gps.getSpeed();
        } else {
            return (Integer) mObdStudio.getAvailableReading("speed").getValue();
        }
    }


    /**
     * This also acts as the init() function since OnResume is called
     * when app starts
     *
     * @param context
     */
    public void onResume(Context context) {
        totalStats.load(context);
        tripStats.load(context);

        mSpartanStudio.onResume(context);
        startSupervisor();
    }

    public void onPause(Context context) {
        stopSupervisor();
    }

    public void onDestroy(Context context) {
        totalStats.save(context);
        tripStats.save(context);
        mObdStudio.saveActivePids();
    }

    public void setObdForFuelConsumption(boolean enabled) {
        if (enabled) {
            mObdStudio.saveActivePids();
            mObdStudio.setActivePids(new ArrayList<>(ObdStudio.FUEL_CONS_OBD_READINGS));
        } else {
            mObdStudio.loadAndSetActivePids();
        }
    }

    public boolean afrIsRich() {
        return mSpartanStudio.lastSensorAfr < 12;
    }

    public void ensureObdAndAfrAreAlive() {
        if (!mObdStudio.isRunning()) {
            mObdStudio.start();
        }

        if (!mSpartanStudio.isRunning()) {
            mSpartanStudio.start();
        }
    }

    @SuppressLint("DefaultLocale")
    public Map<String, String> getReadingsAsString() {
        LinkedHashMap<String, String> readings = new LinkedHashMap<>();

        readings.put("Total km", String.format("%.1f", totalStats.getDistanceKm()));
        readings.put("Total l", String.format("%.1f", totalStats.getLiters()));
        readings.put("Total l / hour", String.format("%.1f", totalStats.getLitersPer100km()));

        readings.put("Trip km", String.format("%.1f", tripStats.getDistanceKm()));
        readings.put("Trip l", String.format("%.1f", tripStats.getLiters()));
        readings.put("Trip l / hour", String.format("%.1f", tripStats.getLitersPer100km()));

        readings.put("Inst l / hour", String.format("%.1f", instStats.getLph()));
        readings.put("Inst (avg) l / hour", String.format("%.1f", instStats.getLphAvg()));
        readings.put("Inst l / 100", String.format("%.1f", instStats.getLp100km()));
        readings.put("Inst (avg) l / 100", String.format("%.1f", instStats.getLp100kmAvg()));

        return readings;
    }

    private void logReadings() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.putAll(mSpartanStudio.getReadingsAsString());
        values.putAll(mObdStudio.getReadingsAsString());
        values.putAll(gps.getReadingsAsString());
        values.putAll(getReadingsAsString());

        mDataLog.addEntry(new DataLogEntry(values));
    }

    @Override
    public void onObdConnectionPulse(boolean isActive) {
        for (TripComputerListener l : listeners.values()) {
            l.onObdPulse(isActive);
        }
    }

    @Override
    public void onObdReadingUpdate(ObdReading reading) {
        for (TripComputerListener l : listeners.values()) {
            l.onObdValue(reading);
        }

        onDataUpdated();
    }

    @Override
    public void onObdActivePidsChanged() {
        for (TripComputerListener l : listeners.values()) {
            l.onObdActivePidsChanged();
        }
    }

    @Override
    public void onObdConnectionError(String s) {
        for (TripComputerListener l : listeners.values()) {
            l.onObdPulse(false);
        }
    }

    @Override
    public void onTargetAfrUpdated(double targetAfr) {
        for (TripComputerListener l : listeners.values()) {
            l.onAfrTargetValue(targetAfr);
        }
    }

    @Override
    public void onSensorAfrReceived(Double afr) {
        onDataUpdated();

        for (TripComputerListener l : listeners.values()) {
            l.onAfrValue(afr);
        }
    }

    @Override
    public void onSensorTempReceived(Double temp) {

    }

    @Override
    public void onAfrConnectionPulse(boolean isActive) {
        for (TripComputerListener l : listeners.values()) {
            l.onAfrPulse(isActive);
        }
    }
}
