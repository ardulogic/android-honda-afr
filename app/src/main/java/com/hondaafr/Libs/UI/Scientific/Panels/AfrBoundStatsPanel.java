package com.hondaafr.Libs.UI.Scientific.Panels;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.Button;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class AfrBoundStatsPanel extends Panel {
    private final Button mToggleClearAfrMin;
    private final Button mToggleClearAfrAll;
    private final Button mToggleClearAfrMax;

    public AfrBoundStatsPanel(MainActivity mainActivity, TripComputer mTripComputer, UiView view) {
        super(mainActivity, mTripComputer, view);

        mToggleClearAfrMin = mainActivity.findViewById(R.id.buttonClearAfrMin);
        mToggleClearAfrMin.setOnClickListener(v -> mTripComputer.afrHistory.clearMin());

        mToggleClearAfrAll = mainActivity.findViewById(R.id.buttonClearAfrAll);
        mToggleClearAfrAll.setOnClickListener(v -> mTripComputer.afrHistory.clear());

        mToggleClearAfrMax = mainActivity.findViewById(R.id.buttonClearAfrMax);
        mToggleClearAfrMax.setOnClickListener(v -> mTripComputer.afrHistory.clearMax());
    }


    @Override
    public int getContainerId() {
        return R.id. toggleAfrBoundStatsGroup;
    }

    @Override
    public String getListenerId() {
        return "scientific_afr_bound_stats";
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onAfrValue(Double afr) {
        mToggleClearAfrMin.setText(String.format("%.1f", tripComputer.afrHistory.getMinValue()));
        mToggleClearAfrAll.setText(String.format("%.1f", tripComputer.afrHistory.getAvg()));
        mToggleClearAfrMax.setText(String.format("%.1f", tripComputer.afrHistory.getMaxValue()));
    }
}
