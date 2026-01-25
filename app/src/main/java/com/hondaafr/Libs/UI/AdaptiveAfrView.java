package com.hondaafr.Libs.UI;

import android.view.View;

import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.AdaptiveAfr.Panels.AfrAfrChartPanel;
import com.hondaafr.Libs.UI.AdaptiveAfr.Panels.AdaptiveAfrEnablePanel;
import com.hondaafr.Libs.UI.AdaptiveAfr.Panels.AfrInfoPanel;
import com.hondaafr.Libs.UI.AdaptiveAfr.Panels.AdaptiveAfrPresetTogglePanel;
import com.hondaafr.Libs.UI.AdaptiveAfr.Panels.AfrMapChartPanel;
import com.hondaafr.Libs.UI.AdaptiveAfr.Panels.AfrRpmChartPanel;
import com.hondaafr.Libs.UI.AdaptiveAfr.Panels.AfrTablePanel;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class AdaptiveAfrView extends UiView {

    public AdaptiveAfrView(MainActivity mainActivity, TripComputer tripComputer, AfrComputer afrComputer, View rootView) {
        super(mainActivity, tripComputer, afrComputer, rootView);
    }

    @Override
    String getListenerId() {
        return "adaptive_afr_view";
    }

    @Override
    public int getContainerId() {
        return R.id.layoutAdaptiveAfr;
    }

    @Override
    public Panel[] initPanels() {       
        return new Panel[]{
                new AfrRpmChartPanel(mainActivity, tripComputer, this, afrComputer),
                new AfrMapChartPanel(mainActivity, tripComputer, this, afrComputer),
                new AfrAfrChartPanel(mainActivity, tripComputer, this, afrComputer),
                new AfrInfoPanel(mainActivity, tripComputer, this, afrComputer),
                new AfrTablePanel(mainActivity, tripComputer, this, afrComputer),
                new AdaptiveAfrEnablePanel(mainActivity, tripComputer, this, afrComputer),
                new AdaptiveAfrPresetTogglePanel(mainActivity, tripComputer, this, afrComputer),
        };
    }

}

