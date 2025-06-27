package com.hondaafr.Libs.Helpers.TripComputer;

import android.annotation.SuppressLint;
import android.content.Context;

import com.hondaafr.Libs.Devices.Obd.ObdStudio;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneBarometer;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Devices.Phone.PhoneGpsListener;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Helpers.AverageList;
import com.hondaafr.Libs.Helpers.TotalLitersConsumed;
import com.hondaafr.Libs.Helpers.ReadingHistory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class TripComputer {
    public final ObdStudio mObdStudio;
    public final SpartanStudio mSpartanStudio;
    public final PhoneGps gps;
    public final ReadingHistory afrHistory = new ReadingHistory();
    public final TotalStats totalStats = new TotalStats("TotalStatsPrefs");
    public final TripStats tripStats = new TripStats("TripStatsPrefs");
    public final InstantStats instStats;
    private final TripComputerListener listener;
    private final Context context;
    private long timeDistanceLogged = 0L;

    public TripComputer(Context context, ObdStudio mObdStudio, SpartanStudio mSpartanStudio, TripComputerListener listener) {
        this.context = context;
        this.instStats = new InstantStats(context);
        this.mObdStudio = mObdStudio;
        this.mSpartanStudio = mSpartanStudio;
        this.listener = listener;

        // Modify GPS listener
        this.gps = new PhoneGps(context, new PhoneGpsListener() {
            @Override
            public void onGpsSpeedUpdated(double speedKmh) {
                listener.onGpsUpdated(speedKmh, tripStats.getDistanceKm());
            }

            @Override
            public void onGpsDistanceIncrement(double deltaKm) {
                if (mObdStudio.isAlive() && mSpartanStudio.isAlive()) {
                    tripStats.addDistance(deltaKm);
                    totalStats.addDistance(deltaKm);

                    timeDistanceLogged = System.currentTimeMillis();
                }
            }
        });

        this.gps.setMinDistanceDeltaInMeters(25);
    }

    public void init() {
        totalStats.load(context);
        tripStats.load(context);

        listener.onTripComputerReadingsUpdated();
    }

    public void tick() {
        afrHistory.add(mSpartanStudio.lastSensorAfr);

        calculateStats(mSpartanStudio.lastSensorAfr);

        listener.onTripComputerReadingsUpdated();
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

    private void calculateStats(Double afr) {
        if (mObdStudio.readingsForFuelAreActive()) {
            Integer iatObd = (Integer) mObdStudio.getAvailableReading("iat").getValue();
            Integer rpmObd = (Integer) mObdStudio.getAvailableReading("rpm").getValue();
            Integer mapObd = (Integer) mObdStudio.getAvailableReading("map").getValue();

            instStats.onReadingsReceived(afr, iatObd, rpmObd, mapObd, 1.590, getSpeed());

            double litersIncrement = instStats.getLitersIncrement();

            tripStats.addLiters(litersIncrement);
            totalStats.addLiters(litersIncrement);
        }
    }

    public void onPause(Context context) {
        totalStats.save(context);
        tripStats.save(context);
    }

    public void onDestroy(Context context) {
        totalStats.save(context);
        tripStats.save(context);
    }

    public void onResume(Context context) {
        totalStats.load(context);
        tripStats.load(context);
    }

    public void resetTotals() {
        totalStats.reset();
        listener.onTripComputerReadingsUpdated();
    }

    public void resetTrip() {
        tripStats.reset();
        listener.onTripComputerReadingsUpdated();
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
}
