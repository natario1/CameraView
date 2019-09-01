package com.otaliastudios.cameraview.engine;

import android.graphics.PointF;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
 * - Call {@link #lock(CaptureResult, PointF)} to start
 * - Call {@link #onCapture(CaptureResult)} when they have partial or total results, as long as the
 *   locker is still in a locking operation, which can be checked through {@link #isLocking()} ()}
 * - Call {@link #unlock()} to reset the locked parameters if needed.
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
         * @param point a point
         * @param success success
         */
        void onLocked(@Nullable PointF point, boolean success);

        /**
         * Notifies that locking has been undone. From now on, this locker instance
         * is done, although in theory it could be reused by calling
         * {@link #lock(CaptureResult, PointF)} again.
         * @param point point
         */
        void onUnlocked(@Nullable PointF point);

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

    private PointF mPoint;
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
    @SuppressWarnings("WeakerAccess")
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
     * @param point a point
     */
    @SuppressWarnings("WeakerAccess")
    public void lock(@NonNull CaptureResult lastResult, @Nullable PointF point) {
        mIsLocking = true;
        mPoint = point;
        mLockingStartTime = System.currentTimeMillis();
        mAutoFocus.lock(mCharacteristics, mCallback.getLockingBuilder(), lastResult);
        mAutoWhiteBalance.lock(mCharacteristics, mCallback.getLockingBuilder(), lastResult);
        mAutoExposure.lock(mCharacteristics, mCallback.getLockingBuilder(), lastResult);
    }

    /**
     * True if we're locking. False if we're not, for example
     * if {@link #lock(CaptureResult, PointF)} was never called.
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
    @SuppressWarnings("WeakerAccess")
    public void onCapture(@NonNull CaptureResult result) {
        if (!mIsLocking) return; // We're not interested in results anymore
        if (!(result instanceof TotalCaptureResult)) return; // Let's ignore these, contents are missing/wrong
        
        if (!mAutoFocus.isLocked()) mAutoFocus.onCapture(mCallback.getLockingBuilder(), result);
        if (!mAutoExposure.isLocked()) mAutoExposure.onCapture(mCallback.getLockingBuilder(), result);
        if (!mAutoWhiteBalance.isLocked()) mAutoWhiteBalance.onCapture(mCallback.getLockingBuilder(), result);
        if (mAutoFocus.isLocked() && mAutoExposure.isLocked() && mAutoWhiteBalance.isLocked()) {
            LOG.i("onCapture:", "all Parameters have converged. Dispatching onMeteringEnd");
            boolean success = mAutoFocus.isSuccessful()
                    && mAutoExposure.isSuccessful()
                    && mAutoWhiteBalance.isSuccessful();
            dispatchEnd(success);
        } else if (System.currentTimeMillis() - mLockingStartTime >= FORCED_END_DELAY) {
            LOG.i("onCapture:", "FORCED_END_DELAY was reached. Some Parameter is stuck. Forcing end.");
            dispatchEnd(false);
        }
    }
    
    private void dispatchEnd(boolean success) {
        mCallback.onLocked(mPoint, success);
        mIsLocking = false;
    }

    /**
     * Should be called to unlock.
     * Note that {@link Callback#onUnlocked(PointF)} will be called immediately,
     * we're not waiting for the results.
     */
    @SuppressWarnings("WeakerAccess")
    public void unlock() {
        LOG.i("Unlocking.");
        mAutoFocus.unlock(mCharacteristics, mCallback.getLockingBuilder());
        mAutoExposure.unlock(mCharacteristics, mCallback.getLockingBuilder());
        mAutoWhiteBalance.unlock(mCharacteristics, mCallback.getLockingBuilder());
        mCallback.onUnlocked(mPoint);
    }
}
