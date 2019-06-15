package com.otaliastudios.cameraview;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * A {@link VideoRecorder} that uses {@link android.media.MediaCodec} APIs.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class SnapshotVideoRecorder extends VideoRecorder implements GlCameraPreview.RendererFrameCallback,
        MediaEncoderEngine.Listener {

    private static final String TAG = SnapshotVideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private static final int DEFAULT_VIDEO_FRAMERATE = 30;
    private static final int DEFAULT_VIDEO_BITRATE = 1000000;
    private static final int DEFAULT_AUDIO_BITRATE = 64000;

    private static final int STATE_RECORDING = 0;
    private static final int STATE_NOT_RECORDING = 1;

    private MediaEncoderEngine mEncoderEngine;
    private GlCameraPreview mPreview;

    private int mCurrentState = STATE_NOT_RECORDING;
    private int mDesiredState = STATE_NOT_RECORDING;
    private int mTextureId = 0;
    private int mOverlayTextureId = 0;
    private SurfaceTexture mOverlaySurfaceTexture;
    private Surface mOverlaySurface;
    private List<SurfaceDrawer> mSurfaceDrawerList;

    private boolean mWithOverlay;

    SnapshotVideoRecorder(@NonNull VideoResult stub, @Nullable VideoResultListener listener, @NonNull GlCameraPreview preview, @NonNull List<SurfaceDrawer> surfaceDrawerList) {
        super(stub, listener);
        mPreview = preview;
        mPreview.addRendererFrameCallback(this);
        // TODO should it always be true?
        mWithOverlay = true;
        mSurfaceDrawerList = surfaceDrawerList;
    }

    @Override
    void start() {
        mDesiredState = STATE_RECORDING;
    }

    @Override
    void stop() {
        mDesiredState = STATE_NOT_RECORDING;
    }

    @RendererThread
    @Override
    public void onRendererTextureCreated(int textureId) {
        mTextureId = textureId;
        if (mWithOverlay) {
            EglViewport temp = new EglViewport();
            mOverlayTextureId = temp.createTexture();

            mOverlaySurfaceTexture = new SurfaceTexture(mOverlayTextureId);
            mOverlaySurfaceTexture.setDefaultBufferSize(mResult.size.getWidth(), mResult.size.getHeight());
            mOverlaySurface = new Surface(mOverlaySurfaceTexture);
        }
    }

    @RendererThread
    @Override
    public void onRendererFrame(@NonNull SurfaceTexture surfaceTexture, float scaleX, float scaleY) {
        if (mCurrentState == STATE_NOT_RECORDING && mDesiredState == STATE_RECORDING) {
            // Set default options
            if (mResult.videoBitRate <= 0) mResult.videoBitRate = DEFAULT_VIDEO_BITRATE;
            if (mResult.videoFrameRate <= 0) mResult.videoFrameRate = DEFAULT_VIDEO_FRAMERATE;
            if (mResult.audioBitRate <= 0) mResult.audioBitRate = DEFAULT_AUDIO_BITRATE;

            // Video. Ensure width and height are divisible by 2, as I have read somewhere.
            Size size = mResult.getSize();
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
            LOG.w("Creating frame encoder. Rotation:", mResult.rotation);
            TextureMediaEncoder.Config config = new TextureMediaEncoder.Config(width, height,
                    mResult.videoBitRate,
                    mResult.videoFrameRate,
                    mResult.rotation,
                    type, mTextureId,
                    mWithOverlay ? mOverlayTextureId : 0,
                    scaleX, scaleY,
                    mPreview.mInputFlipped,
                    EGL14.eglGetCurrentContext()
            );
            TextureMediaEncoder videoEncoder = new TextureMediaEncoder(config);

            // Audio
            AudioMediaEncoder audioEncoder = null;
            if (mResult.audio == Audio.ON) {
                audioEncoder = new AudioMediaEncoder(new AudioMediaEncoder.Config(mResult.audioBitRate));
            }

            // Engine
            mEncoderEngine = new MediaEncoderEngine(mResult.file, videoEncoder, audioEncoder,
                    mResult.maxDuration, mResult.maxSize, SnapshotVideoRecorder.this);
            mEncoderEngine.start();
            mResult.rotation = 0; // We will rotate the result instead.
            mCurrentState = STATE_RECORDING;
        }

        if (mCurrentState == STATE_RECORDING) {
            TextureMediaEncoder textureEncoder = (TextureMediaEncoder) mEncoderEngine.getVideoEncoder();
            TextureMediaEncoder.TextureFrame textureFrame = textureEncoder.acquireFrame();
            textureFrame.timestamp = surfaceTexture.getTimestamp();
            surfaceTexture.getTransformMatrix(textureFrame.transform);

            // get overlay
            if (mWithOverlay) {
                for (SurfaceDrawer surfaceDrawer : mSurfaceDrawerList) {
                    surfaceDrawer.drawOnSurfaceForVideoSnapshot(mOverlaySurface);
                }
                mOverlaySurfaceTexture.updateTexImage();
                mOverlaySurfaceTexture.getTransformMatrix(textureFrame.overlayTransform);
            }

            /* TODO
             * Sometimes when the video ends an Exception is thrown saying that notify was called on
             * a null object. This might be highlighting a bigger problem, but checking for null
             * fixes the Exception.
             */
            if (mEncoderEngine != null) {
                mEncoderEngine.notify(TextureMediaEncoder.FRAME_EVENT, textureFrame);
            }
        }

        if (mCurrentState == STATE_RECORDING && mDesiredState == STATE_NOT_RECORDING) {
            mCurrentState = STATE_NOT_RECORDING; // before nulling encoderEngine!
            mEncoderEngine.stop();
            mEncoderEngine = null;
            mPreview.removeRendererFrameCallback(SnapshotVideoRecorder.this);
            mPreview = null;
            if (mOverlaySurfaceTexture != null) {
                mOverlaySurfaceTexture.release();
                mOverlaySurfaceTexture = null;
            }
            if (mOverlaySurface != null) {
                mOverlaySurface.release();
                mOverlaySurface = null;
            }
        }

    }

    @Override
    public void onEncoderStop(int stopReason, @Nullable Exception e) {
        // If something failed, undo the result, since this is the mechanism
        // to notify Camera1 about this.
        if (e != null) {
            mResult = null;
        } else {
            if (stopReason == MediaEncoderEngine.STOP_BY_MAX_DURATION) {
                mResult.endReason = VideoResult.REASON_MAX_DURATION_REACHED;
            } else if (stopReason == MediaEncoderEngine.STOP_BY_MAX_SIZE) {
                mResult.endReason = VideoResult.REASON_MAX_SIZE_REACHED;
            }
        }
        mEncoderEngine = null;
        if (mPreview != null) {
            mPreview.removeRendererFrameCallback(SnapshotVideoRecorder.this);
            mPreview = null;
        }
        dispatchResult();
    }
}
