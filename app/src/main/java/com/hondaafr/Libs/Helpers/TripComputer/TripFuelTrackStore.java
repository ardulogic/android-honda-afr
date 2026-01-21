package com.hondaafr.Libs.Helpers.TripComputer;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripFuelTrackStore {
    private static final String FILE_PREFIX = "trip_fuel_track_";
    private static final long FLUSH_INTERVAL_MS = 2000;
    private static final int MAX_BUFFER_LINES = 40;

    private final File file;
    private final StringBuilder buffer = new StringBuilder();
    private long lastFlushMs = 0;

    public TripFuelTrackStore(Context context, String sessionId) {
        this.file = new File(context.getFilesDir(), FILE_PREFIX + sessionId + ".csv");
    }

    public void append(TrackPoint point) {
        if (point.isBreak) {
            buffer.append("#\n");
            flushIfNeeded();
            return;
        }

        buffer.append(point.latitude)
                .append(',')
                .append(point.longitude)
                .append(',')
                .append(point.litersPer100km)
                .append(',')
                .append(point.litersPerHour)
                .append('\n');
        flushIfNeeded();
    }

    public List<TrackPoint> loadAll() {
        List<TrackPoint> points = new ArrayList<>();
        if (!file.exists()) {
            return points;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    points.add(TrackPoint.breakMarker());
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    continue;
                }
                try {
                    double lat = Double.parseDouble(parts[0]);
                    double lon = Double.parseDouble(parts[1]);
                    double fuel = Double.parseDouble(parts[2]);
                    double lph = Double.NaN;
                    if (parts.length >= 4) {
                        lph = Double.parseDouble(parts[3]);
                    }
                    points.add(new TrackPoint(lat, lon, fuel, lph));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException ignored) {
        }

        return points;
    }

    public void clear() {
        buffer.setLength(0);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public boolean exportToDownloads(Context context) {
        flush();
        if (!file.exists()) {
            return false;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(new Date());
        String fileName = "Trip - " + timestamp + ".csv";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HondaAfr");

            Uri uri = context.getContentResolver()
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                return false;
            }

            try (OutputStream output = context.getContentResolver().openOutputStream(uri);
                 InputStream input = context.getContentResolver()
                         .openInputStream(Uri.fromFile(file))) {
                if (output == null || input == null) {
                    return false;
                }
                copyStream(input, output);
                return true;
            } catch (IOException ignored) {
                return false;
            }
        } else {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File outDir = new File(downloads, "HondaAfr");
            if (!outDir.exists() && !outDir.mkdirs()) {
                return false;
            }
            File outFile = new File(outDir, fileName);
            try (OutputStream output = new java.io.FileOutputStream(outFile);
                 InputStream input = new java.io.FileInputStream(file)) {
                copyStream(input, output);
                return true;
            } catch (IOException ignored) {
                return false;
            }
        }
    }

    public void flush() {
        if (buffer.length() == 0) {
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(buffer.toString());
            buffer.setLength(0);
            lastFlushMs = System.currentTimeMillis();
        } catch (IOException ignored) {
        }
    }

    private void flushIfNeeded() {
        long now = System.currentTimeMillis();
        if (buffer.length() >= MAX_BUFFER_LINES * 20 || now - lastFlushMs > FLUSH_INTERVAL_MS) {
            flush();
        }
    }

    public static void cleanupOldTracks(Context context, String keepSessionId) {
        File dir = context.getFilesDir();
        File[] files = dir.listFiles((d, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(".csv"));
        if (files == null) {
            return;
        }
        String keepName = FILE_PREFIX + keepSessionId + ".csv";
        for (File f : files) {
            if (!f.getName().equals(keepName)) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    private static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    public static class TrackPoint {
        public final double latitude;
        public final double longitude;
        public final double litersPer100km;
        public final double litersPerHour;
        public final boolean isBreak;

        public TrackPoint(double latitude, double longitude, double litersPer100km, double litersPerHour) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.litersPer100km = litersPer100km;
            this.litersPerHour = litersPerHour;
            this.isBreak = false;
        }

        private TrackPoint(boolean isBreak) {
            this.latitude = 0.0;
            this.longitude = 0.0;
            this.litersPer100km = 0.0;
            this.litersPerHour = Double.NaN;
            this.isBreak = isBreak;
        }

        public static TrackPoint breakMarker() {
            return new TrackPoint(true);
        }
    }
}

