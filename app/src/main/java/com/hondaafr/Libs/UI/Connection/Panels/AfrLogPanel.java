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
import com.hondaafr.Libs.Devices.Spartan.AfrLogStore;
import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Fragments.AfrLog.AfrLogAdapter;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import java.util.List;

public class AfrLogPanel extends Panel implements AfrLogStore.LogListener {

    private AfrLogAdapter afrLogAdapter;
    private RecyclerView recyclerAfrLog;
    private LinearLayout layoutAfrLogSection;
    
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
            }
        }
    };

    @Override
    public int getContainerId() {
        return R.id.layoutAfrLogSection;
    }

    @Override
    public String getListenerId() {
        return "afr_log_panel";
    }

    @Override
    public boolean visibleInPip() {
        return true;
    }

    public AfrLogPanel(MainActivity mainActivity, TripComputer tripComputer, AfrComputer afrComputer, UiView parent) {
        super(mainActivity, tripComputer, afrComputer, parent);
        
        recyclerAfrLog = rootView.findViewById(R.id.recyclerAfrLog);
        afrLogAdapter = new AfrLogAdapter();
        recyclerAfrLog.setLayoutManager(new LinearLayoutManager(mainActivity));
        recyclerAfrLog.setAdapter(afrLogAdapter);
        afrLogAdapter.setMaxItems(Integer.MAX_VALUE);
        
        Button clearButton = rootView.findViewById(R.id.buttonClearAfrLog);
        clearButton.setOnClickListener(v -> AfrLogStore.clear());
        
        layoutAfrLogSection = rootView.findViewById(R.id.layoutAfrLogSection);
    }

    @Override
    public void onResume(Context context) {
        AfrLogStore.addListener(this);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter, Context.RECEIVER_EXPORTED);
            } else {
                mainActivity.registerReceiver(btReceiver, btUiUpdateIntentFilter);
            }
        } catch (IllegalArgumentException e) {
            Log.w("AfrLogPanel", "Receiver was already registered");
        }
    }

    @Override
    public void onPause(Context context) {
        try {
            mainActivity.unregisterReceiver(btReceiver);
        } catch (IllegalArgumentException e) {
            Log.w("AfrLogPanel", "Receiver was already unregistered");
        }
        // Don't remove listener if we're in PiP mode - we want logs to keep updating
        boolean activityInPip = context instanceof android.app.Activity && 
            ((android.app.Activity) context).isInPictureInPictureMode();
        if (!isInPip && !activityInPip) {
            AfrLogStore.removeListener(this);
        }
    }

    @Override
    public void enterPip() {
        super.enterPip();
        // Ensure listener is added when entering PiP mode
        AfrLogStore.addListener(this);
    }

    @Override
    public void exitPip() {
        super.exitPip();
        // Listener will be re-added in onResume when exiting PiP
    }

    @Override
    public void onLogUpdated(List<AfrLogStore.LogEntry> entries) {
        if (afrLogAdapter == null) {
            return;
        }
        afrLogAdapter.setItems(entries);
    }

    private void onBluetoothStateChanged(int state, String deviceId) {
        if ("spartan".equals(deviceId)) {
            String statusMessage = BluetoothStates.labelOfState(state);
            AfrLogStore.logBt(statusMessage);
        }
    }
}

