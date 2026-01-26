package com.hondaafr.Libs.UI;

import android.graphics.Color;
import android.view.View;

import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Cluster.Panels.GaugePanel;
import com.hondaafr.Libs.UI.Cluster.Panels.KnobsPanel;
import com.hondaafr.Libs.UI.Cluster.Sound.BeeperPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ClusterView extends UiView {

    @Override
    String getListenerId() {
        return "cluster_view";
    }

    public ClusterView(MainActivity mainActivity, TripComputer tripComputer, AfrComputer afrComputer, View rootView) {
        super(mainActivity, tripComputer, afrComputer, rootView);

        getContainerView().setOnClickListener(v -> toggleSystemUI());
    }

    @Override
    public Panel[] initPanels() {
        return new Panel[]{
                new BeeperPanel(mainActivity, tripComputer, afrComputer, this),
                new GaugePanel(mainActivity, tripComputer, afrComputer, this),
                new KnobsPanel(mainActivity, tripComputer, afrComputer, this),
        };
    }

    @Override
    public int getContainerId() {
        return R.id.layoutCluster;
    }

    @Override
    protected void onNightModeChanged(boolean isNight) {
        getContainerView().setBackgroundColor(Color.parseColor(isNight ? "#000000" : "#0E0E0E"));
    }
}
