package com.hondaafr.Libs.Helpers.AfrComputer;

import com.hondaafr.Libs.UI.AdaptiveAfr.AdaptiveAfrState;

public interface AfrComputerListener {
    void onAdaptiveAfrDataUpdated(AdaptiveAfrState state);
    
    default void onTableDataChanged(AdaptiveAfrState state) {
        // Optional: panels can override if they need to respond to table changes
    }
    
    default void onIsActivatedChanged(AdaptiveAfrState state) {
        // Optional: panels can override if they need to respond to enabled state changes
    }
}

