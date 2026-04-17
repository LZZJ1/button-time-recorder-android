package com.example.buttontimerecorder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 记录表格适配器 - 对应原Python中的Treeview表格
 */
public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

    private final List<TimeRecord> records;

    public RecordAdapter(List<TimeRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimeRecord record = records.get(position);

        holder.tvIndex.setText(String.valueOf(record.getId()));
        holder.tvName.setText(record.getName());

        // 按下时间（只显示 HH:mm）
        String pressTime = record.getPressTime();
        if (pressTime != null && !pressTime.isEmpty()) {
            // 提取 HH:mm 部分
            holder.tvPressTime.setText(formatTime(pressTime));
        } else {
            holder.tvPressTime.setText("—");
        }

        // 弹起时间
        String releaseTime = record.getReleaseTime();
        if (releaseTime == null || releaseTime.isEmpty()) {
            holder.tvReleaseTime.setText("-未弹起");
            holder.tvReleaseTime.setTextColor(0xFFe74c3c);
        } else {
            holder.tvReleaseTime.setText(formatTime(releaseTime));
            holder.tvReleaseTime.setTextColor(0xFF666666);
        }

        // 持续时长
        String duration = record.getDuration();
        if (duration != null && !duration.isEmpty()) {
            holder.tvDuration.setText(duration);
        } else {
            holder.tvDuration.setText("—");
        }

        // 交替行背景色
        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(0xFFFFFFFF);
        } else {
            holder.itemView.setBackgroundColor(0xFFF8F9FA);
        }
    }

    private String formatTime(String fullTime) {
        if (fullTime == null) return "—";
        // 如果是完整时间 yyyy-MM-dd HH:mm:ss，提取 HH:mm
        if (fullTime.length() >= 16 && fullTime.contains(" ")) {
            return fullTime.substring(11, 16);
        }
        // 如果已经是 HH:mm 格式
        if (fullTime.length() == 5 && fullTime.contains(":")) {
            return fullTime;
        }
        return fullTime;
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex, tvName, tvPressTime, tvReleaseTime, tvDuration;

        ViewHolder(View view) {
            super(view);
            tvIndex = view.findViewById(R.id.tvIndex);
            tvName = view.findViewById(R.id.tvName);
            tvPressTime = view.findViewById(R.id.tvPressTime);
            tvReleaseTime = view.findViewById(R.id.tvReleaseTime);
            tvDuration = view.findViewById(R.id.tvDuration);
        }
    }
}
