package com.hondaafr.Libs.UI.Scientific;

import android.widget.TextView;

import com.hondaafr.MainActivity;
import com.hondaafr.R;


public class GenericStatusPanel {

    private final TextView textStatusSpartan;
    private final TextView textStatusObd;
    private final TextView textStatusGeneric;

    public GenericStatusPanel(MainActivity mainActivity) {

        textStatusGeneric = mainActivity.findViewById(R.id.textStatusGeneric);
        textStatusObd = mainActivity.findViewById(R.id.textStatusObd);
        textStatusSpartan = mainActivity.findViewById(R.id.textStatusSpartan);
    }

    public void onSpartanUpdate(String message) {
        textStatusSpartan.setText(message);
    }

    public void onObdUpdate(String message) {
        textStatusObd.setText(message);
    }

    public void onGenericUpdate(String message) {
        textStatusGeneric.setText(message);
    }

}
