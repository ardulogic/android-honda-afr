package com.hondaafr.Libs.Devices.Phone;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.*;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

public class PhoneGps {

    private static final long UPDATE_INTERVAL_MS = 2000;
    private static final int DISTANCE_THRESHOLD_METERS = 15;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private final Context context;
    private final PhoneGpsListener listener;
    private final FusedLocationProviderClient fusedLocationClient;

    private LocationCallback locationCallback;
    private final LinkedList<Location> locationHistory = new LinkedList<>();
    private double speedKmh = 0.0;
    private long lastUpdateTime = 0L;

    private LocalTime sunriseTime;
    private LocalTime sunsetTime;
    private int minDistanceDelta = DISTANCE_THRESHOLD_METERS;

    public PhoneGps(Context context, PhoneGpsListener listener) {
        this.context = context;
        this.listener = listener;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        startLocationUpdates();
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(Context context) {
        ActivityCompat.requestPermissions(
                (Activity) context,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!hasLocationPermission(context)) {
            Log.w("PhoneGps", "Location permission not granted.");
            return;
        }

        LocationRequest locationRequest;
        
        // Use new Builder API for Android 12+ (API 31+), fallback to older API for compatibility
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MS)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setIntervalMillis(UPDATE_INTERVAL_MS)
                    .build();
        } else {
            // Use older API for Android versions below 12 (Huawei P30 compatibility)
            locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(UPDATE_INTERVAL_MS)
                    .setFastestInterval(UPDATE_INTERVAL_MS);
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (result == null || result.getLocations().isEmpty()) return;

                for (Location location : result.getLocations()) {
                    if (sunsetTime == null) {
                        updateSunriseSunset(location);
                    }

                    speedKmh = location.getSpeed() * 3.6;
                    lastUpdateTime = System.currentTimeMillis();

                    if (location.getAccuracy() < 50) {
                        if (!locationHistory.isEmpty()) {
                            Location last = locationHistory.getLast();
                            float distance = last.distanceTo(location);
                            Log.d("PhoneGps", "Received: " + distance + "m" + " lat: " + location.getLatitude() + " lon: " + location.getLongitude() + " acc:" + location.getAccuracy());

                            if (distance >= minDistanceDelta) {
                                locationHistory.add(location);
                                listener.onUpdate(speedKmh, distance / 1000.0, location.getAccuracy()); // Convert to km
                            } else {
                                Log.d("PhoneGps", "Ignored small movement: " + distance + "m");
                            }
                        } else {
                            locationHistory.add(location);
                            listener.onUpdate(speedKmh, 0, location.getAccuracy());
                        }
                    } else {
                        Log.d("PhoneGps", "Ignored accuracy: " + location.getAccuracy() + "m");
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d("PhoneGps", "Location updates started successfully.");
        } catch (SecurityException e) {
            Log.e("PhoneGps", "SecurityException starting location updates: " + e.getMessage());
        } catch (Exception e) {
            Log.e("PhoneGps", "Error starting location updates: " + e.getMessage());
        }
    }

    private void updateSunriseSunset(@NonNull Location location) {
        com.luckycatlabs.sunrisesunset.dto.Location dto = new com.luckycatlabs.sunrisesunset.dto.Location(
                location.getLatitude(), location.getLongitude());

        SunriseSunsetCalculator calc = new SunriseSunsetCalculator(dto, ZoneId.systemDefault().getId());

        Calendar calendar = Calendar.getInstance();
        Calendar sunrise = calc.getOfficialSunriseCalendarForDate(calendar);
        Calendar sunset = calc.getOfficialSunsetCalendarForDate(calendar);

        sunriseTime = LocalTime.of(sunrise.get(Calendar.HOUR_OF_DAY), sunrise.get(Calendar.MINUTE));
        sunsetTime = LocalTime.of(sunset.get(Calendar.HOUR_OF_DAY), sunset.get(Calendar.MINUTE));
    }

    public boolean isAlive() {
        return System.currentTimeMillis() - lastUpdateTime < UPDATE_INTERVAL_MS * 1.5;
    }

    public double getSpeed() {
        return speedKmh;
    }

    public LocalTime getSunsetTime() {
        return sunsetTime;
    }

    public LocalTime getSunriseTime() {
        return sunriseTime;
    }

    public Map<String, String> getReadingsAsString() {
        Map<String, String> readings = new LinkedHashMap<>();
        readings.put("GPS Speed (km/h)", String.format("%.1f", speedKmh));

        if (!locationHistory.isEmpty()) {
            Location last = locationHistory.getLast();
            readings.put("GPS Latitude", String.valueOf(last.getLatitude()));
            readings.put("GPS Longitude", String.valueOf(last.getLongitude()));
        }

        return readings;
    }

    @Nullable
    public Location getLastLocation() {
        if (locationHistory.isEmpty()) {
            return null;
        }
        return locationHistory.getLast();
    }

    public void setMinDistanceDeltaInMeters(int delta) {
        this.minDistanceDelta = delta;
    }
}
