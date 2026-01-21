package com.hondaafr.Libs.UI.Fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.ClusterView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ClusterFragment extends Fragment implements PipAware {
    private ClusterView clusterView;
    private TripComputer tripComputer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cluster, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tripComputer = ((MainActivity) requireActivity()).getTripComputer();
        clusterView = new ClusterView((MainActivity) requireActivity(), tripComputer, view);
        clusterView.setVisibility(true);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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

