package com.otaliastudios.cameraview.internal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.otaliastudios.cameraview.controls.Grid;

/**
 * A layout overlay that draws grid lines based on the {@link Grid} parameter.
 */
public class GridLinesLayout extends View {

    private final static float GOLDEN_RATIO_INV = 0.61803398874989f;
    public final static int DEFAULT_COLOR = Color.argb(160, 255, 255, 255);

    private Grid gridMode;
    private int gridColor = DEFAULT_COLOR;

    private ColorDrawable horiz;
    private ColorDrawable vert;
    private final float width;

    interface DrawCallback {
        void onDraw(int lines);
    }

    @VisibleForTesting DrawCallback callback;

    public GridLinesLayout(@NonNull Context context) {
        this(context, null);
    }

    public GridLinesLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        horiz = new ColorDrawable(gridColor);
        vert = new ColorDrawable(gridColor);
        width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0.9f,
                context.getResources().getDisplayMetrics());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        horiz.setBounds(left, 0, right, (int) width);
        vert.setBounds(0, top, (int) width, bottom);
    }

    /**
     * Returns the current grid value.
     * @return the grid mode
     */
    @NonNull
    public Grid getGridMode() {
        return gridMode;
    }

    /**
     * Sets a new grid value
     * @param gridMode the new value
     */
    public void setGridMode(@NonNull Grid gridMode) {
        this.gridMode = gridMode;
        postInvalidate();
    }

    /**
     * Returns the current grid color.
     * @return the grid color
     */
    public int getGridColor() {
        return gridColor;
    }

    /**
     * Sets a new grid color.
     * @param gridColor the new color
     */
    public void setGridColor(@ColorInt int gridColor) {
        this.gridColor = gridColor;
        horiz.setColor(gridColor);
        vert.setColor(gridColor);
        postInvalidate();
    }

    private int getLineCount() {
        switch (gridMode) {
            case OFF: return 0;
            case DRAW_3X3: return 2;
            case DRAW_PHI: return 2;
            case DRAW_4X4: return 3;
        }
        return 0;
    }

    private float getLinePosition(int lineNumber) {
        int lineCount = getLineCount();
        if (gridMode == Grid.DRAW_PHI) {
            // 1 = 2x + GRIx
            // 1 = x(2+GRI)
            // x = 1/(2+GRI)
            float delta = 1f/(2+GOLDEN_RATIO_INV);
            return lineNumber == 1 ? delta : (1 - delta);
        } else {
            return (1f / (lineCount + 1)) * (lineNumber + 1f);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int count = getLineCount();
        for (int n = 0; n < count; n++) {
            float pos = getLinePosition(n);

            // Draw horizontal line
            canvas.translate(0, pos * getHeight());
            horiz.draw(canvas);
            canvas.translate(0, - pos * getHeight());

            // Draw vertical line
            canvas.translate(pos * getWidth(), 0);
            vert.draw(canvas);
            canvas.translate(- pos * getWidth(), 0);
        }
        if (callback != null) {
            callback.onDraw(count);
        }
    }
}
