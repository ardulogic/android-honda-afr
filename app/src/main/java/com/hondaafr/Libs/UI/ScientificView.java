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
import com.hondaafr.Libs.UI.Scientific.CornerStatsPanel;
import com.hondaafr.Libs.UI.Scientific.FuelStatsPanel;
import com.hondaafr.Libs.UI.Scientific.ObdPanel;
import com.hondaafr.Libs.UI.Scientific.Panel;
import com.hondaafr.Libs.UI.Scientific.SoundPanel;
import com.hondaafr.Libs.UI.Scientific.TopButtonsPanel;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ScientificView {

    private final AfrPresetsPanel afrPresetsPanel;
    private final AfrBoundStatsPanel afrBoundStatsPanel;
    private final MainActivity mainActivity;
    private final TripComputer mTripComputer;
    public final FuelStatsPanel fuelStatsPanel;
    private final Panel[] panels;
    private final AfrPreciseControlsPanel afrPreciseControlsPanel;
    public final ObdPanel obdPanel;
    private final TopButtonsPanel topButtonsPanel;
    private final ConnectPanel connectPanel;
    public final ChartPanel chartPanel;
    public final SoundPanel soundPanel;
    public final CornerStatsPanel cornerStatsPanel;
    private boolean isInPip = false;

    public ScientificView(MainActivity mainActivity, TripComputer tripComputer) {
        this.mainActivity = mainActivity;
        this.mTripComputer = tripComputer;

        afrPresetsPanel = new AfrPresetsPanel(mainActivity, mTripComputer);
        afrPreciseControlsPanel = new AfrPreciseControlsPanel(mainActivity, mTripComputer);
        afrBoundStatsPanel = new AfrBoundStatsPanel(mainActivity, mTripComputer);
        fuelStatsPanel = new FuelStatsPanel(mainActivity, this, mTripComputer);
        obdPanel = new ObdPanel(mainActivity, mTripComputer);
        connectPanel = new ConnectPanel(mainActivity, mTripComputer);
        topButtonsPanel = new TopButtonsPanel(mainActivity, this, mTripComputer);
        chartPanel = new ChartPanel(mainActivity, mTripComputer);
        soundPanel = new SoundPanel(mainActivity, mTripComputer);
        cornerStatsPanel = new CornerStatsPanel(mainActivity, this, tripComputer);

        panels = new Panel[]{
                afrPresetsPanel,
                afrPreciseControlsPanel,
                afrBoundStatsPanel,
                fuelStatsPanel,
                obdPanel,
                connectPanel,
                topButtonsPanel,
                chartPanel,
                soundPanel,
                cornerStatsPanel
        };
    }

    public void setVisibility(boolean visible) {
        mainActivity.findViewById(R.id.layoutScientific).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void showPipView() {
        isInPip = true;
        for (Panel p : panels) {
            p.enterPip();
        }
    }

    public void restoreFullView() {
        isInPip = false;
        for (Panel p : panels) {
            p.exitPip();
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
