package com.hondaafr.Libs.Bluetooth;

public interface BluetoothConnectionListener {
    public void onStateChanged(int state, String device_id);
    public void onDataReceived(String line, String device_id);
    public void onNotification(int notification_id, String message, String device_id);
}
