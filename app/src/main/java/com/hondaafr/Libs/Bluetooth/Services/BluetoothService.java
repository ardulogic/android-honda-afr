package com.hondaafr.Libs.Bluetooth.Services;

import android.app.ActivityManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.hondaafr.Libs.Bluetooth.BluetoothConnection;
import com.hondaafr.Libs.Bluetooth.BluetoothDeviceData;
import com.hondaafr.Libs.Bluetooth.BluetoothHelper;
import com.hondaafr.Libs.Bluetooth.BluetoothStates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class BluetoothService extends Service {

    private static final String LOG_NAME = "BluetoothService";
    private static int D = 0;

    private static final int STOP_DELAY = 60000;

    public static final String ACTION_BT_COMMAND = "com.hondaafr.Libs.Bluetooth.Services.action.bt.service.bt.action";
    public static final String ACTION_UI_UPDATE = "com.hondaafr.Libs.Bluetooth.Services.action.bt.service.ui.action";

    public static final int COMMAND_BT_CONNECT = 2;
    public static final int COMMAND_BT_DISCONNECT = 3;
    public static final int COMMAND_BT_SEND = 5;

    public static final String KEY_COMMAND = "key";
    public static final String PARAM_DEVICE_ID = "id";
    public static final String PARAM_DATA = "data";
    public static final String PARAM_SSID = "ssid";
    public static final String PARAM_LINES = "lines";

    public static final String PARAM_UUID = "uuid";


    private static final boolean AUTO_NEWLINE = false;

    private Map<String, ArrayList<String>> queuedLines = new HashMap<>();
    private Timer mTimer;
    private BluetoothHelper mBtHelper;
    private BluetoothConnection mBtConnection;

    private Map<String, BluetoothConnection> mBtConnections = new HashMap<>();

    private BluetoothConnectionListener mBtListener;



    private class TerminateServiceTask extends TimerTask {
        @Override
        public void run() {
            notifyUIOfServiceStateChange(BluetoothStates.STATE_SERVICE_STOPPED);
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        d("onStartCommand", 2);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        d("onCreate", 2);

        notifyUIOfServiceStateChange(BluetoothStates.STATE_SERVICE_STARTED);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_BT_COMMAND);
        intentFilter.addAction(BluetoothConnection.ACTION);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(BluetoothBroadcastReceiver, intentFilter);
        setAutoShutdownTimer();

        this.mBtHelper = new BluetoothHelper(this);
        this.mBtListener = new BluetoothConnectionListener();
    }

    @Override
    public void onDestroy() {
        d("onDestroy", 2);
        disconnectAll();
        unregisterReceiver(BluetoothBroadcastReceiver);
    }

    public void connectToPrevious() {
        // TODO: Connect to previous connections
//        mBtConnections

    }

    public void connectTo(String ssid_or_mac, String device_id, @Nullable String uuid) {
        if (!mBtHelper.isEnabled()) {
            notifyUIOfBtStateChange(BluetoothStates.STATE_BT_DISABLED, device_id);
            return;
        }

        // Check if the device is already in the connection map
        if (mBtConnections.containsKey(device_id)) {
            BluetoothConnection existingConnection = mBtConnections.get(device_id);

            // Ensure the connection is actually connected
            if (existingConnection.isConnected()) {
                notifyUIOfBtStateChange(BluetoothStates.STATE_BT_CONNECTED, device_id);
                return;
            } else {
                existingConnection.connect();
                return;
            }
        }

        BluetoothDevice device = null;
        if (isMacAddress(ssid_or_mac)) {
            device = mBtHelper.getPairedDeviceByMAC(ssid_or_mac);
        } else {
            device = mBtHelper.getPairedDeviceBySSID(ssid_or_mac);
        }


        if (device != null) {
            BluetoothDeviceData deviceData = new BluetoothDeviceData(device, "BT Device");

            BluetoothConnection btConnection = new BluetoothConnection(this, deviceData, mBtListener, device_id, uuid);
            mBtConnections.put(device_id, btConnection);
            btConnection.connect();
        } else {
            notifyUIOfBtStateChange(BluetoothStates.STATE_BT_UNPAIRED, device_id);
        }
    }

    private boolean isMacAddress(String identifier) {
        // Simple check for MAC address format
        return identifier != null && identifier.matches("[0-9A-Fa-f:]{17}");
    }


    public void connectTo(String ssid) {
        this.connectTo(ssid, "default", null);
    }

    public class BluetoothConnectionListener implements com.hondaafr.Libs.Bluetooth.BluetoothConnectionListener {

        @Override
        public void onStateChanged(int state, String device_id) {
            d("Bluetooth State (" + state + "):" + BluetoothStates.labelOfState(state), 1);

            switch (state) {
                case BluetoothStates.STATE_BT_CONNECTED:
                    sendQueuedLines(device_id);
                    break;

                case BluetoothStates.STATE_BT_DISCONNECTED:
                    disconnect(device_id);
                    break;
            }

            notifyUIOfBtStateChange(state, device_id);
        }

        @Override
        public void onDataReceived(String line, String device_id) {
            notifyUIOfDataReceived(line, device_id);
        }

        @Override
        public void onNotification(int notification_id, String message, String device_id) {
            notifyUIOfGeneralNotification(notification_id, message, device_id);
        }
    }

    public static void start(Context context) {
        if (!isRunning(context)) {
            context.startService(new Intent(context, BluetoothService.class));
        }
    }

    public static void connect(Context context, String ssid_or_mac, String device_id) {
        Intent intent = new Intent(ACTION_BT_COMMAND);
        intent.putExtra(KEY_COMMAND, COMMAND_BT_CONNECT);
        intent.putExtra(PARAM_SSID, ssid_or_mac);
        intent.putExtra(PARAM_DEVICE_ID, device_id);
        context.sendBroadcast(intent);
    }

    public static void connect(Context context, String ssid_or_mac, String uuid, String device_id) {
        Intent intent = new Intent(ACTION_BT_COMMAND);
        intent.putExtra(KEY_COMMAND, COMMAND_BT_CONNECT);
        intent.putExtra(PARAM_SSID, ssid_or_mac);
        intent.putExtra(PARAM_UUID, uuid);
        intent.putExtra(PARAM_DEVICE_ID, device_id);
        context.sendBroadcast(intent);
    }


    public static void connect(Context context, String ssid_or_mac) {
        Intent intent = new Intent(ACTION_BT_COMMAND);
        intent.putExtra(KEY_COMMAND, COMMAND_BT_CONNECT);
        intent.putExtra(PARAM_SSID, ssid_or_mac);
        context.sendBroadcast(intent);
    }

    public static void disconnect(Context context) {
        Intent intent = new Intent(ACTION_BT_COMMAND);
        intent.putExtra(KEY_COMMAND, COMMAND_BT_DISCONNECT);
        context.sendBroadcast(intent);
    }

    public static void disconnect(Context context, String id) {
        Intent intent = new Intent(ACTION_BT_COMMAND);
        intent.putExtra(KEY_COMMAND, COMMAND_BT_DISCONNECT);
        intent.putExtra(PARAM_DEVICE_ID, id);
        context.sendBroadcast(intent);
    }

    private void disconnect(String id) {
        if (id != null) {
            BluetoothConnection connection = mBtConnections.get(id);
            if (connection != null) {
                connection.stop();
                mBtConnections.remove(id);
            }
        } else {
            disconnectAll();
        }
    }

    private void disconnectAll() {
        for (BluetoothConnection connection : mBtConnections.values()) {
            connection.stop();
        }
        mBtConnections.clear();
    }

    public static void send(Context context, ArrayList<String> lines, String device_id) {
        Intent intent = new Intent(ACTION_BT_COMMAND);
        intent.putExtra(KEY_COMMAND, COMMAND_BT_SEND);
        intent.putExtra(PARAM_DEVICE_ID, device_id);
        intent.putStringArrayListExtra(PARAM_LINES, lines);
        context.sendBroadcast(intent);
    }

    public static void send(Context context, ArrayList<String> lines) {
        send(context, lines, "default");
    }

    public static void send(Context context, String line, String device_id) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(line);
        send(context, lines, device_id);
    }

    public static void send(Context context, String line) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(line);
        send(context, lines, "default");
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("BluetoothService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void sendQueuedLines() {
        sendQueuedLines("default");
    }

    private void sendQueuedLines(String device_id) {
        // Retrieve the Bluetooth connection for the specified device_id
        BluetoothConnection connection = mBtConnections.get(device_id);

        if (connection != null) {
            // Get the queued lines for the specified device_id
            ArrayList<String> queuedLinesForDevice = this.queuedLines.get(device_id);

            if (queuedLinesForDevice != null && !queuedLinesForDevice.isEmpty()) {
                // Write the queued lines to the Bluetooth connection
                connection.write(queuedLinesForDevice, AUTO_NEWLINE);

                // Clear the queued lines for the specified device_id
                this.queuedLines.get(device_id).clear();
            }
        } else {
            // Handle the case where there is no connection for the specified device_id
            System.out.println("No connection found for device_id: " + device_id);
        }
    }

    private void setQueuedLines(ArrayList<String> lines) {
        setQueuedLines(lines, "default");
    }

    private void setQueuedLines(ArrayList<String> lines, String id) {
        queuedLines.put(id, lines);
    }

    private void setAutoShutdownTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        mTimer = new Timer();
        mTimer.schedule(new TerminateServiceTask(), STOP_DELAY);
    }

    private final BroadcastReceiver BluetoothBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setAutoShutdownTimer();
            processIntent(intent);
        }
    };

    private void processIntent(Intent intent) {
        // This happens when app is closed abruptly
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();
        d("Intent Received:" + action, 3);

        switch (action) {
            case ACTION_BT_COMMAND:
                processUiCommandIntent(intent);
                break;

            case BluetoothAdapter.ACTION_STATE_CHANGED:
                processBluetoothAdapterIntent(intent);
                break;
        }
    }

    private void processBluetoothAdapterIntent(Intent intent) {
        final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        final String device_id = intent.getStringExtra(BluetoothStates.KEY_DEVICE_ID);

        switch (bluetoothState) {
            case BluetoothAdapter.STATE_CONNECTING:
                notifyUIOfBtStateChange(BluetoothStates.STATE_BT_CONNECTING, device_id);
                break;

            case BluetoothAdapter.STATE_TURNING_OFF:
                notifyUIOfBtStateChange(BluetoothStates.STATE_BT_DISABLING, device_id);
                break;

            case BluetoothAdapter.STATE_TURNING_ON:
                notifyUIOfBtStateChange(BluetoothStates.STATE_BT_ENABLING, device_id);
                break;

            case BluetoothAdapter.STATE_ON:
                notifyUIOfBtStateChange(BluetoothStates.STATE_BT_ENABLED, device_id);
                connectToPrevious();
                break;
            case BluetoothAdapter.STATE_OFF:
                notifyUIOfBtStateChange(BluetoothStates.STATE_BT_DISABLED, device_id);
                break;
        }
    }

    private void processUiCommandIntent(Intent intent) {
        final int cmd = intent.getIntExtra(KEY_COMMAND, 0);
        String device_id = intent.getStringExtra(PARAM_DEVICE_ID);

        switch (cmd) {
            case COMMAND_BT_CONNECT:
                d("Command: connect", 1);
                String ssid = intent.getStringExtra(PARAM_SSID);
                String uuid = intent.getStringExtra(PARAM_UUID);
                connectTo(ssid, device_id, uuid);

                break;
            case COMMAND_BT_DISCONNECT:
                d("Command: disconnect", 1);
                disconnect(device_id);
                break;
            case COMMAND_BT_SEND:
                d("Command: send", 3);
                setQueuedLines(intent.getStringArrayListExtra(PARAM_LINES), device_id);
                sendQueuedLines(device_id);
                break;
        }
    }



    private void notifyUIOfGeneralNotification(int notification_id, String message, String bt_id) {
        sendBroadcast(BluetoothStates.intentForNotification(ACTION_UI_UPDATE, notification_id, message, bt_id));
    }

    private void notifyUIOfBtStateChange(int state_key, String device_id) {
        Intent intent = BluetoothStates.intentForBtStateChange(ACTION_UI_UPDATE, state_key, device_id);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, getWidgetIds());

        sendBroadcast(intent);
    }

    private void notifyUIOfServiceStateChange(int state_key) {
        Intent intent = BluetoothStates.intentForServiceStateChange(ACTION_UI_UPDATE, state_key);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, getWidgetIds());

        sendBroadcast(intent);
    }

    private void notifyUIOfDataReceived(String data, String bt_device_id) {
        sendBroadcast(BluetoothStates.intentForDataReceived(ACTION_UI_UPDATE, data, bt_device_id));
    }

    private int[] getWidgetIds() {
//        return AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, NamaiLightsWidget.class));
        return new int[] {};
    }

    private static void d(String msg, int level) {
        if (D >= level) {
            Log.d(LOG_NAME, msg);
        }
    }
}
