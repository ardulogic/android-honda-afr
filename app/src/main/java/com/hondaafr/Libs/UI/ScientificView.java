package com.hondaafr.Libs.UI;

import android.view.View;

import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.Panels.AfrBoundStatsPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.AfrPreciseControlsPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.AfrPresetsPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.ChartPanel;
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

    public ScientificView(MainActivity mainActivity, TripComputer tripComputer, AfrComputer afrComputer, View rootView) {
        super(mainActivity, tripComputer, afrComputer, rootView);
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
                new AfrBoundStatsPanel(mainActivity, tripComputer, afrComputer, this),
                new AfrPreciseControlsPanel(mainActivity, tripComputer, afrComputer, this),
                new AfrPresetsPanel(mainActivity, tripComputer, afrComputer, this),
                new ChartPanel(mainActivity, tripComputer, afrComputer, this),
                new CornerStatsPanel(mainActivity, tripComputer, afrComputer, this),
                new FuelStatsPanel(mainActivity, tripComputer, afrComputer, this),
                new GenericStatusPanel(mainActivity, tripComputer, afrComputer, this),
                new ObdPanel(mainActivity, tripComputer, afrComputer, this),
                new SoundPanel(mainActivity, tripComputer, afrComputer, this),
                new TopButtonsPanel(mainActivity, tripComputer, afrComputer, this),
        };
    }

}
