package com.hondaafr.Libs.UI;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.annotation.RequiresApi;

import com.hondaafr.Libs.Devices.Obd.Readings.ObdReading;
import com.hondaafr.Libs.Devices.Phone.PhoneGps;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.UI.Scientific.Panels.Panel;
import com.hondaafr.MainActivity;

abstract public class UiView implements TripComputerListener {

    protected final MainActivity mainActivity;
    protected final TripComputer tripComputer;
    protected final Panel[] panels;
    private final View container;
    protected boolean isInPip = false;

    protected boolean isNightMode = false;

    public UiView(MainActivity mainActivity, TripComputer tripComputer) {
        this.mainActivity = mainActivity;
        this.tripComputer = tripComputer;
        this.container = mainActivity.findViewById(getContainerId());

        panels = initPanels();
        notifyPanelsLoaded();
    }

    private void notifyPanelsLoaded() {
        for (Panel p : panels) {
            p.onParentReady();

            if (this.isVisible()) {
                p.attachTripComputerListener();
            }
        }
    }

    abstract public int getContainerId();

    public View getContainerView() {
        return this.container;
    }

    abstract public Panel[] initPanels();

    public void setVisibility(boolean visible) {
        container.setVisibility(visible ? View.VISIBLE : View.GONE);

        for (Panel p : panels) {
            if (visible) {
                p.attachTripComputerListener();
            } else {
                p.detachTripComputerListener();
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
                boolean isVisible = mainActivity.getWindow()
                        .getDecorView()
                        .getRootWindowInsets()
                        .isVisible(WindowInsets.Type.navigationBars());

                if (isVisible) {
                    insetsController.hide(WindowInsets.Type.navigationBars());
                    insetsController.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                } else {
                    insetsController.show(WindowInsets.Type.navigationBars());
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
