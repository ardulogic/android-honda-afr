package com.hondaafr.Libs.Helpers;

import static android.content.Context.POWER_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.Set;

public class Permissions {

    // Unique request codes
    public static final int REQUEST_CODE_ALL_PERMISSIONS = 100;
    public static final int REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATION = 101;
    public static final int REQUEST_CODE_ENABLE_BLUETOOTH = 102;
    public static final int REQUEST_CODE_ENABLE_LOCATION_SERVICES = 103;

    // Active requests tracker
    private static final Set<Integer> activeRequests = new HashSet<>();

    private static boolean isRequestActive(int requestCode) {
        return activeRequests.contains(requestCode);
    }

    private static void markRequestActive(int requestCode) {
        activeRequests.add(requestCode);
    }

    public static void clearRequest(int requestCode) {
        activeRequests.remove(requestCode);
    }

    // Permissions list
    private static final String[] ALL_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE,
    };

    // Request all permissions
    public static void askForAllPermissions(Activity activity) {
        askIgnoreBatteryOptimization(activity);

        if (!arePermissionsGranted(activity, ALL_PERMISSIONS)) {
            if (!isRequestActive(REQUEST_CODE_ALL_PERMISSIONS)) {
                ActivityCompat.requestPermissions(activity, ALL_PERMISSIONS, REQUEST_CODE_ALL_PERMISSIONS);
                markRequestActive(REQUEST_CODE_ALL_PERMISSIONS);
            }
        } else {
            checkLocationServices(activity);
        }
    }

    // Check if permissions are granted
    private static boolean arePermissionsGranted(Activity activity, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Prompt to enable location services
    private static void checkLocationServices(Activity activity) {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Activity.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {}

        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {}

        if (!gpsEnabled && !networkEnabled && !isRequestActive(REQUEST_CODE_ENABLE_LOCATION_SERVICES)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivityForResult(intent, REQUEST_CODE_ENABLE_LOCATION_SERVICES);
            markRequestActive(REQUEST_CODE_ENABLE_LOCATION_SERVICES);
        }
    }

    // Ask to ignore battery optimization
    public static void askIgnoreBatteryOptimization(Activity activity) {
        PowerManager pm = (PowerManager) activity.getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (pm != null && !pm.isIgnoringBatteryOptimizations(activity.getPackageName())) {
                if (!isRequestActive(REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATION)) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivityForResult(intent, REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATION);
                    markRequestActive(REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATION);
                }
            }
        }
    }

    // Prompt user to enable Bluetooth
    @SuppressLint("MissingPermission")
    public static void promptEnableBluetooth(Activity activity) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            if (!isRequestActive(REQUEST_CODE_ENABLE_BLUETOOTH)) {
                Log.d("Permissions", "Prompting to enable Bluetooth");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BLUETOOTH);
                markRequestActive(REQUEST_CODE_ENABLE_BLUETOOTH);
            } else {
                Log.d("Permissions", "Bluetooth prompt already active");
            }
        }
    }

    // Central handler for all request results
    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        clearRequest(requestCode);

        switch (requestCode) {
            case REQUEST_CODE_ENABLE_BLUETOOTH:
                Log.d("Permissions", "Bluetooth request result: " +
                        (resultCode == Activity.RESULT_OK ? "Enabled" : "Cancelled"));
                break;

            case REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATION:
                Log.d("Permissions", "Battery optimization request handled.");
                break;

            case REQUEST_CODE_ENABLE_LOCATION_SERVICES:
                Log.d("Permissions", "Returned from location settings.");
                break;

            case REQUEST_CODE_ALL_PERMISSIONS:
                Log.d("Permissions", "Permissions request finished.");
                break;

            default:
                Log.w("Permissions", "Unknown request code: " + requestCode);
                break;
        }
    }
}
