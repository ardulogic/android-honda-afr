package com.hondaafr.Libs.UI.Fragments;

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
import com.hondaafr.Libs.UI.Map.MapMetric;
import com.hondaafr.Libs.UI.Map.MapFollowController;
import com.hondaafr.Libs.UI.Map.MapTrackDataSource;
import com.hondaafr.Libs.UI.Map.MapTrackRenderer;
import com.hondaafr.Libs.UI.Map.MapUiController;
import com.hondaafr.MainActivity;
import com.hondaafr.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import com.hondaafr.Libs.UI.Map.NoFollowMyLocationOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapFragment extends Fragment implements TripComputerListener, PipAware {
    private static final String TAG = "MapFragment";
    private static final String LISTENER_ID = "map_fragment";
    private static final double MAX_CONSUMPTION = 30.0;
    private static final long RENDER_THROTTLE_MS = 250;

    private MapView mapView;
    private NoFollowMyLocationOverlay myLocationOverlay;
    private TripComputer tripComputer;
    private String sessionId = "";
    private TripFuelTrackStore trackStore;
    private long lastRenderMs = 0;
    private boolean didInitialZoom = false;
    private boolean isNightMode = false;
    private boolean mapReady = false;

    private MapTrackDataSource trackDataSource;
    private MapTrackRenderer trackRenderer;
    private MapFollowController followController;
    private MapUiController uiController;

    private MapMetric currentMetric = MapMetric.L_PER_100KM;
    private ExecutorService trackLoadExecutor;

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
        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true);
        mapView.setBuiltInZoomControls(false);
        mapView.getController().setZoom(16.0);

        isNightMode = isSystemNightMode();
        myLocationOverlay = new NoFollowMyLocationOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        mapView.getOverlays().add(myLocationOverlay);

        trackDataSource = new MapTrackDataSource();
        trackRenderer = new MapTrackRenderer(mapView);
        followController = new MapFollowController(mapView, myLocationOverlay);
        View followToggleButton = view.findViewById(R.id.buttonMapFollow);
        TextView legendTitle = view.findViewById(R.id.textLegendTitle);
        View zoomInButton = view.findViewById(R.id.buttonZoomIn);
        View zoomOutButton = view.findViewById(R.id.buttonZoomOut);
        View exportTripButton = view.findViewById(R.id.buttonExportTrip);
        uiController = new MapUiController(mapView, legendTitle, zoomInButton, zoomOutButton,
                exportTripButton, followToggleButton);

        tripComputer = ((MainActivity) requireActivity()).getTripComputer();
        sessionId = tripComputer.tripStats.getSessionId(requireContext());
        trackStore = tripComputer.getTrackStore();
        if (trackStore == null) {
            trackStore = new TripFuelTrackStore(requireContext(), sessionId);
        }
        loadTrackAsync();

        uiController.updateFollowButton(followController.isFollowEnabled());
        followToggleButton.setOnClickListener(v -> toggleFollowMode());
        updateFollowInteraction();
        zoomInButton.setOnClickListener(v -> mapView.getController().zoomIn());
        zoomOutButton.setOnClickListener(v -> mapView.getController().zoomOut());
        exportTripButton.setOnClickListener(v -> exportTripCsv());
        legendTitle.setOnClickListener(v -> toggleMetric());

        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                refreshTrackOverlays(false);
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                refreshTrackOverlays(true);
                return false;
            }
        });

        uiController.applyNightMode(isNightMode);
        uiController.updateLegendTitle(currentMetric);
        uiController.updatePipUiState(requireActivity().isInPictureInPictureMode());
        mapReady = true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (trackLoadExecutor != null) {
            trackLoadExecutor.shutdownNow();
            trackLoadExecutor = null;
        }
        if (trackRenderer != null) {
            trackRenderer.clear();
            trackRenderer = null;
        }
        if (mapView != null) {
            mapView.getOverlays().clear();
            mapView.onDetach();
            mapView = null;
        }
        trackDataSource = null;
        followController = null;
        uiController = null;
        myLocationOverlay = null;
        mapReady = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        if (myLocationOverlay != null) {
            ensureMyLocationProvider();
            myLocationOverlay.enableMyLocation();
            if (followController != null) {
                followController.applyToOverlay();
            }
        }
        if (tripComputer != null) {
            tripComputer.addListener(LISTENER_ID, this);
            String currentSession = tripComputer.getCurrentSessionId();
            if (!currentSession.equals(sessionId)) {
                sessionId = currentSession;
                trackStore = tripComputer.getTrackStore();
                if (trackStore == null) {
                    trackStore = new TripFuelTrackStore(requireContext(), sessionId);
                }
                if (trackDataSource != null) {
                    trackDataSource.clear();
                }
                if (followController != null) {
                    followController.setLastPoint(null);
                }
                loadTrackAsync();
            } else {
                trackStore = tripComputer.getTrackStore();
                if (trackStore != null) {
                    loadTrackAsync();
                }
            }
        }
        if (uiController != null) {
            uiController.updatePipUiState(requireActivity().isInPictureInPictureMode());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        boolean inPip = requireActivity().isInPictureInPictureMode();
        if (!inPip) {
            if (tripComputer != null) {
                tripComputer.removeListener(LISTENER_ID);
            }
            if (myLocationOverlay != null) {
                myLocationOverlay.disableMyLocation();
            }
            if (mapView != null) {
                mapView.onPause();
            }
        }
        if (trackStore != null) {
            trackStore.flush();
        }
    }

    private void ensureMyLocationProvider() {
        if (mapView == null) {
            return;
        }
        if (myLocationOverlay == null || myLocationOverlay.getMyLocationProvider() == null) {
            if (myLocationOverlay != null) {
                mapView.getOverlays().remove(myLocationOverlay);
            }
            myLocationOverlay = new NoFollowMyLocationOverlay(new GpsMyLocationProvider(requireContext()), mapView);
            mapView.getOverlays().add(myLocationOverlay);
        }
    }

    @Override
    public void onGpsUpdate(Double speed, double distanceIncrement) {
        if (tripComputer == null || tripComputer.gps == null || mapView == null) {
            return;
        }
        if (!tripComputer.mObdStudio.isAlive() || !tripComputer.mSpartanStudio.isAlive()) {
            if (followController != null) {
                followController.setLastPoint(null);
            }
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
        GeoPoint prev = followController != null ? followController.getLastPoint() : null;
        if (prev != null && prev.equals(currPoint)) {
            return;
        }

        boolean isFirst = (prev == null);
        if (isFirst) {
            appendCurrentPointToFuelSamples(lat, lon);
            if (followController != null) {
                followController.setLastPoint(currPoint);
                followController.onLocationUpdate(currPoint, true);
            }
            runIfAlive(() -> refreshTrackOverlays(false));
            return;
        }

        appendCurrentPointToFuelSamples(lat, lon);
        if (followController != null && mapView != null) {
            followController.setLastPoint(currPoint);
            mapView.post(() -> followController.onLocationUpdate(currPoint, false));
        }
        runIfAlive(() -> refreshTrackOverlays(false));
    }

    private void runIfAlive(Runnable r) {
        if (isAdded() && mapView != null && mapView.isAttachedToWindow()) {
            r.run();
        }
    }

    /**
     * Appends the current location as a track point for display.
     * TripComputer has already appended this point to trackStore; we only update in-memory display.
     */
    private void appendCurrentPointToFuelSamples(double lat, double lon) {
        double lp100kmAvg = tripComputer.instStats.getLp100kmAvg();
        double lp100km = (lp100kmAvg > 0) ? lp100kmAvg : tripComputer.instStats.getLp100km();
        Double sanitized = sanitizeMetric(lp100km);
        Double lph = sanitizeMetric(tripComputer.instStats.getLphAvg());
        double consumption = sanitized != null ? sanitized : 0.0;
        double lphVal = lph != null ? lph : Double.NaN;
        double afr = tripComputer.getLastAfr();
        double rpm = tripComputer.getLastRpm();
        double mapKpa = tripComputer.getLastMapKpa();
        double speedKmh = tripComputer.getSpeed();
        TripFuelTrackStore.TrackPoint sample = new TripFuelTrackStore.TrackPoint(
                lat, lon, consumption, lphVal, afr, rpm, mapKpa, speedKmh);
        if (trackDataSource != null) {
            trackDataSource.append(sample);
        }
    }

    private void handleSessionChange() {
        String currentSession = tripComputer.tripStats.getSessionId(requireContext());
        if (!currentSession.equals(sessionId)) {
            sessionId = currentSession;
            trackStore = tripComputer.getTrackStore();
            if (trackStore == null) {
                trackStore = new TripFuelTrackStore(requireContext(), sessionId);
            }
            if (trackDataSource != null) {
                trackDataSource.clear();
            }
            if (trackRenderer != null) {
                trackRenderer.clear();
            }
            if (followController != null) {
                followController.setLastPoint(null);
            }
            runIfAlive(() -> refreshTrackOverlays(false));
            loadTrackAsync();
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

    private void ensureInitialViewport() {
        if (didInitialZoom || mapView == null || trackDataSource == null || trackDataSource.isEmpty()) {
            return;
        }
        List<TripFuelTrackStore.TrackPoint> samples = trackDataSource.getSamples();
        for (int i = samples.size() - 1; i >= 0; i--) {
            TripFuelTrackStore.TrackPoint p = samples.get(i);
            if (!p.isBreak) {
                mapView.getController().setCenter(new GeoPoint(p.latitude, p.longitude));
                break;
            }
        }
        didInitialZoom = true;
    }

    private void loadTrackAsync() {
        if (trackStore == null) {
            return;
        }
        if (trackLoadExecutor == null) {
            trackLoadExecutor = Executors.newSingleThreadExecutor();
        }
        trackLoadExecutor.submit(() -> {
            List<TripFuelTrackStore.TrackPoint> loaded = trackStore.loadAll();
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || trackDataSource == null) {
                    return;
                }
                trackDataSource.setSamples(loaded);
                if (!trackDataSource.isEmpty() && followController != null) {
                    List<TripFuelTrackStore.TrackPoint> samples = trackDataSource.getSamples();
                    for (int i = samples.size() - 1; i >= 0; i--) {
                        TripFuelTrackStore.TrackPoint p = samples.get(i);
                        if (!p.isBreak) {
                            followController.setLastPoint(new GeoPoint(p.latitude, p.longitude));
                            break;
                        }
                    }
                }
                runIfAlive(() -> {
                    refreshTrackOverlays(false);
                    ensureInitialViewport();
                });
            });
        });
    }

    private void refreshTrackOverlays(boolean force) {
        if (mapView == null || !isAdded() || trackRenderer == null || trackDataSource == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && (now - lastRenderMs < RENDER_THROTTLE_MS)) {
            return;
        }
        lastRenderMs = now;

        if (!mapView.isAttachedToWindow() || mapView.getRepository() == null) {
            return;
        }

        List<TripFuelTrackStore.TrackPoint> samples = trackDataSource.getSamples();
        org.osmdroid.util.BoundingBox bounds = trackDataSource.getViewportBox(mapView);
        TripFuelTrackStore.TrackPoint lastTrackPoint = getLastTrackPoint(samples);

        if (samples.size() < 2) {
            trackRenderer.clear();
            trackRenderer.updateLabels(trackDataSource.getVisibleSamples(mapView), currentMetric, lastTrackPoint);
            trackRenderer.invalidate();
            return;
        }

        trackRenderer.render(samples, currentMetric, bounds != null ? bounds : mapView.getBoundingBox());
        trackRenderer.updateLabels(trackDataSource.getVisibleSamples(mapView), currentMetric, lastTrackPoint);
        trackRenderer.invalidate();
    }

    /** Returns the last non-break point of the track (most recent), or null if none. */
    private TripFuelTrackStore.TrackPoint getLastTrackPoint(List<TripFuelTrackStore.TrackPoint> samples) {
        if (samples == null || samples.isEmpty()) return null;
        for (int i = samples.size() - 1; i >= 0; i--) {
            TripFuelTrackStore.TrackPoint p = samples.get(i);
            if (!p.isBreak) return p;
        }
        return null;
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
        currentMetric = currentMetric.next();
        if (uiController != null) {
            uiController.updateLegendTitle(currentMetric);
        }
        refreshTrackOverlays(false);
    }

    private void toggleFollowMode() {
        if (followController == null || uiController == null) {
            return;
        }
        followController.toggle();
        uiController.updateFollowButton(followController.isFollowEnabled());
        updateFollowInteraction();

        MainActivity activity = (MainActivity) requireActivity();
        activity.setSwipeEnabled(followController.isFollowEnabled());

        followController.applyToOverlay();
        followController.centerOnLastPoint();
    }

    private void updateFollowInteraction() {
        if (mapView == null || followController == null) {
            return;
        }
        boolean follow = followController.isFollowEnabled();
        mapView.setMultiTouchControls(!follow);
        mapView.setOnTouchListener((v, event) -> follow);
    }

    @Override
    public void onEnterPip() {
        if (tripComputer != null) {
            tripComputer.addListener(LISTENER_ID, this);
        }
        if (myLocationOverlay != null && mapView != null) {
            ensureMyLocationProvider();
            myLocationOverlay.enableMyLocation();
            if (followController != null) {
                followController.applyToOverlay();
            }
        }
        if (mapView != null) {
            mapView.onResume();
        }
        if (uiController != null) {
            uiController.updatePipUiState(true);
        }
    }

    @Override
    public void onExitPip() {
        if (uiController != null) {
            uiController.updatePipUiState(false);
        }
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
        if (tripComputer == null || mapView == null || !isAdded()) {
            return;
        }
        handleSessionChange();
    }

    @Override
    public void onNightModeUpdated(boolean isNight) {
        boolean systemNight = isSystemNightMode();
        if (isNightMode != systemNight && uiController != null) {
            isNightMode = systemNight;
            uiController.applyNightMode(isNightMode);
        }
    }
}

