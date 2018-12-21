package com.otaliastudios.cameraview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface for picture capturing.
 * Don't call start if already started. Don't call stop if already stopped.
 * Don't reuse.
 */
abstract class PictureRecorder {

    /* tests */ PictureResult mResult;
    /* tests */ PictureResultListener mListener;

    PictureRecorder(@NonNull PictureResult stub, @Nullable PictureResultListener listener) {
        mResult = stub;
        mListener = listener;
    }

    abstract void take();

    @SuppressWarnings("WeakerAccess")
    protected void dispatchOnShutter(boolean didPlaySound) {
        if (mListener != null) mListener.onPictureShutter(didPlaySound);
    }

    protected void dispatchResult() {
        if (mListener != null) {
            mListener.onPictureResult(mResult);
            mListener = null;
            mResult = null;
        }
    }

    interface PictureResultListener {
        void onPictureShutter(boolean didPlaySound);
        void onPictureResult(@Nullable PictureResult result);
    }
}
