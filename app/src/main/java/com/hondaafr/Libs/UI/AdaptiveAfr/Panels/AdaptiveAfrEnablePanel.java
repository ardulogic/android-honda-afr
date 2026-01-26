package com.hondaafr.Libs.UI.AdaptiveAfr.Panels;

import android.content.Context;
import android.view.View;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputerListener;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.AdaptiveAfr.AdaptiveAfrState;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class AdaptiveAfrEnablePanel extends Panel implements AfrComputerListener {

    private MaterialButtonToggleGroup toggleAdaptive;

    @Override
    public int getContainerId() {
        return R.id.layoutAdaptiveAfr;
    }

    @Override
    public String getListenerId() {
        return "adaptive_afr_enable_panel";
    }

    public AdaptiveAfrEnablePanel(MainActivity mainActivity, TripComputer tripComputer, AfrComputer afrComputer, UiView parent) {
        super(mainActivity, tripComputer, afrComputer, parent);
        
        toggleAdaptive = rootView.findViewById(R.id.toggleAdaptiveAfr);
        
        setupListener();
    }

    private void setupListener() {
        if (toggleAdaptive != null) {
            toggleAdaptive.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    afrComputer.setAdaptiveEnabled(checkedId == R.id.buttonAdaptiveOn);
                }
            });
        }
    }

    @Override
    public void setVisibility(boolean visible) {
        // Don't hide the container - manage toggle visibility individually
    }

    @Override
    public View[] getViewsHiddenInPip() {
        return new View[]{toggleAdaptive};
    }

    @Override
    public void onParentReady() {
        super.onParentReady();
        updateAdaptiveToggle();
    }

    @Override
    public void onResume(Context context) {
        super.onResume(context);
        updateAdaptiveToggle();
    }

    @Override
    public void onDestroy(Context context) {
        super.onDestroy(context);
    }

    @Override
    public void onAdaptiveAfrDataUpdated(AdaptiveAfrState state) {
        // Not needed for this panel
    }

    @Override
    public void onIsActivatedChanged(AdaptiveAfrState state) {
        updateAdaptiveToggle();
    }

    public void updateAdaptiveToggle() {
        if (toggleAdaptive != null) {
            AdaptiveAfrState state = afrComputer.getState();
            if (state != null) {
                if (state.isAdaptiveEnabled()) {
                    toggleAdaptive.check(R.id.buttonAdaptiveOn);
                } else {
                    toggleAdaptive.check(R.id.buttonAdaptiveOff);
                }
            }
        }
    }

}

