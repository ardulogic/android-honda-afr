package com.hondaafr.Libs.Bluetooth.Services;

import android.app.ActivityManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.hondaafr.Libs.Bluetooth.BluetoothConnection;
import com.hondaafr.Libs.Bluetooth.BluetoothDeviceData;
import com.hondaafr.Libs.Bluetooth.BluetoothHelper;
import com.hondaafr.Libs.Bluetooth.BluetoothStates;

import java.util.ArrayList;
import java.util.Objects;
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
    public static final String PARAM_DATA = "data";
    public static final String PARAM_SSID = "ssid";
    public static final String PARAM_LINES = "lines";

    private static final boolean AUTO_NEWLINE = false;

    private String mSSID;
    private ArrayList<String> queuedLines = new ArrayList<String>();
    private Timer mTimer;
    private BluetoothHelper mBtHelper;
    private BluetoothConnection mBtConnection;
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
        mSSID = intent.getStringExtra(PARAM_SSID);

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
        disconnect();
        unregisterReceiver(BluetoothBroadcastReceiver);
    }

    public void connectToPrevious() {
        if (mSSID != null) {
            connectTo(mSSID);
        }
    }

    public void connectTo(String ssid) {
        if (!mBtHelper.isEnabled()) {
            notifyUIOfBtStateChange(BluetoothStates.STATE_BT_DISABLED);
            return;
        }

        if (mBtConnection != null && Objects.equals(ssid, mSSID)) {
            notifyUIOfBtStateChange(BluetoothStates.STATE_BT_CONNECTED);
            return;
        }

        BluetoothDevice device = mBtHelper.getPairedDeviceBySSID(ssid);

        if (device != null) {
            BluetoothDeviceData deviceData = new BluetoothDeviceData(device, "BT Device");

            if (mBtConnection != null) {
                mBtConnection.stop();
            }

            mBtConnection = new BluetoothConnection(this, deviceData, mBtListener);
            mBtConnection.connect();
            mSSID = ssid;
        } else {
            notifyUIOfBtStateChange(BluetoothStates.STATE_BT_UNPAIRED);
        }
    }

    public class BluetoothConnectionListener implements com.hondaafr.Libs.Bluetooth.BluetoothConnectionListener {

        @Override
        public void onStateChanged(int state) {
            d("Bluetooth State (" + state + "):" + BluetoothStates.labelOfState(state), 1);

            switch (state) {
                case BluetoothStates.STATE_BT_CONNECTED:
                    sendQueuedLines();
                    break;

                case BluetoothStates.STATE_BT_DISCONNECTED:
                    disconnect();
                    break;
            }

            notifyUIOfBtStateChange(state);
        }

        @Override
        public void onDataReceived(String line) {
            notifyUIOfDataReceived(line);
        }

        @Override
        public void onNotification(int notification_id, String message) {
            notifyUIOfGeneralNotification(notification_id, message);
        }
    }

    public static void start(Context context) {
        if (!isRunning(context)) {
            context.startService(new Intent(context, BluetoothService.class));
        }
    }

    public static void connect(Context context, String ssid) {
        Intent intent = new Intent(ACTION_BT_COMMAND);
        intent.putExtra(KEY_COMMAND, COMMAND_BT_CONNECT);
        intent.putExtra(PARAM_SSID, ssid);
        context.sendBroadcast(intent);
    }

    public static void disconnect(Context context) {
        Intent intent = new Intent(ACTION_BT_COMMAND);
        intent.putExtra(KEY_COMMAND, COMMAND_BT_DISCONNECT);
        context.sendBroadcast(intent);
    }

    public static void send(Context context, ArrayList<String> lines) {
        Intent intent = new Intent(ACTION_BT_COMMAND);
        intent.putExtra(KEY_COMMAND, COMMAND_BT_SEND);
        intent.putStringArrayListExtra(PARAM_LINES, lines);
        context.sendBroadcast(intent);
    }

    public static void send(Context context, String line) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(line);
        send(context, lines);
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

    private void disconnect() {
        if (mBtConnection != null) {
            mBtConnection.stop();
            mBtConnection = null;
        }
    }

    private void setQueuedLines(ArrayList<String> lines) {
        queuedLines = lines;
    }

    private void sendQueuedLines() {
        if (mBtConnection != null && queuedLines != null && queuedLines.size() > 0) {
            mBtConnection.write(queuedLines, AUTO_NEWLINE);
        }
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

        switch (bluetoothState) {
            case BluetoothAdapter.STATE_CONNECTING:
                notifyUIOfBtStateChange(BluetoothStates.STATE_BT_CONNECTING);
                break;

            case BluetoothAdapter.STATE_TURNING_OFF:
                notifyUIOfBtStateChange(BluetoothStates.STATE_BT_DISABLING);
                break;

            case BluetoothAdapter.STATE_TURNING_ON:
                notifyUIOfBtStateChange(BluetoothStates.STATE_BT_ENABLING);
                break;

            case BluetoothAdapter.STATE_ON:
                notifyUIOfBtStateChange(BluetoothStates.STATE_BT_ENABLED);
                connectToPrevious();
                break;
            case BluetoothAdapter.STATE_OFF:
                notifyUIOfBtStateChange(BluetoothStates.STATE_BT_DISABLED);
                break;
        }
    }

    private void processUiCommandIntent(Intent intent) {
        final int cmd = intent.getIntExtra(KEY_COMMAND, 0);

        switch (cmd) {
            case COMMAND_BT_CONNECT:
                d("Command: connect", 1);
                connectTo(intent.getStringExtra(PARAM_SSID));
                break;
            case COMMAND_BT_DISCONNECT:
                d("Command: disconnect", 1);
                disconnect();
                break;
            case COMMAND_BT_SEND:
                d("Command: send", 3);
                setQueuedLines(intent.getStringArrayListExtra(PARAM_LINES));
                sendQueuedLines();
                break;
        }
    }

    private void notifyUIOfGeneralNotification(int notification_id, String message) {
        sendBroadcast(BluetoothStates.intentForNotification(ACTION_UI_UPDATE, notification_id, message));
    }

    private void notifyUIOfBtStateChange(int state_key) {
        Intent intent = BluetoothStates.intentForBtStateChange(ACTION_UI_UPDATE, state_key);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, getWidgetIds());

        sendBroadcast(intent);
    }

    private void notifyUIOfServiceStateChange(int state_key) {
        Intent intent = BluetoothStates.intentForServiceStateChange(ACTION_UI_UPDATE, state_key);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, getWidgetIds());

        sendBroadcast(intent);
    }

    private void notifyUIOfDataReceived(String data) {
        sendBroadcast(BluetoothStates.intentForDataReceived(ACTION_UI_UPDATE, data));
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
