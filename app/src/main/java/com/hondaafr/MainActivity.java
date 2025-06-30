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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hondaafr.Libs.Bluetooth.Services.BluetoothForegroundService;
import com.hondaafr.Libs.UI.ClusterView;
import com.hondaafr.Libs.Helpers.Permissions;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.UI.ScientificView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private MainActivity mContext;
    private boolean canEnterPip = false;
    private TripComputer mTripComputer;
    private ClusterView mCluster;
    private ScientificView mScientific;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutScientific), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Permissions.askForAllPermissions(this);

        mContext = this;

        // Keep the screen on while this activity is visible
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTripComputer = new TripComputer(this);
        mScientific = new ScientificView(this, mTripComputer);
        mCluster = new ClusterView(this, mTripComputer);

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
        mTripComputer.setObdForFuelConsumption(true);

        mCluster.setVisibility(true);
        mScientific.setVisibility(false);
    }

    public void showScientific() {
        mCluster.setVisibility(false);
        mScientific.setVisibility(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("MainActivity", "onStop");

        mScientific.onStop();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onStart() {
        super.onStart();
        Log.d("MainActivity", "onStart");

        mScientific.onStart();
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
            mCluster.showPipView();
            mScientific.showPipView();
        } else {
            mCluster.restoreFullView();
            mScientific.restoreFullView();
        }
    }

    @Override
    public void onResume() {
        Log.d("MainActivity", "onResume");
        super.onResume();

        mScientific.onResume();
        mTripComputer.onResume(this);
    }

    @Override
    public void onPause() {
        Log.d("MainActivity", "onPause");
        super.onPause();

        mTripComputer.onPause(this);
        mScientific.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d("Lifecycle", "onDestroy");

        mScientific.onDestroy();
        mTripComputer.onDestroy(this);

        super.onDestroy();
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


}


