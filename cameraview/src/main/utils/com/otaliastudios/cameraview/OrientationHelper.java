package com.otaliastudios.cameraview;

import android.content.Context;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

class OrientationHelper {

    final OrientationEventListener mListener;

    private final Callback mCallback;
    private int mDeviceOrientation = -1;
    private int mDisplayOffset = -1;

    interface Callback {
        void onDeviceOrientationChanged(int deviceOrientation);
    }

    OrientationHelper(Context context, @NonNull Callback callback) {
        mCallback = callback;
        mListener = new OrientationEventListener(context.getApplicationContext(), SensorManager.SENSOR_DELAY_NORMAL) {

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

    void enable(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        switch (display.getRotation()) {
            case Surface.ROTATION_0: mDisplayOffset = 0; break;
            case Surface.ROTATION_90: mDisplayOffset = 90; break;
            case Surface.ROTATION_180: mDisplayOffset = 180; break;
            case Surface.ROTATION_270: mDisplayOffset = 270; break;
            default: mDisplayOffset = 0; break;
        }
        mListener.enable();
    }

    void disable() {
        mListener.disable();
        mDisplayOffset = -1;
        mDeviceOrientation = -1;
    }

    int getDeviceOrientation() {
        return mDeviceOrientation;
    }

    int getDisplayOffset() {
        return mDisplayOffset;
    }
}
