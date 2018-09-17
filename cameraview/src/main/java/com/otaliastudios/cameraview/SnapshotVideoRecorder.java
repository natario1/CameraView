package com.otaliastudios.cameraview;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;

/**
 * A {@link VideoRecorder} that uses {@link android.media.MediaCodec} APIs.
 *
 * TODO rotation support. Currently we pass the wrong size
 * TODO audio support
 * TODO when cropping is huge, the first frame of the video result, noticeably, has no transformation applied. Don't know why.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class SnapshotVideoRecorder extends VideoRecorder implements GLCameraPreview.RendererFrameCallback {

    private static final String TAG = SnapshotVideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private static final int STATE_RECORDING = 0;
    private static final int STATE_NOT_RECORDING = 1;

    private VideoTextureEncoder mEncoder;
    private GLCameraPreview mPreview;

    private int mCurrentState = STATE_NOT_RECORDING;
    private int mDesiredState = STATE_NOT_RECORDING;
    private int mTextureId = 0;

    SnapshotVideoRecorder(VideoResult stub, VideoResultListener listener, GLCameraPreview preview) {
        super(stub, listener);
        mEncoder = new VideoTextureEncoder();
        mPreview = preview;
        mPreview.addRendererFrameCallback(this);
    }

    @Override
    void start() {
        mDesiredState = STATE_RECORDING;
        // TODO respect maxSize by doing inspecting frameRate, bitRate and frame size?
        // TODO do this at the encoder level, not here with a handler.
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
    public void onRendererFrame(SurfaceTexture surfaceTexture, float scaleX, float scaleY) {
        if (mCurrentState == STATE_NOT_RECORDING && mDesiredState == STATE_RECORDING) {
            Size size = mResult.getSize();
            // Ensure width and height are divisible by 2, as I have read somewhere.
            int width = size.getWidth();
            int height = size.getHeight();
            width = width % 2 == 0 ? width : width + 1;
            height = height % 2 == 0 ? height : height + 1;
            String type = "";
            switch (mResult.codec) {
                case H_263: type = "video/3gpp"; break; // MediaFormat.MIMETYPE_VIDEO_H263;
                case H_264: type = "video/avc"; break; // MediaFormat.MIMETYPE_VIDEO_AVC:
                case DEVICE_DEFAULT: type = "video/avc"; break;
            }
            VideoTextureEncoder.Config configuration = new VideoTextureEncoder.Config(
                    mResult.getFile(),
                    width,
                    height,
                    1000000,
                    30,
                    mResult.getRotation(),
                    scaleX,
                    scaleY,
                    type,
                    EGL14.eglGetCurrentContext()
            );
            mResult.rotation = 0; // We will rotate the result instead.
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
            mPreview.removeRendererFrameCallback(SnapshotVideoRecorder.this);
            mPreview = null;
        }

    }
}
