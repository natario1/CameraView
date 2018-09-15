package com.otaliastudios.cameraview;

import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.opengl.EGL14;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;

/**
 * A {@link VideoRecorder} that uses {@link android.media.MediaCodec} APIs.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class MediaCodecVideoRecorder extends VideoRecorder implements GLCameraPreview.RendererFrameCallback {

    private static final String TAG = MediaCodecVideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private static final int STATE_RECORDING = 0;
    private static final int STATE_NOT_RECORDING = 1;

    private CamcorderProfile mProfile;
    private VideoTextureEncoder mEncoder;
    private GLCameraPreview mPreview;

    private int mCurrentState = STATE_NOT_RECORDING;
    private int mDesiredState = STATE_NOT_RECORDING;
    private int mTextureId = 0;

    MediaCodecVideoRecorder(VideoResult stub, VideoResultListener listener, GLCameraPreview preview, int cameraId) {
        super(stub, listener);
        mProfile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        mEncoder = new VideoTextureEncoder();
        mPreview = preview;
        mPreview.setRendererFrameCallback(this);
    }

    @Override
    void start() {
        mDesiredState = STATE_RECORDING;
        if (mResult.maxDuration > 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDesiredState = STATE_NOT_RECORDING;
                }
            }, (long) mResult.maxDuration);
        }
    }

    @Override
    void stop() {
        mDesiredState = STATE_NOT_RECORDING;
    }

    @Override
    public void onRendererTextureCreated(int textureId) {
        mTextureId = textureId;
    }

    @Override
    public void onRendererFrame(SurfaceTexture surfaceTexture) {
        if (mCurrentState == STATE_NOT_RECORDING && mDesiredState == STATE_RECORDING) {
            VideoTextureEncoder.Config configuration = new VideoTextureEncoder.Config(
                    mResult.file,
                    mResult.size.getWidth(),
                    mResult.size.getHeight(),
                    1000000,
                    EGL14.eglGetCurrentContext()
            );
            mEncoder.startRecording(configuration);
            mEncoder.setTextureId(mTextureId);
            mCurrentState = STATE_RECORDING;
        }

        if (mCurrentState == STATE_RECORDING) {
            mEncoder.frameAvailable(surfaceTexture);
        }

        if (mCurrentState == STATE_RECORDING && mDesiredState == STATE_NOT_RECORDING) {
            mEncoder.stopRecording(new Runnable() {
                @Override
                public void run() {
                    // We are in the encoder thread.
                    dispatchResult();
                }
            });
            mCurrentState = STATE_NOT_RECORDING;

            mEncoder = null;
            mPreview.setRendererFrameCallback(null);
            mPreview = null;
        }

    }
}
