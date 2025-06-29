package com.hondaafr.Libs.UI.Scientific;

import android.widget.TextView;

import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class GenericStatusPanel {

    private final TextView textStatusSpartan;
    private final TextView textStatusObd;
    private final TextView textStatusGeneric;

    private String lastSpartanMessage = "";
    private String secondLastSpartanMessage = "";

    private String lastObdMessage = "";
    private String secondLastObdMessage = "";

    public GenericStatusPanel(MainActivity mainActivity) {
        textStatusGeneric = mainActivity.findViewById(R.id.textStatusGeneric);
        textStatusObd = mainActivity.findViewById(R.id.textStatusObd);
        textStatusSpartan = mainActivity.findViewById(R.id.textStatusSpartan);
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
