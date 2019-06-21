package com.otaliastudios.cameraview.video;

import com.otaliastudios.cameraview.VideoResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface for video recording.
 * Don't call start if already started. Don't call stop if already stopped.
 * Don't reuse.
 */
public abstract class VideoRecorder {

    /* tests */ VideoResult.Stub mResult;
    /* tests */ VideoResultListener mListener;
    protected Exception mError;

    VideoRecorder(@NonNull VideoResult.Stub stub, @Nullable VideoResultListener listener) {
        mResult = stub;
        mListener = listener;
    }

    public abstract void start();

    public abstract void stop();

    @SuppressWarnings("WeakerAccess")
    protected void dispatchResult() {
        if (mListener != null) {
            mListener.onVideoResult(mResult, mError);
            mListener = null;
            mResult = null;
            mError = null;
        }
    }

    public interface VideoResultListener {
        void onVideoResult(@Nullable VideoResult.Stub result, @Nullable Exception exception);
    }
}
