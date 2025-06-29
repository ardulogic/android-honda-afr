package com.hondaafr.Libs.UI.Scientific;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.ImageButtonRounded;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import java.util.HashMap;
import java.util.Map;

public class ObdPanel {

    private final LinearLayout panel;
    private final TripComputer mTripComputer;
    private final MainActivity mainActivity;
    private final Map<String, TextView> obdButtons = new HashMap<>();
    private TextView mTextSpeedSource;
    private ImageButtonRounded mToggleFuelCons;

    public ObdPanel(MainActivity mainActivity, TripComputer mTripComputer) {
        this.mTripComputer = mTripComputer;
        this.mainActivity = mainActivity;

        panel = mainActivity.findViewById(R.id.layoutObd);
        mToggleFuelCons = mainActivity.findViewById(R.id.buttonShowFuelPanel);
        mTextSpeedSource = mainActivity.findViewById(R.id.textSpeedSource);

        setObdOnClickListeners();

        mTripComputer.addListener("obd_panel", tcListener);
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
        TextView textView = mainActivity.findViewById(textViewId);
        obdButtons.put(reading_name, textView);
        updateToggleAppearance(textView, mTripComputer.mObdStudio.readings.isActive(reading_name));

        textView.setOnClickListener(v -> {
            boolean isActive =  mTripComputer.mObdStudio.readings.toggleActive(reading_name);
            updateToggleAppearance(textView, isActive);
        });
    }


    private void setObdReadingText(int textViewId, ObdReading reading) {
        TextView textView = mainActivity.findViewById(textViewId);
        textView.setText(reading.getDisplayValue());
        updateToggleAppearance(textView, true);
    }

    private void updateObdButtonsAppearance() {
        for (Map.Entry<String, TextView> entry : obdButtons.entrySet()) {
            boolean buttonIsActive =  mTripComputer.mObdStudio.readings.active.containsKey(entry.getKey());
            updateToggleAppearance(entry.getValue(), buttonIsActive);
        }
    }

    private void updateToggleAppearance(TextView textView, boolean isActive) {
        textView.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
    }

    public void show() {
        panel.setVisibility(View.VISIBLE);
    }

    public void hide() {
        panel.setVisibility(View.GONE);
    }

    @SuppressLint("DefaultLocale")
    public void updateObdReadingDisplay(ObdReading reading) {
        switch (reading.getMachineName()) {
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
                if (!mTripComputer.isGpsSpeedUsed()) {
                    mTextSpeedSource.setText("OBD");
                    setObdReadingText(R.id.textSpeed, reading);
                }
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

    private TripComputerListener tcListener = new TripComputerListener() {
        @Override
        public void onGpsUpdate(Double speed, double distanceIncrement) {
            if (mTripComputer.isGpsSpeedUsed()) {
                mTextSpeedSource.setText("GPS");

                TextView textView = mainActivity.findViewById(R.id.textSpeed);
                textView.setText(String.format("%.1f km/h", speed));
            }
        }

        @Override
        public void onGpsPulse(PhoneGps gps) {

        }

        @Override
        public void onAfrPulse(boolean isActive) {

        }

        @Override
        public void onAfrTargetValue(double targetAfr) {

        }

        @Override
        public void onAfrValue(Double afr) {

        }

        @Override
        public void onObdPulse(boolean isActive) {

        }

        @Override
        public void onObdActivePidsChanged() {
            updateObdButtonsAppearance();

            boolean canMeasureFuel = mTripComputer.mObdStudio.readingsForFuelAreActive();

            if (mTripComputer.mObdStudio.readingsForFuelAreActive()) {
                mToggleFuelCons.setIconState(canMeasureFuel);
            }
        }

        @Override
        public void onObdValue(ObdReading reading) {
            updateObdReadingDisplay(reading);
        }

        @Override
        public void onCalculationsUpdated() {

        }
    };


}
