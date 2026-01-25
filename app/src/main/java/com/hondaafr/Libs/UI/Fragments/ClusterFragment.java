package com.hondaafr.Libs.UI.Fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.ClusterView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ClusterFragment extends BaseFragment {
    private ClusterView clusterView;
    private TripComputer tripComputer;
    private AfrComputer afrComputer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cluster, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity mainActivity = (MainActivity) requireActivity();
        tripComputer = mainActivity.getTripComputer();
        afrComputer = mainActivity.getAdaptiveAfrComputer();
        clusterView = new ClusterView(mainActivity, tripComputer, afrComputer, view);
        clusterView.setVisibility(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (clusterView != null) {
            clusterView.setActive(true);
            clusterView.onResume(requireContext());
        }
    }

    @Override
    public void onPause() {
        if (clusterView != null) {
            clusterView.onPause(requireContext());
            clusterView.setActive(false);
        }
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (clusterView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clusterView.onStart(requireContext());
        }
    }

    @Override
    public void onStop() {
        if (clusterView != null) {
            clusterView.onStop(requireContext());
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (clusterView != null) {
            clusterView.onDestroy(requireContext());
        }
        super.onDestroy();
    }

    @Override
    public void onEnterPip() {
        if (clusterView != null) {
            clusterView.showPipView();
        }
    }

    @Override
    public void onExitPip() {
        if (clusterView != null) {
            clusterView.restoreFullView();
        }
    }
}

