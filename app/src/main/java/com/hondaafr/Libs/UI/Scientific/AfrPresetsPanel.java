package com.hondaafr.Libs.UI.Scientific;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.hondaafr.Libs.Devices.Spartan.SpartanStudio;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import java.util.HashMap;
import java.util.Map;

public class AfrPresetsPanel {

    private final MaterialButtonToggleGroup toggleAfrGroup;
    private final Map<String, Double> presets = new HashMap<>();
    private final SharedPreferences prefs;
    private final SpartanStudio mSpartanStudio;

    public AfrPresetsPanel(MainActivity mainActivity, TripComputer tripComputer) {
        prefs = mainActivity.getSharedPreferences("afr_presets", Context.MODE_PRIVATE);

        presets.put("sportPlusAfr", (double) prefs.getFloat("sportPlusAfr", 12.7f));
        presets.put("sportAfr", (double) prefs.getFloat("sportAfr", 14.7f));
        presets.put("ecoAfr", (double) prefs.getFloat("ecoAfr", 15.4f));
        presets.put("ecoPlusAfr", (double) prefs.getFloat("ecoPlusAfr", 16.4f));

        toggleAfrGroup = mainActivity.findViewById(R.id.toggleAfrGroup);
        this.mSpartanStudio = tripComputer.mSpartanStudio;

        Map<Integer, String> buttonPresetMap = new HashMap<>();
        buttonPresetMap.put(R.id.buttonAfrSportPlus, "sportPlusAfr");
        buttonPresetMap.put(R.id.buttonAfrSport, "sportAfr");
        buttonPresetMap.put(R.id.buttonAfrEco, "ecoAfr");
        buttonPresetMap.put(R.id.buttonAfrEcoPlus, "ecoPlusAfr");

        toggleAfrGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
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

    public void hide() {
        toggleAfrGroup.setVisibility(View.GONE);
    }

    public void show() {
        toggleAfrGroup.setVisibility(View.VISIBLE);
    }
}
