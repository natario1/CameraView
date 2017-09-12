package com.otaliastudios.cameraview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

// TODO animate lines!
class GridLinesLayout extends View {

    private Grid gridMode;

    private Drawable horiz;
    private Drawable vert;
    private final float width;

    Task<Integer> drawTask = new Task<>();

    private final static float GOLDEN_RATIO_INV = 0.61803398874989f;

    public GridLinesLayout(@NonNull Context context) {
        this(context, null);
    }

    public GridLinesLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        horiz = new ColorDrawable(Color.WHITE); horiz.setAlpha(160);
        vert = new ColorDrawable(Color.WHITE); vert.setAlpha(160);
        width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0.9f, context.getResources().getDisplayMetrics());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        horiz.setBounds(left, 0, right, (int) width);
        vert.setBounds(0, top, (int) width, bottom);
    }

    public Grid getGridMode() {
        return gridMode;
    }

    public void setGridMode(Grid gridMode) {
        this.gridMode = gridMode;
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTask.start();
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
        drawTask.end(count);
    }
}
