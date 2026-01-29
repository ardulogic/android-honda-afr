package com.hondaafr.Libs.UI.Map;

import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

/**
 * Handles follow-mode state and map centering. Pans by scrollBy each frame using
 * Handler.postDelayed so animation runs in PiP (Choreographer is throttled for PiP).
 * Elapsed real time so duration is correct when "Animator duration scale" is off.
 */
public final class MapFollowController {
    private static final String TAG = "MapFollow";
    /**
     * Duration in ms for smooth pan when following new points.
     */
    private static final long FOLLOW_ANIMATION_DURATION_MS = 600L;
    /**
     * Min ms between starting a new pan so we don't restart animation on every GPS tick.
     */
    private static final long MIN_PAN_INTERVAL_MS = 400L;
    /** Interval between animation frames (Handler), works in PiP. */
    private static final long FRAME_INTERVAL_MS = 16L;
    private final MapView mapView;
    private final MyLocationNewOverlay myLocationOverlay;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable panRunnable;
    private boolean followEnabled = true;
    private GeoPoint lastPoint;
    private boolean panAnimationActive = false;
    private long lastPanStartTime = 0L;
    private int panFrameCount = 0;

    public MapFollowController(MapView mapView, MyLocationNewOverlay myLocationOverlay) {
        this.mapView = mapView;
        this.myLocationOverlay = myLocationOverlay;
    }

    public boolean isFollowEnabled() {
        return followEnabled;
    }

    public void setFollowEnabled(boolean enabled) {
        this.followEnabled = enabled;
    }

    public GeoPoint getLastPoint() {
        return lastPoint;
    }

    public void setLastPoint(GeoPoint point) {
        this.lastPoint = point;
    }

    /**
     * Toggles follow mode and returns the new state. Caller should update overlay and UI.
     */
    public boolean toggle() {
        followEnabled = !followEnabled;
        return followEnabled;
    }

    /**
     * Applies follow state to the overlay. We never enable the overlay's built-in follow * (which does instant setCenter on every fix); we move the map ourselves via * onLocationUpdate with animateTo for smooth panning.
     */
    public void applyToOverlay() {
        if (myLocationOverlay == null) {
            return;
        }
        myLocationOverlay.disableFollowLocation();
    }

    /**
     * Call when location updates. If follow is enabled, moves the map to the new point. * First point: immediate setCenter. Later points: custom ValueAnimator + invalidate each frame.
     */
    public void onLocationUpdate(GeoPoint point, boolean isFirstPoint) {
        this.lastPoint = point;
        Log.d(TAG, "onLocationUpdate isFirst=" + isFirstPoint + " lat=" + point.getLatitude() + " lon=" + point.getLongitude());
        if (!followEnabled || mapView == null) {
            Log.d(TAG, "onLocationUpdate skip: follow=" + followEnabled + " mapView=" + (mapView != null));
            return;
        }
        if (isFirstPoint) {
            Log.d(TAG, "onLocationUpdate first point -> setCenter");
            cancelPanAnimation();
            mapView.getController().stopPanning();
            mapView.getController().setCenter(point);
            lastPanStartTime = System.currentTimeMillis();
        } else {
            long now = System.currentTimeMillis();
            long elapsed = now - lastPanStartTime;
            if (elapsed >= MIN_PAN_INTERVAL_MS) {
                Log.d(TAG, "onLocationUpdate starting smoothPanTo elapsedMs=" + elapsed);
                smoothPanTo(point);
                lastPanStartTime = now;
            } else {
                Log.d(TAG, "onLocationUpdate throttled elapsedMs=" + elapsed + " need=" + MIN_PAN_INTERVAL_MS);
            }
        }
    }

    /**
     * Centers map on last known point if follow is enabled. Call after toggle to follow.
     */
    public void centerOnLastPoint() {
        if (followEnabled && lastPoint != null && mapView != null) {
            smoothPanTo(lastPoint);
        }
    }

    private void cancelPanAnimation() {
        if (panRunnable != null) {
            mainHandler.removeCallbacks(panRunnable);
            panRunnable = null;
        }
        if (panAnimationActive) {
            Log.d(TAG, "cancelPanAnimation (was running, frames=" + panFrameCount + ")");
            panAnimationActive = false;
        }
    }

    /**
     * Pans from current center to target over FOLLOW_ANIMATION_DURATION_MS using elapsed
     * real time. Uses Handler.postDelayed so animation runs in PiP (Choreographer is
     * throttled for PiP windows).
     */
    private void smoothPanTo(GeoPoint target) {
        if (mapView == null || mapView.getProjection() == null) {
            Log.d(TAG, "smoothPanTo skip: mapView or projection null");
            return;
        }
        cancelPanAnimation();
        IGeoPoint start = mapView.getMapCenter();
        if (start == null) {
            Log.d(TAG, "smoothPanTo start center null -> setCenter(target)");
            mapView.getController().setCenter(target);
            return;
        }
        final double startLat = start.getLatitude();
        final double startLon = start.getLongitude();
        final double endLat = target.getLatitude();
        final double endLon = target.getLongitude();
        final int centerX = mapView.getWidth() / 2;
        final int centerY = mapView.getHeight() / 2;
        final Point pixel = new Point();
        final GeoPoint finalTarget = new GeoPoint(endLat, endLon);
        final long startTimeUptime = SystemClock.uptimeMillis();
        double zoom = mapView.getZoomLevelDouble();
        Log.d(TAG, "smoothPanTo start start=(" + startLat + "," + startLon + ") end=(" + endLat + "," + endLon + ") centerPx=(" + centerX + "," + centerY + ") zoom=" + zoom + " durationMs=" + FOLLOW_ANIMATION_DURATION_MS);
        panAnimationActive = true;
        panFrameCount = 0;
        panRunnable = new Runnable() {
            @Override
            public void run() {
                if (!panAnimationActive || mapView == null || mapView.getProjection() == null) {
                    return;
                }
                long elapsed = SystemClock.uptimeMillis() - startTimeUptime;
                float t = elapsed / (float) FOLLOW_ANIMATION_DURATION_MS;
                if (t >= 1f) {
                    panAnimationActive = false;
                    panRunnable = null;
                    Log.d(TAG, "smoothPanTo done frames=" + panFrameCount + " setCenter(finalTarget)");
                    if (mapView != null) {
                        mapView.getController().setCenter(finalTarget);
                    }
                    if (mapView != null) mapView.postInvalidate();
                    return;
                }
                panFrameCount++;
                double lat = startLat + (endLat - startLat) * t;
                double lon = startLon + (endLon - startLon) * t;
                GeoPoint interpolated = new GeoPoint(lat, lon);
                mapView.getProjection().toPixels(interpolated, pixel);
                int dx = pixel.x - centerX;
                int dy = pixel.y - centerY;
                if (panFrameCount <= 3 || panFrameCount % 10 == 0) {
                    Log.d(TAG, "smoothPanTo frame=" + panFrameCount + " elapsed=" + elapsed + " t=" + String.format("%.3f", t) + " scrollBy(" + dx + "," + dy + ")");
                }
                mapView.getController().scrollBy(dx, dy);
                mapView.postInvalidate();
                mainHandler.postDelayed(this, FRAME_INTERVAL_MS);
            }
        };
        mainHandler.post(panRunnable);
    }
}