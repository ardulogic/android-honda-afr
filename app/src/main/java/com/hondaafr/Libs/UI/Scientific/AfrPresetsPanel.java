package com.hondaafr.Libs.UI.Scientific;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import java.util.HashMap;
import java.util.Map;

public class AfrPresetsPanel  extends Panel {

    private final Map<String, Double> presets = new HashMap<>();
    private final SharedPreferences prefs;
    private final SpartanStudio mSpartanStudio;
    private MaterialButtonToggleGroup panel;

    public AfrPresetsPanel(MainActivity mainActivity, TripComputer tripComputer) {
        panel = mainActivity.findViewById(R.id.toggleAfrPresets);

        prefs = mainActivity.getSharedPreferences("afr_presets", Context.MODE_PRIVATE);

        presets.put("sportPlusAfr", (double) prefs.getFloat("sportPlusAfr", 12.7f));
        presets.put("sportAfr", (double) prefs.getFloat("sportAfr", 14.7f));
        presets.put("ecoAfr", (double) prefs.getFloat("ecoAfr", 15.4f));
        presets.put("ecoPlusAfr", (double) prefs.getFloat("ecoPlusAfr", 16.4f));

        this.mSpartanStudio = tripComputer.mSpartanStudio;

        Map<Integer, String> buttonPresetMap = new HashMap<>();
        buttonPresetMap.put(R.id.buttonAfrSportPlus, "sportPlusAfr");
        buttonPresetMap.put(R.id.buttonAfrSport, "sportAfr");
        buttonPresetMap.put(R.id.buttonAfrEco, "ecoAfr");
        buttonPresetMap.put(R.id.buttonAfrEcoPlus, "ecoPlusAfr");

        panel.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && buttonPresetMap.containsKey(checkedId)) {
                apply(buttonPresetMap.get(checkedId));
            }
        });

        for (Map.Entry<Integer, String> entry : buttonPresetMap.entrySet()) {
            mainActivity.findViewById(entry.getKey()).setOnLongClickListener(v -> {
                save(entry.getValue(), mSpartanStudio.targetAfr);
                return true;
            });
        }
    }

    public double get(String key) {
        return presets.getOrDefault(key, 14.7);
    }

    public void save(String key, double value) {
        presets.put(key, value);
        prefs.edit().putFloat(key, (float) value).apply();
    }

    public void apply(String key) {
        mSpartanStudio.setAFR(get(key));
    }

    @Override
    public View getContainerView() {
        return panel;
    }
}
