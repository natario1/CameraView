package com.otaliastudios.cameraview;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.Matrix;
import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;

/**
 * A {@link PictureResult} that uses standard APIs.
 */
class SnapshotPictureRecorder extends PictureRecorder {

    private static final String TAG = SnapshotPictureRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private Camera1 mController;
    private Camera mCamera;
    private CameraPreview mPreview;
    private AspectRatio mOutputRatio;
    private Size mSensorPreviewSize;
    private int mFormat;

    SnapshotPictureRecorder(@NonNull PictureResult stub, @NonNull Camera1 controller,
                            @NonNull Camera camera, @NonNull AspectRatio outputRatio) {
        super(stub, controller);
        mController = controller;
        mPreview = controller.mPreview;
        mCamera = camera;
        mOutputRatio = outputRatio;
        mFormat = mController.mPreviewFormat;
        mSensorPreviewSize = mController.mPreviewSize;
    }

    @Override
    void take() {
        if (mPreview instanceof GlCameraPreview) {
            takeGl((GlCameraPreview) mPreview);
        } else {
            takeLegacy();
        }
    }

    @SuppressLint("NewApi")
    private void takeGl(@NonNull final GlCameraPreview preview) {
        preview.addRendererFrameCallback(new GlCameraPreview.RendererFrameCallback() {

            int mTextureId;
            SurfaceTexture mSurfaceTexture;
            float[] mTransform;

            @RendererThread
            public void onRendererTextureCreated(int textureId) {
                mTextureId = textureId;
                mSurfaceTexture = new SurfaceTexture(mTextureId, true);
                // Need to crop the size.
                Rect crop = CropHelper.computeCrop(mResult.size, mOutputRatio);
                mResult.size = new Size(crop.width(), crop.height());
                mSurfaceTexture.setDefaultBufferSize(mResult.size.getWidth(), mResult.size.getHeight());
                mTransform = new float[16];
            }

            @RendererThread
            @Override
            public void onRendererFrame(SurfaceTexture surfaceTexture, final float scaleX, final float scaleY) {
                preview.removeRendererFrameCallback(this);

                // This kinda work but has drawbacks:
                // - output is upside down due to coordinates in GL: need to flip the byte[] someway
                // - output is not rotated as we would like to: need to create a bitmap copy...
                // - works only in the renderer thread, where it allocates the buffer and reads pixels. Bad!
                /*
                ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                buffer.rewind();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(buffer.array().length);
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
                bitmap.recycle(); */

                // For this reason it is better to create a new surface,
                // and draw the last frame again there.
                final EGLContext eglContext = EGL14.eglGetCurrentContext();
                final EglCore core = new EglCore(eglContext, EglCore.FLAG_RECORDABLE);
                // final EGLSurface oldSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
                // final EGLDisplay oldDisplay = EGL14.eglGetCurrentDisplay();
                WorkerHandler.run(new Runnable() {
                    @Override
                    public void run() {
                        EglWindowSurface surface = new EglWindowSurface(core, mSurfaceTexture);
                        surface.makeCurrent();
                        EglViewport viewport = new EglViewport();
                        mSurfaceTexture.updateTexImage();
                        mSurfaceTexture.getTransformMatrix(mTransform);

                        // Apply scale and crop:
                        // NOTE: scaleX and scaleY are in REF_VIEW, while our input appears to be in REF_SENSOR.
                        boolean flip = mController.flip(CameraController.REF_VIEW, CameraController.REF_SENSOR);
                        float realScaleX = flip ? scaleY : scaleX;
                        float realScaleY = flip ? scaleX : scaleY;
                        float scaleTranslX = (1F - realScaleX) / 2F;
                        float scaleTranslY = (1F - realScaleY) / 2F;
                        Matrix.translateM(mTransform, 0, scaleTranslX, scaleTranslY, 0);
                        Matrix.scaleM(mTransform, 0, realScaleX, realScaleY, 1);

                        // Fix rotation:
                        // TODO Not sure why we need the minus here... It makes no sense to me.
                        LOG.w("Recording frame. Rotation:", mResult.rotation, "Actual:", -mResult.rotation);
                        int rotation = -mResult.rotation;
                        mResult.rotation = 0;

                        // Go back to 0,0 so that rotate and flip work well.
                        Matrix.translateM(mTransform, 0, 0.5F, 0.5F, 0);

                        // Apply rotation:
                        Matrix.rotateM(mTransform, 0, rotation, 0, 0, 1);

                        // Flip horizontally for front camera:
                        if (mResult.facing == Facing.FRONT) {
                            Matrix.scaleM(mTransform, 0, -1, 1, 1);
                        }

                        // Go back to old position.
                        Matrix.translateM(mTransform, 0, -0.5F, -0.5F, 0);

                        // Future note: passing scale values to the viewport?
                        // They are simply realScaleX and realScaleY.
                        viewport.drawFrame(mTextureId, mTransform);
                        // don't - surface.swapBuffers();
                        mResult.data = surface.saveFrameTo(Bitmap.CompressFormat.JPEG);
                        mResult.format = PictureResult.FORMAT_JPEG;
                        mSurfaceTexture.releaseTexImage();

                        // EGL14.eglMakeCurrent(oldDisplay, oldSurface, oldSurface, eglContext);
                        surface.release();
                        viewport.release();
                        mSurfaceTexture.release();
                        core.release();
                        dispatchResult();
                    }
                });
            }
        });
    }


    private void takeLegacy() {
        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(@NonNull final byte[] yuv, Camera camera) {
                dispatchOnShutter(false);

                // Got to rotate the preview frame, since byte[] data here does not include
                // EXIF tags automatically set by camera. So either we add EXIF, or we rotate.
                // Adding EXIF to a byte array, unfortunately, is hard.
                final int sensorToOutput = mResult.rotation;
                final Size outputSize = mResult.size;
                WorkerHandler.run(new Runnable() {
                    @Override
                    public void run() {
                        // Rotate the picture, because no one will write EXIF data,
                        // then crop if needed. In both cases, transform yuv to jpeg.
                        byte[] data = RotationHelper.rotate(yuv, mSensorPreviewSize, sensorToOutput);
                        YuvImage yuv = new YuvImage(data, mFormat, outputSize.getWidth(), outputSize.getHeight(), null);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Rect outputRect = CropHelper.computeCrop(outputSize, mOutputRatio);
                        yuv.compressToJpeg(outputRect, 90, stream);
                        data = stream.toByteArray();

                        mResult.data = data;
                        mResult.size = new Size(outputRect.width(), outputRect.height());
                        mResult.rotation = 0;
                        mResult.format = PictureResult.FORMAT_JPEG;
                        dispatchResult();
                    }
                });

                // It seems that the buffers are already cleared here, so we need to allocate again.
                camera.setPreviewCallbackWithBuffer(null); // Release anything left
                camera.setPreviewCallbackWithBuffer(mController); // Add ourselves
                mController.mFrameManager.allocate(ImageFormat.getBitsPerPixel(mFormat), mController.mPreviewSize);
            }
        });
    }

    @Override
    protected void dispatchResult() {
        mController = null;
        mCamera = null;
        mOutputRatio = null;
        mFormat = 0;
        mSensorPreviewSize = null;
        super.dispatchResult();
    }
}
