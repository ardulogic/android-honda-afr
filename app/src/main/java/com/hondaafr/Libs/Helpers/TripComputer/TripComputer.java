package com.hondaafr.Libs.Helpers.TripComputer;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.hondaafr.Libs.Devices.Obd.ObdStudio;
import com.hondaafr.Libs.Devices.Phone.PhoneBarometer;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Devices.Phone.PhoneGpsListener;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Helpers.FuelConsumption;
import com.hondaafr.Libs.Helpers.FuelTotalHistory;
import com.hondaafr.Libs.Helpers.ReadingHistory;

import java.util.ArrayList;

public class TripComputer {
    private final ObdStudio mObdStudio;
    private final SpartanStudio mSpartanStudio;
    public final PhoneGps gps;
    public final ReadingHistory afrHistory = new ReadingHistory();

    private final FuelTotalHistory fuelTripHistory = new FuelTotalHistory();
    private final TripComputerListener listener;
    private final PhoneBarometer barometer;

    private static final String PREFS_NAME = "TripComputerPrefs";
    private static final String KEY_TOTAL_LITRES = "totalLitres";
    private static final String KEY_TOTAL_DISTANCE = "totalDistance";
    private final Context context;

    private float totalConsumedLiters = 0;
    private float totalDistanceKm = 0;


    public TripComputer(Context context, ObdStudio mObdStudio, SpartanStudio mSpartanStudio, TripComputerListener listener) {
        this.context = context;
        this.mObdStudio = mObdStudio;
        this.mSpartanStudio = mSpartanStudio;
        this.listener = listener;

        this.barometer = new PhoneBarometer(context);
        this.gps = new PhoneGps(context, new PhoneGpsListener() {
            @Override
            public void onGpsSpeedUpdated(Double speedKmh) {
                listener.onGpsUpdated(gps.getSpeed(), gps.getTotalDistanceKm());
            }
        });
    }

    public void init() {
        loadTripData(context);

        listener.onTripComputerReadingsUpdated();
    }

    public void tick() {

        gps.setDistanceLoggingEnabled(true);

        afrHistory.add(mSpartanStudio.lastSensorAfr);

        calculateFuelConsumption(mSpartanStudio.lastSensorAfr);

        listener.onTripComputerReadingsUpdated();
    }

    public boolean isGpsSpeedUsed() {
        Double speedGps = gps.getSpeed();
        Integer speedObd = (Integer) mObdStudio.getAvailableReading("speed").getValue();
        Long timeSinceReading = mObdStudio.getAvailableReading("speed").getTimeSinceLastUpdate();

        if (timeSinceReading < 2000) {
            if (speedObd == 0) {
                return speedGps > 25;
            } else if (speedObd < 50) {
                return false;
            } else {
                return speedGps > 40;
            }
        } else {
            return true;
        }
    }

    public double getSpeed() {
        Integer speedObd = (Integer) mObdStudio.getAvailableReading("speed").getValue();

        if (isGpsSpeedUsed()) {
            return gps.getSpeed();
        } else {
            return speedObd;
        }
    }

    private void calculateFuelConsumption(Double afr) {
        if (mObdStudio.readingsForFuelAreActive()) {
            Integer iatObd = (Integer) mObdStudio.getAvailableReading("iat").getValue();
            Integer rpmObd = (Integer) mObdStudio.getAvailableReading("rpm").getValue();
            Integer mapObd = (Integer) mObdStudio.getAvailableReading("map").getValue();

            double iat = iatObd > 0 ? iatObd : 35;

            double fuelConsPerHour = FuelConsumption.calculateFuelConsumptionLperHour(
                    afr,
                    rpmObd,
                    mapObd,
                    iat,
                    barometer.getPressureKPa(),
                    1.590);

            fuelTripHistory.add(fuelConsPerHour, getSpeed());
        }
    }

    public Double getTripLitres() {
        return fuelTripHistory.getTotalConsumedLitres();
    }

    public Double getTripLitersPer100km() {
        double totalLitres = fuelTripHistory.getTotalConsumedLitres();
        double totalKm = gps.getTotalDistanceKm();

        if (totalKm > 0) {
            return Math.min(totalLitres / totalKm * 100, 30);
        } else {
            return 0D;
        }
    }

    public Double getTotalGpsDistance() {
        return totalDistanceKm + gps.getTotalDistanceKm();
    }

    public Double getTripGpsDistance() {
        return gps.getTotalDistanceKm();
    }

    public void saveTripData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putFloat(KEY_TOTAL_LITRES, (float) getTotalLiters());
        editor.putFloat(KEY_TOTAL_DISTANCE, (float) getTotalDistanceKm());
        editor.apply();  // Asynchronous save
    }

    public void loadTripData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        totalConsumedLiters = prefs.getFloat(KEY_TOTAL_LITRES, 0f);
        totalDistanceKm = prefs.getFloat(KEY_TOTAL_DISTANCE, 0f);
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm + gps.getTotalDistanceKm();
    }

    public double getTotalLiters() {
        return totalConsumedLiters + fuelTripHistory.getTotalConsumedLitres();
    }

    public Double getTotalLitersPer100km() {
        double totalLitres = getTotalLiters();
        double totalKm = getTotalDistanceKm();

        if (totalKm > 0) {
            return Math.min(totalLitres / totalKm * 100, 30);
        } else {
            return 0D;
        }
    }

    public Double getTripCurrentLitersPerHour() {
        return fuelTripHistory.getCurrentFuelPerHour();
    }

    public Double getTripCurrentLitersPer100km() {
        return fuelTripHistory.getCurrentFuelPer100km();
    }

    public void resetTotals() {
        Toast.makeText(context, "Totals have been reset.", Toast.LENGTH_SHORT);

        totalDistanceKm = 0;
        totalConsumedLiters = 0;

        listener.onTripComputerReadingsUpdated();
    }

    public void resetTrip() {
        gps.resetDistance();
        fuelTripHistory.clear();
    }

    public void pauseUntilTick() {
        gps.setDistanceLoggingEnabled(false);
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

    public boolean isObdAlive() {
        return mObdStudio.isAlive();
    }

    public boolean isAfrAlive() { return mSpartanStudio.isAlive(); }

    public void ensureObdAndAfrAreAlive() {
        if (!mObdStudio.isRunning()) {
            mObdStudio.start();
        }

        if (!mSpartanStudio.isRunning()) {
            mSpartanStudio.start();
        }
    }
}
