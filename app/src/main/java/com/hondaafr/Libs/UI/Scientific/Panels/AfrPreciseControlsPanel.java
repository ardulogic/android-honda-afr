package com.hondaafr.Libs.UI.Scientific.Panels;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.ScientificView;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class AfrPreciseControlsPanel  extends Panel {

    private final Button buttonIncreaseAfr, buttonDecreaseAfr;
    private final TextView textTargetAfr;

    public AfrPreciseControlsPanel(MainActivity mainActivity, TripComputer tripComputer, UiView parent) {
        super(mainActivity, tripComputer, parent);

        buttonIncreaseAfr = mainActivity.findViewById(R.id.buttonIncreaseAFR);
        buttonDecreaseAfr = mainActivity.findViewById(R.id.buttonDecreaseAFR);
        textTargetAfr = mainActivity.findViewById(R.id.textTargetAFR);

        buttonIncreaseAfr.setOnClickListener(view -> tripComputer.mSpartanStudio.adjustAFR(0.05));
        buttonDecreaseAfr.setOnClickListener(view -> tripComputer.mSpartanStudio.adjustAFR(-0.05));
    }

    @Override
    public int getContainerId() {
        return R.id.layoutAfrControls;
    }

    @Override
    public String getListenerId() {
        return "scientific_afr_controls";
    }

    @Override
    public void onAfrTargetValue(double targetAfr) {
        d("Target value received:" + targetAfr, VERBOSE);
        textTargetAfr.setText(String.format("%.2f", targetAfr));
    }
}
