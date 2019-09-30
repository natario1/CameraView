package com.otaliastudios.cameraview.markers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.PointF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * A default implementation of {@link AutoFocusMarker}.
 * You can call {@link com.otaliastudios.cameraview.CameraView#setAutoFocusMarker(AutoFocusMarker)}
 * passing in this class to have basic marker drawing.
 */
public class DefaultAutoFocusMarker implements AutoFocusMarker {

    @VisibleForTesting View mContainer;
    @VisibleForTesting View mFill;

    @Nullable
    @Override
    public View onAttach(@NonNull Context context, @NonNull ViewGroup container) {
        View view = LayoutInflater.from(context).inflate(R.layout.cameraview_layout_focus_marker,
                container, false);
        mContainer = view.findViewById(R.id.focusMarkerContainer);
        mFill = view.findViewById(R.id.focusMarkerFill);
        return view;
    }

    @Override
    public void onAutoFocusStart(@NonNull AutoFocusTrigger trigger, @NonNull PointF point) {
        if (trigger == AutoFocusTrigger.METHOD) return;
        mContainer.clearAnimation();
        mFill.clearAnimation();
        mContainer.setScaleX(1.36f);
        mContainer.setScaleY(1.36f);
        mContainer.setAlpha(1f);
        mFill.setScaleX(0);
        mFill.setScaleY(0);
        mFill.setAlpha(1f);
        animate(mContainer, 1, 1, 300, 0, null);
        animate(mFill, 1, 1, 300, 0, null);
    }

    @Override
    public void onAutoFocusEnd(@NonNull AutoFocusTrigger trigger,
                               boolean successful,
                               @NonNull PointF point) {
        if (trigger == AutoFocusTrigger.METHOD) return;
        if (successful) {
            animate(mContainer, 1, 0, 500, 0, null);
            animate(mFill, 1, 0, 500, 0, null);
        } else {
            animate(mFill, 0, 0, 500, 0, null);
            animate(mContainer, 1.36f, 1, 500, 0,
                    new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    animate(mContainer, 1.36f, 0, 200, 1000,
                            null);
                }
            });
        }
    }

    private static void animate(@NonNull View view, float scale, float alpha, long duration,
                                long delay, @Nullable Animator.AnimatorListener listener) {
        view.animate()
                .scaleX(scale)
                .scaleY(scale)
                .alpha(alpha)
                .setDuration(duration)
                .setStartDelay(delay)
                .setListener(listener)
                .start();
    }
}
