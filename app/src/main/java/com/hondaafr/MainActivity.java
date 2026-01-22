package com.hondaafr;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Rational;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.hondaafr.Libs.Bluetooth.Services.BluetoothForegroundService;
import com.hondaafr.Libs.Helpers.Permissions;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.Fragments.PipAware;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "main_pager";
    private static final String PREF_LAST_PAGE = "last_page";
    private MainActivity mContext;
    private boolean canEnterPip = false;
    private TripComputer mTripComputer;
    private ViewPager2 viewPager;
    private MainPagerAdapter pagerAdapter;
    private com.hondaafr.Libs.Bluetooth.BluetoothConnectionManager bluetoothConnectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Permissions.askForAllPermissions(this);

        mContext = this;

        // Keep the screen on while this activity is visible
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTripComputer = new TripComputer(this);
        bluetoothConnectionManager = new com.hondaafr.Libs.Bluetooth.BluetoothConnectionManager(this, mTripComputer);
        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        // Keep all pages in memory to prevent fragment destruction/recreation
        // This helps with map tile loading and other fragment state preservation
        viewPager.setOffscreenPageLimit(4); // Keep all 5 pages (current + 4 offscreen)
        int startPage = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(PREF_LAST_PAGE, MainPagerAdapter.PAGE_CLUSTER);
        viewPager.setCurrentItem(startPage, false);
        mTripComputer.setObdForFuelConsumption(false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                boolean needsFuelConsumption = position == MainPagerAdapter.PAGE_CLUSTER
                        || position == MainPagerAdapter.PAGE_MAP;
                mTripComputer.setObdForFuelConsumption(needsFuelConsumption);
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putInt(PREF_LAST_PAGE, position)
                        .apply();
            }
        });

//        keepInBackground();
    }

    public void keepInBackground() {
        Intent svc = new Intent(this, BluetoothForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
    }

    public void showCluster() {
        viewPager.setCurrentItem(MainPagerAdapter.PAGE_CLUSTER, true);
    }

    public void showScientific() {
        viewPager.setCurrentItem(MainPagerAdapter.PAGE_SCIENTIFIC, true);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("MainActivity", "onStop");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("MainActivity", "onStart");
        if (bluetoothConnectionManager != null) {
            bluetoothConnectionManager.onStart();
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        Log.d("MainActivity", "onUserLeaveHint");

        if (!canEnterPip) {
            canEnterPip = true;
            return;
        }

        new Handler().post(() -> {
            Rational aspectRatio = new Rational(1, 1);
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();

            enterPictureInPictureMode(params);
        });
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        Log.d("MainActivity", "onPictureInPictureModeChanged");


        if (isInPictureInPictureMode) {
            dispatchPipState(true);
        } else {
            dispatchPipState(false);
        }
    }

    @Override
    public void onResume() {
        Log.d("MainActivity", "onResume");
        super.onResume();

        mTripComputer.onResume(this);
        if (bluetoothConnectionManager != null) {
            bluetoothConnectionManager.onResume();
        }
    }

    @Override
    public void onPause() {
        Log.d("MainActivity", "onPause");
        super.onPause();

        mTripComputer.onPause(this);
    }

    @Override
    protected void onDestroy() {
        Log.d("Lifecycle", "onDestroy");

        if (bluetoothConnectionManager != null) {
            bluetoothConnectionManager.onDestroy();
        }
        mTripComputer.onDestroy(this);

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Permissions.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, do the location-related task.
            // You can use the location now.
        } else {
            Log.e("Permissions", "Permission denied for : " + requestCode);
        }
    }

    public TripComputer getTripComputer() {
        return mTripComputer;
    }

    public void setSwipeEnabled(boolean enabled) {
        if (viewPager != null) {
            viewPager.setUserInputEnabled(enabled);
        }
    }

    private void dispatchPipState(boolean isInPip) {
        for (androidx.fragment.app.Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof PipAware) {
                if (isInPip) {
                    ((PipAware) fragment).onEnterPip();
                } else {
                    ((PipAware) fragment).onExitPip();
                }
            }
        }
    }


}


