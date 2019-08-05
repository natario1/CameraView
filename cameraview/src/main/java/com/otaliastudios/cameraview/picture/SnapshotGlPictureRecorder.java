package com.otaliastudios.cameraview.picture;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.Matrix;
import android.os.Build;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.overlay.Overlay;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.engine.CameraEngine;
import com.otaliastudios.cameraview.engine.offset.Axis;
import com.otaliastudios.cameraview.engine.offset.Reference;
import com.otaliastudios.cameraview.internal.egl.EglCore;
import com.otaliastudios.cameraview.internal.egl.EglViewport;
import com.otaliastudios.cameraview.internal.egl.EglWindowSurface;
import com.otaliastudios.cameraview.internal.utils.CropHelper;
import com.otaliastudios.cameraview.internal.utils.WorkerHandler;
import com.otaliastudios.cameraview.preview.GlCameraPreview;
import com.otaliastudios.cameraview.preview.RendererFrameCallback;
import com.otaliastudios.cameraview.preview.RendererThread;
import com.otaliastudios.cameraview.filters.Filter;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.Surface;

public class SnapshotGlPictureRecorder extends PictureRecorder {

    private static final String TAG = SnapshotGlPictureRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private CameraEngine mEngine;
    private GlCameraPreview mPreview;
    private AspectRatio mOutputRatio;

    private Overlay mOverlay;
    private boolean mHasOverlay;

