package com.otaliastudios.cameraview.internal;

import android.content.Context;
import android.hardware.SensorManager;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Helps with keeping track of both device orientation (which changes when device is rotated)
 * and the display offset (which depends on the activity orientation wrt the device default
 * orientation).
 */
public class OrientationHelper {

    /**
     * Receives callback about the orientation changes.
     */
    public interface Callback {
        void onDeviceOrientationChanged(int deviceOrientation);
        void onDisplayOffsetChanged(int displayOffset, boolean willRecreate);
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Context mContext;
    private final Callback mCallback;

    @VisibleForTesting
    final OrientationEventListener mDeviceOrientationListener;
    private int mDeviceOrientation = -1;

    @VisibleForTesting
    final DisplayManager.DisplayListener mDisplayOffsetListener;
    private int mDisplayOffset = -1;
    
    private boolean mEnabled;

    /**
     * Creates a new orientation helper.
     * @param context a valid context
     * @param callback a {@link Callback}
     */
    public OrientationHelper(@NonNull Context context, @NonNull Callback callback) {
        mContext = context;
        mCallback = callback;
        mDeviceOrientationListener = new OrientationEventListener(context.getApplicationContext(),
                SensorManager.SENSOR_DELAY_NORMAL) {

            @SuppressWarnings("ConstantConditions")
            @Override
            public void onOrientationChanged(int orientation) {
                int deviceOrientation = 0;
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    deviceOrientation = mDeviceOrientation != -1 ? mDeviceOrientation : 0;
                } else if (orientation >= 315 || orientation < 45) {
                    deviceOrientation = 0;
                } else if (orientation >= 45 && orientation < 135) {
                    deviceOrientation = 90;
                } else if (orientation >= 135 && orientation < 225) {
                    deviceOrientation = 180;
                } else if (orientation >= 225 && orientation < 315) {
                    deviceOrientation = 270;
                }

                if (deviceOrientation != mDeviceOrientation) {
                    mDeviceOrientation = deviceOrientation;
                    mCallback.onDeviceOrientationChanged(mDeviceOrientation);
                }
            }
        };
        if (Build.VERSION.SDK_INT >= 17) {
            mDisplayOffsetListener = new DisplayManager.DisplayListener() {
                public void onDisplayAdded(int displayId) { }
                public void onDisplayRemoved(int displayId) { }

                @Override
                public void onDisplayChanged(int displayId) {
                    int oldDisplayOffset = mDisplayOffset;
                    int newDisplayOffset = findDisplayOffset();
                    if (newDisplayOffset != oldDisplayOffset) {
                        mDisplayOffset = newDisplayOffset;
                        // With 180 degrees flips, the activity is not recreated.
                        boolean willRecreate = Math.abs(newDisplayOffset - oldDisplayOffset) != 180;
                        mCallback.onDisplayOffsetChanged(newDisplayOffset, willRecreate);
                    }
                }
            };
        } else {
            mDisplayOffsetListener = null;
        }
    }

    /**
     * Enables this listener.
     */
    public void enable() {
        if (mEnabled) return;
        mEnabled = true;
        mDisplayOffset = findDisplayOffset();
        if (Build.VERSION.SDK_INT >= 17) {
            DisplayManager manager = (DisplayManager)
                    mContext.getSystemService(Context.DISPLAY_SERVICE);
            // Without the handler, this can crash if called from a thread without a looper
            manager.registerDisplayListener(mDisplayOffsetListener, mHandler);
        }
        mDeviceOrientationListener.enable();
    }

    /**
     * Disables this listener.
     */
    public void disable() {
        if (!mEnabled) return;
        mEnabled = false;
        mDeviceOrientationListener.disable();
        if (Build.VERSION.SDK_INT >= 17) {
            DisplayManager manager = (DisplayManager)
                    mContext.getSystemService(Context.DISPLAY_SERVICE);
            manager.unregisterDisplayListener(mDisplayOffsetListener);
        }
        mDisplayOffset = -1;
        mDeviceOrientation = -1;
    }

    /**
     * Returns the current device orientation.
     * @return device orientation
     */
    @SuppressWarnings("WeakerAccess")
    public int getLastDeviceOrientation() {
        return mDeviceOrientation;
    }

    /**
     * Returns the current display offset.
     * @return display offset
     */
    public int getLastDisplayOffset() {
        return mDisplayOffset;
    }

    private int findDisplayOffset() {
        Display display = ((WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        switch (display.getRotation()) {
            case Surface.ROTATION_0: return 0;
            case Surface.ROTATION_90: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
            default: return 0;
        }
    }
}
