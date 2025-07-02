package com.hondaafr.Libs.UI.Cluster.Sound;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.hondaafr.R;

public class ClusterConfigurationDialogFragment extends DialogFragment {

    public interface ConfigCallback {
        void onConfigSaved(boolean beepOnAfr, float afr,
                           boolean beepOnLp100, float lp100,
                           boolean beepOnLph, float lph);
    }

    private static final String ARG_BEEP_ON_AFR = "beep_on_afr";
    private static final String ARG_AFR = "afr";
    private static final String ARG_BEEP_ON_LPH = "beep_on_lph";
    private static final String ARG_LPH = "lph";
    private static final String ARG_BEEP_ON_LP100 = "beep_on_lp100";
    private static final String ARG_LP100 = "lp100";

    private ConfigCallback callback;

    public static ClusterConfigurationDialogFragment newInstance(
            boolean beepOnAfr, float afr,
            boolean beepOnLp100, float lp100,
            boolean beepOnLph, float lph,
            ConfigCallback callback) {

        ClusterConfigurationDialogFragment fragment = new ClusterConfigurationDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_BEEP_ON_AFR, beepOnAfr);
        args.putFloat(ARG_AFR, afr);
        args.putBoolean(ARG_BEEP_ON_LPH, beepOnLph);
        args.putFloat(ARG_LPH, lph);
        args.putBoolean(ARG_BEEP_ON_LP100, beepOnLp100);
        args.putFloat(ARG_LP100, lp100);
        fragment.setArguments(args);
        fragment.setCallback(callback);
        return fragment;
    }

    public void setCallback(ConfigCallback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View root = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_cluster_configuration, null);

        CheckBox cbBeepOnAfr = root.findViewById(R.id.checkBeepOnLowAfr);
        EditText etAfr = root.findViewById(R.id.editBeepAfr);

        CheckBox cbBeepOnLph = root.findViewById(R.id.checkBeepOnLph);
        EditText etLph = root.findViewById(R.id.editBeepLph);

        CheckBox cbBeepOnLp100 = root.findViewById(R.id.checkBeepOnLp100);
        EditText etLp100 = root.findViewById(R.id.editBeepLp100);

        // Load arguments
        Bundle args = getArguments();
        if (args != null) {
            cbBeepOnAfr.setChecked(args.getBoolean(ARG_BEEP_ON_AFR, false));
            etAfr.setText(String.valueOf(args.getFloat(ARG_AFR, 12.0f)));

            cbBeepOnLph.setChecked(args.getBoolean(ARG_BEEP_ON_LPH, false));
            etLph.setText(String.valueOf(args.getFloat(ARG_LPH, 5.0f)));

            cbBeepOnLp100.setChecked(args.getBoolean(ARG_BEEP_ON_LP100, false));
            etLp100.setText(String.valueOf(args.getFloat(ARG_LP100, 20.0f)));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(root)
                .create();

        Button btnSave = root.findViewById(R.id.btnSave);
        Button btnCancel = root.findViewById(R.id.btnCancel);

        btnSave.setOnClickListener(v -> {
            if (callback != null) {
                boolean beepOnAfr = cbBeepOnAfr.isChecked();
                float afr = parseFloat(etAfr.getText());

                boolean beepOnLph = cbBeepOnLph.isChecked();
                float lph = parseFloat(etLph.getText());

                boolean beepOnLp100 = cbBeepOnLp100.isChecked();
                float lp100 = parseFloat(etLp100.getText());

                callback.onConfigSaved(beepOnAfr, afr, beepOnLp100, lp100, beepOnLph, lph);
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    private float parseFloat(CharSequence s) {
        try {
            return TextUtils.isEmpty(s) ? 0f : Float.parseFloat(s.toString());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
