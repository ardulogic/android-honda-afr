package com.hondaafr.Libs.UI.Fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.ConnectionView;
import com.hondaafr.Libs.UI.Connection.Panels.ObdLogPanel;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ConnectionFragment extends BaseFragment {
    private ConnectionView connectionView;
    private TripComputer tripComputer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tripComputer = ((MainActivity) requireActivity()).getTripComputer();
        connectionView = new ConnectionView((MainActivity) requireActivity(), tripComputer, view);
        connectionView.setVisibility(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (connectionView != null) {
            connectionView.setActive(true);
            connectionView.onResume(requireContext());
        }
    }

    @Override
    public void onPause() {
        if (connectionView != null) {
            connectionView.onPause(requireContext());
            connectionView.setActive(false);
        }
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (connectionView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            connectionView.onStart(requireContext());
        }
    }

    @Override
    public void onStop() {
        if (connectionView != null) {
            connectionView.onStop(requireContext());
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (connectionView != null) {
            connectionView.onDestroy(requireContext());
        }
        super.onDestroy();
    }

    @Override
    public void onEnterPip() {
        if (connectionView != null) {
            ObdLogPanel obdLogPanel = connectionView.getPanel(ObdLogPanel.class);
            if (obdLogPanel != null) {
                obdLogPanel.setShowTimestamp(false);
                obdLogPanel.setMaxItems(5);
            }
        }
    }

    @Override
    public void onExitPip() {
        if (connectionView != null) {
            ObdLogPanel obdLogPanel = connectionView.getPanel(ObdLogPanel.class);
            if (obdLogPanel != null) {
                obdLogPanel.setShowTimestamp(true);
                obdLogPanel.setMaxItems(Integer.MAX_VALUE);
            }
        }
    }
}
