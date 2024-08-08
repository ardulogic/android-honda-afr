package com.hondaafr.Libs.Bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by Dr. Automate on 9/2/2017.
 */

public class BluetoothHelper {
    Context mContext;
    BluetoothAdapter mBluetooth;

    public BluetoothHelper(Context context) {
        this.mContext = context;
        this.mBluetooth = BluetoothAdapter.getDefaultAdapter();
    }

    public Set<BluetoothDevice> getPairedDevices() {
        return mBluetooth.getBondedDevices();
    }

    public boolean deviceIsPaired(String ssid) {
        Set<BluetoothDevice> pairedDevices = getPairedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (ssid.equals(device.getName())) {
                return true;
            }
        }

        return false;
    }

    public static boolean deviceIsConnected(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected", (Class[]) null);
            boolean connected = (boolean) m.invoke(device, (Object[]) null);
            return connected;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public BluetoothDevice getPairedDeviceBySSID(String ssid) {
        Set<BluetoothDevice> pairedDevices = getPairedDevices();
        for (BluetoothDevice device : pairedDevices) {
            String savedSsid = device.getName().trim().toLowerCase();
            String inputSsid = ssid.trim().toLowerCase();

            if (savedSsid.equals(inputSsid)) {
                return device;
            }
        }

        return null;
    }

    public boolean isEnabled() {
        return mBluetooth.isEnabled();
    }



    public void disable() {
        if (isEnabled()) {
            mBluetooth.disable();
        }
    }
//
//    public void send(ArrayList<String> lines) {
//        for (String line : lines) {
//            send(line);
//        }
//    }
//
//    public void send(String line) {
//        if (D) Log.d(LOG_NAME, "Sending line to BT:" + line);
//        if (mBluetoothConnector.getState() == BluetoothStates.STATE_BT_CONNECTED) {
//            mBluetoothConnector.write(line.getBytes());
//        }
//    }
//
//    public boolean isSending() {
//        return mBluetoothConnector.isSending;
//    }

}