    public SnapshotGlPictureRecorder(
            @NonNull PictureResult.Stub stub,
            @NonNull CameraEngine engine,
            @NonNull GlCameraPreview preview,
            @NonNull AspectRatio outputRatio,
            @Nullable Overlay overlay) {
        super(stub, engine);
        mEngine = engine;
        mPreview = preview;
        mOutputRatio = outputRatio;
        mOverlay = overlay;
        mHasOverlay = overlay != null && overlay.drawsOn(Overlay.Target.PICTURE_SNAPSHOT);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void take() {
        mPreview.addRendererFrameCallback(new RendererFrameCallback() {

            int mTextureId;
            SurfaceTexture mSurfaceTexture;
            float[] mTransform;

            int mOverlayTextureId = 0;
            SurfaceTexture mOverlaySurfaceTexture;
            Surface mOverlaySurface;
            float[] mOverlayTransform;

            EglViewport mViewport;

            @RendererThread
            public void onRendererTextureCreated(int textureId) {
                mTextureId = textureId;
                mViewport = new EglViewport();
                mSurfaceTexture = new SurfaceTexture(mTextureId, true);
                // Need to crop the size.
                Rect crop = CropHelper.computeCrop(mResult.size, mOutputRatio);
                mResult.size = new Size(crop.width(), crop.height());
                mSurfaceTexture.setDefaultBufferSize(mResult.size.getWidth(), mResult.size.getHeight());
                mTransform = new float[16];

                if (mHasOverlay) {
                    mOverlayTextureId = mViewport.createTexture();
                    mOverlaySurfaceTexture = new SurfaceTexture(mOverlayTextureId, true);
                    mOverlaySurfaceTexture.setDefaultBufferSize(mResult.size.getWidth(), mResult.size.getHeight());
                    mOverlaySurface = new Surface(mOverlaySurfaceTexture);
                    mOverlayTransform = new float[16];
                }
            }

            @RendererThread
            @Override
            public void onRendererFrame(@NonNull SurfaceTexture surfaceTexture, final float scaleX, final float scaleY, Filter shaderEffect) {
                mPreview.removeRendererFrameCallback(this);

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

                //set the current shader before taking the snapshot
                mViewport.changeShaderEffect(shaderEffect);

                // final EGLSurface oldSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
                // final EGLDisplay oldDisplay = EGL14.eglGetCurrentDisplay();
                WorkerHandler.execute(new Runnable() {
                    @Override
                    public void run() {
                        // 1. Get latest texture
                        EglWindowSurface surface = new EglWindowSurface(core, mSurfaceTexture);
                        surface.makeCurrent();
                        mSurfaceTexture.updateTexImage();
                        mSurfaceTexture.getTransformMatrix(mTransform);

                        // 2. Apply scale and crop:
                        // scaleX and scaleY are in REF_VIEW, while our input appears to be in REF_SENSOR.
                        boolean flip = mEngine.getAngles().flip(Reference.VIEW, Reference.SENSOR);
                        float realScaleX = flip ? scaleY : scaleX;
                        float realScaleY = flip ? scaleX : scaleY;
                        float scaleTranslX = (1F - realScaleX) / 2F;
                        float scaleTranslY = (1F - realScaleY) / 2F;
                        Matrix.translateM(mTransform, 0, scaleTranslX, scaleTranslY, 0);
                        Matrix.scaleM(mTransform, 0, realScaleX, realScaleY, 1);

                        // 3. Go back to 0,0 so that rotate and flip work well.
                        Matrix.translateM(mTransform, 0, 0.5F, 0.5F, 0);

                        // 4. Apply rotation:
                        // Not sure why we need the minus here.
                        Matrix.rotateM(mTransform, 0, -mResult.rotation, 0, 0, 1);
                        mResult.rotation = 0;

                        // 5. Flip horizontally for front camera:
                        if (mResult.facing == Facing.FRONT) {
                            Matrix.scaleM(mTransform, 0, -1, 1, 1);
                        }

                        // 6. Go back to old position.
                        Matrix.translateM(mTransform, 0, -0.5F, -0.5F, 0);

                        // 7. Do pretty much the same for overlays, though with
                        // some differences.
                        if (mHasOverlay) {
                            // 1. First we must draw on the texture and get latest image.
                            try {
                                final Canvas surfaceCanvas = mOverlaySurface.lockCanvas(null);
                                surfaceCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                                mOverlay.drawOn(Overlay.Target.PICTURE_SNAPSHOT, surfaceCanvas);
                                mOverlaySurface.unlockCanvasAndPost(surfaceCanvas);
                            } catch (Surface.OutOfResourcesException e) {
                                LOG.w("Got Surface.OutOfResourcesException while drawing picture overlays", e);
                            }
                            mOverlaySurfaceTexture.updateTexImage();
                            mOverlaySurfaceTexture.getTransformMatrix(mOverlayTransform);

                            // 2. Then we can apply the transformations.
                            int rotation = mEngine.getAngles().offset(Reference.VIEW, Reference.OUTPUT, Axis.ABSOLUTE);
                            Matrix.translateM(mOverlayTransform, 0, 0.5F, 0.5F, 0);
                            Matrix.rotateM(mOverlayTransform, 0, rotation, 0, 0, 1);
                            // No need to flip the x axis for front camera, but need to flip the y axis always.
                            Matrix.scaleM(mOverlayTransform, 0, 1, -1, 1);
                            Matrix.translateM(mOverlayTransform, 0, -0.5F, -0.5F, 0);
                        }

                        // 8. Draw and save
                        mViewport.drawFrame(mTextureId, mTransform);
                        if (mHasOverlay) mViewport.drawFrame(mOverlayTextureId, mOverlayTransform);
                        // don't - surface.swapBuffers();
                        mResult.data = surface.saveFrameTo(Bitmap.CompressFormat.JPEG);
                        mResult.format = PictureResult.FORMAT_JPEG;

                        // 9. Cleanup
                        mSurfaceTexture.releaseTexImage();
                        surface.release();
                        mViewport.release();
                        mSurfaceTexture.release();
                        if (mHasOverlay) {
                            mOverlaySurface.release();
                            mOverlaySurfaceTexture.release();
                        }
                        core.release();
                        dispatchResult();
                    }
                });
            }
        });
    }

    @Override
    protected void dispatchResult() {
        mEngine = null;
        mOutputRatio = null;
        super.dispatchResult();
    }
}
