package com.hondaafr;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.hondaafr.Libs.Bluetooth.BluetoothStates;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudioListener;
import com.hondaafr.Libs.Helpers.Permissions;
import com.hondaafr.Libs.Helpers.TimeChart;

public class MainActivity extends AppCompatActivity implements SpartanStudioListener {

    private MainActivity mContext;
    private Button buttonConnect;

    private SpartanStudio mSpartanStudio;
    private TextView mTextStatus;
    private Button buttonTrackSensor;
    private TextView mTextTargetAfr;
    private Button buttonDecreaseAfr;
    private Button buttonIncreaseAfr;

    private Double afrMin = null;
    private Double afrMax = null;

    private TimeChart mChart;

    private long startTimestamp = 0L;
    private Button mToggleClearAfrMin;
    private Button mToggleClearAfrMax;
    private Button mToggleClearAfrAll;
    private Button buttonClearLog;
    private Button buttonClearAmplitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        startTimestamp = System.currentTimeMillis();

        mContext = this;

        buttonConnect = findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(view -> {
            BT_connect();
        });

        buttonIncreaseAfr = findViewById(R.id.buttonIncreaseAFR);
        buttonIncreaseAfr.setOnClickListener(view -> {
            mSpartanStudio.adjustAFR(0.25);
        });

        buttonDecreaseAfr = findViewById(R.id.buttonDecreaseAFR);
        buttonDecreaseAfr.setOnClickListener(view -> {
            mSpartanStudio.adjustAFR(-0.25);
        });

        buttonTrackSensor = findViewById(R.id.buttonTrackSensor);
        buttonTrackSensor.setOnClickListener(view -> {
            mSpartanStudio.start();
        });

        buttonClearLog = findViewById(R.id.buttonClear);
        buttonClearLog.setOnClickListener(view -> {
            mChart.clearData();
            startTimestamp = System.currentTimeMillis();
        });

        buttonClearAmplitude = findViewById(R.id.buttonClearAmp);
        buttonClearAmplitude.setOnClickListener(view -> {
            afrMax = null;
            afrMin = null;
        });


        Permissions.askIgnoreBatteryOptimization(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Permissions.askBluetoothPermission(this);
        }

        mSpartanStudio = new SpartanStudio(this, this);
        mTextStatus = findViewById(R.id.textStatus);
        mTextTargetAfr = findViewById(R.id.textTargetAFR);
        mTextTargetAfr.setOnClickListener(v -> {
            SpartanStudio.requestCurrentAFR(mContext);
        });

        mToggleClearAfrMin = findViewById(R.id.buttonClearAfrMin);
        mToggleClearAfrMin.setOnClickListener(v -> afrMin = null);
        mToggleClearAfrAll = findViewById(R.id.buttonClearAfrAll);
        mToggleClearAfrAll.setOnClickListener(v -> {
            afrMin = null;
            afrMax = null;
        });
        mToggleClearAfrMax = findViewById(R.id.buttonClearAfrMax);
        mToggleClearAfrMax.setOnClickListener(v -> afrMax = null);

