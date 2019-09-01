package com.otaliastudios.cameraview.picture;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.engine.Camera2Engine;
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
public class Snapshot2PictureRecorder extends SnapshotGlPictureRecorder {

    private final static String TAG = Snapshot2PictureRecorder.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    private final static int STATE_IDLE = 0;
    private final static int STATE_WAITING_FIRST_FRAME = 1;
    private final static int STATE_WAITING_LOCK = 2;
    private final static int STATE_WAITING_TORCH = 3;
    private final static int STATE_WAITING_IMAGE = 4;

    private final Camera2Engine mEngine;
    private final CameraCaptureSession mSession;
    private final CameraCaptureSession.CaptureCallback mCallback;
    private final CaptureRequest.Builder mBuilder;
    private int mState = STATE_IDLE;
    private Integer mOriginalAeMode;
    private Integer mOriginalFlashMode;
    private Integer mOriginalAfMode;
    private boolean mNeedsFlash;

    public Snapshot2PictureRecorder(@NonNull PictureResult.Stub stub,
                                       @NonNull Camera2Engine engine,
                                       @NonNull GlCameraPreview preview,
                                       @NonNull AspectRatio outputRatio,
                                       @NonNull CameraCaptureSession session,
                                       @NonNull CameraCaptureSession.CaptureCallback callback,
                                       @NonNull CaptureRequest.Builder builder) {
        super(stub, engine, preview, outputRatio);
        mEngine = engine;
        mSession = session;
        mCallback = callback;
        mBuilder = builder;
    }

    @Override
    public void take() {
        if (!mEngine.getPictureSnapshotMetering()) {
            LOG.i("take:", "Engine does no metering, taking fast snapshot.");
            super.take();
        } else {
            LOG.i("take:", "Engine does metering, caring about flash.");
            mState = STATE_WAITING_FIRST_FRAME;
            mOriginalAeMode = mBuilder.get(CaptureRequest.CONTROL_AE_MODE);
            mOriginalFlashMode = mBuilder.get(CaptureRequest.FLASH_MODE);
            mOriginalAfMode = mBuilder.get(CaptureRequest.CONTROL_AF_MODE);
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

    /**
     * This works by inspecting {@link CaptureResult#CONTROL_AE_STATE}, and specifically
     * the {@link CaptureRequest#CONTROL_AE_STATE_FLASH_REQUIRED} value.
     * The AE state will have this value iff:
     * 1. flash is required
     * 2. device has a flash
     * 3. we are not in Flash.OFF
     * So when we see this value we just have to open the torch and wait a bit more,
     * just to have a correct exposure.
     *
     * @param result a total result
     */
    public void onCaptureCompleted(@NonNull TotalCaptureResult result) {
        if (mState == STATE_WAITING_FIRST_FRAME) {
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            mNeedsFlash = aeState != null && aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED;
            LOG.i("onCaptureCompleted:", "aeState:", aeState, "needsFlash:", mNeedsFlash);
            mState = STATE_WAITING_LOCK;
            // Removing any ongoing precapture or trigger. This should not be needed.
            // if (Build.VERSION.SDK_INT >= 23) {
            //    mBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
            // }
            // mBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            // Lock AE, AWB, AF to their current values
            mBuilder.set(CaptureRequest.CONTROL_AE_LOCK, true);
            mBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, true);
            mBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            applyBuilder();
            return;
        }

        if (mState == STATE_WAITING_LOCK) {
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            Integer awbState = result.get(CaptureResult.CONTROL_AWB_STATE);
            boolean aeLocked = aeState != null && aeState == CaptureResult.CONTROL_AE_STATE_LOCKED;
            boolean awbLocked = awbState != null && awbState == CaptureResult.CONTROL_AWB_STATE_LOCKED;
            if (aeLocked && awbLocked) {
                if (mNeedsFlash) {
                    LOG.i("onCaptureCompleted:", "AE/AWB locked!", "Flash needed, opening torch.");
                    mState = STATE_WAITING_TORCH;
                    mBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                    mBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    applyBuilder();
                } else {
                    LOG.i("onCaptureCompleted:", "AE/AWB locked!", "No flash needed, taking picture.");
                    mState = STATE_WAITING_IMAGE;
                    super.take();
                }
            } else {
                LOG.i("onCaptureCompleted:", "Waiting for AE/AWB locks...",
                        "aeState:", aeState, "aeLocked:", aeLocked,
                        "awbState:", awbState, "awbLocked:", awbLocked);
            }
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
                LOG.i("onCaptureCompleted:", "aeState is FLASH_REQUIRED but flashState is",
                        flashState, ". Waiting...");
            }
            //noinspection UnnecessaryReturnStatement
            return;
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
                mSession.capture(mBuilder.build(), mCallback, null);
                // Revert locks.
                mBuilder.set(CaptureRequest.CONTROL_AF_MODE, mOriginalAfMode);
                mBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false);
                mBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, false);
                mSession.setRepeatingRequest(mBuilder.build(), mCallback, null);
            } catch (CameraAccessException ignore) {}
        }
        mState = STATE_IDLE;
        super.dispatchResult();
    }
}
