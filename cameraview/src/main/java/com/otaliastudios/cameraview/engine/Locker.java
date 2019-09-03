package com.otaliastudios.cameraview.engine;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.engine.locking.AutoExposure;
import com.otaliastudios.cameraview.engine.locking.AutoFocus;
import com.otaliastudios.cameraview.engine.locking.AutoWhiteBalance;
import com.otaliastudios.cameraview.engine.locking.Parameter;

/**
 * Helps Camera2-based engines to perform 3A locking and unlocking.
 * Users are required to:
 *
 * - Call {@link #lock(CaptureResult)} to start
 * - Call {@link #onCapture(CaptureResult)} when they have partial or total results, as long as the
 *   locker is still in a locking operation, which can be checked through {@link #isLocking()}
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Locker {

    /**
     * The locker callback.
     */
    public interface Callback extends Parameter.LockingChangeCallback {

        /**
         * Notifies that locking has ended. No action is required for implementors.
         * From now on, {@link #isLocking()} will return false.
         * @param success success
         */
        void onLocked(boolean success);

        /**
         * Returns the currently used builder. This can change while a locking
         * operation happens, so the locker will never cache this value.
         * It is the engine responsibility to copy over values to the new builder
         * when it changes.
         * @return a builder
         */
        @NonNull
        CaptureRequest.Builder getLockingBuilder();
    }

    private static final String TAG = Locker.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);
    private static final int FORCED_END_DELAY = 2500;

    private final CameraCharacteristics mCharacteristics;
    private final Callback mCallback;

    private boolean mIsLocking;
    private Parameter mAutoFocus;
    private Parameter mAutoWhiteBalance;
    private Parameter mAutoExposure;
    private long mLockingStartTime;

    /**
     * Creates a new locker.
     * @param characteristics the camera characteristics
     * @param callback the callback
     */
    public Locker(@NonNull CameraCharacteristics characteristics,
                  @NonNull Callback callback) {
        mCharacteristics = characteristics;
        mCallback = callback;
        mAutoFocus = new AutoFocus(callback);
        mAutoExposure = new AutoExposure(callback);
        mAutoWhiteBalance = new AutoWhiteBalance(callback);
    }

    /**
     * Locks 3A values.
     * @param lastResult the last result
     */
    public void lock(@NonNull CaptureResult lastResult) {
        mIsLocking = true;
        mLockingStartTime = System.currentTimeMillis();
        // TODO mAutoFocus.lock(mCharacteristics, mCallback.getLockingBuilder(), lastResult);
        // TODO mAutoWhiteBalance.lock(mCharacteristics, mCallback.getLockingBuilder(), lastResult);
        mAutoExposure.lock(mCharacteristics, mCallback.getLockingBuilder(), lastResult);
    }

    /**
     * True if we're locking. False if we're not, for example
     * if {@link #lock(CaptureResult)} was never called.
     * @return true if locking
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isLocking() {
        return mIsLocking;
    }

    /**
     * Should be called when we have partial or total CaptureResults,
     * but only while {@link #isLocking()} returns true.
     * @param result result
     */
    public void onCapture(@NonNull CaptureResult result) {
        if (!mIsLocking) return; // We're not interested in results anymore
        if (!(result instanceof TotalCaptureResult)) return; // Let's ignore these, contents are missing/wrong
        
        // TODO if (!mAutoFocus.isLocked()) mAutoFocus.onCapture(mCallback.getLockingBuilder(), result);
        if (!mAutoExposure.isLocked()) mAutoExposure.onCapture(mCallback.getLockingBuilder(), result);
        // TODO if (!mAutoWhiteBalance.isLocked()) mAutoWhiteBalance.onCapture(mCallback.getLockingBuilder(), result);
        if (/* TODO mAutoFocus.isLocked() && */ mAutoExposure.isLocked() /* && mAutoWhiteBalance.isLocked() */) {
            LOG.i("onCapture:", "all Parameters have converged. Dispatching onMeteringEnd");
            boolean success = /* TODO mAutoFocus.isSuccessful()
                    && */ mAutoExposure.isSuccessful()
                    /* TODO && mAutoWhiteBalance.isSuccessful() */;
            mCallback.onLocked(success);
            mIsLocking = false;
        } else if (System.currentTimeMillis() - mLockingStartTime >= FORCED_END_DELAY) {
            LOG.e("onCapture:", "FORCED_END_DELAY was reached. Some Parameter is stuck. Forcing end.");
            mCallback.onLocked(false);
            mIsLocking = false;
        }
    }
}
