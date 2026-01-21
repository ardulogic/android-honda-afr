package com.hondaafr.Libs.UI.Fragments.ObdLog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hondaafr.Libs.Devices.Obd.ObdLogStore;
import com.hondaafr.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ObdLogAdapter extends RecyclerView.Adapter<ObdLogAdapter.LogViewHolder> {
    private final List<ObdLogStore.LogEntry> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

    public void setItems(List<ObdLogStore.LogEntry> entries) {
        items.clear();
        if (entries != null) {
            items.addAll(entries);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_obd_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        ObdLogStore.LogEntry entry = items.get(position);
        holder.textTime.setText(timeFormat.format(new Date(entry.timestamp)));
        holder.textDirection.setText(entry.direction);
        holder.textMessage.setText(entry.message);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTime;
        private final TextView textDirection;
        private final TextView textMessage;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            textTime = itemView.findViewById(R.id.textLogTime);
            textDirection = itemView.findViewById(R.id.textLogDirection);
            textMessage = itemView.findViewById(R.id.textLogMessage);
        }
    }
}

