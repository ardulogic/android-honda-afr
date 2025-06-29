package com.hondaafr.Libs.UI.Scientific;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.ImageButtonRounded;
import com.hondaafr.Libs.UI.ScientificView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class TopButtonsPanel {
    public final LinearLayout panel;
    private final TripComputer mTripComputer;
    private final MainActivity mainActivity;
    private final Context context;
    private final ImageButtonRounded buttonShowCluster;
    private final ScientificView scView;
    private final Button buttonClear;
    private final ImageButtonRounded buttonToggleEngineSounds;

    private ImageButtonRounded buttonToggleFuel;
    private Button buttonRecord;

    public TopButtonsPanel(MainActivity mainActivity, ScientificView scView, TripComputer mTripComputer) {
        this.context = mainActivity;
        this.mainActivity = mainActivity;
        this.mTripComputer = mTripComputer;
        this.scView = scView;

        this.panel = mainActivity.findViewById(R.id.panelTopButtons);
        this.buttonRecord = mainActivity.findViewById(R.id.buttonRecord);
        this.buttonToggleFuel = mainActivity.findViewById(R.id.buttonShowFuelPanel);
        this.buttonShowCluster = mainActivity.findViewById(R.id.buttonShowCluster);
        this.buttonToggleEngineSounds = mainActivity.findViewById(R.id.buttonToggleSound);

        buttonClear = mainActivity.findViewById(R.id.buttonClear);
        buttonClear.setOnClickListener(view -> {
            scView.chartPanel.clear();
        });

        buttonRecord.setOnClickListener(view -> {
            if (mTripComputer.isRecording) {
                mTripComputer.stopRecording();
                buttonRecord.setText("Record");
            } else {
                mTripComputer.startRecording();
                buttonRecord.setText("Stop");
            }
        });

        buttonToggleFuel.setIconState(false);
        buttonToggleFuel.setOnClickListener(view -> {
            scView.fuelStatsPanel.toggleMode();

            if (scView.fuelStatsPanel.isVisible()) {
                buttonToggleFuel.setIconState(true);
            } else {
                buttonToggleFuel.setIconState(false);
            }
        });

        buttonToggleEngineSounds.setIconState(false);
        buttonToggleEngineSounds.setOnClickListener(v -> {
                scView.soundPanel.toggle();
                buttonToggleEngineSounds.setIconState(scView.soundPanel.isEnabled());
                Log.d("TopButtonsPanel", "Sound enabled:" + scView.soundPanel.isEnabled());
            });

        buttonShowCluster.setOnClickListener(v -> mainActivity.showCluster());
    }
}
