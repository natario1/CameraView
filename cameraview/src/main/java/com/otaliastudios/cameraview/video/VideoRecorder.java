package com.otaliastudios.cameraview.video;

import com.otaliastudios.cameraview.VideoResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Interface for video recording.
 * Don't call start if already started. Don't call stop if already stopped.
 * Don't reuse.
 */
public abstract class VideoRecorder {

    /**
     * Listens for video recorder events.
     */
    public interface VideoResultListener {

        /**
         * The operation was completed, either with success or with an error.
         * @param result the result or null if error
         * @param exception the error or null if everything went fine
         */
        void onVideoResult(@Nullable VideoResult.Stub result, @Nullable Exception exception);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) VideoResult.Stub mResult;
    @VisibleForTesting VideoResultListener mListener;
    protected Exception mError;

    /**
     * Creates a new video recorder.
     * @param stub a video stub
     * @param listener a listener
     */
    VideoRecorder(@NonNull VideoResult.Stub stub, @Nullable VideoResultListener listener) {
        mResult = stub;
        mListener = listener;
    }

    /**
     * Starts recording a video.
     */
    public abstract void start();

    /**
     * Stops recording.
     */
    public abstract void stop();

    /**
     * Subclasses can call this to notify that the result was obtained,
     * either with some error (null result) or with the actual stub, filled.
     */
    @SuppressWarnings("WeakerAccess")
    protected void dispatchResult() {
        if (mListener != null) {
            mListener.onVideoResult(mResult, mError);
            mListener = null;
            mResult = null;
            mError = null;
        }
    }
}
