package com.hondaafr.Libs.Helpers.TripComputer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.hondaafr.Libs.Devices.Obd.ObdStudio;
import com.hondaafr.Libs.Devices.Obd.ObdStudioListener;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Devices.Phone.PhoneLightSensor;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudioListener;
import com.hondaafr.Libs.Helpers.DataLog;
import com.hondaafr.Libs.Helpers.DataLogEntry;
import com.hondaafr.Libs.Helpers.Debuggable;
import com.hondaafr.Libs.Helpers.ReadingHistory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class TripComputer extends Debuggable implements ObdStudioListener, SpartanStudioListener {
    private final Context context;
    public final ObdStudio mObdStudio;
    public final SpartanStudio mSpartanStudio;
    public final ReadingHistory afrHistory = new ReadingHistory();
    public final TotalStats totalStats = new TotalStats("TotalStatsPrefs");
    public final TripStats tripStats = new TripStats("TripStatsPrefs");
    public final InstantStats instStats;
    public final PhoneGps gps;
    private final Handler supervisorHandler = new Handler(Looper.getMainLooper());
    private final Map<String, TripComputerListener> listeners = new LinkedHashMap<>();
    private DataLog mDataLog;
    private boolean supervisorRunning = false;
    public boolean isRecording = false;
    private long timeStatsSaved = 0;
    private long timeDistanceLogged = 0L;
    private TripFuelTrackStore trackStore;
    private String currentSessionId = "";
    private double lastTrackLat = Double.NaN;
    private double lastTrackLon = Double.NaN;
    private boolean segmentBreakPending = false;

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
                
                // Log to map track regardless of which fragment is active
                logToMapTrack();
            } else {
                // Mark segment break when devices disconnect
                lastTrackLat = Double.NaN;
                lastTrackLon = Double.NaN;
                segmentBreakPending = true;
            }

            for (TripComputerListener l : listeners.values()) {
                l.onGpsUpdate(speedKmh, deltaKm);
                onDataUpdated();
            }
        });

        this.gps.setMinDistanceDeltaInMeters(25);

        new PhoneLightSensor(context, intensity -> {
            boolean lowLight = intensity < 15;
            boolean afterSunset = false;
            boolean beforeSunrise = false;

            LocalTime now = LocalTime.now();

            if (gps.getSunsetTime() != null && gps.getSunriseTime() != null) {
                afterSunset = now.isAfter(gps.getSunsetTime());
                beforeSunrise = now.isBefore(gps.getSunriseTime());
            }

            boolean isNightModeUpdated = lowLight || afterSunset || beforeSunrise;
            for (TripComputerListener l : listeners.values()) {
                l.onNightModeUpdated(isNightModeUpdated);
            }
        });
    }

    public void addListener(String key, TripComputerListener listener) {
        listeners.put(key, listener);
    }

    public void removeListener(String key) {
        listeners.remove(key);
    }

    public void onDataUpdated() {
        if (mSpartanStudio.lastSensorAfr > 0) {
            afrHistory.add(mSpartanStudio.lastSensorAfr);
        }

        if (mObdStudio.readingsForFuelAreActive()) {
            Double afr = mSpartanStudio.lastSensorAfr;
            Integer iatObd = (Integer) mObdStudio.getAvailableReading("iat").getValue();
            Integer rpmObd = (Integer) mObdStudio.getAvailableReading("rpm").getValue();
            Integer mapObd = (Integer) mObdStudio.getAvailableReading("map").getValue();

            instStats.onReadingsReceived(afr, iatObd, rpmObd, mapObd, 1.590, getSpeed());

            double litersIncrement = instStats.getLitersIncrement();

            tripStats.addLiters(litersIncrement);
            totalStats.addLiters(litersIncrement);

            lazySaveStats();
        }

        for (TripComputerListener l : listeners.values()) {
            l.onCalculationsUpdated();
        }

        if (isRecording) {
            logReadings();
        }
    }

    public void notifyCalculationsUpdated() {
        for (TripComputerListener l : listeners.values()) {
            l.onCalculationsUpdated();
        }
    }

    private void saveStats() {
        tripStats.save(context);
        totalStats.save(context);

        timeStatsSaved = System.currentTimeMillis();
    }

    private void lazySaveStats() {
        if (System.currentTimeMillis() - timeStatsSaved > 5000) {
            saveStats();
        }
    }

    private void startSupervisor() {
        if (!supervisorRunning) {
            supervisorRunning = true;
            supervisorHandler.postDelayed(supervisorRunnable, 1000);
        }
    }

    public void stopSupervisor() {
        supervisorRunning = false;
        supervisorHandler.removeCallbacks(supervisorRunnable);
    }

    public boolean isSupervisorRunning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return supervisorHandler.hasCallbacks(supervisorRunnable);
        } else {
            return supervisorRunning;
        }
    }
    private final Runnable supervisorRunnable = new Runnable() {
        @Override
        public void run() {
            d("Supervisor tick", 1);
            ensureObdAndAfrAreAlive();

            for (TripComputerListener l : listeners.values()) {
                l.onGpsPulse(gps);
                l.onAfrPulse(mSpartanStudio.isAlive());
                l.onObdPulse(mObdStudio.isAlive());
            }

            supervisorHandler.postDelayed(this, 1000);
        }
    };

    public void startRecording() {
        mDataLog.clearAllEntries();
        isRecording = true;
    }

    public void stopRecording() {
        mDataLog.saveAsCsv();
        isRecording = false;
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
        
        // Initialize map track store
        initializeMapTrackStore(context);
    }
    
    private void initializeMapTrackStore(Context context) {
        String sessionId = tripStats.getSessionId(context);
        if (!sessionId.equals(currentSessionId)) {
            currentSessionId = sessionId;
            trackStore = new TripFuelTrackStore(context, sessionId);
            lastTrackLat = Double.NaN;
            lastTrackLon = Double.NaN;
            segmentBreakPending = false;
        }
    }
    
    private void logToMapTrack() {
        if (trackStore == null || gps == null) {
            return;
        }
        
        android.location.Location lastLocation = gps.getLastLocation();
        if (lastLocation == null) {
            return;
        }
        
        double lat = lastLocation.getLatitude();
        double lon = lastLocation.getLongitude();
        
        // Skip if same point (within small tolerance)
        if (!Double.isNaN(lastTrackLat) && !Double.isNaN(lastTrackLon)) {
            double latDiff = Math.abs(lat - lastTrackLat);
            double lonDiff = Math.abs(lon - lastTrackLon);
            if (latDiff < 0.000001 && lonDiff < 0.000001) {
                return;
            }
        }
        
        double lp100km = instStats.getLp100kmAvg();
        Double sanitized = sanitizeMetric(lp100km);
        if (sanitized == null) {
            return;
        }
        
        // Handle segment breaks
        if (segmentBreakPending && !Double.isNaN(lastTrackLat)) {
            trackStore.append(TripFuelTrackStore.TrackPoint.breakMarker());
            segmentBreakPending = false;
        } else if (segmentBreakPending) {
            segmentBreakPending = false;
        }
        
        // Create and append track point
        Double lph = sanitizeMetric(instStats.getLphAvg());
        TripFuelTrackStore.TrackPoint sample = new TripFuelTrackStore.TrackPoint(
            lat,
            lon,
            sanitized,
            lph == null ? Double.NaN : lph
        );
        
        trackStore.append(sample);
        lastTrackLat = lat;
        lastTrackLon = lon;
    }
    
    private Double sanitizeMetric(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        if (value < 0) {
            return 0.0;
        }
        return value;
    }
    
    public TripFuelTrackStore getTrackStore() {
        return trackStore;
    }
    
    public String getCurrentSessionId() {
        return currentSessionId;
    }

    public void onPause(Context context) {
        saveStats();
        // Flush map track store
        if (trackStore != null) {
            trackStore.flush();
        }
    }

    public void onDestroy(Context context) {
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
