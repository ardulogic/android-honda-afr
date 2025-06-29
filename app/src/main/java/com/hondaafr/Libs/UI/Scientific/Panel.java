package com.hondaafr.Libs.UI.Scientific;

import android.view.View;

abstract public class Panel {

    abstract public View getContainerView();

    protected boolean visibleBeforePip = false;

    public boolean visibleInPip() {
        return false;
    }

    public void enterPip() {
        visibleBeforePip = getContainerView().getVisibility() == View.VISIBLE;

        if (!visibleInPip()) {
            getContainerView().setVisibility(View.GONE);
        }
    }

    public void exitPip() {
        if (visibleBeforePip)
            getContainerView().setVisibility(View.VISIBLE);
    }
}
