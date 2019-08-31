package com.otaliastudios.cameraview.picture;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.engine.Camera2Engine;
import com.otaliastudios.cameraview.engine.CameraEngine;
import com.otaliastudios.cameraview.overlay.Overlay;
import com.otaliastudios.cameraview.preview.GlCameraPreview;
import com.otaliastudios.cameraview.size.AspectRatio;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Snapshot2PictureRecorder extends SnapshotGlPictureRecorder {

    private final static String TAG = Snapshot2PictureRecorder.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    private final static int STATE_IDLE = 0;
    private final static int STATE_WAITING_CAPTURE = 1;
    private final static int STATE_WAITING_IMAGE = 2;

    private final Camera2Engine mEngine;
    private final CameraCaptureSession mSession;
    private final CameraCaptureSession.CaptureCallback mCallback;
    private final CaptureRequest.Builder mBuilder;
    private int mState = STATE_IDLE;
    private Integer mOldCaptureIntent;
    private int mSequenceId;

    public Snapshot2PictureRecorder(@NonNull PictureResult.Stub stub,
                                    @NonNull Camera2Engine engine,
                                    @NonNull GlCameraPreview preview,
                                    @NonNull AspectRatio outputRatio,
                                    @NonNull CameraCaptureSession session,
                                    @NonNull CameraCaptureSession.CaptureCallback callback,
                                    @NonNull CaptureRequest.Builder builder) {
        super(stub, engine, preview, outputRatio, engine.getOverlay());
        mEngine = engine;
        mSession = session;
        mCallback = callback;
        mBuilder = builder;
    }

    @Override
    public void take() {
        if (mEngine.getPictureSnapshotMetering()) {
            try {
                LOG.i("take:", "Engine does metering, adding our CONTROL_CAPTURE_INTENT.");
                mState = STATE_WAITING_CAPTURE;
                mOldCaptureIntent = mBuilder.get(CaptureRequest.CONTROL_CAPTURE_INTENT);
                mBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
                mSequenceId = mSession.setRepeatingRequest(mBuilder.build(), mCallback, null);
            } catch (CameraAccessException e) {
                LOG.w("take:", "Camera error while applying our CONTROL_CAPTURE_INTENT", e);
                mResult = null;
                mError = e;
                dispatchResult();
            }
        } else {
            super.take();
        }
    }

    public void onCaptureCompleted(@NonNull TotalCaptureResult result) {
        LOG.i("onCaptureCompleted:",
                "aeState:", result.get(CaptureResult.CONTROL_AE_STATE),
                "flashState:", result.get(CaptureResult.FLASH_STATE));
        if (mState == STATE_WAITING_CAPTURE) {
            if (result.getSequenceId() == mSequenceId) {
                // This is the request we launched and we're waiting for the right frame.
                LOG.w("onCaptureCompleted:",
                        "aeState:", result.get(CaptureResult.CONTROL_AE_STATE),
                        "flashState:", result.get(CaptureResult.FLASH_STATE));
                mState = STATE_WAITING_IMAGE;
                LOG.i("onCaptureCompleted:", "Got first result! Calling the GL recorder.");
                super.take();
            }
        }
    }

    @Override
    protected void dispatchResult() {
        if (mState == STATE_WAITING_IMAGE) {
            // Revert our changes.
            LOG.i("dispatchResult:", "Reverting the capture intent changes.");
            try {
                mBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, mOldCaptureIntent);
                mSession.setRepeatingRequest(mBuilder.build(), mCallback, null);
            } catch (CameraAccessException ignore) {}
        }
        mState = STATE_IDLE;
        super.dispatchResult();
    }
}
