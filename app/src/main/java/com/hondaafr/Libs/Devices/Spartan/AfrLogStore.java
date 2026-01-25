package com.hondaafr.Libs.Devices.Spartan;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AfrLogStore {
    private static final int MAX_ENTRIES = 100;
    private static final Deque<LogEntry> entries = new ArrayDeque<>();
    private static final CopyOnWriteArrayList<LogListener> listeners = new CopyOnWriteArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void logTx(String message) {
        append("TX", message);
    }

    public static void logRx(String message) {
        append("RX", message);
    }

    public static void logBt(String message) {
        append("BT", message);
    }

    public static void addListener(LogListener listener) {
        listeners.addIfAbsent(listener);
        notifyListener(listener);
    }

    public static void removeListener(LogListener listener) {
        listeners.remove(listener);
    }

    public static void clear() {
        synchronized (entries) {
            entries.clear();
        }
        notifyAllListeners();
    }

    private static void append(String direction, String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        synchronized (entries) {
            entries.addLast(new LogEntry(System.currentTimeMillis(), direction, message.trim()));
            while (entries.size() > MAX_ENTRIES) {
                entries.removeFirst();
            }
        }
        notifyAllListeners();
    }

    private static void notifyAllListeners() {
        for (LogListener listener : listeners) {
            notifyListener(listener);
        }
    }

    private static void notifyListener(LogListener listener) {
        List<LogEntry> snapshot;
        synchronized (entries) {
            snapshot = new ArrayList<>(entries);
        }
        mainHandler.post(() -> listener.onLogUpdated(snapshot));
    }

    public interface LogListener {
        void onLogUpdated(List<LogEntry> entries);
    }

    public static class LogEntry {
        public final long timestamp;
        public final String direction;
        public final String message;

        public LogEntry(long timestamp, String direction, String message) {
            this.timestamp = timestamp;
            this.direction = direction;
            this.message = message;
        }
    }
}

