package com.otaliastudios.cameraview.picture;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.engine.Camera2Engine;
import com.otaliastudios.cameraview.engine.action.Action;
import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.Actions;
import com.otaliastudios.cameraview.engine.action.BaseAction;
import com.otaliastudios.cameraview.engine.action.CompletionCallback;
import com.otaliastudios.cameraview.engine.lock.LockAction;
import com.otaliastudios.cameraview.preview.RendererCameraPreview;
import com.otaliastudios.cameraview.size.AspectRatio;

/**
 * Wraps {@link SnapshotGlPictureRecorder} for Camera2.
 *
 * Camera2 engine supports metering for snapshots and we expect for them to correctly fire flash as well.
 * The first idea, and in theory, the most correct one, was to set
 * {@link CaptureRequest#CONTROL_CAPTURE_INTENT} to
 * {@link CaptureRequest#CONTROL_CAPTURE_INTENT_STILL_CAPTURE}.
 *
 * According to documentation, this will automatically trigger the flash if parameters says so.
 * In fact this is what happens, but it is a very fast flash that only lasts for 1 or 2 frames.
 * It's not easy to call super.take() at the exact time so that we capture the frame that was lit.
 * I have tried by comparing {@link SurfaceTexture#getTimestamp()} and
 * {@link CaptureResult#SENSOR_TIMESTAMP} to identify the correct frame. These timestamps match,
 * but the frame is not the correct one.
 *
 * So what we do here is ignore the {@link CaptureRequest#CONTROL_CAPTURE_INTENT} and instead
 * open the torch, if requested to do so. Then wait for exposure to settle again and finally
 * take a snapshot. I'd still love to use the capture intent instead of this, but was not able yet.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Snapshot2PictureRecorder extends SnapshotGlPictureRecorder {

    private final static long LOCK_TIMEOUT = 2500;

    private class FlashAction extends BaseAction {

        @Override
        protected void onStart(@NonNull ActionHolder holder) {
            super.onStart(holder);
            LOG.i("FlashAction:", "Parameters locked, opening torch.");
            holder.getBuilder(this).set(CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_TORCH);
            holder.getBuilder(this).set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);
            holder.applyBuilder(this);
        }

        @Override
        public void onCaptureCompleted(@NonNull ActionHolder holder,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(holder, request, result);
            Integer flashState = result.get(CaptureResult.FLASH_STATE);
            if (flashState == null) {
                LOG.w("FlashAction:", "Waiting flash, but flashState is null!",
                        "Taking snapshot.");
                setState(STATE_COMPLETED);
            } else if (flashState == CaptureResult.FLASH_STATE_FIRED) {
                LOG.i("FlashAction:", "Waiting flash and we have FIRED state!",
                        "Taking snapshot.");
                setState(STATE_COMPLETED);
            } else {
                LOG.i("FlashAction:", "Waiting flash but flashState is",
                        flashState, ". Waiting...");
            }
        }
    }

    private class ResetFlashAction extends BaseAction {

        @Override
        protected void onStart(@NonNull ActionHolder holder) {
            super.onStart(holder);
            try {
                // See Camera2Engine.setFlash() comments: turning TORCH off has bugs and we must do
                // as follows.
                LOG.i("ResetFlashAction:", "Reverting the flash changes.");
                CaptureRequest.Builder builder = holder.getBuilder(this);
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                builder.set(CaptureRequest.FLASH_MODE, CaptureResult.FLASH_MODE_OFF);
                holder.applyBuilder(this, builder);
                builder.set(CaptureRequest.CONTROL_AE_MODE, mOriginalAeMode);
                builder.set(CaptureRequest.FLASH_MODE, mOriginalFlashMode);
                holder.applyBuilder(this);
            } catch (CameraAccessException ignore) {}
        }
    }

    private final Action mAction;
    private final ActionHolder mHolder;
    private final boolean mActionNeeded;
    private Integer mOriginalAeMode;
    private Integer mOriginalFlashMode;

    public Snapshot2PictureRecorder(@NonNull PictureResult.Stub stub,
                                    @NonNull Camera2Engine engine,
                                    @NonNull RendererCameraPreview preview,
                                    @NonNull AspectRatio outputRatio) {
        super(stub, engine, preview, outputRatio, engine.getOverlay());
        mHolder = engine;

        mAction = Actions.sequence(
                Actions.timeout(LOCK_TIMEOUT, new LockAction()),
                new FlashAction());
        mAction.addCallback(new CompletionCallback() {
            @Override
            protected void onActionCompleted(@NonNull Action action) {
                LOG.i("Taking picture with super.take().");
                Snapshot2PictureRecorder.super.take();
            }
        });

        CaptureResult lastResult = mHolder.getLastResult(mAction);
        if (lastResult == null) {
            LOG.w("Picture snapshot requested very early, before the first preview frame.",
                    "Metering might not work as intended.");
        }
        Integer aeState = lastResult == null ? null
                : lastResult.get(CaptureResult.CONTROL_AE_STATE);
        mActionNeeded = engine.getPictureSnapshotMetering()
                && aeState != null
                && aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED;
        mOriginalAeMode = mHolder.getBuilder(mAction).get(CaptureRequest.CONTROL_AE_MODE);
        mOriginalFlashMode = mHolder.getBuilder(mAction).get(CaptureRequest.FLASH_MODE);
    }

    @Override
    public void take() {
        if (!mActionNeeded) {
            LOG.i("take:", "Engine does no metering or needs no flash.",
                    "Taking fast snapshot.");
            super.take();
        } else {
            LOG.i("take:", "Engine needs flash. Starting action");
            mAction.start(mHolder);
        }
    }

    @Override
    protected void dispatchResult() {
        // Revert our changes.
        new ResetFlashAction().start(mHolder);
        super.dispatchResult();
    }
}
