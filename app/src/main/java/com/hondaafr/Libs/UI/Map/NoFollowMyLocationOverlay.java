package com.hondaafr.Libs.UI.Map;

import android.location.Location;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

/**
 * Location overlay that never moves the map on location update.
 * Map centering is handled by {@link MapFollowController} with smooth animation.
 * This prevents osmdroid's built-in follow from doing an instant setCenter when
 * the overlay receives a fix.
 */
public final class NoFollowMyLocationOverlay extends MyLocationNewOverlay {

    public NoFollowMyLocationOverlay(IMyLocationProvider provider, MapView mapView) {
        super(provider, mapView);
    }

    public NoFollowMyLocationOverlay(MapView mapView) {
        this(new GpsMyLocationProvider(mapView.getContext()), mapView);
    }

    @Override
    protected void setLocation(Location location) {
        mIsFollowing = false;
        super.setLocation(location);
    }
}
