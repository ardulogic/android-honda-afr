package com.hondaafr.Libs.Helpers;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Helper class to handle EdgeToEdge window insets at the activity level.
 * Applies padding to the root layout to prevent content from overlapping system bars.
 */
public class EdgeToEdgeHelper {
    
    /**
     * Sets up window insets handling for the root view of an activity.
     * This should be called after setContentView() in onCreate().
     * 
     * @param rootView The root view of the activity (typically the root layout from setContentView)
     */
    public static void setup(@NonNull View rootView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            // Get insets for system bars (status bar + navigation bar)
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding based on current system bar visibility
            // When bars are hidden, insets will be 0, so padding will be removed
            // When bars are shown, insets will have values, so padding will be applied
            //v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            // Consume insets to prevent propagation to child views
            return WindowInsetsCompat.CONSUMED;
        });
        
        // Request window insets to ensure the listener is called
        ViewCompat.requestApplyInsets(rootView);
    }
}