        // Keep the screen on while this activity is visible
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mChart = new TimeChart(this, findViewById(R.id.graph));
        mChart.init();
        mChart.invalidate();

        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleAfrGroup);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.buttonAfrSportPlus) {
                    mSpartanStudio.setAFR(12.7);
                } else if (checkedId == R.id.buttonAfrSport) {
                    mSpartanStudio.setAFR(14.7);
                } else if (checkedId == R.id.buttonAfrEco) {
                    mSpartanStudio.setAFR(15.4);
                } else if (checkedId == R.id.buttonAfrEcoPlus) {
                    mSpartanStudio.setAFR(16.4);
                }
            }
        });
    }

    IntentFilter intentFilter = new IntentFilter(BluetoothService.ACTION_UI_UPDATE);


    @Override
    public void onTargetAfrUpdated(double targetAfr) {
        runOnUiThread(() -> {
            // Format targetAfr to two decimal points
            @SuppressLint("DefaultLocale")
            String formattedTargetAfr = String.format("%.2f", targetAfr);

            // Set the formatted text to mTextTargetAfr
            mTextTargetAfr.setText(formattedTargetAfr);

            mChart.setLimitLines(null, null, (float) targetAfr);
        });
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorAfrReceived(Double afr) {
        runOnUiThread(() -> {
            // Format targetAfr to two decimal points
            @SuppressLint("DefaultLocale")
            String formattedTargetAfr = String.format("%.2f", afr);

            // Set the formatted text to mTextTargetAfr
            mToggleClearAfrAll.setText(formattedTargetAfr);
            float time = System.currentTimeMillis() - startTimestamp; // Cant use full timestamp, too big
            mChart.addToData(time, afr.floatValue(), true);

            if (afrMax == null || afr > afrMax) {
                afrMax = afr;
            }

            if (afrMin == null || afr < afrMin) {
                afrMin = afr;
            }

            mToggleClearAfrMin.setText(String.format("%.1f", afrMin));
            mToggleClearAfrMax.setText(String.format("%.1f", afrMax));
        });
    }

    @Override
    public void onSensorTempReceived(Double temp) {

    }

    public void BT_startService() {
        if (!BluetoothService.isRunning(this)) {
            startService(new Intent(this, BluetoothService.class));
        }
    }

    public void BT_connect() {
        BT_startService();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String ssid = settings.getString("pref_ssid", "AutoLights");
        BluetoothService.connect(this, "AutoLights");
    }

    private BroadcastReceiver btReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            UI_processIntent(intent);
        }
    };

    public void UI_processIntent(Intent intent) {
        final int key = intent.getIntExtra(BluetoothStates.KEY_EVENT, -1);

        switch (key) {
            case BluetoothStates.EVENT_SERVICE_STATE_CHANGED:
                OnBluetoothServiceStateChanged(intent.getIntExtra(BluetoothStates.KEY_DATA, -1));
                break;

            case BluetoothStates.EVENT_BT_STATE_CHANGED:
                OnBluetoothStateChanged(intent.getIntExtra(BluetoothStates.KEY_DATA, -1));
                break;

            case BluetoothStates.EVENT_DATA_RECEIVED:
                OnBluetoothDataReceived(intent.getStringExtra(BluetoothStates.KEY_DATA));
                break;
        }
    }

    public void OnBluetoothDataReceived(String data) {
        mSpartanStudio.onDataReceived(data);

        runOnUiThread(() -> {
            mTextStatus.setText(data);
        });
    }

    public void OnBluetoothServiceStateChanged(int state) {
        switch (state) {
            case BluetoothStates.STATE_SERVICE_STARTED:
                mTextStatus.setText("Bluetooth service started.");
                break;

            case BluetoothStates.STATE_SERVICE_STOPPED:
                mTextStatus.setText("Bluetooth service stopped.");
                break;
        }
    }

    public void OnBluetoothStateChanged(int state) {
        switch (state) {
            case BluetoothStates.STATE_BT_CONNECTED:
                buttonConnect.setText("Connected");
                buttonConnect.setEnabled(false);
                mTextStatus.setText("Successfully connected to device!");
                break;

            case BluetoothStates.STATE_BT_BUSY:
            case BluetoothStates.STATE_BT_CONNECTING:
            case BluetoothStates.STATE_BT_ENABLING:
                buttonConnect.setText("Connecting");
                buttonConnect.setEnabled(false);
                break;

            case BluetoothStates.STATE_BT_NONE:
            case BluetoothStates.STATE_BT_DISCONNECTED:
            case BluetoothStates.STATE_BT_UNPAIRED:
                buttonConnect.setText("Connect");
                buttonConnect.setEnabled(true);
                break;

            case BluetoothStates.STATE_BT_DISABLED:
                Permissions.promptEnableBluetooth(this);
                buttonConnect.setEnabled(true);


            default:
                buttonConnect.setText("Connect");
                buttonConnect.setEnabled(true);

        }

        mTextStatus.setText(BluetoothStates.labelOfState(state));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
//            mSpectrumVisualiser = new SpectrumVisualiser((ImageView) this.findViewById(R.id.imageSpectrum));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(btReceiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onStart() {
        super.onStart();
        registerReceiver(btReceiver, intentFilter, Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onResume() {
        super.onResume();

        BT_startService();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}