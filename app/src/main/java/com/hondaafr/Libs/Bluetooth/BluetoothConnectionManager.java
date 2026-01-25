package com.hondaafr.Libs.Bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.BackgroundServiceStartNotAllowedException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Bluetooth.BluetoothUtils;
import com.hondaafr.Libs.Helpers.Studio;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;

/**
 * App-wide Bluetooth connection manager that handles auto-connect logic.
 * This ensures connections are managed centrally, not tied to specific fragments.
 */
public class BluetoothConnectionManager {
    private static final String TAG = "BluetoothConnMgr";
    private static final int AUTO_CONNECT_DELAY_MS = 2000;
    
    private final Activity activity;
    private final TripComputer tripComputer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final IntentFilter btUiUpdateIntentFilter = new IntentFilter(BluetoothService.ACTION_UI_UPDATE);
    
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int key = intent.getIntExtra(BluetoothStates.KEY_EVENT, -1);
            
            switch (key) {
                case BluetoothStates.EVENT_SERVICE_STATE_CHANGED:
                    onBluetoothServiceStateChanged(intent.getIntExtra(BluetoothStates.KEY_DATA, -1));
                    break;
                    
                case BluetoothStates.EVENT_BT_STATE_CHANGED:
                    onBluetoothStateChanged(
                            intent.getIntExtra(BluetoothStates.KEY_DATA, -1),
                            intent.getStringExtra(BluetoothStates.KEY_DEVICE_ID)
                    );
                    break;
                    
                case BluetoothStates.EVENT_DATA_RECEIVED:
                    onBluetoothDataReceived(
                            intent.getStringExtra(BluetoothStates.KEY_DATA),
                            intent.getStringExtra(BluetoothStates.KEY_DEVICE_ID)
                    );
                    break;
            }
        }
    };
    
    public BluetoothConnectionManager(Activity activity, TripComputer tripComputer) {
        this.activity = activity;
        this.tripComputer = tripComputer;
    }
    
    public void onStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(btReceiver, btUiUpdateIntentFilter, Context.RECEIVER_EXPORTED);
        } else {
            activity.registerReceiver(btReceiver, btUiUpdateIntentFilter);
        }
        startBtService();
    }
    
    public void onResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(btReceiver, btUiUpdateIntentFilter, Context.RECEIVER_EXPORTED);
        } else {
            activity.registerReceiver(btReceiver, btUiUpdateIntentFilter);
        }
        startBtService();
    }
    
    public void onDestroy() {
        try {
            activity.unregisterReceiver(btReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver was already unregistered");
        }
    }
    
    private void onBluetoothServiceStateChanged(int state) {
        switch (state) {
            case BluetoothStates.STATE_SERVICE_STARTED:
                Log.d(TAG, "Bluetooth service started - auto-connecting to devices");
                connectSpartanSoon();
                connectObdSoon();
                break;
            case BluetoothStates.STATE_SERVICE_STOPPED:
                Log.d(TAG, "Bluetooth service stopped");
                break;
        }
    }
    
    private void onBluetoothStateChanged(int state, String deviceId) {
        if (deviceId == null) {
            // Global Bluetooth state change
            handleGlobalBluetoothState(state);
        } else {
            // Device-specific state change
            handleDeviceBluetoothState(state, deviceId);
        }
    }
    
    private void handleGlobalBluetoothState(int state) {
        switch (state) {
            case BluetoothStates.STATE_BT_ENABLED:
                Log.d(TAG, "Bluetooth enabled - auto-connecting to devices");
                connectSpartanSoon();
                connectObdSoon();
                break;
        }
    }
    
    private void handleDeviceBluetoothState(int state, String deviceId) {
        Studio studio = getStudio(deviceId);
        
        switch (state) {
            case BluetoothStates.STATE_BT_CONNECTED:
                Log.d(TAG, "Device connected: " + deviceId);
                if (studio != null) {
                    studio.start();
                }
                break;
                
            case BluetoothStates.STATE_BT_DISCONNECTED:
                Log.d(TAG, "Device disconnected: " + deviceId + " - auto-reconnecting");
                reconnectDeviceSoon(deviceId);
                break;
        }
    }
    
    private void onBluetoothDataReceived(String data, String deviceId) {
        // Route data to the appropriate studio for processing (app-wide handling)
        if (tripComputer != null && data != null && !data.trim().isEmpty()) {
            if ("spartan".equals(deviceId) && tripComputer.mSpartanStudio != null) {
                tripComputer.mSpartanStudio.onDataReceived(data);
            } else if ("obd".equals(deviceId) && tripComputer.mObdStudio != null) {
                tripComputer.mObdStudio.onDataReceived(data);
            }
        }
    }
    
    private Studio getStudio(String deviceId) {
        if (tripComputer == null) return null;
        return "obd".equals(deviceId) ? tripComputer.mObdStudio : tripComputer.mSpartanStudio;
    }
    
    private void reconnectDeviceSoon(String deviceId) {
        if ("spartan".equals(deviceId)) {
            connectSpartanSoon();
        } else if ("obd".equals(deviceId)) {
            connectObdSoon();
        }
    }
    
    public void connectSpartan() {
        BluetoothService.connect(activity, "AutoLights", "spartan");
    }
    
    public void connectObd() {
        BluetoothService.connect(activity, "OBDII", BluetoothUtils.OBD_UUID, "obd");
    }
    
    private void connectSpartanSoon() {
        handler.postDelayed(this::connectSpartan, AUTO_CONNECT_DELAY_MS);
    }
    
    private void connectObdSoon() {
        handler.postDelayed(this::connectObd, AUTO_CONNECT_DELAY_MS);
    }
    
    private void startBtService() {
        if (!BluetoothService.isRunning(activity)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    activity.startService(new Intent(activity, BluetoothService.class));
                } else {
                    activity.startService(new Intent(activity, BluetoothService.class));
                }
            } catch (BackgroundServiceStartNotAllowedException e) {
                Log.e(TAG, "Can't start bluetooth service - not allowed!");
            }
        }
    }
}

