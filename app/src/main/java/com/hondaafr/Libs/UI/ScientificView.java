package com.hondaafr.Libs.UI;

import android.view.View;

import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.Panels.AfrBoundStatsPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.AfrPreciseControlsPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.AfrPresetsPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.ChartPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.ConnectPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.CornerStatsPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.FuelStatsPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.GenericStatusPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.ObdPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.Scientific.Panels.SoundPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.TopButtonsPanel;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ScientificView extends UiView {

    public ScientificView(MainActivity mainActivity, TripComputer tripComputer) {
        super(mainActivity, tripComputer);
    }

    @Override
    String getListenerId() {
        return "scientific_view";
    }

    @Override
    public int getContainerId() {
        return R.id.layoutScientific;
    }

    @Override
    public Panel[] initPanels() {
        return new Panel[]{
                new AfrBoundStatsPanel(mainActivity, tripComputer, this),
                new AfrPreciseControlsPanel(mainActivity, tripComputer, this),
                new AfrPresetsPanel(mainActivity, tripComputer, this),
                new ChartPanel(mainActivity, tripComputer, this),
                new ConnectPanel(mainActivity, tripComputer, this),
                new CornerStatsPanel(mainActivity, tripComputer, this),
                new FuelStatsPanel(mainActivity, tripComputer, this),
                new GenericStatusPanel(mainActivity, tripComputer, this),
                new ObdPanel(mainActivity, tripComputer, this),
                new SoundPanel(mainActivity, tripComputer, this),
                new TopButtonsPanel(mainActivity, tripComputer, this),
        };
    }

}
