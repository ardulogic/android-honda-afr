package com.hondaafr.Libs.UI.AdaptiveAfr.Panels;

import android.widget.TextView;

import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputerListener;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.AdaptiveAfr.AdaptiveAfrState;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class AfrInfoPanel extends Panel implements AfrComputerListener {

    private TextView textRpm;
    private TextView textMap;
    private TextView textAfr;
    private TextView textTarget;

    @Override
    public int getContainerId() {
        return R.id.layoutAdaptiveAfrInfo;
    }

    @Override
    public String getListenerId() {
        return "adaptive_afr_info_panel";
    }

    public AfrInfoPanel(MainActivity mainActivity, TripComputer tripComputer, AfrComputer afrComputer, UiView parent) {
        super(mainActivity, tripComputer, afrComputer, parent);
        
        textRpm = rootView.findViewById(R.id.textAdaptiveRpm);
        textMap = rootView.findViewById(R.id.textAdaptiveMap);
        textAfr = rootView.findViewById(R.id.textAdaptiveAfr);
        textTarget = rootView.findViewById(R.id.textAdaptiveTarget);
    }

    @Override
    public void onAdaptiveAfrDataUpdated(AdaptiveAfrState state) {
        updateInfo(state);
    }

    private void updateInfo(AdaptiveAfrState state) {
        if (textRpm == null || textMap == null || textAfr == null || textTarget == null || state == null) {
            return;
        }
        
        if (state.getLastRpm() != null) {
            textRpm.setText(String.format("RPM\n%d", state.getLastRpm()));
        }
        if (state.getLastMap() != null) {
            textMap.setText(String.format("MAP\n%d", state.getLastMap()));
        }
        if (!Double.isNaN(state.getLastAfr())) {
            textAfr.setText(String.format("AFR\n%.2f", state.getLastAfr()));
        }
        
        // Display target AFR from state
        if (!Double.isNaN(state.getLastTargetAfr())) {
            textTarget.setText(String.format("AFRT\n%.2f", state.getLastTargetAfr()));
        } else {
            textTarget.setText("AFRT\n--");
        }
    }

    @Override
    public void attachTripComputerListener() {
        super.attachTripComputerListener();
        // Refresh UI with current state when listener is attached
        if (afrComputer.getState() != null) {
            updateInfo(afrComputer.getState());
        }
    }

    @Override
    public void onResume(android.content.Context context) {
        super.onResume(context);
        // Refresh UI with current state when fragment resumes
        if (afrComputer.getState() != null) {
            updateInfo(afrComputer.getState());
        }
    }
}

