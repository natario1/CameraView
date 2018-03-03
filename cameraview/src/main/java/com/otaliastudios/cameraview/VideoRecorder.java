package com.otaliastudios.cameraview;

import android.support.annotation.Nullable;

import java.io.File;

/**
 * Interface for video recording.
 * Don't call start if already started. Don't call stop if already stopped.
 * Don't reuse.
 */
abstract class VideoRecorder {

    /* tests */ VideoResult mResult;
    /* tests */ VideoResultListener mListener;

    VideoRecorder(VideoResult stub, VideoResultListener listener) {
        mResult = stub;
        mListener = listener;
    }

    abstract void start();

    final void stop() {
        if (mListener != null) {
            close();
            mListener.onVideoResult(mResult);
            mListener = null;
            mResult = null;
        }
    }

    abstract void close();

    interface VideoResultListener {
        void onVideoResult(@Nullable VideoResult result);
    }
}
