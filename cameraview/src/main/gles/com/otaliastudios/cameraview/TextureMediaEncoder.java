package com.otaliastudios.cameraview;

import android.opengl.EGLContext;
import android.opengl.Matrix;
import android.os.Build;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class TextureMediaEncoder extends VideoMediaEncoder<TextureMediaEncoder.Config> {

    private static final String TAG = TextureMediaEncoder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    final static String FRAME_EVENT = "frame";

    static class Config extends VideoMediaEncoder.Config {
        int textureId;
        float scaleX;
        float scaleY;
        boolean scaleFlipped;
        EGLContext eglContext;
        int transformRotation;

        Config(int width, int height, int bitRate, int frameRate, int rotation, String mimeType,
               int textureId, float scaleX, float scaleY, boolean scaleFlipped, EGLContext eglContext) {
            // We rotate the texture using transformRotation. Pass rotation=0 to super so that
            // no rotation metadata is written into the output file.
            super(width, height, bitRate, frameRate, 0, mimeType);
            this.transformRotation = rotation;
            this.textureId = textureId;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleFlipped = scaleFlipped;
            this.eglContext = eglContext;
        }
    }

    private EglCore mEglCore;
    private EglWindowSurface mWindow;
    private EglViewport mViewport;
    private Pool<TextureFrame> mFramePool = new Pool<>(100, new Pool.Factory<TextureFrame>() {
        @Override
        public TextureFrame create() {
            return new TextureFrame();
        }
    });

    TextureMediaEncoder(@NonNull Config config) {
        super(config);
    }

    static class TextureFrame {
        private TextureFrame() {}
        // Nanoseconds, in no meaningful time-base. Should be for offsets only.
        // Typically coming from SurfaceTexture.getTimestamp().
        long timestamp;
        float[] transform = new float[16];
    }

    @NonNull
    TextureFrame acquireFrame() {
        if (!mFramePool.canGet()) {
            throw new RuntimeException("Need more frames than this! Please increase the pool size.");
        } else {
            //noinspection ConstantConditions
            return mFramePool.get();
        }
    }


    @EncoderThread
    @Override
    void onPrepare(@NonNull MediaEncoderEngine.Controller controller, long maxLengthMillis) {
        super.onPrepare(controller, maxLengthMillis);
        mEglCore = new EglCore(mConfig.eglContext, EglCore.FLAG_RECORDABLE);
        mWindow = new EglWindowSurface(mEglCore, mSurface, true);
        mWindow.makeCurrent(); // drawing will happen on the InputWindowSurface, which
        // is backed by mVideoEncoder.getInputSurface()
        mViewport = new EglViewport();
    }

    @EncoderThread
    @Override
    void onStart() {
        super.onStart();
        // Nothing to do here. Waiting for the first frame.
    }

    @EncoderThread
    @Override
    void onEvent(@NonNull String event, @Nullable Object data) {
        if (!event.equals(FRAME_EVENT)) return;
        TextureFrame frame = (TextureFrame) data;
        if (frame == null) return; // Should not happen
        if (frame.timestamp == 0 || mFrameNum < 0) {
            // The first condition comes from grafika.
            // The second condition means we were asked to stop.
            mFramePool.recycle(frame);
            return;
        }

        mFrameNum++;
        LOG.v("Incoming frame timestamp:", frame.timestamp);
        // We must scale this matrix like GlCameraPreview does, because it might have some cropping.
        // Scaling takes place with respect to the (0, 0, 0) point, so we must apply a Translation to compensate.
        float[] transform = frame.transform;
        float scaleX = mConfig.scaleX;
        float scaleY = mConfig.scaleY;
        float scaleTranslX = (1F - scaleX) / 2F;
        float scaleTranslY = (1F - scaleY) / 2F;
        Matrix.translateM(transform, 0, scaleTranslX, scaleTranslY, 0);
        Matrix.scaleM(transform, 0, scaleX, scaleY, 1);

        // We also must rotate this matrix. In GlCameraPreview it is not needed because it is a live
        // stream, but the output video, must be correctly rotated based on the device rotation at the moment.
        // Rotation also takes place with respect to the origin (the Z axis), so we must
        // translate to origin, rotate, then back to where we were.

        Matrix.translateM(transform, 0, 0.5F, 0.5F, 0);
        Matrix.rotateM(transform, 0, mConfig.transformRotation, 0, 0, 1);
        Matrix.translateM(transform, 0, -0.5F, -0.5F, 0);

        drainOutput(false);
        // Future note: passing scale values to the viewport? They are scaleX and scaleY,
        // but flipped based on the mConfig.scaleFlipped boolean.
        mViewport.drawFrame(mConfig.textureId, transform);
        mWindow.setPresentationTime(frame.timestamp);
        mWindow.swapBuffers();
        mFramePool.recycle(frame);
    }

    @Override
    void onRelease() {
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
