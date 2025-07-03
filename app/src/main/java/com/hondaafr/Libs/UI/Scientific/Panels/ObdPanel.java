package com.hondaafr.Libs.UI.Scientific.Panels;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.widget.TextView;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.ImageButtonRounded;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import java.util.HashMap;
import java.util.Map;

public class ObdPanel extends Panel {

    private final Map<String, TextView> obdButtons = new HashMap<>();
    private final TextView mTextSpeedSource;
    @Override
    public int getContainerId() {
        return R.id.layoutObd;
    }

    @Override
    public String getListenerId() {
        return "obd_panel";
    }

    public ObdPanel(MainActivity mainActivity, TripComputer tripComputer, UiView parentView) {
        super(mainActivity, tripComputer, parentView);

        mTextSpeedSource = mainActivity.findViewById(R.id.textSpeedSource);

        setObdOnClickListeners();
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
        updateToggleAppearance(textView, tripComputer.mObdStudio.readings.isActive(reading_name));

        textView.setOnClickListener(v -> {
            boolean isActive = tripComputer.mObdStudio.readings.toggleActive(reading_name);
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
            boolean buttonIsActive = tripComputer.mObdStudio.readings.active.containsKey(entry.getKey());
            updateToggleAppearance(entry.getValue(), buttonIsActive);
        }
    }

    private void updateToggleAppearance(TextView textView, boolean isActive) {
        textView.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
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
                if (!tripComputer.isGpsSpeedUsed()) {
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

    @Override
    public void onGpsUpdate(Double speed, double distanceIncrement) {
        if (tripComputer.isGpsSpeedUsed()) {
            mTextSpeedSource.setText("GPS");

            TextView textView = mainActivity.findViewById(R.id.textSpeed);
            textView.setText(String.format("%.1f km/h", speed));
        }
    }

    @Override
    public void onObdActivePidsChanged() {
        updateObdButtonsAppearance();
    }

    @Override
    public void onObdValue(ObdReading reading) {
        updateObdReadingDisplay(reading);
    }
}
