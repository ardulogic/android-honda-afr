package com.hondaafr.Libs.UI.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class LegendGradientView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearGradient gradient;

    public LegendGradientView(Context context) {
        super(context);
    }

    public LegendGradientView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LegendGradientView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0) {
            return;
        }
        // 0-3 (20%), 3-6 (20%), 6-8 (13.33%), 8-15 (46.67%)
        gradient = new LinearGradient(
                0,
                0,
                w,
                0,
                new int[]{
                        0xFF6EC6FF, // light blue
                        0xFF2ECC71, // green
                        0xFFF1C40F, // yellow
                        0xFFE74C3C, // red
                        0xFFE74C3C  // red tail
                },
                new float[]{0f, 0.2f, 0.4f, 0.53333336f, 1f},
                Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (gradient == null) {
            return;
        }
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}

