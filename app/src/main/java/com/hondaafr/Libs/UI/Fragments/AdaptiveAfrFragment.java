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
import com.hondaafr.Libs.UI.AdaptiveAfrView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class AdaptiveAfrFragment extends BaseFragment {
    private AdaptiveAfrView adaptiveAfrView;
    private TripComputer tripComputer;
    private AfrComputer afrComputer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_adaptive_afr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity mainActivity = (MainActivity) requireActivity();
        tripComputer = mainActivity.getTripComputer();
        afrComputer = mainActivity.getAdaptiveAfrComputer();
        adaptiveAfrView = new AdaptiveAfrView(mainActivity, tripComputer, afrComputer, view);
        adaptiveAfrView.setVisibility(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adaptiveAfrView != null) {
            adaptiveAfrView.setActive(true);
            adaptiveAfrView.onResume(requireContext());
        }
    }

    @Override
    public void onPause() {
        if (adaptiveAfrView != null) {
            adaptiveAfrView.onPause(requireContext());
            adaptiveAfrView.setActive(false);
        }
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adaptiveAfrView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            adaptiveAfrView.onStart(requireContext());
        }
    }

    @Override
    public void onStop() {
        if (adaptiveAfrView != null) {
            adaptiveAfrView.onStop(requireContext());
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (adaptiveAfrView != null) {
            adaptiveAfrView.onDestroy(requireContext());
        }
        super.onDestroy();
    }

    @Override
    public void onEnterPip() {
        if (adaptiveAfrView != null) {
            // Individual chart panels handle hiding RPM and MAP charts via getViewsHiddenInPip()
            adaptiveAfrView.showPipView();
        }
    }

    @Override
    public void onExitPip() {
        if (adaptiveAfrView != null) {
            adaptiveAfrView.restoreFullView();
        }
    }
}
