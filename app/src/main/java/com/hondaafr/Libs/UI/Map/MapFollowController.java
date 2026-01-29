package com.hondaafr.Libs.UI.Map;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

/**
 * Handles follow-mode state and map centering. Fragment wires button and swipe behavior.
 */
public final class MapFollowController {

    /** Duration in ms for smooth pan when following new points. */
    private static final long FOLLOW_ANIMATION_DURATION_MS = 500L;

    private final MapView mapView;
    private final MyLocationNewOverlay myLocationOverlay;

    private boolean followEnabled = true;
    private GeoPoint lastPoint;

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

    /** Toggles follow mode and returns the new state. Caller should update overlay and UI. */
    public boolean toggle() {
        followEnabled = !followEnabled;
        return followEnabled;
    }

    /** Applies current follow state to the location overlay (enable/disable follow). */
    public void applyToOverlay() {
        if (myLocationOverlay == null) {
            return;
        }
        if (followEnabled) {
            myLocationOverlay.enableFollowLocation();
        } else {
            myLocationOverlay.disableFollowLocation();
        }
    }

    /**
     * Call when location updates. If follow is enabled, moves the map to the new point.
     * First point: immediate setCenter. Later points: smooth animateTo with duration.
     */
    public void onLocationUpdate(GeoPoint point, boolean isFirstPoint) {
        this.lastPoint = point;
        if (!followEnabled || mapView == null) {
            return;
        }
        if (isFirstPoint) {
            mapView.getController().setCenter(point);
        } else {
            double zoom = mapView.getZoomLevelDouble();
            mapView.getController().animateTo(point, zoom, FOLLOW_ANIMATION_DURATION_MS);
        }
    }

    /** Centers map on last known point if follow is enabled. Call after toggle to follow. */
    public void centerOnLastPoint() {
        if (followEnabled && lastPoint != null && mapView != null) {
            double zoom = mapView.getZoomLevelDouble();
            mapView.getController().animateTo(lastPoint, zoom, FOLLOW_ANIMATION_DURATION_MS);
        }
    }
}
