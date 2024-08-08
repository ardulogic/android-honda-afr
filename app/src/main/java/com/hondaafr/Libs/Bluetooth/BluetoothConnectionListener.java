package com.hondaafr.Libs.Bluetooth;

public interface BluetoothConnectionListener {
    public void onStateChanged(int state);
    public void onDataReceived(String line);
    public void onNotification(int notification_id, String message);
}
