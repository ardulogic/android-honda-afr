package com.hondaafr.Libs.UI.Fragments;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hondaafr.Libs.Devices.Obd.ObdLogStore;
import com.hondaafr.Libs.UI.Fragments.PipAware;
import com.hondaafr.Libs.UI.Fragments.ObdLog.ObdLogAdapter;
import com.hondaafr.R;

import java.util.List;

public class ObdLogFragment extends Fragment implements ObdLogStore.LogListener, PipAware {
    private ObdLogAdapter adapter;
    private RecyclerView recyclerView;
    private boolean autoScrollEnabled = true;
    private android.widget.Button autoScrollButton;
    private View headerView;
    private boolean inPip = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_obd_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerObdLog);
        adapter = new ObdLogAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        adapter.setMaxItems(Integer.MAX_VALUE);

        headerView = view.findViewById(R.id.layoutObdLogHeader);
        autoScrollButton = view.findViewById(R.id.buttonAutoScroll);
        updateAutoScrollButton();
        autoScrollButton.setOnClickListener(v -> {
            autoScrollEnabled = !autoScrollEnabled;
            updateAutoScrollButton();
        });

        view.findViewById(R.id.buttonClearObdLog).setOnClickListener(v -> {
            ObdLogStore.clear();
        });

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ObdLogStore.addListener(this);
    }

    @Override
    public void onPause() {
        if (!inPip) {
            ObdLogStore.removeListener(this);
        }
        super.onPause();
    }

    @Override
    public void onLogUpdated(List<ObdLogStore.LogEntry> entries) {
        if (adapter == null) {
            return;
        }
        adapter.setItems(entries);
        if (autoScrollEnabled && entries != null && !entries.isEmpty()) {
            if (!recyclerView.canScrollVertically(1)) {
                recyclerView.scrollToPosition(entries.size() - 1);
            }
        }
    }

    private void updateAutoScrollButton() {
        if (autoScrollButton == null) {
            return;
        }
        autoScrollButton.setText(autoScrollEnabled ? "Auto" : "Manual");
        autoScrollButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                autoScrollEnabled ? 0xFF2ECC71 : 0xFF333333));
    }

    @Override
    public void onEnterPip() {
        inPip = true;
        ObdLogStore.addListener(this);
        if (adapter != null) {
            adapter.setShowTimestamp(false);
            adapter.setMaxItems(5);
        }
        updatePipUiState(true);
    }

    @Override
    public void onExitPip() {
        inPip = false;
        if (adapter != null) {
            adapter.setShowTimestamp(true);
            adapter.setMaxItems(Integer.MAX_VALUE);
        }
        updateAutoScrollButton();
        updatePipUiState(false);
    }

    private void updatePipUiState(boolean isInPip) {
        if (headerView == null) {
            return;
        }
        headerView.setVisibility(isInPip ? View.GONE : View.VISIBLE);
    }
}

