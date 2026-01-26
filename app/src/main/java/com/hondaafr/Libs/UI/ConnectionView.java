package com.hondaafr.Libs.UI;

import android.view.View;

import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Connection.Panels.AfrLogPanel;
import com.hondaafr.Libs.UI.Connection.Panels.LogTabsPanel;
import com.hondaafr.Libs.UI.Connection.Panels.ObdLogPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ConnectionView extends UiView {

    public ConnectionView(MainActivity mainActivity, TripComputer tripComputer, AfrComputer afrComputer, View rootView) {
        super(mainActivity, tripComputer, afrComputer, rootView);
    }

    @Override
    String getListenerId() {
        return "connection_view";
    }

    @Override
    public int getContainerId() {
        return R.id.layoutConnection;
    }

    @Override
    public Panel[] initPanels() {
        return new Panel[]{
                new com.hondaafr.Libs.UI.Connection.Panels.SimulatorPanel(mainActivity, tripComputer, afrComputer, this),
                new LogTabsPanel(mainActivity, tripComputer, afrComputer, this),
                new ObdLogPanel(mainActivity, tripComputer, afrComputer, this),
                new AfrLogPanel(mainActivity, tripComputer, afrComputer, this),
        };
    }

}

