package com.otaliastudios.cameraview.video;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.VideoResult;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Interface for video recording.
 * Don't call start if already started. Don't call stop if already stopped.
 */
public abstract class VideoRecorder {

    private final static String TAG = VideoRecorder.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

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

        /**
         * The callback for the actual video recording starting.
         */
        void onVideoRecordingStart();

        /**
         * Video recording has ended. We will finish processing the file
         * and soon {@link #onVideoResult(VideoResult.Stub, Exception)} will be called.
         */
        void onVideoRecordingEnd();
    }

    private final static int STATE_IDLE = 0;
    private final static int STATE_RECORDING = 1;
    private final static int STATE_STOPPING = 2;

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) VideoResult.Stub mResult;
    private final VideoResultListener mListener;
    @SuppressWarnings("WeakerAccess")
    protected Exception mError;
    private int mState;
    private final Object mStateLock = new Object();

    /**
     * Creates a new video recorder.
     * @param listener a listener
     */
    VideoRecorder(@Nullable VideoResultListener listener) {
        mListener = listener;
        mState = STATE_IDLE;
    }

    /**
     * Starts recording a video.
     *
     * @param stub the video stub
     */
    public final void start(@NonNull VideoResult.Stub stub) {
        synchronized (mStateLock) {
            if (mState != STATE_IDLE) {
                LOG.e("start:", "called twice, or while stopping! " +
                        "Ignoring. state:", mState);
                return;
            }
            LOG.i("start:", "Changed state to STATE_RECORDING");
            mState = STATE_RECORDING;
        }
        mResult = stub;
        onStart();
    }

    /**
     * Stops recording.
     * @param isCameraShutdown whether this is a full shutdown, camera is being closed
     */
    public final void stop(boolean isCameraShutdown) {
        synchronized (mStateLock) {
            if (mState == STATE_IDLE) {
                // Do not check for STOPPING! See onStop().
                LOG.e("stop:", "called twice, or called before start! " +
                        "Ignoring. isCameraShutdown:", isCameraShutdown);
                return;
            }
            LOG.i("stop:", "Changed state to STATE_STOPPING");
            mState = STATE_STOPPING;
        }
        onStop(isCameraShutdown);
    }

    /**
     * Returns true if it is currently recording.
     * @return true if recording
     */
    public boolean isRecording() {
        // true if not idle.
        synchronized (mStateLock) {
            return mState != STATE_IDLE;
        }
    }

    protected abstract void onStart();

    /**
     * Should stop recording as fast as possible. This can be called twice because the
     * shutdown boolean might be different.
     *
     * @param isCameraShutdown whether camera is shutting down
     */
    protected abstract void onStop(boolean isCameraShutdown);

    /**
     * Subclasses can call this to notify that the result was obtained,
     * either with some error (null result) or with the actual stub, filled.
     */
    protected final void dispatchResult() {
        synchronized (mStateLock) {
            if (!isRecording()) {
                LOG.w("dispatchResult:", "Called, but not recording! Aborting.");
                return;
            }
            LOG.i("dispatchResult:", "Changed state to STATE_IDLE.");
            mState = STATE_IDLE;
        }
        onDispatchResult();
        LOG.i("dispatchResult:", "About to dispatch result:", mResult, mError);
        if (mListener != null) {
            mListener.onVideoResult(mResult, mError);
        }
        mResult = null;
        mError = null;
    }

    /**
     * Subclasses can override this to release resources.
     */
    protected void onDispatchResult() {
        // No-op
    }

    /**
     * Subclasses can call this to notify that the video recording has started,
     * this will be called when camera is prepared and started.
     */
    @SuppressWarnings("WeakerAccess")
    @CallSuper
    protected void dispatchVideoRecordingStart() {
        LOG.i("dispatchVideoRecordingStart:", "About to dispatch.");
        if (mListener != null) {
            mListener.onVideoRecordingStart();
        }
    }

    /**
     * Subclasses can call this to notify that the video recording has ended,
     * although the video result might still be processed.
     */
    @SuppressWarnings("WeakerAccess")
    @CallSuper
    protected void dispatchVideoRecordingEnd() {
        LOG.i("dispatchVideoRecordingEnd:", "About to dispatch.");
        if (mListener != null) {
            mListener.onVideoRecordingEnd();
        }
    }
}
