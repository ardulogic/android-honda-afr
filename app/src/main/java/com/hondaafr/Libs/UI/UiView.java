package com.hondaafr.Libs.UI;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.annotation.RequiresApi;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Helpers.AfrComputer.AfrComputer;
import com.hondaafr.Libs.Helpers.Debuggable;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.MainActivity;

abstract public class UiView extends Debuggable implements TripComputerListener {

    protected final MainActivity mainActivity;
    protected final TripComputer tripComputer;
    protected final AfrComputer afrComputer;
    protected final Panel[] panels;
    private final View rootView;
    private final View container;
    protected boolean isInPip = false;

    protected boolean isNightMode = false;

    abstract String getListenerId();

    abstract public int getContainerId();

    public UiView(MainActivity mainActivity, TripComputer tripComputer, AfrComputer afrComputer, View rootView) {
        this.mainActivity = mainActivity;
        this.tripComputer = tripComputer;
        this.afrComputer = afrComputer;
        this.rootView = rootView;
        this.container = rootView.findViewById(getContainerId());
        panels = initPanels();

        attachListener();
        notifyPanelsLoaded();
    }

    private void attachListener() {
        d("Attaching listener", 1);
        tripComputer.addListener(getListenerId(), this);
    }

    private void detachListener() {
        d("Detaching listener", 1);
        tripComputer.removeListener(getListenerId());
    }

    private void notifyPanelsLoaded() {
        for (Panel p : panels) {
            p.onParentReady();

            if (this.isVisible()) {
                p.attachTripComputerListener();
                p.attachAfrComputerListener();
            }
        }
    }

    public View getContainerView() {
        return this.container;
    }

    public View getRootView() {
        return rootView;
    }

    abstract public Panel[] initPanels();

    public void setVisibility(boolean visible) {
        container.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (visible) {
            attachPanelListeners();
            attachListener();
        } else {
            detachPanelListeners();
            detachListener();
        }
    }

    public void setActive(boolean active) {
        if (active) {
            attachPanelListeners();
            attachListener();
        } else {
            detachPanelListeners();
            detachListener();
        }
    }

    private void attachPanelListeners() {
        for (Panel p : panels) {
            p.attachTripComputerListener();
            p.attachAfrComputerListener();
        }
    }

    private void detachPanelListeners() {
        for (Panel p : panels) {
            // Don't detach listeners if panel is in PiP mode and visible in PiP
            if (!p.isInPip() || !p.visibleInPip()) {
                p.detachTripComputerListener();
                p.detachAfrComputerListener();
            }
        }
    }

    public boolean isVisible() {
        return container.getVisibility() == View.VISIBLE;
    }

    public void showPipView() {
        isInPip = true;
        for (Panel p : panels) {
            p.enterPip();
        }
    }

    public void restoreFullView() {
        isInPip = false;
        for (Panel p : panels) {
            p.exitPip();
        }
    }

    public void toggleSystemUI() {
        View decorView = mainActivity.getWindow().getDecorView();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController insetsController = mainActivity.getWindow().getInsetsController();
            if (insetsController != null) {
                // Check if system bars (status + navigation) are visible
                boolean isVisible = mainActivity.getWindow()
                        .getDecorView()
                        .getRootWindowInsets()
                        .isVisible(WindowInsets.Type.systemBars());

                if (isVisible) {
                    // Hide both status and navigation bars
                    insetsController.hide(WindowInsets.Type.systemBars());
                    insetsController.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                } else {
                    // Show both status and navigation bars
                    insetsController.show(WindowInsets.Type.systemBars());
                }
            }
        } else {
            // For Android 10 and below
            int uiOptions = decorView.getSystemUiVisibility();
            boolean isVisible = (uiOptions & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;

            if (isVisible) {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            } else {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Panel> T getPanel(Class<T> panelClass) {
        for (Panel p : panels) {
            if (panelClass.isInstance(p)) {
                return (T) p;
            }
        }
        return null;
    }

    @Override
    public void onNightModeUpdated(boolean isNight) {
        if (this.isNightMode != isNight) {
            this.isNightMode = isNight;
            onNightModeChanged(isNight);
        }
    }

    protected void onNightModeChanged(boolean isNight) {
    }


    public void onPause(Context context) {
        for (Panel p : panels) {
            p.onPause(context);
        }
    }

    public void onResume(Context context) {
        for (Panel p : panels) {
            p.onResume(context);

            if (isVisible()) {
                // Re-attach listeners in case they were detached
                p.attachTripComputerListener();
                p.attachAfrComputerListener();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void onStart(Context context) {
        for (Panel p : panels) {
            p.onStart(context);
        }
    }

    public void onStop(Context context) {
        for (Panel p : panels) {
            p.onStop(context);
        }
    }

    public void onDestroy(Context context) {
        for (Panel p : panels) {
            p.onDestroy(context);
        }
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

}
