package com.hondaafr.Libs.UI.Connection.Panels;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class AfrLogPanel extends Panel {

    private RecyclerView recyclerAfrLog;
    private LinearLayout layoutAfrLogSection;

    @Override
    public int getContainerId() {
        return R.id.layoutAfrLogSection;
    }

    @Override
    public String getListenerId() {
        return "afr_log_panel";
    }

    public AfrLogPanel(MainActivity mainActivity, TripComputer tripComputer, UiView parent) {
        super(mainActivity, tripComputer, parent);
        
        recyclerAfrLog = rootView.findViewById(R.id.recyclerAfrLog);
        recyclerAfrLog.setLayoutManager(new LinearLayoutManager(mainActivity));
        
        // TODO: Setup AFR log adapter when available
        Button clearButton = rootView.findViewById(R.id.buttonClearAfrLog);
        clearButton.setOnClickListener(v -> {
            // TODO: Clear AFR log when available
        });
        
        layoutAfrLogSection = rootView.findViewById(R.id.layoutAfrLogSection);
    }
}

