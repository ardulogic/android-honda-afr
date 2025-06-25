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
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.hondaafr.Libs.Bluetooth.BluetoothStates;
import com.hondaafr.Libs.Bluetooth.BluetoothUtils;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Obd.ObdStudio;
import com.hondaafr.Libs.Devices.Obd.ObdStudioListener;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudioListener;
import com.hondaafr.Libs.EngineSound.EngineSound;
import com.hondaafr.Libs.Helpers.Cluster;
import com.hondaafr.Libs.Helpers.DataLog;
import com.hondaafr.Libs.Helpers.DataLogEntry;
import com.hondaafr.Libs.Helpers.Permissions;
import com.hondaafr.Libs.Helpers.TimeChart;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.ImageButtonRounded;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SpartanStudioListener, ObdStudioListener, TripComputerListener {
    private static final String TAG = "MainActivity";
    private MainActivity mContext;
    private Button buttonConnectSpartan;
    private Button buttonConnectObd;

    private SpartanStudio mSpartanStudio;
    private ObdStudio mObdStudio;
    private final Map<String, TextView> obdButtons = new HashMap<>();
    private DataLog mDataLog;
    private TextView mTextStatusSpartan;
    private TextView mTextStatusObd;
    private TextView mTextStatusGeneric;
    private Button buttonTrackSensor;
    private TextView mTextTargetAfr;
    private Button buttonDecreaseAfr;
    private Button buttonIncreaseAfr;
    private TimeChart mChart;

    private long startTimestamp = 0L;
    private Button mToggleClearAfrMin;
    private Button mToggleClearAfrMax;
    private Button mToggleClearAfrAll;
    private Button buttonClearLog;

    private Button buttonRecord;
    private ImageButton buttonToggleCluster;
    private ImageButtonRounded mToggleFuelCons;

    private ImageButtonRounded mToggleEngineSounds;
    private TextView mTextSpeed;
    private TextView mTextSpeedSource;

    private Double sportPlusAfr = 12.7;
    private Double sportAfr = 14.7;
    private Double ecoAfr = 15.4;
    private Double ecoPlusAfr = 16.4;
    private boolean recording = false;

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private TextView textMeasurementBig;
    private TextView textMeasurementSmall;

    private TextView textTotalDistance;
    private TextView textTotalConsLiters;
    private TextView textTotalConsPer100km;

    private MainViewModel viewModel;

    private EngineSound mEngineSound;
    private TripComputer mTripComputer;
    private TextView textTotalInfo;
    private Cluster mCluster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutScientific), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadSettings();

        Permissions.askForAllPermissions(this);

        startTimestamp = System.currentTimeMillis();

        mContext = this;

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        buttonConnectSpartan = findViewById(R.id.buttonConnectSpartan);
        buttonConnectSpartan.setOnClickListener(view -> BT_connect_spartan());

        buttonConnectObd = findViewById(R.id.buttonConnectObd);
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


        mSpartanStudio = new SpartanStudio(this, this);
        mObdStudio = new ObdStudio(this, this);
        mDataLog = new DataLog(this);

        mTextStatusGeneric = findViewById(R.id.textStatusGeneric);
        mTextStatusSpartan = findViewById(R.id.textStatusSpartan);
        mTextStatusObd = findViewById(R.id.textStatusObd);

        mTextTargetAfr = findViewById(R.id.textTargetAFR);
        mTextTargetAfr.setOnClickListener(v -> SpartanStudio.requestCurrentAFR(mContext));

        mTextSpeed = findViewById(R.id.textSpeed);
        mTextSpeedSource = findViewById(R.id.textSpeedSource);
        setObdOnClickListeners();

        mToggleClearAfrMin = findViewById(R.id.buttonClearAfrMin);
        mToggleClearAfrMin.setOnClickListener(v -> mTripComputer.afrHistory.clearMin());

        mToggleClearAfrAll = findViewById(R.id.buttonClearAfrAll);
        mToggleClearAfrAll.setOnClickListener(v -> mTripComputer.afrHistory.clear());

        mToggleClearAfrMax = findViewById(R.id.buttonClearAfrMax);
        mToggleClearAfrMax.setOnClickListener(v -> mTripComputer.afrHistory.clearMax());

        mToggleFuelCons = findViewById(R.id.buttonToggleShowGasConsumption);
        mToggleFuelCons.setOnClickListener(new ButtonShowFuelConsOnClickListener());

        mToggleEngineSounds = findViewById(R.id.buttonToggleSound);
        mToggleEngineSounds.setOnClickListener(new ButtonToggleSoundOnClickListener());

        // Keep the screen on while this activity is visible
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mChart = new TimeChart(this, findViewById(R.id.graph));
        mChart.init();
        mChart.invalidate();

        ((SeekBar) findViewById(R.id.seekTps)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mEngineSound.setTargetTPS(seekBar.getProgress());

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        ((SeekBar) findViewById(R.id.seekRev)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mEngineSound.setTargetRpm((int) (6000 * ((float) seekBar.getProgress() / 100)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        ((SeekBar) findViewById(R.id.seekSmooth)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mEngineSound.setTargetSmoothness(seekBar.getProgress());

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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

        textMeasurementBig = findViewById(R.id.textMeasurementBig);
        textMeasurementSmall = findViewById(R.id.textMeasurementSmall);

        textTotalDistance = findViewById(R.id.textOdometer);
        textTotalDistance.setOnClickListener(v -> {
            viewModel.showTotalFuelConsumption.postValue(Boolean.FALSE.equals(viewModel.showTotalFuelConsumption.getValue()));
            updateDisplayedReadings();
        });
        textTotalDistance.setOnLongClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.showTotalFuelConsumption.getValue())) {
                mTripComputer.resetTotals();
            } else {
                mTripComputer.resetTrip();
            }

            updateDisplayedReadings();

            return true;
        });

        textTotalInfo = findViewById(R.id.textTotalInfo);
        textTotalConsLiters = findViewById(R.id.textTotalLiters);
        textTotalConsPer100km = findViewById(R.id.textTotalLiters100km);

        viewModel.showFuelConsumption.observe(this, show -> {
            findViewById(R.id.layoutTrip).setVisibility(show ? View.VISIBLE : View.GONE);
            mToggleFuelCons.setState(show);

            updateDisplayedReadings();
        });

        viewModel.showTotalFuelConsumption.observe(this, showTotals -> {
            int color = ContextCompat.getColor(mContext, showTotals ? R.color.black : R.color.orange);
            textTotalDistance.setTextColor(color);
            updateDisplayedReadings();
        });

        viewModel.engineSoundsEnabled.observe(this, show -> {
            findViewById(R.id.layoutSound).setVisibility(show ? View.VISIBLE : View.GONE);
            mToggleEngineSounds.setState(show);
            mEngineSound.setState(show);
        });

        viewModel.fuelConsumptionAvailable.observe(this, fuelAvailable -> {
            mToggleFuelCons.setIconState(fuelAvailable);
        });

        mEngineSound = new EngineSound();
        mEngineSound.init(this);

        mTripComputer = new TripComputer(this, mObdStudio, mSpartanStudio, this);
        mTripComputer.init();

        buttonToggleCluster = findViewById(R.id.buttonToggleCluster);
        buttonToggleCluster.setOnClickListener(v -> viewModel.showCluster.postValue(Boolean.FALSE.equals(viewModel.showCluster.getValue())));
        findViewById(R.id.layoutCluster).setOnLongClickListener(v -> {
            viewModel.showCluster.postValue(Boolean.FALSE.equals(viewModel.showCluster.getValue()));

            return false;
        });

        viewModel.showCluster.observe(this, show -> {
            findViewById(R.id.layoutScientific).setVisibility(!show ? View.VISIBLE : View.GONE);
            findViewById(R.id.layoutCluster).setVisibility(show ? View.VISIBLE : View.GONE);

            if (show && !viewModel.showFuelConsumption.getValue()) {
                mTripComputer.setObdForFuelConsumption(true);
                viewModel.showFuelConsumption.postValue(true);
            }
        });


        mCluster = new Cluster(this, mTripComputer);
        viewModel.showCluster.postValue(true);
        BT_startService();
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

            mTripComputer.tick();

            if (recording) {
                logReadings();
            }
        });
    }

    private void logReadings() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.putAll(mSpartanStudio.getReadingsAsString());
        values.putAll(mObdStudio.getReadingsAsString());

        values.putAll(mTripComputer.gps.getReadingsAsString());

        mDataLog.addEntry(new DataLogEntry(values));
    }

    @Override
    public void onSensorTempReceived(Double temp) {

    }

    @Override
    public void onObdConnectionError(String s) {
        mCluster.onDataUpdated();
    }

    @Override
    public void onObdConnectionActive() {
        mCluster.onDataUpdated();
    }

    @Override
    public void onObdConnectionLost() {
        mCluster.onDataUpdated();
    }

    @Override
    public void onObdReadingUpdate(ObdReading reading) {
        final String name = reading.getMachineName();

        switch (name) {
            case "ect":
                setObdReadingText(R.id.textEct, reading);
                break;
            case "tps":
                setObdReadingText(R.id.textTps, reading);
                mEngineSound.setTargetTPS(((Double) reading.getValue()).intValue());
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
                if (!mTripComputer.isGpsSpeedUsed()) {
                    mTextSpeedSource.setText("OBD");
                    setObdReadingText(R.id.textSpeed, reading);
                }
                break;
            case "rpm":
                setObdReadingText(R.id.textRpm, reading);
                mEngineSound.setTargetRpm((Integer) reading.getValue());
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
        obdButtons.put(reading_name, textView);
        updateToggleAppearance(textView, mObdStudio.readings.isActive(reading_name));

        textView.setOnClickListener(v -> {
            boolean isActive = mObdStudio.readings.toggleActive(reading_name);
            updateToggleAppearance(textView, isActive);

            if (!mObdStudio.readingsForFuelAreActive()) {
                viewModel.showFuelConsumption.postValue(false);
                viewModel.fuelConsumptionAvailable.postValue(false);
            }
        });
    }

    private void updateObdButtonsAppearance() {
        for (Map.Entry<String, TextView> entry : obdButtons.entrySet()) {
            boolean buttonIsActive = mObdStudio.readings.active.containsKey(entry.getKey());
            updateToggleAppearance(entry.getValue(), buttonIsActive);
        }
    }

    private void updateToggleAppearance(TextView textView, boolean isActive) {
        textView.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
    }

    public void BT_startService() {
        if (!BluetoothService.isRunning(this)) {
            startService(new Intent(this, BluetoothService.class));
        }
    }

    public void BT_connect_spartan() {
        BluetoothService.connect(this, "AutoLights", "spartan");
    }

    public void BT_connect_spartan_soon() {
        Handler handler = new Handler();
        handler.postDelayed(this::BT_connect_spartan, 2000);// Delay in milliseconds
    }

    public void BT_connect_obd() {
        BluetoothService.connect(this, "OBDII", BluetoothUtils.OBD_UUID, "obd");
    }

    public void BT_connect_obd_soon() {
        Handler handler = new Handler();
        handler.postDelayed(this::BT_connect_obd, 2000);// Delay in milliseconds
    }

    public void BT_disconnect() {
        try {
            unregisterReceiver(btReceiver);
        } catch (IllegalArgumentException e) {
            Log.w("BTReceiver", "Receiver was already unregistered");
        }

        BluetoothService.disconnect(this, "obd");
        BluetoothService.disconnect(this, "spartan");
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

                BT_connect_spartan();
                BT_connect_obd();
                break;

            case BluetoothStates.STATE_SERVICE_STOPPED:
                mTextStatusGeneric.setText("Bluetooth service stopped.");
                break;
        }
    }

    public void OnBluetoothStateChanged(int state, String device_id) {
        if (device_id != null) {
            Log.d("MainActivity bluetoothStateChanged:", device_id);

            mTripComputer.pauseUntilTick();

            if (device_id.equals("obd")) {
                mTextStatusObd.setText(BluetoothStates.labelOfState(state));
            } else if (device_id.equals("spartan")) {
                mTextStatusSpartan.setText(BluetoothStates.labelOfState(state));
            }

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
                        Log.d("STATE", "Busy/connecting/enabling");
                    }

                    if (device_id.equals("obd")) {
                        buttonConnectObd.setText("Connecting");
                        buttonConnectObd.setEnabled(false);
                    }
                    break;

                case BluetoothStates.STATE_BT_DISCONNECTED:
                    if (device_id.equals("spartan")) {
                        buttonConnectSpartan.setText("Connect Spartan");
                        buttonConnectSpartan.setEnabled(true);
                        BT_connect_spartan_soon();
                    }

                    if (device_id.equals("obd")) {
                        buttonConnectObd.setText("Connect OBD");
                        buttonConnectObd.setEnabled(true);
                        BT_connect_obd_soon();
                    }
                    break;

                case BluetoothStates.STATE_BT_NONE:
                case BluetoothStates.STATE_BT_UNPAIRED:
                    if (device_id.equals("spartan")) {
                        buttonConnectSpartan.setText("Connect Spartan");
                        buttonConnectSpartan.setEnabled(true);
                        Log.d("STATE", "None or disconnected or unpaired");
                    }

                    if (device_id.equals("obd")) {
                        buttonConnectObd.setText("Connect OBD");
                        buttonConnectObd.setEnabled(true);
                    }
                    break;

                case BluetoothStates.STATE_BT_DISABLED:
                    Permissions.promptEnableBluetooth(this);
                    buttonConnectSpartan.setEnabled(true);
                    buttonConnectObd.setEnabled(true);
                    break;

                default:
                    buttonConnectObd.setText("Connect OBD");
                    buttonConnectObd.setEnabled(true);
                    buttonConnectSpartan.setText("Connect Spartan");
                    buttonConnectSpartan.setEnabled(true);
            }
        } else {
            switch (state) {
                case BluetoothStates.STATE_BT_ENABLED:
                    mTextStatusGeneric.setText("Bluetooth enabled.");
                    break;

                default:
                    mTextStatusGeneric.setText(BluetoothStates.labelOfState(state));
            }
        }

    }

    @Override
    public void onTripComputerReadingsUpdated() {
        updateDisplayedReadings();
    }

    private void updateDisplayedReadings() {
        mToggleClearAfrMin.setText(String.format("%.1f", mTripComputer.afrHistory.getMinValue()));
        mToggleClearAfrMax.setText(String.format("%.1f", mTripComputer.afrHistory.getMaxValue()));

        if (viewModel.showFuelConsumption.getValue()) {
            boolean showTotals = viewModel.showTotalFuelConsumption.getValue();
            double totalDistance = showTotals ? mTripComputer.getTotalGpsDistance() : mTripComputer.getTripGpsDistance();
            double totalLiters = showTotals ? mTripComputer.getTotalLiters() : mTripComputer.getTripLitres();
            double totalLitersPer100km = showTotals ? mTripComputer.getTotalLitersPer100km() : mTripComputer.getTripLitersPer100km();

            textTotalInfo.setText(showTotals ? "All Time" : "Trip");
            textTotalDistance.setText(String.format("%06.1f", totalDistance));

            textTotalConsLiters.setText(String.format("%.2f l", totalLiters));
            textTotalConsPer100km.setText(String.format("%.1f l", totalLitersPer100km));

            textMeasurementBig.setText(String.format("%.2f l/h", mTripComputer.getTripCurrentLitersPerHour()));
            textMeasurementSmall.setText(String.format("%.2f", mTripComputer.getTripCurrentLitersPer100km()));

            mCluster.onDataUpdated();
        } else {
            textMeasurementBig.setText(String.format("%.2f", mTripComputer.afrHistory.getAvg()));
            textMeasurementSmall.setText(String.format("%.2f", mTripComputer.afrHistory.getAvgDeviation(mSpartanStudio.targetAfr)));
        }
    }

    @Override
    public void onGpsUpdated(Double speed, double totalDistanceKm) {
        String s = String.format("%.1f km/h %.1f km", speed, totalDistanceKm);

        mTextStatusGeneric.setText("GPS: " + s);

        if (mTripComputer.isGpsSpeedUsed()) {
            mTextSpeed.setText(String.format("%.1f km/h", speed));
            mTextSpeedSource.setText("GPS");
        }
    }

    @Override
    public void onActivePidsChanged() {
        updateObdButtonsAppearance();
    }



    public class ButtonToggleSoundOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            viewModel.engineSoundsEnabled.postValue(
                    !Boolean.TRUE.equals(viewModel.engineSoundsEnabled.getValue())
            );
        }
    }

    public class ButtonShowFuelConsOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (!viewModel.showFuelConsumption.getValue()) {
                mTripComputer.setObdForFuelConsumption(true);
                viewModel.showFuelConsumption.postValue(true);
                updateDisplayedReadings();
            } else {
                mTripComputer.setObdForFuelConsumption(false);
                viewModel.showFuelConsumption.postValue(false);
                updateDisplayedReadings();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        BT_disconnect();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onStart() {
        super.onStart();
        registerReceiver(btReceiver, intentFilter, Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(btReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(btReceiver, intentFilter);
        }

        BT_startService();

        BT_connect_spartan();
        BT_connect_obd();

        mEngineSound.onResume(this);

        super.onResume();
    }

    @Override
    public void onPause() {
        mTripComputer.pauseUntilTick();
        mEngineSound.onPause(this);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        BT_disconnect();  // moved here

        mObdStudio.saveActivePids();
        mTripComputer.saveTripData(this);
        mEngineSound.onDestroy();

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


