package com.hondaafr.Libs.Devices.Phone;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class PhoneGps {

    private static final long UPDATE_INTERVAL_MS = 2000;
    private final PhoneGpsListener listener;
    private final Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private static final float DISTANCE_THRESHOLD_METERS = 15.0f; // Adjustable threshold
    private final LinkedList<Location> locationHistory = new LinkedList<>();
    private double totalDistanceMeters = 0.0;
    public Double speed = 0.0D;
    private boolean distanceLoggingEnabled = false;

    private long timeDistanceLogged = 0L;
    private LocalTime sunsetTime;
    private long timeLastUpdated = 0L;

    public PhoneGps(Context mContext, PhoneGpsListener listener) {
        this.listener = listener;
        this.context = mContext;

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        startLocationUpdates();
    }

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(Context context) {
        ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        Log.d("GPS", "Starting Location Updates...");

        if (hasLocationPermission(this.context)) {
            Log.d("Permission", "OK");
        } else {
//            requestLocationPermission(this.context);
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Log.d("GPS", "Building location request");
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY )
                    .setIntervalMillis(UPDATE_INTERVAL_MS)
                    .build();


            Log.d("GPS", "Building location callback");

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult == null) {
                        Log.d("Speed", "No location result");
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        if (sunsetTime == null) {
                            acquireSunsetTime(location);
                        }

                        Log.d("GPS", "Location updated.");

                        // Calculate speed
                        speed = (double) (location.getSpeed() * 3.6f);
                        listener.onGpsSpeedUpdated(speed);

                        timeLastUpdated = System.currentTimeMillis();

                        // Track location and calculate distance
                        if (!locationHistory.isEmpty() && distanceLoggingEnabled) {
                            Location lastLocation = locationHistory.getLast();
                            float distance = lastLocation.distanceTo(location);

                            if (distance >= DISTANCE_THRESHOLD_METERS) {
                                totalDistanceMeters += distance;
                                locationHistory.add(location);
                                timeDistanceLogged = System.currentTimeMillis();
                            } else {
                                Log.d("GPS Filter", "Ignored small movement: " + distance + " meters");
                            }
                        } else {
                            locationHistory.add(location);
                        }
                    }
                }
            };

            Log.d("Speed", "Location updates requested");
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            Log.d("Speed", "Bad sdk");
        }
    }

    private void acquireSunsetTime(@NonNull Location loc) {
        // 1️⃣  Convert to the library’s Location DTO
        com.luckycatlabs.sunrisesunset.dto.Location dto =
                new com.luckycatlabs.sunrisesunset.dto.Location(
                        loc.getLatitude(), loc.getLongitude());

        SunriseSunsetCalculator calc =
                new SunriseSunsetCalculator(dto, ZoneId.systemDefault().getId());

        // 2️⃣  Ask for a Calendar and convert to java.time
        Calendar sunsetCal = calc.getOfficialSunsetCalendarForDate(Calendar.getInstance());
        sunsetTime = LocalTime.of(
                sunsetCal.get(Calendar.HOUR_OF_DAY),
                sunsetCal.get(Calendar.MINUTE),
                sunsetCal.get(Calendar.SECOND));
    }

    public LocalTime getSunsetTime() {
        return sunsetTime;
    }

    @SuppressLint("DefaultLocale")
    public Map<String, String> getReadingsAsString() {
        LinkedHashMap<String, String> readings = new LinkedHashMap<>();
        readings.put("GPS Speed (km/h)", String.format("%.1f", speed));
        readings.put("GPS Distance (km)", String.format("%.1f", getTotalDistanceKm()));

        if (locationHistory.getLast() != null) {
            readings.put("GPS Latitude", String.valueOf(locationHistory.getLast().getLatitude()));
            readings.put("GPS Longitude", String.valueOf(locationHistory.getLast().getLongitude()));
        }

        return readings;
    }

    @SuppressLint("DefaultLocale")
    public Double getSpeed() {
        return speed;
    }

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public double getTotalDistanceKm() {
        return totalDistanceMeters / 1000.0;
    }

    public void resetDistance() {
        totalDistanceMeters = 0;
    }

    public void setDistanceLoggingEnabled(boolean enabled) {
        this.distanceLoggingEnabled = enabled;

        if (!enabled) {
            locationHistory.clear();
        }
    }

    public boolean isLoggingDistance() {
        return System.currentTimeMillis() - this.timeDistanceLogged < UPDATE_INTERVAL_MS * 2;
    }

    public boolean isAlive() {
        return System.currentTimeMillis() - this.timeLastUpdated < UPDATE_INTERVAL_MS * 1.5;
    }
}
