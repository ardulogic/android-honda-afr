package com.hondaafr.Libs.UI.Map;

import android.graphics.Color;
import android.graphics.Point;

import com.hondaafr.Libs.Helpers.TripComputer.TripFuelTrackStore;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.ArrayDeque;

/**
 * Renders track polylines and value labels on the map for the selected dimension.
 * Uses marker pooling to avoid recreating labels on every pan/zoom.
 */
public final class MapTrackRenderer {

    private static final float TRACK_WIDTH = 8f;
    /** Four-zone colors: cool (blue), green, orange, red */
    private static final int COLOR_COOL   = 0xFF3498DB;  // blue
    private static final int COLOR_GREEN  = 0xFF2ECC71;
    private static final int COLOR_ORANGE = 0xFFE67E22;
    private static final int COLOR_RED    = 0xFFE74C3C;
    private static final int MAX_RENDER_SEGMENTS = 500;
    private static final int MIN_LABEL_SPACING_PX = 120;
    private static final int LABEL_FONT_SIZE = 22;

    private final MapView mapView;
    private final List<Polyline> renderedSegments = new ArrayList<>();
    private final List<Marker> activeLabels = new ArrayList<>();
    private final Deque<Marker> markerPool = new ArrayDeque<>();

    public MapTrackRenderer(MapView mapView) {
        this.mapView = mapView;
    }

    public void clear() {
        for (Polyline p : renderedSegments) {
            mapView.getOverlayManager().remove(p);
        }
        renderedSegments.clear();

        for (Marker m : activeLabels) {
            mapView.getOverlayManager().remove(m);
            recycleMarker(m);
        }
        activeLabels.clear();
    }

    /**
     * Renders track polylines from samples. Call after clear() or to replace track.
     */
    public void render(
            List<TripFuelTrackStore.TrackPoint> samples,
            MapMetric metric,
            BoundingBox bounds
    ) {
        for (Polyline p : renderedSegments) {
            mapView.getOverlayManager().remove(p);
        }
        renderedSegments.clear();

        if (samples.size() < 2) {
            return;
        }

        double zoom = mapView.getZoomLevelDouble();
        int stride = calculateStride(samples.size(), zoom);
        boolean hasBounds = mapView.getHeight() > 0 && mapView.getWidth() > 0;

        TripFuelTrackStore.TrackPoint prev = null;
        for (int i = 0; i < samples.size(); i += stride) {
            TripFuelTrackStore.TrackPoint curr = samples.get(i);
            if (curr.isBreak) {
                prev = null;
                continue;
            }
            if (prev == null) {
                prev = curr;
                continue;
            }
            if (hasBounds && !isSegmentVisible(prev, curr, bounds)) {
                prev = curr;
                continue;
            }

            Double value = metric.extract(curr);
            if (value == null) {
                prev = curr;
                continue;
            }

            Polyline segment = createSegment(prev, curr, value, metric);
            renderedSegments.add(segment);
            mapView.getOverlayManager().add(segment);
            prev = curr;
        }
    }

    /**
     * Updates value labels for visible samples. Reuses markers from pool instead of recreating.
     * The last point of the track is always given a label when visible (spacing rule is bypassed).
     *
     * @param lastTrackPoint Last point of the full track (most recent); can be null. When in visibleSamples, always gets a label.
     */
    public void updateLabels(
            List<TripFuelTrackStore.TrackPoint> visibleSamples,
            MapMetric metric,
            TripFuelTrackStore.TrackPoint lastTrackPoint
    ) {
        for (Marker m : activeLabels) {
            mapView.getOverlayManager().remove(m);
            recycleMarker(m);
        }
        activeLabels.clear();

        if (visibleSamples.isEmpty()) {
            return;
        }

        Projection projection = mapView.getProjection();
        if (projection == null) {
            return;
        }

        Point lastPixel = null;
        for (TripFuelTrackStore.TrackPoint sample : visibleSamples) {
            if (!sample.isRenderable()) {
                continue;
            }
            Double value = metric.extract(sample);
            if (value == null) {
                continue;
            }

            Point pixel = projection.toPixels(new GeoPoint(sample.latitude, sample.longitude), null);
            boolean isLastPoint = isSamePosition(sample, lastTrackPoint);
            if (!isLastPoint && lastPixel != null) {
                int dx = pixel.x - lastPixel.x;
                int dy = pixel.y - lastPixel.y;
                if (dx * dx + dy * dy < MIN_LABEL_SPACING_PX * MIN_LABEL_SPACING_PX) {
                    continue;
                }
            }

            addLabelMarker(sample, value);
            lastPixel = pixel;
        }
    }

    private boolean isSamePosition(TripFuelTrackStore.TrackPoint a, TripFuelTrackStore.TrackPoint b) {
        if (a == null || b == null || a.isBreak || b.isBreak) return false;
        return a.latitude == b.latitude && a.longitude == b.longitude;
    }

    private void addLabelMarker(TripFuelTrackStore.TrackPoint sample, double value) {
        Marker marker = obtainMarker();
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setPosition(new GeoPoint(sample.latitude, sample.longitude));
        marker.setTitle(String.format("%.1f", value));
        marker.setTextLabelBackgroundColor(0x66000000);
        marker.setTextLabelForegroundColor(Color.WHITE);
        marker.setTextLabelFontSize(LABEL_FONT_SIZE);
        marker.setInfoWindow(null);
        marker.setTextIcon(String.format("%.1f", value));
        activeLabels.add(marker);
        mapView.getOverlayManager().add(marker);
    }

