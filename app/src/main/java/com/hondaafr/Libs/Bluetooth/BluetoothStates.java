package com.hondaafr.Libs.Bluetooth;

import android.content.Context;
import android.content.Intent;

public class BluetoothStates {

    public static final String KEY_EVENT = "event";
    public static final String KEY_DATA = "data";
    public static final String KEY_NOTIFICATION_ID = "notification_id";

    public static final int EVENT_BT_STATE_CHANGED = 1;
    public static final int EVENT_SERVICE_STATE_CHANGED = 2;
    public static final int EVENT_DATA_RECEIVED = 3;
    public static final int EVENT_NOTIFICATION = 4;

    public static final int STATE_BT_CONNECTING = 0;
    public static final int STATE_BT_CONNECTED = 1;
    public static final int STATE_BT_NONE = 2;
    public static final int STATE_BT_BUSY = 3;
    public static final int STATE_BT_UNPAIRED = 4;
    public static final int STATE_BT_DISCONNECTED = 5;
    public static final int STATE_BT_DISABLED = 6;
    public static final int STATE_BT_ENABLING = 7;
    public static final int STATE_BT_DISABLING = 8;
    public static final int STATE_BT_ENABLED = 9;

    public static final int STATE_SERVICE_STOPPED = 1;
    public static final int STATE_SERVICE_STARTED = 2;

    public static final int NOTIFICATION_CONNECTED_TO_SSID = 0;

    public static Intent intentForBtStateChange(Context context, Class target_class, String action, int state_key) {
        Intent i = new Intent(context, target_class);
        i.setAction(action);
        i.putExtra(BluetoothStates.KEY_EVENT, BluetoothStates.EVENT_BT_STATE_CHANGED);
        i.putExtra(BluetoothStates.KEY_DATA, state_key);
        return i;
    }

    public static Intent intentForServiceStateChange(Context context, Class target_class, String action, int state_key) {
        Intent i = new Intent(context, target_class);
        i.setAction(action);
        i.putExtra(BluetoothStates.KEY_EVENT, BluetoothStates.EVENT_SERVICE_STATE_CHANGED);
        i.putExtra(BluetoothStates.KEY_DATA, state_key);
        return i;
    }

    public static Intent intentForServiceStateChange(String action, int state_key) {
        Intent i = new Intent(action);
        i.putExtra(BluetoothStates.KEY_EVENT, BluetoothStates.EVENT_SERVICE_STATE_CHANGED);
        i.putExtra(BluetoothStates.KEY_DATA, state_key);
        return i;
    }

    public static Intent intentForBtStateChange(String action, int state_key) {
        Intent i = new Intent(action);
        i.putExtra(BluetoothStates.KEY_EVENT, BluetoothStates.EVENT_BT_STATE_CHANGED);
        i.putExtra(BluetoothStates.KEY_DATA, state_key);
        return i;
    }

    public static Intent intentForDataReceived(Context context, Class target_class, String action, String data) {
        Intent i = new Intent(context, target_class);
        i.setAction(action);
        i.putExtra(BluetoothStates.KEY_EVENT, BluetoothStates.EVENT_DATA_RECEIVED);
        i.putExtra(BluetoothStates.KEY_DATA, data);
        return i;
    }

    public static Intent intentForDataReceived(String action, String data) {
        Intent i = new Intent(action);
        i.putExtra(BluetoothStates.KEY_EVENT, BluetoothStates.EVENT_DATA_RECEIVED);
        i.putExtra(BluetoothStates.KEY_DATA, data);
        return i;
    }

    public static Intent intentForNotification(Context context, Class target_class, String action, int notification_id, String message) {
        Intent i = new Intent(context, target_class);
        i.setAction(action);
        i.putExtra(BluetoothStates.KEY_EVENT, BluetoothStates.EVENT_NOTIFICATION);
        i.putExtra(BluetoothStates.KEY_NOTIFICATION_ID, notification_id);
        i.putExtra(BluetoothStates.KEY_DATA, message);
        return i;
    }

    public static Intent intentForNotification(String action, int notification_id, String message) {
        Intent i = new Intent(action);
        i.putExtra(BluetoothStates.KEY_EVENT, BluetoothStates.EVENT_NOTIFICATION);
        i.putExtra(BluetoothStates.KEY_NOTIFICATION_ID, notification_id);
        i.putExtra(BluetoothStates.KEY_DATA, message);
        return i;
    }

    public static String labelOfState(int state) {
        switch (state) {
            case STATE_BT_CONNECTING:
                return "Connecting...";

            case STATE_BT_CONNECTED:
                return "Connected.";

            case STATE_BT_NONE:
                return "Idle.";

            case STATE_BT_BUSY:
                return "Adapter is busy!";

            case STATE_BT_UNPAIRED:
                return "Please pair your device first!";

            case STATE_BT_DISCONNECTED:
                return "Disconnected.";

            case STATE_BT_DISABLED:
                return "Bluetooth is disabled.";

            case STATE_BT_ENABLING:
                return "Bluetooth is enabling...";

            case STATE_BT_DISABLING:
                return "Bluetooth is disabling...";

            case STATE_BT_ENABLED:
                return "Bluetooth is enabled.";

            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

}
