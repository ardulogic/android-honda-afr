package com.hondaafr.Libs.UI;

import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.Scientific.AfrBoundStatsPanel;
import com.hondaafr.Libs.UI.Scientific.AfrPreciseControlsPanel;
import com.hondaafr.Libs.UI.Scientific.AfrPresetsPanel;
import com.hondaafr.Libs.UI.Scientific.ChartPanel;
import com.hondaafr.Libs.UI.Scientific.ConnectPanel;
import com.hondaafr.Libs.UI.Scientific.FuelStatsPanel;
import com.hondaafr.Libs.UI.Scientific.GenericStatusPanel;
import com.hondaafr.Libs.UI.Scientific.ObdPanel;
import com.hondaafr.Libs.UI.Scientific.SoundPanel;
import com.hondaafr.Libs.UI.Scientific.TopButtonsPanel;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ScientificView {

    private final AfrPresetsPanel afrPresetsPanel;
    private final AfrBoundStatsPanel afrBoundStatsPanel;
    private final MainActivity mainActivity;
    private final TripComputer mTripComputer;
    private final TextView textCornerBig, textCornerSmall;
    public final FuelStatsPanel fuelStatsPanel;
    private final View[] hiddenViewsInPip;
    private final AfrPreciseControlsPanel afrPreciseControlsPanel;
    public final ObdPanel obdPanel;
    private final TopButtonsPanel topButtonsPanel;
    private final LinearLayout mainControlsPanel;
    private final GenericStatusPanel genericStatusPanel;
    private final ConnectPanel connectPanel;
    public final ChartPanel chartPanel;
    public final SoundPanel soundPanel;
    private boolean isInPip = false;

    public ScientificView(MainActivity mainActivity, TripComputer tripComputer) {
        this.mainActivity = mainActivity;
        this.mTripComputer = tripComputer;

        mainControlsPanel = mainActivity.findViewById(R.id.layoutMainControls);
        afrPresetsPanel = new AfrPresetsPanel(mainActivity, mTripComputer);
        afrPreciseControlsPanel = new AfrPreciseControlsPanel(mainActivity, mTripComputer);
        afrBoundStatsPanel = new AfrBoundStatsPanel(mainActivity, mTripComputer);
        fuelStatsPanel = new FuelStatsPanel(mainActivity, mTripComputer);
        obdPanel = new ObdPanel(mainActivity, mTripComputer);
        genericStatusPanel = new GenericStatusPanel(mainActivity);
        connectPanel = new ConnectPanel(mainActivity, mTripComputer);
        topButtonsPanel = new TopButtonsPanel(mainActivity, this, mTripComputer);
        chartPanel = new ChartPanel(mainActivity, mTripComputer);
        soundPanel = new SoundPanel(mainActivity, mTripComputer);

        textCornerBig = mainActivity.findViewById(R.id.textCornerStatsBig);
        textCornerSmall = mainActivity.findViewById(R.id.textCornerStatsSmall);

        hiddenViewsInPip = new View[]{
                mainControlsPanel,
                topButtonsPanel.panel,
                fuelStatsPanel.panel
        };

        mTripComputer.addListener("scientific", new TripComputerListener() {
            @Override
            public void onGpsUpdate(Double speed, double distanceIncrement) {
                connectPanel.onGpsUpdate(speed, distanceIncrement);
            }

            @Override
            public void onGpsPulse(PhoneGps gps) {

            }

            @Override
            public void onAfrPulse(boolean isActive) {

            }

            @Override
            public void onAfrTargetValue(double targetAfr) {
                chartPanel.onTargetAfrUpdated(targetAfr);
            }

            @Override
            public void onAfrValue(Double afr) {
                chartPanel.onAfrUpdated(afr);
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

    public void onDataUpdated() {
        displayAfrValues();
    }

    public void displayAfrValues() {
        textCornerBig.setText(String.format("%.2f", mTripComputer.afrHistory.getAvg()));
        textCornerSmall.setText(String.format("%.2f", mTripComputer.afrHistory.getAvgDeviation(
                mTripComputer.mSpartanStudio.targetAfr)));
    }

    public void setVisibility(boolean visible) {
        mainActivity.findViewById(R.id.layoutScientific).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void showPipView() {
        isInPip = true;
        for (View v : hiddenViewsInPip) {
            v.setVisibility(View.GONE);
        }
    }

    public void restoreFullView() {
        isInPip = false;
        for (View v : hiddenViewsInPip) {
            v.setVisibility(View.VISIBLE);
        }
    }

    public void onPause() {
        soundPanel.onPause();
    }

    public void onResume() {
        connectPanel.onResume();
        soundPanel.onResume();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void onStart() {
        connectPanel.onStart();
    }

    public void onStop() {
        connectPanel.onStop();
    }

    public void onDestroy() {
        connectPanel.onDestroy();  // moved here
        soundPanel.onDestroy();
    }

}