    public void invalidate() {
        if (mapView != null) {
            mapView.invalidate();
        }
    }

    private int calculateStride(int sampleCount, double zoom) {
        int targetSegments = (int) Math.min(1200, MAX_RENDER_SEGMENTS + Math.max(0, zoom - 10) * 80);
        return Math.max(1, (sampleCount - 1) / targetSegments);
    }

    private boolean isSegmentVisible(
            TripFuelTrackStore.TrackPoint a,
            TripFuelTrackStore.TrackPoint b,
            BoundingBox box
    ) {
        double padLat = box.getLatitudeSpan() * 0.1;
        double padLon = box.getLongitudeSpan() * 0.1;
        double minLat = box.getLatSouth() - padLat;
        double maxLat = box.getLatNorth() + padLat;
        double minLon = box.getLonWest() - padLon;
        double maxLon = box.getLonEast() + padLon;

        boolean aVisible = a.latitude >= minLat && a.latitude <= maxLat
                && a.longitude >= minLon && a.longitude <= maxLon;
        boolean bVisible = b.latitude >= minLat && b.latitude <= maxLat
                && b.longitude >= minLon && b.longitude <= maxLon;
        return aVisible || bVisible;
    }

    private Polyline createSegment(
            TripFuelTrackStore.TrackPoint prev,
            TripFuelTrackStore.TrackPoint curr,
            double metricValue,
            MapMetric metric
    ) {
        Polyline segment = new Polyline();
        segment.setPoints(Arrays.asList(
                new GeoPoint(prev.latitude, prev.longitude),
                new GeoPoint(curr.latitude, curr.longitude)
        ));
        segment.setColor(valueToColor(metricValue, metric));
        segment.setWidth(TRACK_WIDTH);
        return segment;
    }

    /**
     * Maps value to a color by lerping between the four threshold colors (cool, green, orange, red).
     * t1=coolToGreen, t2=greenToOrange, t3=orangeToRed.
     * Non-inverted: [min, t1] cool→green, [t1, t2] green→orange, [t2, t3] orange→red, [t3, max] red.
     * Inverted: [t1, max] cool, [t2, t1] green, [t3, t2] orange, [min, t3] red (high value = cool).
     */
    private int valueToColor(double value, MapMetric metric) {
        double min = metric.minForColor();
        double max = metric.maxForColor();
        double t1 = metric.coolToGreen();
        double t2 = metric.greenToOrange();
        double t3 = metric.orangeToRed();
        boolean inverted = metric.invertedForColor();

        if (inverted) {
            return valueToColorInverted(value, min, max, t1, t2, t3);
        }
        return valueToColorNormal(value, min, max, t1, t2, t3);
    }

    private int valueToColorNormal(double value, double min, double max, double t1, double t2, double t3) {
        double t;
        if (value <= t1) {
            t = safeLerp(value, min, t1);
            return lerpColor(COLOR_COOL, COLOR_GREEN, t);
        } else if (value <= t2) {
            t = safeLerp(value, t1, t2);
            return lerpColor(COLOR_GREEN, COLOR_ORANGE, t);
        } else if (value <= t3) {
            t = safeLerp(value, t2, t3);
            return lerpColor(COLOR_ORANGE, COLOR_RED, t);
        } else {
            return COLOR_RED;
        }
    }

    private int valueToColorInverted(
            double value,
            double min,
            double max,
            double t1,
            double t2,
            double t3
    ) {
        double t;

        // High → cool
        if (value >= t1) {
            t = safeLerp(value, t1, max);
            return lerpColor(COLOR_GREEN, COLOR_COOL, t);
        }

        // Green zone
        if (value >= t2) {
            t = safeLerp(value, t2, t1);
            return lerpColor(COLOR_ORANGE, COLOR_GREEN, t);
        }

        // Orange zone
        if (value >= t3) {
            t = safeLerp(value, t3, t2);
            return lerpColor(COLOR_RED, COLOR_ORANGE, t);
        }

        // Below t3 = solid red
        return COLOR_RED;
    }

    /** Returns (value - low) / (high - low) clamped to [0, 1], or 0 if span is 0. */
    private static double safeLerp(double value, double low, double high) {
        double span = high - low;
        if (span <= 0) return 0.0;
        double t = (value - low) / span;
        return Math.max(0.0, Math.min(1.0, t));
    }

    private static int lerpColor(int start, int end, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int a = (int) (Color.alpha(start) + (Color.alpha(end) - Color.alpha(start)) * t);
        int r = (int) (Color.red(start) + (Color.red(end) - Color.red(start)) * t);
        int g = (int) (Color.green(start) + (Color.green(end) - Color.green(start)) * t);
        int b = (int) (Color.blue(start) + (Color.blue(end) - Color.blue(start)) * t);
        return Color.argb(a, r, g, b);
    }

    private Marker obtainMarker() {
        Marker m = markerPool.pollFirst();
        return m != null ? m : new Marker(mapView);
    }

    private void recycleMarker(Marker m) {
        // Do not call setPosition(null): osmdroid Marker.clone() on null throws NPE.
        // Position will be overwritten when the marker is reused from the pool.
        markerPool.offerLast(m);
    }
}
