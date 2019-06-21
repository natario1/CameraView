package com.otaliastudios.cameraview.picture;

import com.otaliastudios.cameraview.PictureResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface for picture capturing.
 * Don't call start if already started. Don't call stop if already stopped.
 * Don't reuse.
 */
public abstract class PictureRecorder {

    /* tests */ PictureResult.Stub mResult;
    /* tests */ PictureResultListener mListener;

    PictureRecorder(@NonNull PictureResult.Stub stub, @Nullable PictureResultListener listener) {
        mResult = stub;
        mListener = listener;
    }

    public abstract void take();

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

    public interface PictureResultListener {
        void onPictureShutter(boolean didPlaySound);
        void onPictureResult(@Nullable PictureResult.Stub result);
    }
}
