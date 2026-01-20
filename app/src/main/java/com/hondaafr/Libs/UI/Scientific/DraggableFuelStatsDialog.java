package com.hondaafr.Libs.UI.Scientific;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.hondaafr.Libs.Helpers.TripComputer.InstantStats;
import com.hondaafr.Libs.Helpers.TripComputer.TotalStats;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

public class DraggableFuelStatsDialog extends Dialog {
    private final MainActivity activity;
    private View dialogView;
    private TextView textStatsInfo, textStatsBig, textStatsMedium, textStatsSmall;
    private TextView textStatsMediumLabel, textStatsSmallLabel;
    private Button buttonSwitchMode;
    private OnDismissListener customDismissListener;
    private OnModeSwitchListener modeSwitchListener;
    
    private float dX, dY;
    private int lastAction;
    private boolean isDragging = false;
    
    public interface OnDismissListener {
        void onDismiss();
    }
    
    public interface OnModeSwitchListener {
        void onModeSwitch();
    }
    
    public void setCustomDismissListener(OnDismissListener listener) {
        this.customDismissListener = listener;
    }
    
    public void setModeSwitchListener(OnModeSwitchListener listener) {
        this.modeSwitchListener = listener;
    }
    
    public void updateModeButtonText(String modeText) {
        if (buttonSwitchMode != null) {
            buttonSwitchMode.setText(modeText);
        }
    }

    public DraggableFuelStatsDialog(Context context) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        this.activity = (MainActivity) context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_fuel_stats);
        
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.TOP | Gravity.START;
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.x = 100; // Initial X position
            params.y = 200; // Initial Y position
            window.setAttributes(params);
        }

        dialogView = findViewById(R.id.layoutFuelStatsContent);
        View dragHandle = findViewById(R.id.dragHandle);
        ImageButton closeButton = findViewById(R.id.buttonCloseFuelStats);
        buttonSwitchMode = findViewById(R.id.buttonSwitchMode);
        
        textStatsInfo = findViewById(R.id.textStatsTitle);
        textStatsBig = findViewById(R.id.textStatsBig);
        textStatsMedium = findViewById(R.id.textStatsMedium);
        textStatsSmall = findViewById(R.id.textStatsSmall);
        textStatsMediumLabel = findViewById(R.id.textStatsMediumLabel);
        textStatsSmallLabel = findViewById(R.id.textStatsSmallLabel);
        
        // Set up mode switch button
        if (buttonSwitchMode != null) {
            buttonSwitchMode.setOnClickListener(v -> {
                if (modeSwitchListener != null) {
                    modeSwitchListener.onModeSwitch();
                }
            });
        }
        
        // Set up long click listener for reset functionality
        if (textStatsBig != null) {
            textStatsBig.setOnLongClickListener(v -> {
                // This will be handled by the panel
                return false;
            });
        }

        // Make the entire dialog draggable
        View.OnTouchListener dragListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Window window = getWindow();
                if (window == null) return false;
                
                WindowManager.LayoutParams params = window.getAttributes();
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = params.x - event.getRawX();
                        dY = params.y - event.getRawY();
                        lastAction = MotionEvent.ACTION_DOWN;
                        isDragging = false;
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        isDragging = true;
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;
                        
                        // Keep dialog within screen bounds
                        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                        Point size = new Point();
                        wm.getDefaultDisplay().getSize(size);
                        
                        // Measure dialog view if not already measured
                        if (dialogView.getWidth() == 0 || dialogView.getHeight() == 0) {
                            dialogView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                             View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        }
                        
                        int dialogWidth = dialogView.getMeasuredWidth();
                        int dialogHeight = dialogView.getMeasuredHeight();
                        
                        // Constrain to screen bounds
                        if (newX < 0) newX = 0;
                        if (newY < 0) newY = 0;
                        if (newX + dialogWidth > size.x) {
                            newX = size.x - dialogWidth;
                        }
                        if (newY + dialogHeight > size.y) {
                            newY = size.y - dialogHeight;
                        }
                        
                        params.x = (int) newX;
                        params.y = (int) newY;
                        window.setAttributes(params);
                        lastAction = MotionEvent.ACTION_MOVE;
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        if (lastAction == MotionEvent.ACTION_DOWN && !isDragging) {
                            // It was a click, not a drag
                            return false;
                        }
                        isDragging = false;
                        return true;
                }
                return false;
            }
        };

        // Make drag handle draggable (buttons will handle their own clicks)
        dragHandle.setOnTouchListener(dragListener);
        if (textStatsInfo != null) {
            textStatsInfo.setOnTouchListener(dragListener);
        }
        
        // Close button
        closeButton.setOnClickListener(v -> {
            if (customDismissListener != null) {
                customDismissListener.onDismiss();
            }
            dismiss();
        });
    }
    
    @Override
    public void dismiss() {
        if (customDismissListener != null) {
            customDismissListener.onDismiss();
        }
        super.dismiss();
    }

    public void displayFuelConsumption(TotalStats stats) {
        if (textStatsInfo == null || textStatsBig == null) return;
        
        textStatsInfo.setText(stats.getName());
        textStatsBig.setText(String.format("%06.1f", stats.getDistanceKm()));

        if (textStatsMediumLabel != null) textStatsMediumLabel.setText("TOTAL");
        if (textStatsMedium != null) textStatsMedium.setText(String.format("%.2f l", stats.getLiters()));

        if (textStatsSmallLabel != null) textStatsSmallLabel.setText("100KM");
        if (textStatsSmall != null) textStatsSmall.setText(String.format("%.1f l", stats.getLitersPer100km()));
    }

    public void displayFuelConsumption(InstantStats stats) {
        if (textStatsInfo == null || textStatsBig == null) return;
        
        textStatsInfo.setText(stats.getName());
        textStatsBig.setText(String.format("%.2f l/h", stats.getLphAvg()));

        if (textStatsMediumLabel != null) textStatsMediumLabel.setText("100KM");
        if (textStatsMedium != null) textStatsMedium.setText(String.format("%.2f l", stats.getLp100km()));

        if (textStatsSmallLabel != null) textStatsSmallLabel.setText("100KM");
        if (textStatsSmall != null) textStatsSmall.setText(String.format("%.1f l", stats.getLp100kmAvg()));
    }
    
    public TextView getTextStatsBig() {
        return textStatsBig;
    }
}

