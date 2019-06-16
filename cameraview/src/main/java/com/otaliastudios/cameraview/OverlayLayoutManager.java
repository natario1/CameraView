package com.otaliastudios.cameraview;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * This class manages the allocation of buffers and Frame objects.
 * We are interested in both recycling byte[] buffers so they are not allocated for each
 * preview frame, and in recycling Frame instances (so we don't instantiate a lot).
 *
 * For this, we keep a mPoolSize integer that defines the size of instances to keep.
 * Whether this does make sense, it depends on how slow the frame processors are.
 * If they are very slow, it is possible that some frames will be skipped.
 *
 * - byte[] buffer pool:
 *     this is not kept here, because Camera1 internals already have one that we can't control, but
 *     it should be OK. The only thing we do is allocate mPoolSize buffers when requested.
 * - Frame pool:
 *     We keep a list of mPoolSize recycled instances, to be reused when a new buffer is available.
 */
class OverlayLayoutManager extends FrameLayout implements SurfaceDrawer {

    private Map<OverlayType, OverlayLayout> mLayouts = new HashMap<>();

    public OverlayLayoutManager(@NonNull Context context) {
        super(context);
    }

    public OverlayLayoutManager(@NonNull Context context, @Nullable AttributeSet attrs) {
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
            OverlayLayout newLayout = new OverlayLayout(getContext());
            newLayout.addView(child, params);
            super.addView(newLayout);
            mLayouts.put(viewOverlayType, newLayout);
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
        for (Map.Entry<OverlayType, OverlayLayout> entry : mLayouts.entrySet()) {
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
        for (Map.Entry<OverlayType, OverlayLayout> entry : mLayouts.entrySet()) {
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
