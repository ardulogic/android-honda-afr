package com.hondaafr.Libs.UI.Scientific;

import android.annotation.SuppressLint;
import android.widget.Button;
import android.widget.TextView;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class AfrPreciseControlsPanel {

    private final Button buttonIncreaseAfr, buttonDecreaseAfr;
    private final TextView textTargetAfr;
    private final SpartanStudio mSpartanStudio;

    public AfrPreciseControlsPanel(MainActivity mainActivity, TripComputer tripComputer) {
        this.mSpartanStudio = tripComputer.mSpartanStudio;

        buttonIncreaseAfr = mainActivity.findViewById(R.id.buttonIncreaseAFR);
        buttonDecreaseAfr = mainActivity.findViewById(R.id.buttonDecreaseAFR);
        textTargetAfr = mainActivity.findViewById(R.id.textTargetAFR);

        buttonIncreaseAfr.setOnClickListener(view -> this.mSpartanStudio.adjustAFR(0.05));
        buttonDecreaseAfr.setOnClickListener(view -> this.mSpartanStudio.adjustAFR(-0.05));

        tripComputer.addListener("afr_precise_control", new TripComputerListener() {
            @Override
            public void onGpsUpdate(Double speed, double distanceIncrement) {

            }

            @Override
            public void onGpsPulse(PhoneGps gps) {

            }

            @Override
            public void onAfrPulse(boolean isActive) {

            }

            @SuppressLint("DefaultLocale")
            @Override
            public void onAfrTargetValue(double targetAfr) {
                textTargetAfr.setText(String.format("%.2f", targetAfr));
            }

            @Override
            public void onAfrValue(Double afr) {

            }

            @Override
            public void onObdPulse(boolean isActive) {

            }

            @Override
            public void onObdActivePidsChanged() {

            }

            @Override
            public void onObdValue(ObdReading reading) {

            }

            @Override
            public void onCalculationsUpdated() {

            }
        });
    }

}
