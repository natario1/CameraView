package com.otaliastudios.cameraview.picture;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.engine.Camera2Engine;
import com.otaliastudios.cameraview.filter.Filter;
import com.otaliastudios.cameraview.preview.GlCameraPreview;
import com.otaliastudios.cameraview.preview.RendererFrameCallback;
import com.otaliastudios.cameraview.size.AspectRatio;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Snapshot2PictureRecorder extends SnapshotGlPictureRecorder implements RendererFrameCallback {

    private final static String TAG = Snapshot2PictureRecorder.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    private final static int STATE_IDLE = 0;
    private final static int STATE_WAITING_CAPTURE = 1;
    private final static int STATE_WAITING_CORRECT_FRAME = 2;
    private final static int STATE_WAITING_IMAGE = 3;

    private final Camera2Engine mEngine;
    private final GlCameraPreview mPreview;
    private final CameraCaptureSession mSession;
    private final CameraCaptureSession.CaptureCallback mCallback;
    private final CaptureRequest.Builder mBuilder;
    private int mState = STATE_IDLE;
    private Integer mOldCaptureIntent;
    private int mSequenceId;

    private SurfaceTexture mLastFrameSurfaceTexture;
    private float mLastFrameScaleX;
    private float mLastFrameScaleY;
    private EGLContext mLastFrameScaleEGLContext;
    private Long mDesiredTimestamp = null;

    public Snapshot2PictureRecorder(@NonNull PictureResult.Stub stub,
                                    @NonNull Camera2Engine engine,
                                    @NonNull GlCameraPreview preview,
                                    @NonNull AspectRatio outputRatio,
                                    @NonNull CameraCaptureSession session,
                                    @NonNull CameraCaptureSession.CaptureCallback callback,
                                    @NonNull CaptureRequest.Builder builder) {
        super(stub, engine, preview, outputRatio);
        mEngine = engine;
        mPreview = preview;
        mSession = session;
        mCallback = callback;
        mBuilder = builder;
    }

    @Override
    public void take() {
        if (!mEngine.getPictureSnapshotMetering()) {
            super.take();
            return;
        }

        LOG.i("take:", "Engine does metering, adding our CONTROL_CAPTURE_INTENT.");
        mPreview.addRendererFrameCallback(this);
        mState = STATE_WAITING_CAPTURE;
        try {
            mOldCaptureIntent = mBuilder.get(CaptureRequest.CONTROL_CAPTURE_INTENT);
            mBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
            mSequenceId = mSession.setRepeatingRequest(mBuilder.build(), mCallback, null);
        } catch (CameraAccessException e) {
            LOG.w("take:", "Camera error while applying our CONTROL_CAPTURE_INTENT", e);
            mResult = null;
            mError = e;
            dispatchResult();
        }
    }

    @Override
    public void onRendererTextureCreated(int textureId) {
        super.onRendererTextureCreated(textureId);
    }

    @Override
    public void onRendererFilterChanged(@NonNull Filter filter) {
        super.onRendererFilterChanged(filter);
    }

    @Override
    public void onRendererFrame(@NonNull SurfaceTexture surfaceTexture, float scaleX, float scaleY) {
        mLastFrameSurfaceTexture = surfaceTexture;
        mLastFrameScaleX = scaleX;
        mLastFrameScaleY = scaleY;
        mLastFrameScaleEGLContext = EGL14.eglGetCurrentContext();
        maybeTakeFrame();
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
                mState = STATE_WAITING_CORRECT_FRAME;
                mDesiredTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP);
                if (mDesiredTimestamp == null) mDesiredTimestamp = 0L;
                LOG.i("onCaptureCompleted:", "Got timestamp:", mDesiredTimestamp);
                maybeTakeFrame();
            }
        }
    }

    private void maybeTakeFrame() {
        if (mState != STATE_WAITING_CORRECT_FRAME) {
            LOG.w("maybeTakeFrame:", "we're not waiting for a frame. Ignoring.", mState);
            return;
        }
        if (mDesiredTimestamp == null || mLastFrameSurfaceTexture == null) {
            LOG.w("maybeTakeFrame:", "either timestamp or surfaceTexture are null.", mDesiredTimestamp);
            return;
        }

        long currentTimestamp = mLastFrameSurfaceTexture.getTimestamp();
        if (currentTimestamp == mDesiredTimestamp) {
            LOG.i("maybeTakeFrame:", "taking frame with exact timestamp:", currentTimestamp);
            mState = STATE_WAITING_IMAGE;
            takeFrame(mLastFrameSurfaceTexture, mLastFrameScaleX, mLastFrameScaleY, mLastFrameScaleEGLContext);
        } else if (currentTimestamp > mDesiredTimestamp) {
            LOG.w("maybeTakeFrame:", "taking frame with some delay. Flash might not be respected.");
            mState = STATE_WAITING_IMAGE;
            takeFrame(mLastFrameSurfaceTexture, mLastFrameScaleX, mLastFrameScaleY, mLastFrameScaleEGLContext);
        } else {
            LOG.i("maybeTakeFrame:", "Waiting...", mDesiredTimestamp - currentTimestamp);
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
        mPreview.removeRendererFrameCallback(this);
        mState = STATE_IDLE;
        super.dispatchResult();
    }
}
