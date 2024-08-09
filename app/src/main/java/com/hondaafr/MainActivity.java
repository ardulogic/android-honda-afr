package com.hondaafr;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import com.hondaafr.Libs.Helpers.AverageList;
import com.hondaafr.Libs.Helpers.Permissions;
import com.hondaafr.Libs.Helpers.TimeChart;

import java.util.HashMap;
import java.util.Map;

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

    private Double sportPlusAfr = 12.7;
    private Double sportAfr = 14.7;
    private Double ecoAfr = 15.4;
    private Double ecoPlusAfr = 16.4;

    private final AverageList shortAfrAvg = new AverageList(100);

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private TextView textShortAfrAvg;
    private TextView textShortAfrAvgDeviation;

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

        loadSettings();

        startTimestamp = System.currentTimeMillis();

        mContext = this;

        buttonConnect = findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(view -> BT_connect());

        buttonIncreaseAfr = findViewById(R.id.buttonIncreaseAFR);
        buttonIncreaseAfr.setOnClickListener(view -> mSpartanStudio.adjustAFR(0.05));

        buttonDecreaseAfr = findViewById(R.id.buttonDecreaseAFR);
        buttonDecreaseAfr.setOnClickListener(view -> mSpartanStudio.adjustAFR(-0.05));

        buttonTrackSensor = findViewById(R.id.buttonTrackSensor);
        buttonTrackSensor.setOnClickListener(view -> mSpartanStudio.start());

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
        mTextTargetAfr.setOnClickListener(v -> SpartanStudio.requestCurrentAFR(mContext));

        mToggleClearAfrMin = findViewById(R.id.buttonClearAfrMin);
        mToggleClearAfrMin.setOnClickListener(v -> afrMin = null);
        mToggleClearAfrAll = findViewById(R.id.buttonClearAfrAll);
        mToggleClearAfrAll.setOnClickListener(v -> {
            afrMin = null;
            afrMax = null;
            shortAfrAvg.clear();
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
                    mSpartanStudio.setAFR(sportPlusAfr);
                } else if (checkedId == R.id.buttonAfrSport) {
                    mSpartanStudio.setAFR(sportAfr);
                } else if (checkedId == R.id.buttonAfrEco) {
                    mSpartanStudio.setAFR(ecoAfr);
                } else if (checkedId == R.id.buttonAfrEcoPlus) {
                    mSpartanStudio.setAFR(ecoPlusAfr);
                }
            }
        });

        setLongClickListenersToAfrButtons();

        Handler handler = new Handler();
        handler.postDelayed(this::BT_connect, 2000);// Delay in milliseconds

        textShortAfrAvg = findViewById(R.id.textRecentAfrAvg);
        textShortAfrAvgDeviation = findViewById(R.id.textShortAfrAvgDeviation);
    }

    IntentFilter intentFilter = new IntentFilter(BluetoothService.ACTION_UI_UPDATE);

    public void loadSettings() {
        if (settings == null) {
            settings = PreferenceManager.getDefaultSharedPreferences(this);
            editor = settings.edit();
        }

        sportPlusAfr = Double.parseDouble(settings.getString("sportPlusAfr", "12.7"));
        sportAfr = Double.parseDouble(settings.getString("sportAfr", "14.7"));
        ecoAfr = Double.parseDouble(settings.getString("ecoAfr", "15.4"));
        ecoPlusAfr = Double.parseDouble(settings.getString("ecoPlusAfr", "16.4"));
    }

    public void setSetting(String key, String value) {
        editor.putString(key, value);
        editor.apply();

        loadSettings(); // So the variables are reloaded
    }

    public void setLongClickListenersToAfrButtons() {
        View.OnLongClickListener longClickListener = v -> {
            int id = v.getId();

            // Map button IDs to their corresponding modes
            Map<Integer, String> buttonModeMap = new HashMap<>();
            buttonModeMap.put(R.id.buttonAfrSportPlus, "sportPlusAfr");
            buttonModeMap.put(R.id.buttonAfrSport, "sportAfr");
            buttonModeMap.put(R.id.buttonAfrEco, "ecoAfr");
            buttonModeMap.put(R.id.buttonAfrEcoPlus, "ecoPlusAfr");

            // Get the mode based on the button ID
            String mode = buttonModeMap.get(id);

            if (mode != null) {
                Log.d("Mode saved", mode + ":" + mSpartanStudio.targetAfr);
                setSetting(mode, String.valueOf(mSpartanStudio.targetAfr));
                Toast.makeText(v.getContext(), "Saved", Toast.LENGTH_SHORT).show();
            }

            return true; // Return true to indicate the long press was handled
        };

        View buttonAfrSportPlus = findViewById(R.id.buttonAfrSportPlus);
        View buttonAfrSport = findViewById(R.id.buttonAfrSport);
        View buttonAfrEco = findViewById(R.id.buttonAfrEco);
        View buttonAfrEcoPlus = findViewById(R.id.buttonAfrEcoPlus);
        buttonAfrSportPlus.setOnLongClickListener(longClickListener);
        buttonAfrSport.setOnLongClickListener(longClickListener);
        buttonAfrEco.setOnLongClickListener(longClickListener);
        buttonAfrEcoPlus.setOnLongClickListener(longClickListener);
    }

    @Override
    public void onTargetAfrUpdated(double targetAfr) {
        runOnUiThread(() -> {
            // Format targetAfr to two decimal points
            @SuppressLint("DefaultLocale")
            String formattedTargetAfr = String.format("%.2f", targetAfr);

            // Set the formatted text to mTextTargetAfr
            mTextTargetAfr.setText(formattedTargetAfr);

            mChart.setLimitLines(null, null, (float) targetAfr);

//            addEntryToShortAfr(targetAfr);
        });
    }

    @SuppressLint("DefaultLocale")
    public void addEntryToShortAfr(Double value) {
        shortAfrAvg.addNumber(value);
        textShortAfrAvg.setText(String.format("%.2f", shortAfrAvg.getAvg()));

        textShortAfrAvgDeviation.setText(String.format("%.2f", shortAfrAvg.getAverageDistanceFromTarget(mSpartanStudio.targetAfr)));
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

            addEntryToShortAfr(afr);
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
        BluetoothService.connect(this, "AutoLights");
    }

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {

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

        runOnUiThread(() -> mTextStatus.setText(data));
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
                mSpartanStudio.start();
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
                break;

            case BluetoothStates.STATE_BT_ENABLED:
                BT_connect();

            default:
                buttonConnect.setText("Connect");
                buttonConnect.setEnabled(true);

        }

        mTextStatus.setText(BluetoothStates.labelOfState(state));
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