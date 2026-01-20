package com.hondaafr.Libs.UI.Scientific.Panels;

import android.annotation.SuppressLint;
import android.app.BackgroundServiceStartNotAllowedException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.RequiresApi;

import com.hondaafr.Libs.Bluetooth.BluetoothStates;
import com.hondaafr.Libs.Bluetooth.BluetoothUtils;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Helpers.Permissions;
import com.hondaafr.Libs.Helpers.Studio;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import java.util.Objects;

public class ConnectPanel extends Panel {

    private final GenericStatusPanel genericStatusPanel;
    private final Button buttonConnectSpartan;
    private final Button buttonConnectObd;

    IntentFilter btUiUpdateIntentFilter = new IntentFilter(BluetoothService.ACTION_UI_UPDATE);

    @Override
    public int getContainerId() {
        return R.id.layoutConnection;
    }

    @Override
    public String getListenerId() {
        return "connect_panel";
    }

    public ConnectPanel(MainActivity mainActivity, TripComputer tripComputer, UiView parent) {
        super(mainActivity, tripComputer, parent);

        genericStatusPanel = new GenericStatusPanel(mainActivity, tripComputer, parent);

        buttonConnectSpartan = rootView.findViewById(R.id.buttonConnectSpartan);
        buttonConnectSpartan.setOnClickListener(view -> connectSpartan());

        buttonConnectObd = rootView.findViewById(R.id.buttonConnectObd);
        buttonConnectObd.setOnClickListener(view -> connectObd());
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void onStart(Context context) {
        mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter, Context.RECEIVER_EXPORTED);
        startBtService();
    }

