package com.hondaafr.Libs.UI.Scientific.Panels;

import android.widget.Button;

import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.ImageButtonRounded;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class TopButtonsPanel extends Panel {
    private final ImageButtonRounded buttonShowCluster;
    private final Button buttonClear;
    private final ImageButtonRounded buttonToggleEngineSounds;
    private ImageButtonRounded buttonToggleFuel;
    private Button buttonRecord;

    @Override
    public int getContainerId() {
        return R.id.panelTopButtons;
    }

    @Override
    public String getListenerId() {
        return "top_buttons_panel";
    }

    public TopButtonsPanel(MainActivity mainActivity, TripComputer tripComputer, UiView parentView) {
        super(mainActivity, tripComputer, parentView);

        this.buttonRecord = mainActivity.findViewById(R.id.buttonRecord);
        this.buttonToggleFuel = mainActivity.findViewById(R.id.buttonShowFuelPanel);
        this.buttonShowCluster = mainActivity.findViewById(R.id.buttonShowCluster);
        this.buttonToggleEngineSounds = mainActivity.findViewById(R.id.buttonToggleSound);

        buttonClear = mainActivity.findViewById(R.id.buttonClear);
        buttonClear.setOnClickListener(view -> {
            parent.getPanel(ChartPanel.class).clear();
        });

        buttonRecord.setOnClickListener(view -> {
            if (tripComputer.isRecording) {
                tripComputer.stopRecording();
                buttonRecord.setText("Record");
            } else {
                tripComputer.startRecording();
                buttonRecord.setText("Stop");
            }
        });

        buttonToggleFuel.setIconState(false);
        buttonToggleFuel.setOnClickListener(view -> {
            FuelStatsPanel fuelStatsPanel = parent.getPanel(FuelStatsPanel.class);
            fuelStatsPanel.toggleMode();

            if (fuelStatsPanel.isVisible()) {
                buttonToggleFuel.setIconState(true);
            } else {
                buttonToggleFuel.setIconState(false);
            }
        });

        buttonToggleEngineSounds.setIconState(false);
        buttonToggleEngineSounds.setOnClickListener(v -> {
            SoundPanel soundPanel = parent.getPanel(SoundPanel.class);
            soundPanel.toggle();
            buttonToggleEngineSounds.setIconState(soundPanel.isEnabled());
        });

        buttonShowCluster.setOnClickListener(v -> mainActivity.showCluster());
    }

    public void updateFuelButtonState(boolean isVisible) {
        if (buttonToggleFuel != null) {
            buttonToggleFuel.setIconState(isVisible);
        }
    }

}
