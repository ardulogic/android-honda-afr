package com.hondaafr.Libs.UI.Scientific.Panels;

import android.content.Context;
import android.view.View;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Helpers.Debuggable;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.UiView;
import com.hondaafr.MainActivity;

abstract public class Panel extends Debuggable implements TripComputerListener {

    protected final MainActivity mainActivity;
    protected final TripComputer tripComputer;
    protected final View rootView;
    protected final View container;
    protected final UiView parent;

    protected boolean visibleBeforePip = false;
    protected boolean isInPip = false;
    protected boolean isNightMode = false;

    public boolean visibleInPip() {
        return false;
    }

    public boolean isInPip() {
        return isInPip;
    }

    public Panel (MainActivity mainActivity, TripComputer tripComputer, UiView view) {
        this.mainActivity = mainActivity;
        this.tripComputer = tripComputer;
        this.rootView = view.getRootView();
        this.container = rootView.findViewById(getContainerId());
        this.parent = view;
    }

    public void attachTripComputerListener() {
        d("Attaching trip computer listener", VERBOSE);
        tripComputer.addListener(getListenerId(), this);
    }

    public void detachTripComputerListener() {
        d("Detaching trip computer listener", VERBOSE);
        tripComputer.removeListener(getListenerId());
    }

    public boolean isParentVisible() {
        return parent.isVisible();
    }

    public void setVisibility(boolean visible) {
        container.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public View[] getViewsHiddenInPip() {
        return new View[]{};
    }

    public void isVisible(boolean visible) {
        container.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    abstract public int getContainerId();
    abstract public String getListenerId();

    public View getContainerView() {
        return this.container;
    }

    public void enterPip() {
        isInPip = true;
        visibleBeforePip = getContainerView().getVisibility() == View.VISIBLE;

        if (!visibleInPip()) {
            getContainerView().setVisibility(View.GONE);
        } else {
            View[] viewsHiddenInPip = getViewsHiddenInPip();
            if (viewsHiddenInPip != null) {
                for (View view : viewsHiddenInPip) {
                    if (view != null) {
                        view.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    public void exitPip() {
        isInPip = false;

        if (visibleBeforePip)
            getContainerView().setVisibility(View.VISIBLE);

        // Restore visibility of views hidden during PiP mode
        View[] viewsHiddenInPip = getViewsHiddenInPip();
        if (viewsHiddenInPip != null) {
            for (View view : viewsHiddenInPip) {
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void onPause(Context context) {
    }

    public void onResume(Context context) {
    }

    public void onStart(Context context) {
    }

    public void onStop(Context context) {
    }

    public void onDestroy(Context context) {
    }

    @Override
    public void onGpsUpdate(Double speed, double distanceIncrement) {

    }

    @Override
    public void onGpsPulse(PhoneGps gps) {

    }

    @Override
    public void onAfrPulse(boolean isActive) {

    }

    @Override
    public void onAfrTargetValue(double targetAfr) {

    }

    @Override
    public void onAfrValue(Double afr) {

    }

    @Override
    public void onObdPulse(boolean isActive) {

    }

    @Override
    public void onObdActivePidsChanged() {

    }

    @Override
    public void onObdValue(ObdReading reading) {

    }

    @Override
    public void onCalculationsUpdated() {

    }

    @Override
    public void onNightModeUpdated(boolean isNightMode) {
        if (this.isNightMode != isNightMode) {
            this.isNightMode = isNightMode;

            onNightModeChanged(isNightMode);
        }
    }

    /**
     * Is called only when night mode changes, not only updates value
     * @param isNightMode
     */
    public void onNightModeChanged(boolean isNightMode) {

    }

    /**
     * Is called when parent has finished its own constructor
     */
    public void onParentReady() {
    }
}
