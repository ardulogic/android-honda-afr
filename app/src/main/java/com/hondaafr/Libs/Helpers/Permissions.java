package com.hondaafr.Libs.Helpers;

import static android.content.Context.POWER_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Permissions {
    private static final int IGNORE_BATTERY_OPTIMIZATION_REQUEST = 0;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT = 1;
    private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH_SCAN = 2;
    private static final int MY_PERMISSIONS_REQUEST_ENABLE_BT = 3;

    public static void askIgnoreBatteryOptimization(Activity activity) {
        PowerManager pm = (PowerManager) activity.getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (pm != null && !pm.isIgnoringBatteryOptimizations(activity.getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, IGNORE_BATTERY_OPTIMIZATION_REQUEST);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public static void askBluetoothPermission(Activity activity) {
        // Check if the permission is not granted
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {

            // Request the permission
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT);
        }

        // Check if the permission is not granted
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {

            // Request the permission
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    MY_PERMISSIONS_REQUEST_BLUETOOTH_SCAN);
        }

        // Check if Bluetooth permissions are granted
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {

            // Request Bluetooth permissions
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                    MY_PERMISSIONS_REQUEST_ENABLE_BT);
        }
    }

    @SuppressLint("MissingPermission")
    public static void promptEnableBluetooth(Activity activity) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, 4);
    }

}
