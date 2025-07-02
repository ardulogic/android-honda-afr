package com.hondaafr.Libs.UI;

import android.graphics.Color;

import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Cluster.Panels.GaugePanel;
import com.hondaafr.Libs.UI.Cluster.Panels.KnobsPanel;
import com.hondaafr.Libs.UI.Cluster.Sound.BeeperPanel;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ClusterView extends UiView {


    public ClusterView(MainActivity mainActivity, TripComputer tripComputer) {
        super(mainActivity, tripComputer);

        getContainerView().setOnLongClickListener(v -> {
            mainActivity.showScientific();
            return true;
        });

        getContainerView().setOnClickListener(v -> toggleSystemUI());
    }

    @Override
    public Panel[] initPanels() {
        return new Panel[]{
                new BeeperPanel(mainActivity, tripComputer, this),
                new GaugePanel(mainActivity, tripComputer, this),
                new KnobsPanel(mainActivity, tripComputer, this),
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
