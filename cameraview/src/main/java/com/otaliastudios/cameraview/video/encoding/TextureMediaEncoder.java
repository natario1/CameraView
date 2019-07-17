package com.otaliastudios.cameraview.video.encoding;

import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.os.Build;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.internal.egl.EglCore;
import com.otaliastudios.cameraview.internal.egl.EglViewport;
import com.otaliastudios.cameraview.internal.egl.EglWindowSurface;
import com.otaliastudios.cameraview.internal.utils.Pool;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Default implementation for video encoding.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class TextureMediaEncoder extends VideoMediaEncoder<TextureConfig> {

    private static final String TAG = TextureMediaEncoder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    public final static String FRAME_EVENT = "frame";

    private int mTransformRotation;
    private EglCore mEglCore;
    private EglWindowSurface mWindow;
    private EglViewport mViewport;
    private Pool<Frame> mFramePool = new Pool<>(100, new Pool.Factory<Frame>() {
        @Override
        public Frame create() {
            return new Frame();
        }
    });

    public TextureMediaEncoder(@NonNull TextureConfig config) {
        super(config.copy());
    }

    /**
     * Should be acquired with {@link #acquireFrame()}, filled and then passed
     * to {@link MediaEncoderEngine#notify(String, Object)} with {@link #FRAME_EVENT}.
     */
    public static class Frame {
        private Frame() {}

        /**
         * Nanoseconds, in no meaningful time-base. Will be used for offsets only.
         * Typically this comes from {@link SurfaceTexture#getTimestamp()}.
         */
        public long timestamp;

        /**
         * Milliseconds in the {@link System#currentTimeMillis()} reference.
         * This is actually needed/read only for the first frame.
         */
        public long timestampMillis;

        /**
         * The transformation matrix for the base texture.
         */
        public float[] transform = new float[16];

        /**
         * The transformation matrix for the overlay texture, if any.
         */
        public float[] overlayTransform = new float[16];
    }

    /**
     * Returns a new frame to be filled. See {@link Frame} for details.
     * @return a new frame
     */
    @NonNull
    public Frame acquireFrame() {
        if (mFramePool.isEmpty()) {
            throw new RuntimeException("Need more frames than this! Please increase the pool size.");
        } else {
            //noinspection ConstantConditions
            return mFramePool.get();
        }
    }

    @EncoderThread
    @Override
    protected void onPrepare(@NonNull MediaEncoderEngine.Controller controller, long maxLengthMillis) {
        // We rotate the texture using transformRotation. Pass rotation=0 to super so that
        // no rotation metadata is written into the output file.
        mTransformRotation = mConfig.rotation;
        mConfig.rotation = 0;
        super.onPrepare(controller, maxLengthMillis);
        mEglCore = new EglCore(mConfig.eglContext, EglCore.FLAG_RECORDABLE);
        mWindow = new EglWindowSurface(mEglCore, mSurface, true);
        mWindow.makeCurrent(); // drawing will happen on the InputWindowSurface, which
        // is backed by mVideoEncoder.getInputSurface()
        mViewport = new EglViewport();
    }

    @EncoderThread
    @Override
    protected void onEvent(@NonNull String event, @Nullable Object data) {
        if (!event.equals(FRAME_EVENT)) return;
        Frame frame = (Frame) data;
        if (frame == null) {
            throw new IllegalArgumentException("Got null frame for FRAME_EVENT.");
        }
        if (frame.timestamp == 0) { // grafika
            mFramePool.recycle(frame);
            return;
        }
        if (mFrameNumber < 0) { // We were asked to stop.
            mFramePool.recycle(frame);
            return;
        }
        mFrameNumber++;
        if (mFrameNumber == 1) {
            notifyFirstFrameMillis(frame.timestampMillis);
        }

        // First, drain any previous data.
        LOG.i("onEvent", "frameNumber:", mFrameNumber, "timestamp:", frame.timestamp, "- draining.");
        drainOutput(false);

        // Then draw on the surface.
        LOG.i("onEvent", "frameNumber:", mFrameNumber, "timestamp:", frame.timestamp, "- drawing.");

        // 1. We must scale this matrix like GlCameraPreview does, because it might have some cropping.
        // Scaling takes place with respect to the (0, 0, 0) point, so we must apply a Translation to compensate.
        float[] transform = frame.transform;
        float[] overlayTransform = frame.overlayTransform;
        float scaleX = mConfig.scaleX;
        float scaleY = mConfig.scaleY;
        float scaleTranslX = (1F - scaleX) / 2F;
        float scaleTranslY = (1F - scaleY) / 2F;
        Matrix.translateM(transform, 0, scaleTranslX, scaleTranslY, 0);
        Matrix.scaleM(transform, 0, scaleX, scaleY, 1);

        // 2. We also must rotate this matrix. In GlCameraPreview it is not needed because it is a live
        // stream, but the output video, must be correctly rotated based on the device rotation at the moment.
        // Rotation also takes place with respect to the origin (the Z axis), so we must
        // translate to origin, rotate, then back to where we were.
        Matrix.translateM(transform, 0, 0.5F, 0.5F, 0);
        Matrix.rotateM(transform, 0, mTransformRotation, 0, 0, 1);
        Matrix.translateM(transform, 0, -0.5F, -0.5F, 0);

        // 3. Do the same for overlays with their own rotation.
        if (mConfig.hasOverlay()) {
            Matrix.translateM(overlayTransform, 0, 0.5F, 0.5F, 0);
            Matrix.rotateM(overlayTransform, 0, mConfig.overlayRotation, 0, 0, 1);
            Matrix.translateM(overlayTransform, 0, -0.5F, -0.5F, 0);
        }
        mViewport.drawFrame(mConfig.textureId, transform);
        if (mConfig.hasOverlay()) {
            mViewport.drawFrame(mConfig.overlayTextureId, overlayTransform);
        }
        mWindow.setPresentationTime(frame.timestamp);
        mWindow.swapBuffers();
        mFramePool.recycle(frame);
    }

    @Override
    protected void onStopped() {
        super.onStopped();
        mFramePool.clear();
        if (mWindow != null) {
            mWindow.release();
            mWindow = null;
        }
        if (mViewport != null) {
            mViewport.release(true);
            mViewport = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }
}
