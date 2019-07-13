package com.otaliastudios.cameraview.overlay;


import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.otaliastudios.cameraview.CameraView;

import java.util.HashMap;
import java.util.Map;

/**
 * This class manages {@link OverlayLayoutInner}s.
 * The necessity for this class comes from two features of {@link View}s:
 *  - a {@link View} can only have one parent
 *  - the View framework does not provide a straightforward way for a {@link ViewGroup} to draw
 *    only a subset of it's children.
 * We have three possible target for a overlay {@link View} to be drawn on:
 *  - camera preview
 *  - picture snapshot
 *  - video snapshot
 * Given the two constraints above in order to draw exclusively on a subset of targets we need a
 * different {@link OverlayLayoutInner} for each subset of targets. This class manages those different
 * {@link OverlayLayoutInner}s.
 *
 * A problem remains: the views are drawn on preview when {@link #draw(Canvas)} is called on this
 * class, for not drawing on the preview but drawing on picture snapshot, for instance, we cannot
 * change the child's visibility.
 * One way to solve this problem is to have two instances of {@link OverlayLayout} and layer
 * them so that the one below is covered and hidden by the camera preview. This way only the top
 * {@link OverlayLayout} is shown on top of the camera preview and we can still access the
 * bottom one's {@link OverlayLayoutInner#draw(Canvas)} for drawing on picture snapshots.
 */
public class OverlayLayout extends FrameLayout implements SurfaceDrawer {

    private Map<OverlayType, OverlayLayoutInner> mLayouts = new HashMap<>();

    public OverlayLayout(@NonNull Context context) {
        super(context);
    }

    public OverlayLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        // params must be instance of OverlayLayoutParams
        if (!(params instanceof CameraView.OverlayLayoutParams)) {
            return;
        }

        OverlayType viewOverlayType = new OverlayType((CameraView.OverlayLayoutParams) params);
        if (mLayouts.containsKey(viewOverlayType)) {
            mLayouts.get(viewOverlayType).addView(child, params);
        } else {
            OverlayLayoutInner newLayout = new OverlayLayoutInner(getContext());
            newLayout.addView(child, params);
            super.addView(newLayout);
            mLayouts.put(viewOverlayType, newLayout);
        }
    }

    @Override
    public void removeView(View child) {
        // params must be instance of OverlayLayoutParams
        if (!(child.getLayoutParams() instanceof CameraView.OverlayLayoutParams)) {
            return;
        }

        OverlayType viewOverlayType = new OverlayType((CameraView.OverlayLayoutParams) child.getLayoutParams());
        if (mLayouts.containsKey(viewOverlayType)) {
            mLayouts.get(viewOverlayType).removeView(child);
        }
    }

    @Override
    public void drawOnSurfaceForPictureSnapshot(Canvas surfaceCanvas) {
        surfaceCanvas.save();
        // scale factor between canvas width and this View's width
        float widthScale = surfaceCanvas.getWidth() / (float) getWidth();
        // scale factor between canvas height and this View's height
        float heightScale = surfaceCanvas.getHeight() / (float) getHeight();
        surfaceCanvas.scale(widthScale, heightScale);
        for (Map.Entry<OverlayType, OverlayLayoutInner> entry : mLayouts.entrySet()) {
            if (entry.getKey().pictureSnapshot) {
                entry.getValue().drawOverlay(surfaceCanvas);
            }
        }
        surfaceCanvas.restore();
    }

    @Override
    public void drawOnSurfaceForVideoSnapshot(Canvas surfaceCanvas) {
        surfaceCanvas.save();
        // scale factor between canvas width and this View's width
        float widthScale = surfaceCanvas.getWidth() / (float) getWidth();
        // scale factor between canvas height and this View's height
        float heightScale = surfaceCanvas.getHeight() / (float) getHeight();
        surfaceCanvas.scale(widthScale, heightScale);
        for (Map.Entry<OverlayType, OverlayLayoutInner> entry : mLayouts.entrySet()) {
            if (entry.getKey().videoSnapshot) {
                entry.getValue().drawOverlay(surfaceCanvas);
            }
        }
        surfaceCanvas.restore();
    }

    private class OverlayType {
        boolean preview = false;
        boolean pictureSnapshot = false;
        boolean videoSnapshot = false;

        OverlayType(CameraView.OverlayLayoutParams params) {
            this.preview = params.isDrawInPreview();
            this.pictureSnapshot = params.isDrawInPictureSnapshot();
            this.videoSnapshot = params.isDrawInVideoSnapshot();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OverlayType that = (OverlayType) o;
            return preview == that.preview &&
                    pictureSnapshot == that.pictureSnapshot &&
                    videoSnapshot == that.videoSnapshot;
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31*result + (preview ? 1 : 0);
            result = 31*result + (pictureSnapshot ? 1 : 0);
            result = 31*result + (videoSnapshot ? 1 : 0);
            return result;
        }
    }

}
