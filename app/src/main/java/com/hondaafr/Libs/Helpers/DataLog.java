package com.hondaafr.Libs.Helpers;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DataLog {
    private final Context context;
    private final ArrayList<DataLogEntry> entries;

    // Constructor to initialize the list
    public DataLog(Context context) {
        this.entries = new ArrayList<>();
        this.context = context;
    }

    // Getter for entries
    public ArrayList<DataLogEntry> getEntries() {
        return entries;
    }

    // Method to add a new entry
    public void addEntry(DataLogEntry entry) {
        this.entries.add(entry);
    }

    // Method to clear all entries
    public void clearAllEntries() {
        this.entries.clear();
    }

    public void saveAsCsv() {
        // Check if Android version is above or equal to Android Q (API 29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(new Date());
            String fileName = timeStamp + ".csv";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HondaAfr");


            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try {
                    OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                        CSVWriter csvWriter = new CSVWriter(writer);
                        csvWriter.writeNext(DataLogEntry.getHeader());

                        for (DataLogEntry row : this.entries) {
                            csvWriter.writeNext(row.toStringArray());
                        }

                        csvWriter.close();
                        Toast.makeText(context, "CSV file saved to Downloads successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to open OutputStream", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Failed to save CSV file", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Failed to get URI for Downloads", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Handle case for Android versions below Q if necessary
            Toast.makeText(context, "Saving to Downloads is supported on Android Q and above", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public String toString() {
        return "HistoryData{" +
                "entries=" + entries +
                '}';
    }
}
