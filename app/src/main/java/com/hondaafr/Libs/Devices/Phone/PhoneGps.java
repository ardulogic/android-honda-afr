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
    private long lastDistanceUpdateTime = 0L;
    private long lastLocationUpdateTime = 0L;

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
        if (!hasLocationPermission(context)) return;

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            Log.w("PhoneGps", "Unsupported SDK version for location updates.");
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(UPDATE_INTERVAL_MS)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (result == null || result.getLocations().isEmpty()) return;

                for (Location location : result.getLocations()) {
                    if (sunsetTime == null) {
                        updateSunriseSunset(location);
                    }

                    speedKmh = location.getSpeed() * 3.6;
                    listener.onGpsSpeedUpdated(speedKmh);

                    lastLocationUpdateTime = System.currentTimeMillis();

                    if (!locationHistory.isEmpty()) {
                        Location last = locationHistory.getLast();
                        float distance = last.distanceTo(location);

                        if (distance >= minDistanceDelta) {
                            locationHistory.add(location);
                            lastDistanceUpdateTime = System.currentTimeMillis();

                            listener.onGpsDistanceIncrement(distance / 1000.0); // Convert to km
                        } else {
                            Log.d("PhoneGps", "Ignored small movement: " + distance + "m");
                        }
                    } else {
                        locationHistory.add(location);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
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

    public boolean isLoggingDistance() {
        return System.currentTimeMillis() - lastDistanceUpdateTime < UPDATE_INTERVAL_MS * 2;
    }

    public boolean isAlive() {
        return System.currentTimeMillis() - lastLocationUpdateTime < UPDATE_INTERVAL_MS * 1.5;
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

    public void setMinDistanceDeltaInMeters(int delta) {
        this.minDistanceDelta = delta;
    }
}
