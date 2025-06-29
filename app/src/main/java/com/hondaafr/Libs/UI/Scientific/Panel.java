package com.hondaafr.Libs.UI.Scientific;

import android.view.View;

abstract public class Panel {

    abstract public View getContainerView();

    protected boolean visibleBeforePip = false;
    protected boolean isInPip = false;

    public boolean visibleInPip() {
        return false;
    }

    public boolean isInPip() {
        return isInPip;
    }

    public void enterPip() {
        isInPip = true;
        visibleBeforePip = getContainerView().getVisibility() == View.VISIBLE;

        if (!visibleInPip()) {
            getContainerView().setVisibility(View.GONE);
        }
    }

    public void exitPip() {
        isInPip = false;

        if (visibleBeforePip)
            getContainerView().setVisibility(View.VISIBLE);
    }
}
