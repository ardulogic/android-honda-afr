package com.hondaafr.Libs.UI.AdaptiveAfr.Panels;

import android.view.View;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.AdaptiveAfr.AdaptiveAfrState;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class AdaptiveAfrPresetTogglePanel extends Panel {

    private AfrComputer afrComputer;
    private MaterialButtonToggleGroup togglePresets;

    @Override
    public int getContainerId() {
        return R.id.layoutAdaptiveAfr;
    }

    @Override
    public String getListenerId() {
        return "adaptive_afr_preset_toggle_panel";
    }

    public AdaptiveAfrPresetTogglePanel(MainActivity mainActivity, TripComputer tripComputer, UiView parent, AfrComputer afrComputer) {
        super(mainActivity, tripComputer, parent);
        this.afrComputer = afrComputer;
        
        togglePresets = rootView.findViewById(R.id.toggleAdaptivePresets);
        
        setupListener();
    }

    private void setupListener() {
        if (togglePresets != null && afrComputer != null) {
            togglePresets.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) {
                    return;
                }
                if (checkedId == R.id.buttonAdaptiveSport) {
                    afrComputer.setActivePreset("sport");
                } else if (checkedId == R.id.buttonAdaptiveEco) {
                    afrComputer.setActivePreset("eco");
                } else if (checkedId == R.id.buttonAdaptiveEcoPlus) {
                    afrComputer.setActivePreset("eco_plus");
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
        return new View[]{togglePresets};
    }

    @Override
    public void onParentReady() {
        super.onParentReady();
        applyPresetSelection();
    }

    @Override
    public void onResume(android.content.Context context) {
        super.onResume(context);
        applyPresetSelection();
    }

    public void applyPresetSelection() {
        if (togglePresets == null || afrComputer == null) {
            return;
        }
        AdaptiveAfrState state = afrComputer.getState();
        if (state == null) {
            return;
        }
        if ("sport".equals(state.getActivePreset())) {
            togglePresets.check(R.id.buttonAdaptiveSport);
        } else if ("eco_plus".equals(state.getActivePreset())) {
            togglePresets.check(R.id.buttonAdaptiveEcoPlus);
        } else {
            togglePresets.check(R.id.buttonAdaptiveEco);
        }
    }

    public void saveCell(int r, int m, double value) {
        if (afrComputer != null) {
            afrComputer.saveCell(r, m, value);
        }
    }

}

