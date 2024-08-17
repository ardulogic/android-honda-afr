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

public class GpsSpeed {

    private static final long UPDATE_INTERVAL_MS = 1000;
    private final GpsSpeedListener listener;
    private final Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    public Double speed = 0.0D;

    public GpsSpeed(Context mContext, GpsSpeedListener listener) {
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
        if (hasLocationPermission(this.context)) {
            Log.d("Permission", "OK");
        } else {
//            requestLocationPermission(this.context);
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Log.d("Speed", "Building location request");
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MS)
                    .build();


            Log.d("Speed", "Building location callback");

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult == null) {
                        Log.d("Speed", "No locaiton result");
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        // Calculate speed in km/h
                        speed = (double) (location.getSpeed() * 3.6f);
                        listener.onGpsSpeedUpdated(speed);
                    }
                }
            };

            Log.d("Speed", "Location updates requested");
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            Log.d("Speed", "Bad sdk");
        }
    }

}
