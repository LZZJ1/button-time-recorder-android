package com.example.buttontimerecorder;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * 流式布局 - 支持自动换行，对应原Python中的FlowFrame
 */
public class FlowLayout extends ViewGroup {

    private int horizontalSpacing = 8;
    private int verticalSpacing = 8;

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSpacing(int horizontal, int vertical) {
        this.horizontalSpacing = horizontal;
        this.verticalSpacing = vertical;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int currentX = paddingLeft;
        int currentY = paddingTop;
        int lineHeight = 0;
        int totalHeight = paddingTop;

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (currentX + childWidth + paddingRight > width && currentX > paddingLeft) {
                // 换行
                currentX = paddingLeft;
                currentY += lineHeight + verticalSpacing;
                lineHeight = 0;
            }

            currentX += childWidth + horizontalSpacing;
            lineHeight = Math.max(lineHeight, childHeight);
        }

        totalHeight = currentY + lineHeight + paddingBottom;
        setMeasuredDimension(width, resolveSize(totalHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();

        int currentX = paddingLeft;
        int currentY = paddingTop;
        int lineHeight = 0;

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (currentX + childWidth + paddingRight > width && currentX > paddingLeft) {
                currentX = paddingLeft;
                currentY += lineHeight + verticalSpacing;
                lineHeight = 0;
            }

            child.layout(currentX, currentY, currentX + childWidth, currentY + childHeight);
            currentX += childWidth + horizontalSpacing;
            lineHeight = Math.max(lineHeight, childHeight);
        }
    }
}
