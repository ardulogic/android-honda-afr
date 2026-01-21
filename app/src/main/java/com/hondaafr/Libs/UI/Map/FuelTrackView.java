package com.hondaafr.Libs.UI.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FuelTrackView extends View {
    private static final float DEFAULT_STROKE_WIDTH = 8f;
    private static final double MAX_CONSUMPTION = 30.0;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<TrackPoint> points = new ArrayList<>();

    public FuelTrackView(Context context) {
        super(context);
        init();
    }

    public FuelTrackView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FuelTrackView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void addPoint(double latitude, double longitude, double litersPer100km) {
        points.add(new TrackPoint(latitude, longitude, litersPer100km));
        invalidate();
    }

    public void setTrack(List<TrackPoint> trackPoints) {
        points.clear();
        if (trackPoints != null) {
            points.addAll(trackPoints);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (points.size() < 2) {
            return;
        }

        float padding = Math.max(24f, DEFAULT_STROKE_WIDTH * 2f);
        float width = getWidth() - padding * 2f;
        float height = getHeight() - padding * 2f;

        Bounds bounds = calculateBounds();
        double latRange = Math.max(1e-6, bounds.maxLat - bounds.minLat);
        double lonRange = Math.max(1e-6, bounds.maxLon - bounds.minLon);

        for (int i = 1; i < points.size(); i++) {
            TrackPoint prev = points.get(i - 1);
            TrackPoint curr = points.get(i);

            float startX = padding + (float) ((prev.longitude - bounds.minLon) / lonRange) * width;
            float startY = padding + (float) ((bounds.maxLat - prev.latitude) / latRange) * height;
            float endX = padding + (float) ((curr.longitude - bounds.minLon) / lonRange) * width;
            float endY = padding + (float) ((bounds.maxLat - curr.latitude) / latRange) * height;

            linePaint.setColor(consumptionToColor(curr.litersPer100km));
            canvas.drawLine(startX, startY, endX, endY, linePaint);
        }
    }

    private Bounds calculateBounds() {
        Bounds bounds = new Bounds();
        for (TrackPoint point : points) {
            bounds.minLat = Math.min(bounds.minLat, point.latitude);
            bounds.maxLat = Math.max(bounds.maxLat, point.latitude);
            bounds.minLon = Math.min(bounds.minLon, point.longitude);
            bounds.maxLon = Math.max(bounds.maxLon, point.longitude);
        }
        return bounds;
    }

    private int consumptionToColor(double litersPer100km) {
        double clamped = Math.max(0.0, Math.min(MAX_CONSUMPTION, litersPer100km));
        float ratio = (float) (clamped / MAX_CONSUMPTION);
        int red = (int) (255 * ratio);
        int green = (int) (255 * (1f - ratio));
        return Color.rgb(red, green, 0);
    }

    public static class TrackPoint {
        public final double latitude;
        public final double longitude;
        public final double litersPer100km;

        public TrackPoint(double latitude, double longitude, double litersPer100km) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.litersPer100km = litersPer100km;
        }
    }

    private static class Bounds {
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
    }
}

