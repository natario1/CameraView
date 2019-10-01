package com.otaliastudios.cameraview.internal.utils;

import android.content.Context;
import android.hardware.SensorManager;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Helps with keeping track of both device orientation (which changes when device is rotated)
 * and the display offset (which depends on the activity orientation
 * wrt the device default orientation).
 */
public class OrientationHelper {

    /**
     * Receives callback about the device orientation changes.
     */
    public interface Callback {
        void onDeviceOrientationChanged(int deviceOrientation);
    }

    @VisibleForTesting final OrientationEventListener mListener;
    private final Callback mCallback;
    private int mDeviceOrientation = -1;
    private int mDisplayOffset = -1;

    /**
     * Creates a new orientation helper.
     * @param context a valid context
     * @param callback a {@link Callback}
     */
    public OrientationHelper(@NonNull Context context, @NonNull Callback callback) {
        mCallback = callback;
        mListener = new OrientationEventListener(context.getApplicationContext(),
                SensorManager.SENSOR_DELAY_NORMAL) {

            @SuppressWarnings("ConstantConditions")
            @Override
            public void onOrientationChanged(int orientation) {
                int or = 0;
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    or = mDeviceOrientation != -1 ? mDeviceOrientation : 0;
                } else if (orientation >= 315 || orientation < 45) {
                    or = 0;
                } else if (orientation >= 45 && orientation < 135) {
                    or = 90;
                } else if (orientation >= 135 && orientation < 225) {
                    or = 180;
                } else if (orientation >= 225 && orientation < 315) {
                    or = 270;
                }

                if (or != mDeviceOrientation) {
                    mDeviceOrientation = or;
                    mCallback.onDeviceOrientationChanged(mDeviceOrientation);
                }
            }
        };
    }

    /**
     * Enables this listener.
     * @param context a context
     */
    public void enable(@NonNull Context context) {
        Display display = ((WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        switch (display.getRotation()) {
            case Surface.ROTATION_0: mDisplayOffset = 0; break;
            case Surface.ROTATION_90: mDisplayOffset = 90; break;
            case Surface.ROTATION_180: mDisplayOffset = 180; break;
            case Surface.ROTATION_270: mDisplayOffset = 270; break;
            default: mDisplayOffset = 0; break;
        }
        mListener.enable();
    }

    /**
     * Disables this listener.
     */
    public void disable() {
        mListener.disable();
        mDisplayOffset = -1;
        mDeviceOrientation = -1;
    }

    /**
     * Returns the current device orientation.
     * @return device orientation
     */
    public int getDeviceOrientation() {
        return mDeviceOrientation;
    }

    /**
     * Returns the current display offset.
     * @return display offset
     */
    public int getDisplayOffset() {
        return mDisplayOffset;
    }
}
