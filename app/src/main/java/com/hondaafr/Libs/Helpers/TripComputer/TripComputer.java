package com.hondaafr.Libs.Helpers.TripComputer;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.hondaafr.Libs.Devices.Obd.ObdStudio;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneBarometer;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Devices.Phone.PhoneGpsListener;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Helpers.AverageList;
import com.hondaafr.Libs.Helpers.FuelConsumption;
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

    private float totalDistanceKm = 0;
    private float totalConsumedLiters = 0;
    private final TotalLitersConsumed tripTotalLiters = new TotalLitersConsumed();
    private double instLitersPerHour = 0;
    private final AverageList currLitersPerHourShortHistory = new AverageList(15);
    private final AverageList currLitersPer100ShortHistory = new AverageList(15);
    private final TripComputerListener listener;
    private final PhoneBarometer barometer;

    private static final String PREFS_NAME = "TripComputerPrefs";
    private static final String KEY_TOTAL_LITRES = "totalLitres";
    private static final String KEY_TOTAL_DISTANCE = "totalDistance";
    private final Context context;



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

    private void calculateFuelConsumption(Double afr) {
        if (mObdStudio.readingsForFuelAreActive()) {
            Integer iatObd = (Integer) mObdStudio.getAvailableReading("iat").getValue();
            Integer rpmObd = (Integer) mObdStudio.getAvailableReading("rpm").getValue();
            Integer mapObd = (Integer) mObdStudio.getAvailableReading("map").getValue();

            double iat = iatObd > 0 ? iatObd : 35;

            double litersPerHour = FuelConsumption.calculateFuelConsumptionLperHour(
                    afr,
                    rpmObd,
                    mapObd,
                    iat,
                    barometer.getPressureKPa(),
                    1.590);

            instLitersPerHour = litersPerHour;

            currLitersPerHourShortHistory.add(litersPerHour);
            currLitersPer100ShortHistory.add(FuelConsumption.calculateLiters100km(litersPerHour, getSpeed()));

            tripTotalLiters.add(litersPerHour, getSpeed());
        }
    }

    public Double getTripLitres() {
        return tripTotalLiters.getTotal();
    }

    public Double getTripLitersPer100km() {
        double totalLitres = tripTotalLiters.getTotal();
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
        return totalConsumedLiters + tripTotalLiters.getTotal();
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

    public double getInstLitersPerHour() {
        return instLitersPerHour;
    }

    public Double getShortAvgLitersPerHour() {
        return currLitersPerHourShortHistory.getAvg();
    }

    public Double getShortAvgLitersPer100() {
        return currLitersPer100ShortHistory.getAvg();
    }

    public void resetTotals() {
        Toast.makeText(context, "Totals have been reset.", Toast.LENGTH_SHORT);

        totalDistanceKm = 0;
        totalConsumedLiters = 0;

        listener.onTripComputerReadingsUpdated();
    }

    public void resetTrip() {
        gps.resetDistance();
        tripTotalLiters.clear();
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

    public void ensureObdAndAfrAreAlive() {
        if (!mObdStudio.isRunning()) {
            mObdStudio.start();
        }

        if (!mSpartanStudio.isRunning()) {
            mSpartanStudio.start();
        }
    }

    public Map<String, String> getReadingsAsString() {
        LinkedHashMap<String, String> readings = new LinkedHashMap<>();

        readings.put("Total km", String.format("%.1f", getTotalDistanceKm()));
        readings.put("Total l", String.format("%.1f", getTotalLiters()));

        readings.put("Trip km", String.format("%.1f", getTripGpsDistance()));
        readings.put("Trip l", String.format("%.1f", getTripLitres()));
        readings.put("Trip l / hour", String.format("%.1f", getTripLitersPer100km()));

        readings.put("Inst l / hour", String.format("%.1f", getInstLitersPerHour()));
        readings.put("ShrtAvg l / hour", String.format("%.1f", getShortAvgLitersPerHour()));
        readings.put("ShrtAvg l / 100", String.format("%.1f", getShortAvgLitersPer100()));

        return readings;
    }
}
