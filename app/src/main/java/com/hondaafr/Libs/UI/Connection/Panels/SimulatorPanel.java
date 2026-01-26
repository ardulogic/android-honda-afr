package com.hondaafr.Libs.UI.Connection.Panels;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hondaafr.Libs.Devices.Obd.ObdSimulator;
import com.hondaafr.Libs.Devices.Spartan.AfrSimulator;
import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

/**
 * Panel for enabling/disabling the data simulator.
 * Allows testing without physical Bluetooth devices.
 */
public class SimulatorPanel extends Panel {
    private Button buttonEnableSimulator;
    private Button buttonEnableAfr;
    private Button buttonEnableObd;
    private TextView textSimulatorStatus;
    private LinearLayout layoutSimulatorControls;

    @Override
    public int getContainerId() {
        return R.id.layoutSimulatorSection;
    }

    @Override
    public String getListenerId() {
        return "simulator_panel";
    }

    @Override
    public boolean visibleInPip() {
        return false; // Hide simulator controls in PiP mode
    }

    public SimulatorPanel(MainActivity mainActivity, TripComputer tripComputer, AfrComputer afrComputer, UiView parent) {
        super(mainActivity, tripComputer, afrComputer, parent);
        
        buttonEnableSimulator = rootView.findViewById(R.id.buttonEnableSimulator);
        buttonEnableAfr = rootView.findViewById(R.id.buttonEnableAfrSimulator);
        buttonEnableObd = rootView.findViewById(R.id.buttonEnableObdSimulator);
        textSimulatorStatus = rootView.findViewById(R.id.textSimulatorStatus);
        layoutSimulatorControls = rootView.findViewById(R.id.layoutSimulatorControls);
        
        if (buttonEnableSimulator != null) {
            buttonEnableSimulator.setOnClickListener(v -> toggleSimulator());
        }
        
        if (buttonEnableAfr != null) {
            buttonEnableAfr.setOnClickListener(v -> toggleAfrSimulator());
        }
        
        if (buttonEnableObd != null) {
            buttonEnableObd.setOnClickListener(v -> toggleObdSimulator());
        }
        
        updateUI();
    }

    @Override
    public void onResume(Context context) {
        updateUI();
    }

    @Override
    public void onPause(Context context) {
        // Nothing to do
    }

    private void toggleSimulator() {
        boolean afrEnabled = AfrSimulator.isEnabled(mainActivity);
        boolean obdEnabled = ObdSimulator.isEnabled(mainActivity);
        boolean anyEnabled = afrEnabled || obdEnabled;
        
        // Toggle both if none enabled, disable both if any enabled
        boolean newState = !anyEnabled;
        AfrSimulator.setEnabled(mainActivity, newState);
        ObdSimulator.setEnabled(mainActivity, newState);
        
        // Restart the app's Bluetooth connection manager to apply changes
        // The simulators will be picked up on next connection attempt
        updateUI();
    }

    private void toggleAfrSimulator() {
        boolean enabled = !AfrSimulator.isEnabled(mainActivity);
        AfrSimulator.setEnabled(mainActivity, enabled);
        updateUI();
    }

    private void toggleObdSimulator() {
        boolean enabled = !ObdSimulator.isEnabled(mainActivity);
        ObdSimulator.setEnabled(mainActivity, enabled);
        updateUI();
    }

    private void updateUI() {
        boolean afrEnabled = AfrSimulator.isEnabled(mainActivity);
        boolean obdEnabled = ObdSimulator.isEnabled(mainActivity);
        boolean simulatorEnabled = afrEnabled || obdEnabled;
        
        if (buttonEnableSimulator != null) {
            buttonEnableSimulator.setText(simulatorEnabled ? "Disable Simulator" : "Enable Simulator");
            buttonEnableSimulator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    simulatorEnabled ? 0xFFE74C3C : 0xFF2ECC71));
        }
        
        if (buttonEnableAfr != null) {
            buttonEnableAfr.setText(afrEnabled ? "AFR: ON" : "AFR: OFF");
            buttonEnableAfr.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    afrEnabled ? 0xFF2ECC71 : 0xFF7F8C8D));
        }
        
        if (buttonEnableObd != null) {
            buttonEnableObd.setText(obdEnabled ? "OBD: ON" : "OBD: OFF");
            buttonEnableObd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    obdEnabled ? 0xFF2ECC71 : 0xFF7F8C8D));
        }
        
        if (textSimulatorStatus != null) {
            if (simulatorEnabled) {
                StringBuilder status = new StringBuilder("Simulator: ");
                if (afrEnabled && obdEnabled) {
                    status.append("AFR + OBD");
                } else if (afrEnabled) {
                    status.append("AFR only");
                } else if (obdEnabled) {
                    status.append("OBD only");
                }
                textSimulatorStatus.setText(status.toString());
                textSimulatorStatus.setTextColor(0xFF2ECC71);
            } else {
                textSimulatorStatus.setText("Simulator: OFF");
                textSimulatorStatus.setTextColor(0xFF7F8C8D);
            }
        }
        
        if (layoutSimulatorControls != null) {
            layoutSimulatorControls.setVisibility(simulatorEnabled ? LinearLayout.VISIBLE : LinearLayout.GONE);
        }
    }
}

