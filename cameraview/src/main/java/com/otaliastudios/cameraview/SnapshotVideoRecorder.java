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
class SnapshotVideoRecorder extends VideoRecorder implements GlCameraPreview.RendererFrameCallback {

    private static final String TAG = SnapshotVideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);
    private static final boolean USE_OLD_ENCODER = false;

    private static final int STATE_RECORDING = 0;
    private static final int STATE_NOT_RECORDING = 1;

    private OldMediaEncoder mEncoder;
    private MediaEncoderEngine mEncoderEngine;
    private GlCameraPreview mPreview;

    private int mCurrentState = STATE_NOT_RECORDING;
    private int mDesiredState = STATE_NOT_RECORDING;
    private int mTextureId = 0;

    SnapshotVideoRecorder(VideoResult stub, VideoResultListener listener, GlCameraPreview preview) {
        super(stub, listener);
        if (USE_OLD_ENCODER) {
            mEncoder = new OldMediaEncoder();
        }
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

    @RendererThread
    @Override
    public void onRendererTextureCreated(int textureId) {
        mTextureId = textureId;
    }

    @RendererThread
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
            if (USE_OLD_ENCODER) {
                OldMediaEncoder.Config configuration = new OldMediaEncoder.Config(
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
                mEncoder.startRecording(configuration);
                mEncoder.setTextureId(mTextureId);
            } else {
                TextureMediaEncoder.Config config = new TextureMediaEncoder.Config(
                        width, height,
                        1000000,
                        30,
                        mResult.getRotation(),
                        type, mTextureId,
                        scaleX, scaleY,
                        EGL14.eglGetCurrentContext()
                );
                TextureMediaEncoder videoEncoder = new TextureMediaEncoder(config);
                mEncoderEngine = new MediaEncoderEngine(mResult.file, videoEncoder, null);
                mEncoderEngine.start();
            }
            mResult.rotation = 0; // We will rotate the result instead.
            mCurrentState = STATE_RECORDING;
        }

        if (mCurrentState == STATE_RECORDING) {
            if (USE_OLD_ENCODER) {
                mEncoder.frameAvailable(surfaceTexture);
            } else {
                TextureMediaEncoder.Frame frame = new TextureMediaEncoder.Frame();
                frame.timestamp = surfaceTexture.getTimestamp();
                frame.transform = new float[16];
                surfaceTexture.getTransformMatrix(frame.transform);
                mEncoderEngine.notify(TextureMediaEncoder.FRAME_EVENT, frame);
            }
        }

        if (mCurrentState == STATE_RECORDING && mDesiredState == STATE_NOT_RECORDING) {
            if (USE_OLD_ENCODER) {
                mEncoder.stopRecording(new Runnable() {
                    @Override
                    public void run() {
                        // We are in the encoder thread.
                        dispatchResult();
                    }
                });
                mEncoder = null;
            } else {
                mEncoderEngine.stop(new Runnable() {
                    @Override
                    public void run() {
                        // We are in the encoder thread.
                        dispatchResult();
                    }
                });
                mEncoderEngine = null;
            }
            mCurrentState = STATE_NOT_RECORDING;
            mPreview.removeRendererFrameCallback(SnapshotVideoRecorder.this);
            mPreview = null;
        }

    }
}
