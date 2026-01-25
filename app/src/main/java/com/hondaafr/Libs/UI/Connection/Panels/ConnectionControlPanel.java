package com.hondaafr.Libs.UI.Connection.Panels;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.hondaafr.Libs.Bluetooth.BluetoothStates;
import com.hondaafr.Libs.Bluetooth.BluetoothUtils;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ConnectionControlPanel extends Panel {

    private final Button buttonConnectObd;
    private final Button buttonConnectSpartan;
    
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
                    // Data routing is handled app-wide by BluetoothConnectionManager
                    break;
            }
        }
    };

    @Override
    public int getContainerId() {
        return R.id.layoutConnectButtons;
    }

    @Override
    public String getListenerId() {
        return "connection_control_panel";
    }

    public ConnectionControlPanel(MainActivity mainActivity, TripComputer tripComputer, UiView parent) {
        super(mainActivity, tripComputer, parent);
        
        buttonConnectObd = rootView.findViewById(R.id.buttonConnectObd);
        buttonConnectObd.setOnClickListener(v -> connectObd());
        
        buttonConnectSpartan = rootView.findViewById(R.id.buttonConnectSpartan);
        buttonConnectSpartan.setOnClickListener(v -> connectSpartan());
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onStart(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter, Context.RECEIVER_EXPORTED);
        } else {
            mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter);
        }
    }

    @Override
    public void onResume(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter, Context.RECEIVER_EXPORTED);
        } else {
            mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter);
        }
    }

    @Override
    public void onPause(Context context) {
        try {
            mainActivity.unregisterReceiver(btReceiver);
        } catch (IllegalArgumentException e) {
            Log.w("ConnectionControlPanel", "Receiver was already unregistered");
        }
    }

    @Override
    public void onDestroy(Context context) {
        disconnect();
    }

    private void onBluetoothServiceStateChanged(int state) {
        // Just update UI - auto-connect is handled app-wide
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
            case BluetoothStates.STATE_BT_ENABLING:
                setConnectButtonsEnabled(false, "Connect");
                break;
            case BluetoothStates.STATE_BT_ENABLED:
                setConnectButtonsEnabled(true, "Connect");
                break;
            case BluetoothStates.STATE_BT_DISABLED:
                setConnectButtonsEnabled(false, "Connect");
                break;
        }
    }

    private void handleDeviceBluetoothState(int state, String deviceId) {
        Button connectButton = getConnectButton(deviceId);
        
        switch (state) {
            case BluetoothStates.STATE_BT_CONNECTED:
                updateConnectButton(connectButton, "Disconnect", true);
                break;
            case BluetoothStates.STATE_BT_BUSY:
            case BluetoothStates.STATE_BT_ENABLING:
                updateConnectButton(connectButton, "Connect", false);
                break;
            case BluetoothStates.STATE_BT_CONNECTING:
                updateConnectButton(connectButton, "Connecting...", false);
                break;
            case BluetoothStates.STATE_BT_DISCONNECTED:
                updateConnectButton(connectButton, "Connect", true);
                break;
            case BluetoothStates.STATE_BT_NONE:
            case BluetoothStates.STATE_BT_UNPAIRED:
            default:
                updateConnectButton(connectButton, deviceId.equals("obd") ? "Connect OBD" : "Connect AFR", true);
                break;
        }
    }
    private void setConnectButtonsEnabled(boolean enabled, String label) {
        updateConnectButton(buttonConnectObd, label, enabled);
        updateConnectButton(buttonConnectSpartan, label, enabled);
    }

    private Button getConnectButton(String deviceId) {
        return "obd".equals(deviceId) ? buttonConnectObd : buttonConnectSpartan;
    }

    private void updateConnectButton(Button button, String text, boolean enabled) {
        if (button == null) return;
        button.setText(text);
        button.setEnabled(enabled);
    }

    private void connectObd() {
        BluetoothService.connect(mainActivity, "OBDII", BluetoothUtils.OBD_UUID, "obd");
    }

    private void connectSpartan() {
        BluetoothService.connect(mainActivity, "AutoLights", "spartan");
    }

    private void disconnect() {
        BluetoothService.disconnect(mainActivity, "obd");
        BluetoothService.disconnect(mainActivity, "spartan");
    }
}

