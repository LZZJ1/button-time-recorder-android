package com.example.buttontimerecorder;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * 切换按钮组件 - 对应原Python中的ToggleButton类
 * 支持按下/弹起状态切换，显示状态文字，带删除按钮
 */
public class ToggleButtonView {

    public interface OnToggleListener {
        void onToggle(ToggleButtonView btn);
    }

    public interface OnDeleteListener {
        void onDelete(ToggleButtonView btn);
    }

    private final View rootView;
    private final Button btnToggle;
    private final TextView tvStatus;
    private final TextView btnDelete;

    private final String name;
    private boolean isPressed = false;

    private OnToggleListener toggleListener;
    private OnDeleteListener deleteListener;

    // 颜色常量
    private static final int COLOR_GREEN = Color.parseColor("#4CAF50");
    private static final int COLOR_RED = Color.parseColor("#e74c3c");

    public ToggleButtonView(Context context, String name,
                            OnToggleListener toggleListener,
                            OnDeleteListener deleteListener) {
        this.name = name;
        this.toggleListener = toggleListener;
        this.deleteListener = deleteListener;

        LayoutInflater inflater = LayoutInflater.from(context);
        rootView = inflater.inflate(R.layout.item_toggle_button, null, false);

        btnToggle = rootView.findViewById(R.id.btnToggle);
        tvStatus = rootView.findViewById(R.id.tvStatus);
        btnDelete = rootView.findViewById(R.id.btnDelete);

        btnToggle.setText(name);
        btnToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(COLOR_GREEN));

        btnToggle.setOnClickListener(v -> {
            isPressed = !isPressed;
            updateAppearance();
            if (toggleListener != null) {
                toggleListener.onToggle(this);
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(this);
            }
        });

        updateAppearance();
    }

    private void updateAppearance() {
        if (isPressed) {
            btnToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(COLOR_RED));
            tvStatus.setText("已按下");
            tvStatus.setTextColor(Color.parseColor("#e74c3c"));
        } else {
            btnToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(COLOR_GREEN));
            tvStatus.setText("未按下");
            tvStatus.setTextColor(Color.parseColor("#999999"));
        }
    }

    public View getRootView() {
        return rootView;
    }

    public String getName() {
        return name;
    }

    public boolean isPressed() {
        return isPressed;
    }

    public void setVisible(boolean visible) {
        rootView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public boolean isVisible() {
        return rootView.getVisibility() == View.VISIBLE;
    }

    public void setToggleListener(OnToggleListener listener) {
        this.toggleListener = listener;
    }

    public void setDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }
}
