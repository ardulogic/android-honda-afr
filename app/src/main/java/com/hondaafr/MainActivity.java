package com.hondaafr;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
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
import com.hondaafr.Libs.Bluetooth.BluetoothUtils;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Obd.ObdStudio;
import com.hondaafr.Libs.Devices.Obd.ObdStudioListener;
import com.hondaafr.Libs.Devices.Phone.GpsSpeed;
import com.hondaafr.Libs.Devices.Phone.GpsSpeedListener;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudioListener;
import com.hondaafr.Libs.Helpers.AverageList;
import com.hondaafr.Libs.Helpers.DataLog;
import com.hondaafr.Libs.Helpers.DataLogEntry;
import com.hondaafr.Libs.Helpers.Permissions;
import com.hondaafr.Libs.Helpers.TimeChart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SpartanStudioListener, ObdStudioListener, GpsSpeedListener {

    private MainActivity mContext;
    private Button buttonConnectSpartan;
    private Button buttonConnectObd;

    private SpartanStudio mSpartanStudio;
    private ObdStudio mObdStudio;

    private GpsSpeed mGpsSpeed;

    private DataLog mDataLog;
    private TextView mTextStatusSpartan;
    private TextView mTextStatusObd;
    private TextView mTextStatusGeneric;
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
    private Button buttonRecord;
    private TextView mTextSpeed;

    private Double sportPlusAfr = 12.7;
    private Double sportAfr = 14.7;
    private Double ecoAfr = 15.4;
    private Double ecoPlusAfr = 16.4;
    private boolean recording = false;

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

        Permissions.askForAllPermissions(this);

        startTimestamp = System.currentTimeMillis();

        mContext = this;

        buttonConnectSpartan = findViewById(R.id.buttonConnectSpartan);
//        buttonConnect.setOnClickListener(view -> BT_connect_spartan());
        buttonConnectSpartan.setOnClickListener(view -> BT_connect_spartan());

        buttonConnectObd = findViewById(R.id.buttonConnectObd);
//        buttonConnect.setOnClickListener(view -> BT_connect_spartan());
        buttonConnectObd.setOnClickListener(view -> BT_connect_obd());

        buttonIncreaseAfr = findViewById(R.id.buttonIncreaseAFR);
        buttonIncreaseAfr.setOnClickListener(view -> mSpartanStudio.adjustAFR(0.05));

        buttonDecreaseAfr = findViewById(R.id.buttonDecreaseAFR);
        buttonDecreaseAfr.setOnClickListener(view -> mSpartanStudio.adjustAFR(-0.05));

        buttonTrackSensor = findViewById(R.id.buttonTrackSensor);
        buttonTrackSensor.setOnClickListener(view -> mSpartanStudio.start());

        buttonClearLog = findViewById(R.id.buttonClear);
        buttonClearLog.setOnClickListener(view -> {
            mChart.clearData();
            mDataLog.clearAllEntries();
            startTimestamp = System.currentTimeMillis();
        });

        buttonRecord = findViewById(R.id.buttonRecord);
        buttonRecord.setOnClickListener(view -> {
            if (recording) {
                mDataLog.saveAsCsv();
                recording = false;
                buttonRecord.setText("Record");
            } else {
                mDataLog.clearAllEntries();
                recording = true;
                buttonRecord.setText("Stop");
            }
        });


        ArrayList<String> obdPids = new ArrayList<>();
        obdPids.add("tps");
        obdPids.add("map");
        obdPids.add("stft");

        mSpartanStudio = new SpartanStudio(this, this);
        mObdStudio = new ObdStudio(this, obdPids, this);
        mDataLog = new DataLog(this);
        mGpsSpeed = new GpsSpeed(this, this);

        mTextStatusGeneric = findViewById(R.id.textStatusGeneric);
        mTextStatusSpartan = findViewById(R.id.textStatusSpartan);
        mTextStatusObd = findViewById(R.id.textStatusObd);

        mTextTargetAfr = findViewById(R.id.textTargetAFR);
        mTextTargetAfr.setOnClickListener(v -> SpartanStudio.requestCurrentAFR(mContext));

        mTextSpeed = findViewById(R.id.textSpeed);
        setObdOnClickListeners();

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
        handler.postDelayed(this::BT_connect_spartan, 2000);// Delay in milliseconds
        handler.postDelayed(this::BT_connect_obd, 2000);// Delay in milliseconds


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

            addEntryToShortAfr(targetAfr);
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

            if (recording) {
                logReadings();
            }
        });
    }

    private void logReadings() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.putAll(mSpartanStudio.getReadings());
        values.putAll(mObdStudio.getReadings());
        values.putAll(mGpsSpeed.getReadings());

        mDataLog.addEntry(new DataLogEntry(values));
    }

    @Override
    public void onSensorTempReceived(Double temp) {

    }

    @Override
    public void onObdReadingUpdate(ObdReading reading) {
        final String name = reading.getName();

        switch (name) {
            case "ect":
                setObdReadingText(R.id.textEct, reading);
                break;
            case "tps":
                setObdReadingText(R.id.textTps, reading);
                break;
            case "map":
                setObdReadingText(R.id.textMap, reading);
                break;
            case "iat":
                setObdReadingText(R.id.textIat, reading);
                break;
            case "stft":
                setObdReadingText(R.id.textStft, reading);
                break;
            case "speed":
                setObdReadingText(R.id.textSpeed, reading);
                break;
            case "rpm":
                setObdReadingText(R.id.textRpm, reading);
                break;
            case "ltft":
                setObdReadingText(R.id.textLtft, reading);
                break;
            case "uo2v":
                setObdReadingText(R.id.textLambdaVoltage, reading);
                break;
        }
    }

    private void setObdReadingText(int textViewId, ObdReading reading) {
        TextView textView = findViewById(textViewId);
        textView.setText(reading.getDisplayValue());
        updateToggleAppearance(textView, true);
    }

    private void setObdOnClickListeners() {
        initObdButton(R.id.textIat, "iat");
        initObdButton(R.id.textStft, "stft");
        initObdButton(R.id.textMap, "map");
        initObdButton(R.id.textTps, "tps");
        initObdButton(R.id.textEct, "ect");
        initObdButton(R.id.textSpeed, "speed");
        initObdButton(R.id.textLtft, "ltft");
        initObdButton(R.id.textRpm, "rpm");
        initObdButton(R.id.textLambdaVoltage, "uo2v");
    }

    private void initObdButton(int textViewId, String reading_name) {
        TextView textView = findViewById(textViewId);
        updateToggleAppearance(textView, mObdStudio.readings.isActive(reading_name));

        textView.setOnClickListener(v -> {
            boolean isActive = mObdStudio.readings.toggleActive(reading_name);
            updateToggleAppearance(textView, isActive);
        });
    }

    private void updateToggleAppearance(TextView textView, boolean isActive) {
        textView.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
    }


    @Override
    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    public void onGpsSpeedUpdated(Double speedKmh) {
        String s = String.format("%.1f km/h", speedKmh);
        mTextStatusGeneric.setText("GPS speed: " + s);

        if (!mObdStudio.readings.isActive("speed")) {
            mTextSpeed.setText(s);
        }
    }


    public void BT_startService() {
        if (!BluetoothService.isRunning(this)) {
            startService(new Intent(this, BluetoothService.class));
        }
    }

    public void BT_connect_spartan() {
        BT_startService();
        BluetoothService.connect(this, "AutoLights", "spartan");
    }

    public void BT_connect_obd() {
        BT_startService();
        BluetoothService.connect(this, "OBDII", BluetoothUtils.OBD_UUID, "obd");
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

    public void OnBluetoothDataReceived(String data, String device_id) {
        if (Objects.equals(device_id, "spartan")) {
            mSpartanStudio.onDataReceived(data);
            runOnUiThread(() -> mTextStatusSpartan.setText(data));
        }

        if (Objects.equals(device_id, "obd")) {
            mObdStudio.onDataReceived(data);
            runOnUiThread(() -> mTextStatusObd.setText(data));
        }
    }

    public void OnBluetoothServiceStateChanged(int state) {
        switch (state) {
            case BluetoothStates.STATE_SERVICE_STARTED:
                mTextStatusGeneric.setText("Bluetooth service started.");
                break;

            case BluetoothStates.STATE_SERVICE_STOPPED:
                mTextStatusGeneric.setText("Bluetooth service stopped.");
                break;
        }
    }

    public void OnBluetoothStateChanged(int state, String device_id) {
        Log.d("MainActivity bluetoothStateChanged:", device_id);
        switch (state) {
            case BluetoothStates.STATE_BT_CONNECTED:
                if (device_id.equals("spartan")) {
                    mTextStatusSpartan.setText("Connected");
                    buttonConnectSpartan.setText("Connected");
                    buttonConnectSpartan.setEnabled(false);
                    mSpartanStudio.start();
                }

                if (device_id.equals("obd")) {
                    mTextStatusObd.setText("Connected");
                    buttonConnectObd.setText("Connected");
                    buttonConnectObd.setEnabled(false);
                    mObdStudio.start();
                }
                break;

            case BluetoothStates.STATE_BT_BUSY:
            case BluetoothStates.STATE_BT_CONNECTING:
            case BluetoothStates.STATE_BT_ENABLING:
                if (device_id.equals("spartan")) {
                    buttonConnectSpartan.setText("Connecting");
                    buttonConnectSpartan.setEnabled(false);
                }

                if (device_id.equals("obd")) {
                    buttonConnectObd.setText("Connecting");
                    buttonConnectObd.setEnabled(false);
                }
                break;

            case BluetoothStates.STATE_BT_NONE:
            case BluetoothStates.STATE_BT_DISCONNECTED:
            case BluetoothStates.STATE_BT_UNPAIRED:
                if (device_id.equals("spartan")) {
                    buttonConnectSpartan.setText("Connect");
                    buttonConnectSpartan.setEnabled(true);
                }

                if (device_id.equals("obd")) {
                    buttonConnectObd.setText("Connect");
                    buttonConnectObd.setEnabled(true);
                }
                break;

            case BluetoothStates.STATE_BT_DISABLED:
                Permissions.promptEnableBluetooth(this);
                buttonConnectSpartan.setEnabled(true);
                buttonConnectObd.setEnabled(true);
                break;

            case BluetoothStates.STATE_BT_ENABLED:
                BT_connect_spartan();

            default:
                buttonConnectObd.setText("Connect");
                buttonConnectObd.setEnabled(true);
                buttonConnectSpartan.setText("Connect");
                buttonConnectSpartan.setEnabled(true);


        }

        if (device_id.equals("obd")) {
            mTextStatusObd.setText(BluetoothStates.labelOfState(state));
        } else if (device_id.equals("spartan")) {
            mTextStatusSpartan.setText(BluetoothStates.labelOfState(state));
        } else {
            mTextStatusGeneric.setText(BluetoothStates.labelOfState(state));
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

        Handler handler = new Handler();
        handler.postDelayed(this::BT_connect_spartan, 2000);// Delay in milliseconds
        handler.postDelayed(this::BT_connect_obd, 2000);// Delay in milliseconds
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, do the location-related task.
            // You can use the location now.
        } else {
            Log.e("Permissions", "Permission denied for : " + requestCode);
        }
    }


}


