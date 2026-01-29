package com.hondaafr.Libs.UI.Map;

import android.graphics.Color;
import android.view.View;

import com.hondaafr.R;

import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.views.MapView;

/**
 * Handles map UI: legend (current dimension text), buttons, PiP visibility, night mode.
 */
public final class MapUiController {

    private static final ITileSource DARK_TILE_SOURCE = new XYTileSource(
            "CartoDarkMatter",
            0,
            20,
            256,
            ".png",
            new String[]{
                    "https://a.basemaps.cartocdn.com/dark_all/",
                    "https://b.basemaps.cartocdn.com/dark_all/",
                    "https://c.basemaps.cartocdn.com/dark_all/",
                    "https://d.basemaps.cartocdn.com/dark_all/"
            }
    );

    private final MapView mapView;
    private final android.widget.TextView legendTitle;
    private final View zoomInButton;
    private final View zoomOutButton;
    private final View exportTripButton;
    private final View toggleMetricButton;
    private final View followToggleButton;

    public MapUiController(
            MapView mapView,
            android.widget.TextView legendTitle,
            View zoomInButton,
            View zoomOutButton,
            View exportTripButton,
            View toggleMetricButton,
            View followToggleButton
    ) {
        this.mapView = mapView;
        this.legendTitle = legendTitle;
        this.zoomInButton = zoomInButton;
        this.zoomOutButton = zoomOutButton;
        this.exportTripButton = exportTripButton;
        this.toggleMetricButton = toggleMetricButton;
        this.followToggleButton = followToggleButton;
    }

    public void applyNightMode(boolean nightMode) {
        if (mapView == null) {
            return;
        }
        ITileSource tileSource = nightMode ? DARK_TILE_SOURCE : TileSourceFactory.MAPNIK;
        mapView.setTileSource(tileSource);
        mapView.setBackgroundColor(nightMode ? Color.BLACK : Color.WHITE);
        if (legendTitle != null) {
            legendTitle.setBackgroundColor(nightMode ? 0x66000000 : 0xCCFFFFFF);
            legendTitle.setTextColor(nightMode ? Color.WHITE : Color.BLACK);
        }
        int tint = nightMode ? Color.WHITE : Color.BLACK;
        int bgColor = nightMode ? 0xAAFFFFFF : 0xE6FFFFFF;
        if (zoomInButton instanceof android.widget.ImageButton
                && zoomOutButton instanceof android.widget.ImageButton) {
            android.graphics.drawable.Drawable inBg = zoomInButton.getBackground();
            android.graphics.drawable.Drawable outBg = zoomOutButton.getBackground();
            if (inBg != null) inBg.setTint(bgColor);
            if (outBg != null) outBg.setTint(bgColor);
            ((android.widget.ImageButton) zoomInButton).setColorFilter(tint);
            ((android.widget.ImageButton) zoomOutButton).setColorFilter(tint);
        }
        if (followToggleButton instanceof android.widget.ImageButton) {
            android.graphics.drawable.Drawable followBg = followToggleButton.getBackground();
            if (followBg != null) followBg.setTint(bgColor);
            ((android.widget.ImageButton) followToggleButton).setColorFilter(tint);
        }
        mapView.invalidate();
    }

    public void updatePipUiState(boolean isInPip) {
        int visibility = isInPip ? View.GONE : View.VISIBLE;
        if (legendTitle != null) legendTitle.setVisibility(visibility);
        if (followToggleButton != null) followToggleButton.setVisibility(visibility);
        if (toggleMetricButton != null) toggleMetricButton.setVisibility(visibility);
        if (exportTripButton != null) exportTripButton.setVisibility(visibility);
        if (zoomInButton != null) zoomInButton.setVisibility(visibility);
        if (zoomOutButton != null) zoomOutButton.setVisibility(visibility);
    }

    public void updateLegendTitle(MapMetric metric) {
        if (legendTitle != null) {
            legendTitle.setText(metric.label());
        }
        if (toggleMetricButton instanceof android.widget.Button) {
            ((android.widget.Button) toggleMetricButton).setText("Next");
        }
    }

    public void updateFollowButton(boolean followEnabled) {
        if (followToggleButton instanceof android.widget.ImageButton) {
            ((android.widget.ImageButton) followToggleButton).setImageResource(
                    followEnabled ? R.drawable.ic_map_pan : R.drawable.ic_map_follow);
        }
    }
}
