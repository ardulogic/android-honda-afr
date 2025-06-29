package com.hondaafr.Libs.UI.Scientific;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;

import com.hondaafr.Libs.Bluetooth.BluetoothStates;
import com.hondaafr.Libs.Bluetooth.BluetoothUtils;
import com.hondaafr.Libs.Bluetooth.Services.BluetoothService;
import com.hondaafr.Libs.Helpers.Permissions;
import com.hondaafr.Libs.Helpers.Studio;
import com.hondaafr.Libs.Helpers.TimeChart;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import java.util.Objects;

public class ChartPanel {

    private final LinearLayout panel;
    private final TripComputer mTripComputer;
    private final MainActivity mainActivity;
    private final Context context;
    private TimeChart mChart;

    private long startTimestamp = 0L;

    public ChartPanel(MainActivity mainActivity, TripComputer mTripComputer) {
        this.mTripComputer = mTripComputer;
        this.mainActivity = mainActivity;
        this.context = mainActivity;

        panel = mainActivity.findViewById(R.id.layoutConnection);

        mChart = new TimeChart(mainActivity, mainActivity.findViewById(R.id.graph));
        mChart.init();
        mChart.invalidate();

    }

    public void onTargetAfrUpdated(double targetAfr) {
        mChart.setLimitLines(null, null, (float) targetAfr);
    }

    public void onAfrUpdated(Double afr) {
        mainActivity.runOnUiThread(() -> {
            float time = System.currentTimeMillis() - startTimestamp; // Cant use full timestamp, too big
            mChart.addToData(time, afr.floatValue(), true);
        });
    }

    public void clear() {
        mChart.clearData();
        startTimestamp = System.currentTimeMillis();
    }

}
