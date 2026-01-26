package com.hondaafr.Libs.UI;

import android.view.View;

import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.AdaptiveAfr.Panels.AdaptiveAfrEnablePanel;
import com.hondaafr.Libs.UI.AdaptiveAfr.Panels.AfrInfoPanel;
import com.hondaafr.Libs.UI.AdaptiveAfr.Panels.AdaptiveAfrPresetTogglePanel;
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
                new AfrInfoPanel(mainActivity, tripComputer, afrComputer, this),
                new AfrTablePanel(mainActivity, tripComputer, afrComputer, this),
                new AdaptiveAfrEnablePanel(mainActivity, tripComputer, afrComputer, this),
                new AdaptiveAfrPresetTogglePanel(mainActivity, tripComputer, afrComputer, this),
        };
    }

}

