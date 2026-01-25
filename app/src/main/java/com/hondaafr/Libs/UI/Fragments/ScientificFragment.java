package com.hondaafr.Libs.UI.Fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.ScientificView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class ScientificFragment extends BaseFragment {
    private ScientificView scientificView;
    private TripComputer tripComputer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scientific, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tripComputer = ((MainActivity) requireActivity()).getTripComputer();
        scientificView = new ScientificView((MainActivity) requireActivity(), tripComputer, view);
        scientificView.setVisibility(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (scientificView != null) {
            scientificView.setActive(true);
            scientificView.onResume(requireContext());
        }
    }

    @Override
    public void onPause() {
        if (scientificView != null) {
            scientificView.onPause(requireContext());
            scientificView.setActive(false);
        }
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (scientificView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            scientificView.onStart(requireContext());
        }
    }

    @Override
    public void onStop() {
        if (scientificView != null) {
            scientificView.onStop(requireContext());
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (scientificView != null) {
            scientificView.onDestroy(requireContext());
        }
        super.onDestroy();
    }

    @Override
    public void onEnterPip() {
        if (scientificView != null) {
            scientificView.showPipView();
        }
    }

    @Override
    public void onExitPip() {
        if (scientificView != null) {
            scientificView.restoreFullView();
        }
    }
}

