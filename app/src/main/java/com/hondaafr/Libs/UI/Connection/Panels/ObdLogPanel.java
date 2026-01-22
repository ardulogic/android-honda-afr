package com.hondaafr.Libs.UI.Connection.Panels;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hondaafr.Libs.Devices.Obd.ObdLogStore;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Fragments.ObdLog.ObdLogAdapter;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import java.util.List;

public class ObdLogPanel extends Panel implements ObdLogStore.LogListener {

    private ObdLogAdapter obdLogAdapter;
    private RecyclerView recyclerObdLog;
    private boolean autoScrollEnabled = true;
    private Button autoScrollButton;
    private LinearLayout layoutObdLogSection;

    @Override
    public int getContainerId() {
        return R.id.layoutObdLogSection;
    }

    @Override
    public String getListenerId() {
        return "obd_log_panel";
    }

    public ObdLogPanel(MainActivity mainActivity, TripComputer tripComputer, UiView parent) {
        super(mainActivity, tripComputer, parent);
        
        recyclerObdLog = rootView.findViewById(R.id.recyclerObdLog);
        obdLogAdapter = new ObdLogAdapter();
        recyclerObdLog.setLayoutManager(new LinearLayoutManager(mainActivity));
        recyclerObdLog.setAdapter(obdLogAdapter);
        obdLogAdapter.setMaxItems(Integer.MAX_VALUE);
        
        autoScrollButton = rootView.findViewById(R.id.buttonAutoScroll);
        updateAutoScrollButton();
        autoScrollButton.setOnClickListener(v -> {
            autoScrollEnabled = !autoScrollEnabled;
            updateAutoScrollButton();
        });
        
        Button clearButton = rootView.findViewById(R.id.buttonClearObdLog);
        clearButton.setOnClickListener(v -> ObdLogStore.clear());
        
        layoutObdLogSection = rootView.findViewById(R.id.layoutObdLogSection);
    }

    @Override
    public void onResume(Context context) {
        ObdLogStore.addListener(this);
    }

    @Override
    public void onPause(Context context) {
        ObdLogStore.removeListener(this);
    }

    @Override
    public void onLogUpdated(List<ObdLogStore.LogEntry> entries) {
        if (obdLogAdapter == null) {
            return;
        }
        obdLogAdapter.setItems(entries);
        if (autoScrollEnabled && entries != null && !entries.isEmpty()) {
            if (!recyclerObdLog.canScrollVertically(1)) {
                recyclerObdLog.scrollToPosition(entries.size() - 1);
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

    public void setAutoScrollEnabled(boolean enabled) {
        autoScrollEnabled = enabled;
        updateAutoScrollButton();
    }

    public void setMaxItems(int maxItems) {
        if (obdLogAdapter != null) {
            obdLogAdapter.setMaxItems(maxItems);
        }
    }

    public void setShowTimestamp(boolean show) {
        if (obdLogAdapter != null) {
            obdLogAdapter.setShowTimestamp(show);
        }
    }
}

