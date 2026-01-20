package com.hondaafr.Libs.UI.Scientific.Panels;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.ScientificView;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class GenericStatusPanel  extends Panel {

    private final TextView textStatusSpartan;
    private final TextView textStatusObd;
    private final TextView textStatusGeneric;

    private String lastSpartanMessage = "";
    private String secondLastSpartanMessage = "";

    private String lastObdMessage = "";
    private String secondLastObdMessage = "";

    @Override
    public int getContainerId() {
        return R.id.layoutConnStatus;
    }

    @Override
    public String getListenerId() {
        return "generic_status_panel";
    }

    public GenericStatusPanel(MainActivity mainActivity, TripComputer tripComputer, UiView view) {
        super(mainActivity, tripComputer, view);

        textStatusGeneric = rootView.findViewById(R.id.textStatusGeneric);
        textStatusObd = rootView.findViewById(R.id.textStatusObd);
        textStatusSpartan = rootView.findViewById(R.id.textStatusSpartan);
    }

    private String stripNewlines(String message) {
        return message.replace("\n", " ").replace("\r", " ");
    }

    public void onSpartanUpdate(String message) {
        message = stripNewlines(message);
        secondLastSpartanMessage = lastSpartanMessage;
        lastSpartanMessage = message;
        textStatusSpartan.setText(lastSpartanMessage + "\n" + secondLastSpartanMessage);
    }

    public void onObdUpdate(String message) {
        message = stripNewlines(message);
        secondLastObdMessage = lastObdMessage;
        lastObdMessage = message;
        textStatusObd.setText(lastObdMessage + "\n" + secondLastObdMessage);
    }

    public void onGenericUpdate(String message) {
        message = stripNewlines(message);
        textStatusGeneric.setText(message);
    }

}

