package com.hondaafr.Libs.UI.Connection.Panels;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class LogTabsPanel extends Panel {

    private MaterialButtonToggleGroup toggleLogTabs;
    private LinearLayout layoutObdLogSection;
    private LinearLayout layoutAfrLogSection;

    @Override
    public int getContainerId() {
        // This panel doesn't have a container - it just manages the toggle group
        // Return the scroll view's child LinearLayout as a placeholder
        return R.id.scrollConnection;
    }
    
    @Override
    public void setVisibility(boolean visible) {
        // Don't hide the container - this panel just manages the toggle group
        // The visibility is controlled by the log sections themselves
    }

    @Override
    public String getListenerId() {
        return "log_tabs_panel";
    }

    public LogTabsPanel(MainActivity mainActivity, TripComputer tripComputer, UiView parent) {
        super(mainActivity, tripComputer, parent);
        
        toggleLogTabs = rootView.findViewById(R.id.toggleLogTabs);
        layoutObdLogSection = rootView.findViewById(R.id.layoutObdLogSection);
        layoutAfrLogSection = rootView.findViewById(R.id.layoutAfrLogSection);
        
        toggleLogTabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.buttonTabObd) {
                    layoutObdLogSection.setVisibility(View.VISIBLE);
                    layoutAfrLogSection.setVisibility(View.GONE);
                } else if (checkedId == R.id.buttonTabAfr) {
                    layoutObdLogSection.setVisibility(View.GONE);
                    layoutAfrLogSection.setVisibility(View.VISIBLE);
                }
            }
        });
        
        // Default to OBD tab
        toggleLogTabs.check(R.id.buttonTabObd);
    }
}

