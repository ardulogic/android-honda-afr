package com.hondaafr.Libs.UI.Connection.Panels;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hondaafr.Libs.Bluetooth.BluetoothStates;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Devices.Obd.ObdLogStore;
import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
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
    
    private final IntentFilter btUiUpdateIntentFilter = new IntentFilter(BluetoothService.ACTION_UI_UPDATE);
    
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int key = intent.getIntExtra(BluetoothStates.KEY_EVENT, -1);
            
            switch (key) {
                case BluetoothStates.EVENT_BT_STATE_CHANGED:
                    onBluetoothStateChanged(
                            intent.getIntExtra(BluetoothStates.KEY_DATA, -1),
                            intent.getStringExtra(BluetoothStates.KEY_DEVICE_ID)
                    );
                    break;
                    
                case BluetoothStates.EVENT_SERVICE_STATE_CHANGED:
                    onBluetoothServiceStateChanged(intent.getIntExtra(BluetoothStates.KEY_DATA, -1));
                    break;
            }
        }
    };

    @Override
    public int getContainerId() {
        return R.id.layoutObdLogSection;
    }

    @Override
    public String getListenerId() {
        return "obd_log_panel";
    }

    @Override
    public boolean visibleInPip() {
        return true;
    }

    public ObdLogPanel(MainActivity mainActivity, TripComputer tripComputer, AfrComputer afrComputer, UiView parent) {
        super(mainActivity, tripComputer, afrComputer, parent);
        
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter, Context.RECEIVER_EXPORTED);
            } else {
                mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter);
            }
        } catch (IllegalArgumentException e) {
            Log.w("ObdLogPanel", "Receiver was already registered");
        }
    }

    @Override
    public void onPause(Context context) {
        try {
            mainActivity.unregisterReceiver(btReceiver);
        } catch (IllegalArgumentException e) {
            Log.w("ObdLogPanel", "Receiver was already unregistered");
        }
        // Don't remove listener if we're in PiP mode - we want logs to keep updating
        // Also check if the activity is in PiP mode (in case isInPip hasn't been set yet)
        boolean activityInPip = context instanceof android.app.Activity && 
            ((android.app.Activity) context).isInPictureInPictureMode();
        if (!isInPip && !activityInPip) {
            ObdLogStore.removeListener(this);
        }
    }

    @Override
    public void enterPip() {
        super.enterPip();
        // Ensure listener is added when entering PiP mode
        ObdLogStore.addListener(this);
    }

    @Override
    public void exitPip() {
        super.exitPip();
        // Listener will be re-added in onResume when exiting PiP
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

    private void onBluetoothStateChanged(int state, String deviceId) {
        // Only log OBD-related status changes
        if ("obd".equals(deviceId)) {
            String statusMessage = BluetoothStates.labelOfState(state);
            ObdLogStore.logBt(statusMessage);
        }
    }

    private void onBluetoothServiceStateChanged(int state) {
        // Log service state changes to OBD log
        String statusMessage;
        if (state == BluetoothStates.STATE_SERVICE_STARTED) {
            statusMessage = "Bluetooth service started";
        } else if (state == BluetoothStates.STATE_SERVICE_STOPPED) {
            statusMessage = "Bluetooth service stopped";
        } else {
            return;
        }
        ObdLogStore.logBt(statusMessage);
    }
}

