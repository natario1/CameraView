package com.otaliastudios.cameraview.internal;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Helps with keeping track of both device orientation (which changes when device is rotated)
 * and the display offset (which depends on the activity orientation wrt the device default
 * orientation).
 *
 * Note: any change in the display offset should restart the camera engine, because it reads
 * from the angles container at startup and computes size based on that. This is tricky because
 * activity behavior can differ:
 *
 * - if activity is locked to some orientation, {@link #mDisplayOffset} won't change, and
 *   {@link View#onConfigurationChanged(Configuration)} won't be called.
 *   The library will work fine.
 *
 * - if the activity is unlocked and does NOT handle orientation changes with android:configChanges,
 *   the actual behavior differs depending on the rotation.
 *   - the configuration callback is never called, of course.
 *   - for 90°/-90° rotations, the activity is recreated. Sometime you get {@link #mDisplayOffset}
 *     callback before destruction, sometimes you don't - in any case it's going to recreate.
 *   - for 180°/-180°, the activity is NOT recreated! But we can rely on {@link #mDisplayOffset}
 *     changing with a 180 delta and restart the engine.
 *
 * - lastly, if the activity is unlocked and DOES handle orientation changes with android:configChanges,
 *   as it will often be the case in a modern Compose app,
 *   - you always get the {@link #mDisplayOffset} callback
 *   - for 90°/-90° rotations, the view also gets the configuration changed callback.
 *   - for 180°/-180°, the view won't get it because configuration only cares about portrait vs. landscape.
 *
 * In practice, since we don't control the activity and we can't easily inspect the configChanges
 * flags at runtime, a good solution is to always restart when the display offset changes. We might
 * do useless restarts in one rare scenario (unlocked, no android:configChanges, 90° rotation,
 * display offset callback received before destruction) but that's acceptable.
 *
 * Tried to avoid that by looking at {@link Activity#isChangingConfigurations()}, but it's always
 * false by the time the display offset callback is invoked.
 */
public class OrientationHelper {

    /**
     * Receives callback about the orientation changes.
     */
    public interface Callback {
        void onDeviceOrientationChanged(int deviceOrientation);
        void onDisplayOffsetChanged();
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
                        mCallback.onDisplayOffsetChanged();
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