    public void onResume(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter, Context.RECEIVER_EXPORTED);
        } else {
            mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter);
        }

        startBtService();
    }

    public void onDestroy(Context context) {
        disconnect();
    }

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int key = intent.getIntExtra(BluetoothStates.KEY_EVENT, -1);

            switch (key) {
                case BluetoothStates.EVENT_SERVICE_STATE_CHANGED:
                    OnBluetoothServiceStateChanged(intent.getIntExtra(BluetoothStates.KEY_DATA, -1));
                    break;

                case BluetoothStates.EVENT_BT_STATE_CHANGED:
                    OnBluetoothStateChanged(
                            intent.getIntExtra(BluetoothStates.KEY_DATA, -1),
                            intent.getStringExtra(BluetoothStates.KEY_DEVICE_ID)
                    );
                    break;

                case BluetoothStates.EVENT_DATA_RECEIVED:
                    OnBluetoothDataReceived(
                            intent.getStringExtra(BluetoothStates.KEY_DATA),
                            intent.getStringExtra(BluetoothStates.KEY_DEVICE_ID)
                    );
                    break;
            }
        }
    };

    public void OnBluetoothServiceStateChanged(int state) {
        switch (state) {
            case BluetoothStates.STATE_SERVICE_STARTED:
                onConnectionMessage("generic", "Bluetooth service started.");

                connectSpartanSoon();
                connectObdSoon();
                break;

            case BluetoothStates.STATE_SERVICE_STOPPED:
                onConnectionMessage("generic", "Bluetooth service stopped.");
                break;
        }
    }

    public void OnBluetoothDataReceived(String data, String device_id) {
        if (Objects.equals(device_id, "spartan")) {
            tripComputer.mSpartanStudio.onDataReceived(data);
            mainActivity.runOnUiThread(() -> onConnectionMessage("spartan", data));
        }

        if (Objects.equals(device_id, "obd")) {
            tripComputer.mObdStudio.onDataReceived(data);
            mainActivity.runOnUiThread(() -> onConnectionMessage("obd", data));
        }
    }

    public void OnBluetoothStateChanged(int state, String deviceId) {
        String stateLabel = BluetoothStates.labelOfState(state);

        if (deviceId == null) {
            handleGlobalBluetoothState(state, stateLabel);
        } else {
            handleDeviceBluetoothState(state, deviceId, stateLabel);
        }
    }

    private void handleGlobalBluetoothState(int state, String stateLabel) {
        onConnectionMessage("obd", stateLabel);
        onConnectionMessage("spartan", stateLabel);

        switch (state) {
            case BluetoothStates.STATE_BT_ENABLING:
                setConnectButtonsEnabled(false, "Connect");
                break;

            case BluetoothStates.STATE_BT_ENABLED:
                setConnectButtonsEnabled(true, "Connect");
                connectSpartanSoon();
                connectObdSoon();
                break;

            case BluetoothStates.STATE_BT_DISABLED:
                Permissions.promptEnableBluetooth(mainActivity);
                setConnectButtonsEnabled(false, "Connect");
                break;
        }
    }

    private void handleDeviceBluetoothState(int state, String deviceId, String stateLabel) {
        String deviceLabel = deviceId.toUpperCase();
        Button connectButton = getConnectButton(deviceId);
        Studio studio = getStudio(deviceId);

        onConnectionMessage(deviceId, stateLabel);
        Log.d("MainActivity (" + deviceId + ") bluetoothStateChanged:", stateLabel);

        switch (state) {
            case BluetoothStates.STATE_BT_CONNECTED:
                updateConnectButton(connectButton, "Connected", false);
                studio.start();
                break;

            case BluetoothStates.STATE_BT_BUSY:
            case BluetoothStates.STATE_BT_ENABLING:
                updateConnectButton(connectButton, "Connect", false);
                break;

            case BluetoothStates.STATE_BT_CONNECTING:
                updateConnectButton(connectButton, "Connecting", false);
                break;

            case BluetoothStates.STATE_BT_DISCONNECTED:
                updateConnectButton(connectButton, "Disconnected", true);
                reconnectDeviceSoon(deviceId);
                break;

            case BluetoothStates.STATE_BT_NONE:
            case BluetoothStates.STATE_BT_UNPAIRED:
            default:
                updateConnectButton(connectButton, "Connect", true);
                break;
        }
    }

    private void setConnectButtonsEnabled(boolean enabled, String label) {
        updateConnectButton(buttonConnectSpartan, label, enabled);
        updateConnectButton(buttonConnectObd, label, enabled);
    }

    private Button getConnectButton(String deviceId) {
        return deviceId.equals("spartan") ? buttonConnectSpartan : buttonConnectObd;
    }

    private Studio getStudio(String deviceId) {
        return deviceId.equals("spartan") ? tripComputer.mSpartanStudio : tripComputer.mObdStudio;
    }

    private void reconnectDeviceSoon(String deviceId) {
        if (deviceId.equals("spartan")) {
            connectSpartanSoon();
        } else if (deviceId.equals("obd")) {
            connectObdSoon();
        }
    }

    private void updateConnectButton(Button button, String text, boolean isEnabled) {
        button.setText(text);
        button.setEnabled(isEnabled);
    }

    public void connectSpartan() {
        BluetoothService.connect(mainActivity, "AutoLights", "spartan");
    }

    public void connectObd() {
        BluetoothService.connect(mainActivity, "OBDII", BluetoothUtils.OBD_UUID, "obd");
    }

    public void connectSpartanSoon() {
        Handler handler = new Handler();
        handler.postDelayed(this::connectSpartan, 2000);// Delay in milliseconds
    }


    public void connectObdSoon() {
        Handler handler = new Handler();
        handler.postDelayed(this::connectObd, 2000);// Delay in milliseconds
    }

    public void disconnect() {
        try {
            mainActivity.unregisterReceiver(btReceiver);
        } catch (IllegalArgumentException e) {
            Log.w("BTReceiver", "Receiver was already unregistered");
        }

        BluetoothService.disconnect(mainActivity, "obd");
        BluetoothService.disconnect(mainActivity, "spartan");
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public void startBtService() {
        if (!BluetoothService.isRunning(mainActivity)) {
            try {
                mainActivity.startService(new Intent(mainActivity, BluetoothService.class));
            } catch (BackgroundServiceStartNotAllowedException e) {
                Log.e("ConnectPanel", "Cant start bluetooth service, - not allowed!");
            }
        }
    }

    public void onConnectionMessage(String device, String message) {
        if (device.equals("spartan")) {
            genericStatusPanel.onSpartanUpdate(message);
        } else if (device.equals("obd")) {
            genericStatusPanel.onObdUpdate(message);
        }
    }

    @Override
    public void onGpsUpdate(Double speed, double distanceIncrement) {
        @SuppressLint("DefaultLocale") String message = String.format("Speed: %.1f km/h, Dist: %.1f m", speed, distanceIncrement);
        genericStatusPanel.onGenericUpdate(message);
    }

}

