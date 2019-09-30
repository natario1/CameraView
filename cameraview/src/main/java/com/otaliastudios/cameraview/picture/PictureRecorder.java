package com.otaliastudios.cameraview.picture;

import com.otaliastudios.cameraview.PictureResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Interface for picture capturing.
 * Don't call start if already started. Don't call stop if already stopped.
 * Don't reuse.
 */
public abstract class PictureRecorder {

    /**
     * Listens for picture recorder events.
     */
    public interface PictureResultListener {

        /**
         * The shutter was activated.
         * @param didPlaySound whether a sound was played
         */
        void onPictureShutter(boolean didPlaySound);

        /**
         * Picture was taken or there was some error, if
         * the result is null.
         * @param result the result or null if there was some error
         * @param error the error or null if there wasn't any
         */
        void onPictureResult(@Nullable PictureResult.Stub result, @Nullable Exception error);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) PictureResult.Stub mResult;
    @VisibleForTesting PictureResultListener mListener;
    @SuppressWarnings("WeakerAccess")
    protected Exception mError;

    /**
     * Creates a new picture recorder.
     * @param stub a picture stub
     * @param listener a listener
     */
    @SuppressWarnings("WeakerAccess")
    public PictureRecorder(@NonNull PictureResult.Stub stub,
                           @Nullable PictureResultListener listener) {
        mResult = stub;
        mListener = listener;
    }

    /**
     * Takes a picture.
     */
    public abstract void take();

    /**
     * Subclasses can call this to notify that the shutter was activated,
     * and whether it did play some sound or not.
     * @param didPlaySound whether it played sounds
     */
    @SuppressWarnings("WeakerAccess")
    protected void dispatchOnShutter(boolean didPlaySound) {
        if (mListener != null) mListener.onPictureShutter(didPlaySound);
    }

    /**
     * Subclasses can call this to notify that the result was obtained,
     * either with some error (null result) or with the actual stub, filled.
     */
    protected void dispatchResult() {
        if (mListener != null) {
            mListener.onPictureResult(mResult, mError);
            mListener = null;
            mResult = null;
        }
    }
}
