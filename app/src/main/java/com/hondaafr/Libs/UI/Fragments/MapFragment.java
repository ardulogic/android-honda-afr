package com.hondaafr.Libs.UI.Fragments;

import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hondaafr.BuildConfig;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputer;
import com.hondaafr.Libs.Helpers.TripComputer.TripComputerListener;
import com.hondaafr.Libs.Helpers.TripComputer.TripFuelTrackStore;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.Polyline;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements TripComputerListener, PipAware {
    private static final String LISTENER_ID = "map_fragment";
    private static final float TRACK_WIDTH = 8f;
    private static final double MAX_CONSUMPTION = 15.0;
    private static final int MAX_RENDER_SEGMENTS = 500;
    private static final int MIN_LABEL_SPACING_PX = 120;
    private static final ITileSource DARK_TILE_SOURCE = new XYTileSource(
            "CartoDarkMatter",
            0,
            20,
            256,
            ".png",
            new String[]{"https://a.basemaps.cartocdn.com/dark_all/",
                    "https://b.basemaps.cartocdn.com/dark_all/",
                    "https://c.basemaps.cartocdn.com/dark_all/",
                    "https://d.basemaps.cartocdn.com/dark_all/"}
    );

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private View followToggleButton;
    private View legendContainer;
    private TextView legendTitle;
    private TextView legendMin;
    private TextView legendMid1;
    private TextView legendMid2;
    private TextView legendMid3;
    private TextView legendMax;
    private View zoomInButton;
    private View zoomOutButton;
    private View exportTripButton;
    private View toggleMetricButton;
    private TripComputer tripComputer;
    private GeoPoint lastPoint;
    private boolean followEnabled = true;
    private boolean isNightMode = false;
    private final List<TripFuelTrackStore.TrackPoint> fuelSamples = new ArrayList<>();
    private final List<Marker> fuelLabels = new ArrayList<>();
    private final List<Polyline> renderedSegments = new ArrayList<>();
    private String sessionId = "";
    private TripFuelTrackStore trackStore;
    private long lastRenderMs = 0;
    private boolean didInitialZoom = false;
    private boolean segmentBreakPending = false;
    private boolean useLph = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true);
        mapView.getController().setZoom(16.0);
        mapView.setBuiltInZoomControls(false);
        followToggleButton = view.findViewById(R.id.buttonMapFollow);
        legendContainer = view.findViewById(R.id.layoutFuelLegend);
        legendTitle = view.findViewById(R.id.textLegendTitle);
        legendMin = view.findViewById(R.id.textLegendMin);
        legendMid1 = view.findViewById(R.id.textLegendMid1);
        legendMid2 = view.findViewById(R.id.textLegendMid2);
        legendMid3 = view.findViewById(R.id.textLegendMid3);
        legendMax = view.findViewById(R.id.textLegendMax);
        zoomInButton = view.findViewById(R.id.buttonZoomIn);
        zoomOutButton = view.findViewById(R.id.buttonZoomOut);
        exportTripButton = view.findViewById(R.id.buttonExportTrip);
        toggleMetricButton = view.findViewById(R.id.buttonToggleMetric);

        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);

        tripComputer = ((MainActivity) requireActivity()).getTripComputer();
        sessionId = tripComputer.tripStats.getSessionId(requireContext());
        trackStore = new TripFuelTrackStore(requireContext(), sessionId);
        fuelSamples.clear();
        fuelSamples.addAll(trackStore.loadAll());
        if (!fuelSamples.isEmpty()) {
            TripFuelTrackStore.TrackPoint last = fuelSamples.get(fuelSamples.size() - 1);
            lastPoint = new GeoPoint(last.latitude, last.longitude);
        }
        mapView.post(() -> {
            refreshTrackOverlays();
            ensureInitialViewport();
        });
        updateFollowButton();
        followToggleButton.setOnClickListener(v -> toggleFollowMode());
        updateFollowInteraction();
        zoomInButton.setOnClickListener(v -> mapView.getController().zoomIn());
        zoomOutButton.setOnClickListener(v -> mapView.getController().zoomOut());
        exportTripButton.setOnClickListener(v -> exportTripCsv());
        toggleMetricButton.setOnClickListener(v -> toggleMetric());
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                refreshTrackOverlays();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                refreshTrackOverlays();
                return false;
            }
        });
        applyNightMode(isSystemNightMode());
        updateLegendTitle();
        updatePipUiState(requireActivity().isInPictureInPictureMode());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
            mapView.post(() -> {
                refreshTrackOverlays();
                ensureInitialViewport();
            });
        }
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
        }
        if (tripComputer != null) {
            tripComputer.addListener(LISTENER_ID, this);
        }
        updatePipUiState(requireActivity().isInPictureInPictureMode());
    }

    @Override
    public void onPause() {
        if (tripComputer != null) {
            tripComputer.removeListener(LISTENER_ID);
        }
        if (trackStore != null) {
            trackStore.flush();
        }
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onGpsUpdate(Double speed, double distanceIncrement) {
        if (tripComputer == null || tripComputer.gps == null || mapView == null) {
            return;
        }

        if (!tripComputer.mObdStudio.isAlive() || !tripComputer.mSpartanStudio.isAlive()) {
            lastPoint = null;
            segmentBreakPending = true;
            return;
        }

        handleSessionChange();

        Location last = tripComputer.gps.getLastLocation();
        if (last == null) {
            return;
        }

        double lat = last.getLatitude();
        double lon = last.getLongitude();

        GeoPoint currPoint = new GeoPoint(lat, lon);
        if (lastPoint != null && lastPoint.equals(currPoint)) {
            return;
        }

        double lp100km = tripComputer.instStats.getLp100kmAvg();
        if (lastPoint == null) {
            lastPoint = currPoint;
            if (followEnabled) {
                mapView.getController().setCenter(currPoint);
            }
            addFuelSample(currPoint, lp100km);
            return;
        }

        lastPoint = currPoint;
        addFuelSample(currPoint, lp100km);
        refreshTrackOverlays();

        if (followEnabled) {
            mapView.getController().animateTo(currPoint);
        }
    }

    private int consumptionToColor(double litersPer100km) {
        double value = Math.max(0.0, Math.min(MAX_CONSUMPTION, litersPer100km));
        if (value <= 3.0) {
            return lerpColor(0xFF6EC6FF, 0xFF2ECC71, value / 3.0);
        } else if (value <= 6.0) {
            return lerpColor(0xFF2ECC71, 0xFFF1C40F, (value - 3.0) / 3.0);
        } else if (value <= 8.0) {
            return lerpColor(0xFFF1C40F, 0xFFE74C3C, (value - 6.0) / 2.0);
        }
        return 0xFFE74C3C;
    }


    private void addFuelSample(GeoPoint point, double litersPer100km) {
        Double sanitized = sanitizeMetric(litersPer100km);
        Double lph = sanitizeMetric(tripComputer.instStats.getLphAvg());
        if (sanitized == null) {
            return;
        }
        if (segmentBreakPending && !fuelSamples.isEmpty()) {
            TripFuelTrackStore.TrackPoint breakPoint = TripFuelTrackStore.TrackPoint.breakMarker();
            fuelSamples.add(breakPoint);
            if (trackStore != null) {
                trackStore.append(breakPoint);
            }
            segmentBreakPending = false;
        } else if (segmentBreakPending) {
            segmentBreakPending = false;
        }
        TripFuelTrackStore.TrackPoint sample =
                new TripFuelTrackStore.TrackPoint(
                        point.getLatitude(),
                        point.getLongitude(),
                        sanitized,
                        lph == null ? Double.NaN : lph
                );
        fuelSamples.add(sample);
        if (trackStore != null) {
            trackStore.append(sample);
        }
    }

    private void handleSessionChange() {
        String currentSession = tripComputer.tripStats.getSessionId(requireContext());
        if (!currentSession.equals(sessionId)) {
            sessionId = currentSession;
            trackStore = new TripFuelTrackStore(requireContext(), sessionId);
            fuelSamples.clear();
            renderedSegments.clear();
            fuelLabels.clear();
            lastPoint = null;
            segmentBreakPending = false;
            refreshTrackOverlays();
        }
    }

    private void exportTripCsv() {
        if (trackStore == null) {
            Toast.makeText(requireContext(), "No trip data yet", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean success = trackStore.exportToDownloads(requireContext());
        Toast.makeText(requireContext(),
                success ? "Trip CSV exported to Downloads/HondaAfr" : "Export failed",
                Toast.LENGTH_SHORT).show();
    }

    private List<TripFuelTrackStore.TrackPoint> getVisibleSamples() {
        if (mapView == null) {
            return fuelSamples;
        }
        if (mapView.getHeight() == 0 || mapView.getWidth() == 0) {
            return fuelSamples;
        }
        BoundingBox box = mapView.getBoundingBox();
        double padLat = box.getLatitudeSpan() * 0.1;
        double padLon = box.getLongitudeSpan() * 0.1;
        double minLat = box.getLatSouth() - padLat;
        double maxLat = box.getLatNorth() + padLat;
        double minLon = box.getLonWest() - padLon;
        double maxLon = box.getLonEast() + padLon;

        List<TripFuelTrackStore.TrackPoint> visible = new ArrayList<>();
        for (TripFuelTrackStore.TrackPoint sample : fuelSamples) {
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

    private void ensureInitialViewport() {
        if (didInitialZoom || mapView == null || fuelSamples.isEmpty()) {
            return;
        }
        // Keep current zoom; just center to the last known point.
        TripFuelTrackStore.TrackPoint last = fuelSamples.get(fuelSamples.size() - 1);
        mapView.getController().setCenter(new GeoPoint(last.latitude, last.longitude));
        didInitialZoom = true;
    }

    private void refreshTrackOverlays() {
        if (mapView == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastRenderMs < 250) {
            return;
        }
        lastRenderMs = now;

        for (Polyline segment : renderedSegments) {
            mapView.getOverlayManager().remove(segment);
        }
        renderedSegments.clear();

        if (fuelSamples.size() < 2) {
            updateFuelLabels();
            mapView.invalidate();
            return;
        }

        double zoom = mapView.getZoomLevelDouble();
        int targetSegments = (int) Math.min(1200, MAX_RENDER_SEGMENTS + Math.max(0, zoom - 10) * 80);
        int stride = Math.max(1, (fuelSamples.size() - 1) / targetSegments);

        BoundingBox box = mapView.getBoundingBox();
        boolean hasBounds = mapView.getHeight() > 0 && mapView.getWidth() > 0;

        TripFuelTrackStore.TrackPoint prev = null;
        for (int i = 0; i < fuelSamples.size(); i += stride) {
            TripFuelTrackStore.TrackPoint curr = fuelSamples.get(i);
            if (curr.isBreak) {
                prev = null;
                continue;
            }
            if (prev == null) {
                prev = curr;
                continue;
            }
            if (hasBounds && !shouldRenderSegment(prev, curr, box)) {
                prev = curr;
                continue;
            }

            Double metricValue = getMetricValue(curr);
            if (metricValue == null) {
                prev = curr;
                continue;
            }
            Polyline segment = new Polyline();
            segment.setPoints(Arrays.asList(
                    new GeoPoint(prev.latitude, prev.longitude),
                    new GeoPoint(curr.latitude, curr.longitude)
            ));
            segment.setColor(consumptionToColor(metricValue));
            segment.setWidth(TRACK_WIDTH);
            renderedSegments.add(segment);
            mapView.getOverlayManager().add(segment);
            prev = curr;
        }

        updateFuelLabels();
        mapView.invalidate();
    }

    private boolean shouldRenderSegment(TripFuelTrackStore.TrackPoint a,
                                        TripFuelTrackStore.TrackPoint b,
                                        BoundingBox box) {
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

        // Draw if either end is on-screen (or near) so the path continues naturally.
        return aVisible || bVisible;
    }

    private void updateFuelLabels() {
        if (mapView == null) {
            return;
        }

        for (Marker marker : fuelLabels) {
            mapView.getOverlayManager().remove(marker);
        }
        fuelLabels.clear();

        if (fuelSamples.isEmpty()) {
            mapView.invalidate();
            return;
        }

        Projection projection = mapView.getProjection();
        int minPixelSpacing = MIN_LABEL_SPACING_PX;
        Point lastPixel = null;

        List<TripFuelTrackStore.TrackPoint> visibleSamples = getVisibleSamples();
        for (TripFuelTrackStore.TrackPoint sample : visibleSamples) {
            if (sample.isBreak) {
                continue;
            }
            Double metricValue = getMetricValue(sample);
            if (metricValue == null) {
                continue;
            }
            Point pixel = projection.toPixels(new GeoPoint(sample.latitude, sample.longitude), null);
            if (lastPixel != null) {
                int dx = pixel.x - lastPixel.x;
                int dy = pixel.y - lastPixel.y;
                if (dx * dx + dy * dy < minPixelSpacing * minPixelSpacing) {
                    continue;
                }
            }

            Marker marker = new Marker(mapView);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setPosition(new GeoPoint(sample.latitude, sample.longitude));
            marker.setTitle(String.format("%.1f", metricValue));
            marker.setTextLabelBackgroundColor(0x66000000);
            marker.setTextLabelForegroundColor(Color.WHITE);
            marker.setTextLabelFontSize(22);
            marker.setInfoWindow(null);
            marker.setTextIcon(String.format("%.1f", metricValue));
            fuelLabels.add(marker);
            mapView.getOverlayManager().add(marker);

            lastPixel = pixel;
        }

        mapView.invalidate();
    }

    private int lerpColor(int start, int end, double t) {
        int a = (int) (Color.alpha(start) + (Color.alpha(end) - Color.alpha(start)) * t);
        int r = (int) (Color.red(start) + (Color.red(end) - Color.red(start)) * t);
        int g = (int) (Color.green(start) + (Color.green(end) - Color.green(start)) * t);
        int b = (int) (Color.blue(start) + (Color.blue(end) - Color.blue(start)) * t);
        return Color.argb(a, r, g, b);
    }

    private Double sanitizeMetric(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return null;
        }
        if (value < 0) {
            return 0.0;
        }
        if (value > MAX_CONSUMPTION) {
            return MAX_CONSUMPTION;
        }
        return value;
    }

    private void toggleMetric() {
        useLph = !useLph;
        updateLegendTitle();
        refreshTrackOverlays();
    }

    private void updateLegendTitle() {
        if (legendTitle == null || toggleMetricButton == null) {
            return;
        }
        if (useLph) {
            legendTitle.setText("L/h");
            ((android.widget.Button) toggleMetricButton).setText("L/100Km");
        } else {
            legendTitle.setText("L/100km");
            ((android.widget.Button) toggleMetricButton).setText("L/h");
        }
    }

    private Double getMetricValue(TripFuelTrackStore.TrackPoint sample) {
        if (useLph) {
            if (Double.isNaN(sample.litersPerHour)) {
                return sample.litersPer100km;
            }
            return sample.litersPerHour;
        }
        return sample.litersPer100km;
    }

    private void toggleFollowMode() {
        followEnabled = !followEnabled;
        updateFollowButton();
        updateFollowInteraction();

        MainActivity activity = (MainActivity) requireActivity();
        activity.setSwipeEnabled(followEnabled);

        if (followEnabled && lastPoint != null && mapView != null) {
            mapView.getController().animateTo(lastPoint);
        }

        if (myLocationOverlay != null) {
            if (followEnabled) {
                myLocationOverlay.enableFollowLocation();
            } else {
                myLocationOverlay.disableFollowLocation();
            }
        }
    }

    private void updateFollowInteraction() {
        if (mapView == null) {
            return;
        }
        mapView.setMultiTouchControls(!followEnabled);
        mapView.setOnTouchListener((v, event) -> followEnabled);
    }

    @Override
    public void onEnterPip() {
        updatePipUiState(true);
    }

    @Override
    public void onExitPip() {
        updatePipUiState(false);
    }

    private void updatePipUiState(boolean isInPip) {
        int visibility = isInPip ? View.GONE : View.VISIBLE;
        if (legendContainer != null) {
            legendContainer.setVisibility(visibility);
        }
        if (followToggleButton != null) {
            followToggleButton.setVisibility(visibility);
        }
        if (toggleMetricButton != null) {
            toggleMetricButton.setVisibility(visibility);
        }
        if (exportTripButton != null) {
            exportTripButton.setVisibility(visibility);
        }
        if (zoomInButton != null) {
            zoomInButton.setVisibility(visibility);
        }
        if (zoomOutButton != null) {
            zoomOutButton.setVisibility(visibility);
        }
    }

    private void updateFollowButton() {
        if (followToggleButton instanceof android.widget.Button) {
            android.widget.Button button = (android.widget.Button) followToggleButton;
            button.setText(followEnabled ? "Free Pan" : "Follow Me");
        }
    }

    private void applyNightMode(boolean nightMode) {
        if (mapView == null) {
            return;
        }
        mapView.setTileSource(nightMode ? DARK_TILE_SOURCE : TileSourceFactory.MAPNIK);
        mapView.setBackgroundColor(nightMode ? Color.BLACK : Color.WHITE);
        if (legendContainer != null) {
            legendContainer.setBackgroundColor(nightMode ? 0x66000000 : 0xCCFFFFFF);
        }
        if (legendTitle != null && legendMin != null && legendMid1 != null
                && legendMid2 != null && legendMid3 != null && legendMax != null) {
            int textColor = nightMode ? Color.WHITE : Color.BLACK;
            legendTitle.setTextColor(textColor);
            legendMin.setTextColor(textColor);
            legendMid1.setTextColor(textColor);
            legendMid2.setTextColor(textColor);
            legendMid3.setTextColor(textColor);
            legendMax.setTextColor(textColor);
        }
        if (zoomInButton instanceof android.widget.ImageButton
                && zoomOutButton instanceof android.widget.ImageButton) {
            int tint = nightMode ? Color.WHITE : Color.BLACK;
            int bgColor = nightMode ? 0xAAFFFFFF : 0xE6FFFFFF;
            android.graphics.drawable.Drawable inBg = zoomInButton.getBackground();
            android.graphics.drawable.Drawable outBg = zoomOutButton.getBackground();
            if (inBg != null) {
                inBg.setTint(bgColor);
            }
            if (outBg != null) {
                outBg.setTint(bgColor);
            }
            ((android.widget.ImageButton) zoomInButton).setColorFilter(tint);
            ((android.widget.ImageButton) zoomOutButton).setColorFilter(tint);
        }
        mapView.invalidate();
    }

    private boolean isSystemNightMode() {
        android.content.res.Configuration config = requireContext().getResources().getConfiguration();
        return (config.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public void onGpsPulse(com.hondaafr.Libs.Devices.Phone.PhoneGps gps) {
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
    public void onObdValue(com.hondaafr.Libs.Devices.Obd.Readings.ObdReading reading) {
    }

    @Override
    public void onCalculationsUpdated() {
    }

    @Override
    public void onNightModeUpdated(boolean isNight) {
        boolean systemNight = isSystemNightMode();
        if (isNightMode != systemNight) {
            isNightMode = systemNight;
            applyNightMode(isNightMode);
        }
    }
}

