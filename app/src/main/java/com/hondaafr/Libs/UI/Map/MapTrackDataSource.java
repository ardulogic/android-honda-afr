package com.hondaafr.Libs.UI.Map;

import com.hondaafr.Libs.Helpers.TripComputer.TripFuelTrackStore;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds track samples and provides visibility filtering for the current viewport.
 * Keeps raw data and break markers separate from rendering logic.
 */
public final class MapTrackDataSource {

    private final List<TripFuelTrackStore.TrackPoint> samples = new ArrayList<>();

    public List<TripFuelTrackStore.TrackPoint> getSamples() {
        return samples;
    }

    public void setSamples(List<TripFuelTrackStore.TrackPoint> newSamples) {
        samples.clear();
        if (newSamples != null) {
            samples.addAll(newSamples);
        }
    }

    public void clear() {
        samples.clear();
    }

    public void append(TripFuelTrackStore.TrackPoint point) {
        samples.add(point);
    }

    public boolean isEmpty() {
        return samples.isEmpty();
    }

    public int size() {
        return samples.size();
    }

    /**
     * Returns samples that fall within (or near) the map viewport, including break markers
     * so segment boundaries are preserved. If mapView is null or has zero size, returns all samples.
     */
    public List<TripFuelTrackStore.TrackPoint> getVisibleSamples(MapView mapView) {
        if (mapView == null || mapView.getHeight() == 0 || mapView.getWidth() == 0) {
            return new ArrayList<>(samples);
        }

        BoundingBox box = mapView.getBoundingBox();
        double padLat = box.getLatitudeSpan() * 0.1;
        double padLon = box.getLongitudeSpan() * 0.1;
        double minLat = box.getLatSouth() - padLat;
        double maxLat = box.getLatNorth() + padLat;
        double minLon = box.getLonWest() - padLon;
        double maxLon = box.getLonEast() + padLon;

        List<TripFuelTrackStore.TrackPoint> visible = new ArrayList<>();
        for (TripFuelTrackStore.TrackPoint sample : samples) {
            if (sample.isBreak) {
                visible.add(sample);
                continue;
            }
            if (sample.latitude >= minLat && sample.latitude <= maxLat
                    && sample.longitude >= minLon && sample.longitude <= maxLon) {
                visible.add(sample);
            }
        }
        return visible;
    }

    /** Bounding box for the current viewport; null if map not ready. */
    @androidx.annotation.Nullable
    public BoundingBox getViewportBox(MapView mapView) {
        if (mapView == null || mapView.getHeight() == 0 || mapView.getWidth() == 0) {
            return null;
        }
        return mapView.getBoundingBox();
    }
}
