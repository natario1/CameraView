package com.otaliastudios.cameraview.markers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Manages markers and provides an hierarchy / Canvas for them.
 * It is responsible for calling {@link Marker#onAttach(Context, ViewGroup)}.
 */
public class MarkerLayout extends FrameLayout {

    public final static int TYPE_AUTOFOCUS = 1;

    @SuppressLint("UseSparseArrays")
    private final HashMap<Integer, View> mViews = new HashMap<>();

    public MarkerLayout(@NonNull Context context) {
        super(context);
    }

    /**
     * Notifies that a new marker was added, possibly replacing another one.
     * @param type the marker type
     * @param marker the marker
     */
    public void onMarker(int type, @Nullable Marker marker) {
        // First check if we have a view for a previous marker of this type.
        View oldView = mViews.get(type);
        if (oldView != null) removeView(oldView);
        // If new marker is null, we're done.
        if (marker == null) return;
        // Now see if we have a new view.
        View newView = marker.onAttach(getContext(), this);
        if (newView != null) {
            mViews.put(type, newView);
            addView(newView);
        }
    }

    /**
     * The event that should trigger the drawing is about to be dispatched to
     * markers. If we have a valid View, cancel any animations on it and reposition
     * it.
     * @param type the event type
     * @param points the position
     */
    public void onEvent(int type, @NonNull PointF[] points) {
        View view = mViews.get(type);
        if (view == null) return;
        view.clearAnimation();
        if (type == TYPE_AUTOFOCUS) {
            // TODO can't be sure that getWidth and getHeight are available here.
            PointF point = points[0];
            float x = (int) (point.x - view.getWidth() / 2);
            float y = (int) (point.y - view.getHeight() / 2);
            view.setTranslationX(x);
            view.setTranslationY(y);
        }
    }
}
