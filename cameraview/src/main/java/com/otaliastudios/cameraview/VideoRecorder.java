package com.otaliastudios.cameraview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface for video recording.
 * Don't call start if already started. Don't call stop if already stopped.
 * Don't reuse.
 */
abstract class VideoRecorder {

    /* tests */ VideoResult mResult;
    /* tests */ VideoResultListener mListener;

    VideoRecorder(@NonNull VideoResult stub, @Nullable VideoResultListener listener) {
        mResult = stub;
        mListener = listener;
    }

    abstract void start();

    abstract void stop();

    @SuppressWarnings("WeakerAccess")
    protected void dispatchResult() {
        if (mListener != null) {
            mListener.onVideoResult(mResult);
            mListener = null;
            mResult = null;
        }
    }


    interface VideoResultListener {
        void onVideoResult(@Nullable VideoResult result);
    }
}
