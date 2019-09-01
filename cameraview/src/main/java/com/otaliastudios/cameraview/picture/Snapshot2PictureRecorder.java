package com.otaliastudios.cameraview.picture;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.engine.Camera2Engine;
import com.otaliastudios.cameraview.engine.Locker;
import com.otaliastudios.cameraview.preview.GlCameraPreview;
import com.otaliastudios.cameraview.size.AspectRatio;

/**
 * Wraps {@link SnapshotGlPictureRecorder} for Camera2.
 *
 * Camera2 engine supports metering for snapshots and we expect for them to correctly fire flash as well.
 * The first idea, and in theory, the most correct one, was to set {@link CaptureRequest#CONTROL_CAPTURE_INTENT}
 * to {@link CaptureRequest#CONTROL_CAPTURE_INTENT_STILL_CAPTURE}.
 *
 * According to documentation, this will automatically trigger the flash if parameters says so.
 * In fact this is what happens, but it is a very fast flash that only lasts for 1 or 2 frames.
 * It's not easy to call super.take() at the exact time so that we capture the frame that was lit.
 * I have tried by comparing {@link SurfaceTexture#getTimestamp()} and {@link CaptureResult#SENSOR_TIMESTAMP}
 * to identify the correct frame. These timestamps match, but the frame is not the correct one.
 *
 * So what we do here is ignore the {@link CaptureRequest#CONTROL_CAPTURE_INTENT} and instead open the
 * torch, if requested to do so. Then wait for exposure to settle again and finally take a snapshot.
 * I'd still love to use the capture intent instead of this, but was not able yet.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Snapshot2PictureRecorder extends SnapshotGlPictureRecorder implements Locker.Callback {

    private final static String TAG = Snapshot2PictureRecorder.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    private final static int STATE_IDLE = 0;
    private final static int STATE_WAITING_LOCKER = 1;
    private final static int STATE_WAITING_TORCH = 2;
    private final static int STATE_WAITING_IMAGE = 3;

    private final CameraCaptureSession mSession;
    private final CameraCaptureSession.CaptureCallback mCallback;
    private final CaptureRequest.Builder mBuilder;
    private final CameraCharacteristics mCharacteristics;
    private final CaptureResult mFirstResult;
    private final boolean mActionNeeded;

    private int mState = STATE_IDLE;
    private Integer mOriginalAeMode;
    private Integer mOriginalFlashMode;
    private Locker mLocker;

    public Snapshot2PictureRecorder(@NonNull PictureResult.Stub stub,
                                    @NonNull Camera2Engine engine,
                                    @NonNull GlCameraPreview preview,
                                    @NonNull AspectRatio outputRatio,
                                    @NonNull CameraCharacteristics characteristics,
                                    @NonNull CameraCaptureSession session,
                                    @NonNull CameraCaptureSession.CaptureCallback callback,
                                    @NonNull CaptureRequest.Builder builder,
                                    @NonNull CaptureResult lastResult) {
        super(stub, engine, preview, outputRatio);
        mSession = session;
        mCallback = callback;
        mBuilder = builder;
        mCharacteristics = characteristics;
        mFirstResult = lastResult;

        Integer aeState = lastResult.get(CaptureResult.CONTROL_AE_STATE);
        mActionNeeded = engine.getPictureSnapshotMetering()
                && aeState != null
                && aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED;
    }

    @Override
    public void take() {
        if (!mActionNeeded) {
            LOG.i("take:", "Engine does no metering or needs no flash, taking fast snapshot.");
            super.take();
        } else {
            LOG.i("take:", "Engine needs flash. Locking parameters");
            mState = STATE_WAITING_LOCKER;
            mOriginalAeMode = mBuilder.get(CaptureRequest.CONTROL_AE_MODE);
            mOriginalFlashMode = mBuilder.get(CaptureRequest.FLASH_MODE);
            mLocker = new Locker(mCharacteristics, this);
            mLocker.lock(mFirstResult);
        }
    }

    private void applyBuilder() {
        try {
            mSession.setRepeatingRequest(mBuilder.build(), mCallback, null);
        } catch (CameraAccessException e) {
            mResult = null;
            mError = e;
            dispatchResult();
        }
    }

    @NonNull
    @Override
    public CaptureRequest.Builder getLockingBuilder() {
        return mBuilder;
    }

    @Override
    public void onLockingChange() {
        applyBuilder();
    }

    @Override
    public void onLocked(boolean success) {
        LOG.i("onLocked:", "Parameters locked, opening torch.");
        mState = STATE_WAITING_TORCH;
        mBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
        mBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        applyBuilder();
    }

    public void onCaptureCompleted(@NonNull TotalCaptureResult result) {
        if (mState == STATE_WAITING_LOCKER) {
            mLocker.onCapture(result);
            return;
        }

        if (mState == STATE_WAITING_TORCH) {
            Integer flashState = result.get(CaptureResult.FLASH_STATE);
            if (flashState == null) {
                LOG.w("onCaptureCompleted:", "Waiting flash, but flashState is null! Taking snapshot.");
                mState = STATE_WAITING_IMAGE;
                super.take();
            } else if (flashState == CaptureResult.FLASH_STATE_FIRED) {
                LOG.i("onCaptureCompleted:", "Waiting flash and we have FIRED state! Taking snapshot.");
                mState = STATE_WAITING_IMAGE;
                super.take();
            } else {
                LOG.i("onCaptureCompleted:", "Waiting flash but flashState is",
                        flashState, ". Waiting...");
            }
        }
    }

    @Override
    protected void dispatchResult() {
        if (mState == STATE_WAITING_IMAGE) {
            // Revert our changes.
            LOG.i("dispatchResult:", "Reverting the capture intent changes.");
            try {
                // See Camera2Engine.setFlash(). It's better to change these one by one.
                mBuilder.set(CaptureRequest.CONTROL_AE_MODE, mOriginalAeMode);
                mSession.capture(mBuilder.build(), mCallback, null);
                mBuilder.set(CaptureRequest.FLASH_MODE, mOriginalFlashMode);
                mSession.setRepeatingRequest(mBuilder.build(), mCallback, null);
            } catch (CameraAccessException ignore) {}
        }
        mState = STATE_IDLE;
        super.dispatchResult();
    }
}
