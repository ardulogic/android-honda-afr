package com.hondaafr.Libs.UI.Scientific;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;

import com.hondaafr.Libs.Bluetooth.BluetoothStates;
import com.hondaafr.Libs.Bluetooth.BluetoothUtils;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Helpers.Permissions;
import com.hondaafr.Libs.Helpers.Studio;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import java.util.Objects;

public class ConnectPanel {

    private final LinearLayout panel;
    private final TripComputer mTripComputer;
    private final MainActivity mainActivity;
    private final Context context;
    private final GenericStatusPanel genericStatusPanel;

    private Button buttonConnectSpartan;
    private Button buttonConnectObd;

    IntentFilter btUiUpdateIntentFilter = new IntentFilter(BluetoothService.ACTION_UI_UPDATE);

    public ConnectPanel(MainActivity mainActivity, TripComputer mTripComputer) {
        this.mTripComputer = mTripComputer;
        this.mainActivity = mainActivity;
        this.context = mainActivity;

        panel = mainActivity.findViewById(R.id.layoutConnection);
        genericStatusPanel = new GenericStatusPanel(mainActivity);

        buttonConnectSpartan = mainActivity.findViewById(R.id.buttonConnectSpartan);
        buttonConnectSpartan.setOnClickListener(view -> connectSpartan());

        buttonConnectObd = mainActivity.findViewById(R.id.buttonConnectObd);
        buttonConnectObd.setOnClickListener(view -> connectObd());

    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void onStart() {
        mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter, Context.RECEIVER_EXPORTED);
        startBtService();
    }

    public void onResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter, Context.RECEIVER_EXPORTED);
        } else {
            mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter);
        }

        startBtService();
    }

    public void onStop() {

    }
    public void onDestroy() {
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
            mTripComputer.mSpartanStudio.onDataReceived(data);
            mainActivity.runOnUiThread(() -> onConnectionMessage("spartan", data));
        }

        if (Objects.equals(device_id, "obd")) {
            mTripComputer.mObdStudio.onDataReceived(data);
            mainActivity.runOnUiThread(() -> onConnectionMessage("obd", data));
        }
    }

    public void OnBluetoothStateChanged(int state, String device_id) {
        if (device_id != null) {
            String stateLabel =  BluetoothStates.labelOfState(state);
            String deviceLabel = device_id.toUpperCase();
            Button connectButton = device_id.equals("spartan") ? buttonConnectSpartan : buttonConnectObd;
            Studio studio =  device_id.equals("spartan") ? mTripComputer.mSpartanStudio : mTripComputer.mObdStudio;

            onConnectionMessage(device_id, stateLabel);
            Log.d("MainActivity (" + device_id + ") bluetoothStateChanged:", stateLabel);

            switch (state) {
                case BluetoothStates.STATE_BT_CONNECTED:
                    updateConnectButton(connectButton, stateLabel, false);
                    studio.start();
                    break;

                case BluetoothStates.STATE_BT_BUSY:
                case BluetoothStates.STATE_BT_CONNECTING:
                case BluetoothStates.STATE_BT_ENABLING:
                    updateConnectButton(connectButton, stateLabel, false);
                    break;

                case BluetoothStates.STATE_BT_DISCONNECTED:
                    updateConnectButton(connectButton, stateLabel, true);

                    if (device_id.equals("spartan")) {
                        connectSpartanSoon();
                    } else if (device_id.equals("obd")) {
                        connectObdSoon();
                    }

                    break;

                case BluetoothStates.STATE_BT_NONE:
                case BluetoothStates.STATE_BT_UNPAIRED:
                    updateConnectButton(connectButton, "Connect " + deviceLabel, true);
                    break;

                case BluetoothStates.STATE_BT_DISABLED:
                    Permissions.promptEnableBluetooth(mainActivity);
                    updateConnectButton(connectButton, "Connect " + deviceLabel, false);
                    break;

                default:
                    updateConnectButton(connectButton, "Connect " + deviceLabel, true);
            }
        }

    }

    private void updateConnectButton(Button button, String text, boolean isEnabled) {
        button.setText(text);
        button.setEnabled(isEnabled);
    }

    public void connectSpartan() {
        BluetoothService.connect(context, "AutoLights", "spartan");
    }

    public void connectObd() {
        BluetoothService.connect(context, "OBDII", BluetoothUtils.OBD_UUID, "obd");
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

        BluetoothService.disconnect(context, "obd");
        BluetoothService.disconnect(context, "spartan");
    }

    public void startBtService() {
        if (!BluetoothService.isRunning(context)) {
            mainActivity.startService(new Intent(context, BluetoothService.class));
        }
    }

    public void onConnectionMessage(String device, String message) {
        if (device.equals("spartan")) {
            genericStatusPanel.onSpartanUpdate(message);
        } else if (device.equals("obd")) {
            genericStatusPanel.onObdUpdate(message);
        }
    }


    public void onGpsUpdate(Double speed, double distanceIncrement) {
        @SuppressLint("DefaultLocale") String message = String.format("Speed: %.1f km/h, Dist: %.1f m", speed, distanceIncrement);
        genericStatusPanel.onGenericUpdate(message);
    }
}
