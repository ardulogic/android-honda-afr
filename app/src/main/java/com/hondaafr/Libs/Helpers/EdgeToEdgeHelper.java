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
            return WindowInsetsCompat.CONSUMED;
        });
        
        ViewCompat.requestApplyInsets(rootView);
    }
}

