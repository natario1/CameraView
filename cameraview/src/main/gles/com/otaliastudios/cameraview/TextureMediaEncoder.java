package com.otaliastudios.cameraview;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.opengl.Matrix;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class TextureMediaEncoder extends VideoMediaEncoder<TextureMediaEncoder.Config> {

    final static String FRAME_EVENT = "frame";

    static class Frame {
        float[] transform;
        long timestamp;
    }
    static class Config extends VideoMediaEncoder.Config {
        int textureId;
        float scaleX;
        float scaleY;
        EGLContext eglContext;

        Config(int width, int height, int bitRate, int frameRate, int rotation, String mimeType,
               int textureId, float scaleX, float scaleY, EGLContext eglContext) {
            super(width, height, bitRate, frameRate, rotation, mimeType);
            this.textureId = textureId;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.eglContext = eglContext;
        }
    }

    private EglCore mEglCore;
    private EglWindowSurface mWindow;
    private EglViewport mViewport;

    public TextureMediaEncoder(@NonNull Config config) {
        super(config);
    }

    @EncoderThread
    @Override
    void prepare(MediaEncoderEngine.Controller controller) {
        super.prepare(controller);
        mEglCore = new EglCore(mConfig.eglContext, EglCore.FLAG_RECORDABLE);
        mWindow = new EglWindowSurface(mEglCore, mSurface, true);
        mWindow.makeCurrent(); // drawing will happen on the InputWindowSurface, which
        // is backed by mVideoEncoder.getInputSurface()
        mViewport = new EglViewport();
    }

    @EncoderThread
    @Override
    void release() {
        super.release();
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

    @EncoderThread
    @Override
    void start() {
        super.start();
        // Nothing to do here. Waiting for the first frame.
    }

    @EncoderThread
    @Override
    void notify(String event, Object data) {
        if (event.equals(FRAME_EVENT)) {
            Frame frame = (Frame) data;

            // Seeing this after device is toggled off/on with power button.  The
            // first frame back has a zero timestamp.
            // MPEG4Writer thinks this is cause to abort() in native code, so it's very
            // important that we just ignore the frame.
            if (frame.timestamp == 0) return;
            if (mFrameNum < 0) return;
            mFrameNum++;

            int arg1 = (int) (frame.timestamp >> 32);
            int arg2 = (int) frame.timestamp;
            long timestamp = (((long) arg1) << 32) | (((long) arg2) & 0xffffffffL);
            float[] transform = frame.transform;

            // We must scale this matrix like GlCameraPreview does, because it might have some cropping.
            // Scaling takes place with respect to the (0, 0, 0) point, so we must apply a Translation to compensate.

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
            Matrix.rotateM(transform, 0, mConfig.rotation, 0, 0, 1);
            Matrix.translateM(transform, 0, -0.5F, -0.5F, 0);

            drain(false);
            mViewport.drawFrame(mConfig.textureId, transform);
            mWindow.setPresentationTime(timestamp);
            mWindow.swapBuffers();
        }
    }
}
