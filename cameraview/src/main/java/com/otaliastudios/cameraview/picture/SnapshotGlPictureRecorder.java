package com.otaliastudios.cameraview.picture;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.Matrix;
import android.os.Build;

import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.internal.GlTextureDrawer;
import com.otaliastudios.cameraview.overlay.Overlay;
import com.otaliastudios.cameraview.engine.offset.Reference;
import com.otaliastudios.cameraview.internal.CropHelper;
import com.otaliastudios.cameraview.internal.WorkerHandler;
import com.otaliastudios.cameraview.overlay.OverlayDrawer;
import com.otaliastudios.cameraview.preview.RendererCameraPreview;
import com.otaliastudios.cameraview.preview.RendererFrameCallback;
import com.otaliastudios.cameraview.preview.RendererThread;
import com.otaliastudios.cameraview.filter.Filter;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.opengl.core.EglCore;
import com.otaliastudios.opengl.surface.EglSurface;
import com.otaliastudios.opengl.surface.EglWindowSurface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import android.view.Surface;

/**
 * API 19.
 * Records a picture snapshots from the {@link RendererCameraPreview}. It works as follows:
 *
 * - We register a one time {@link RendererFrameCallback} on the preview
 * - We get the textureId and the frame callback on the {@link RendererThread}
 * - [Optional: we construct another textureId for overlays]
 * - We take a handle of the EGL context from the {@link RendererThread}
 * - We move to another thread, and create a new EGL surface for that EGL context.
 * - We make this new surface current, and re-draw the textureId on it
 * - [Optional: fill the overlayTextureId and draw it on the same surface]
 * - We use glReadPixels (through {@link EglSurface#toByteArray(Bitmap.CompressFormat)})
 *   and save to file.
 *
 * We create a new EGL surface and redraw the frame because:
 * 1. We want to go off the renderer thread as soon as possible
 * 2. We have overlays to be drawn - we don't want to draw them on the preview surface,
 *    not even for a frame.
 */
public class SnapshotGlPictureRecorder extends SnapshotPictureRecorder {

    private RendererCameraPreview mPreview;
    private AspectRatio mOutputRatio;

    private Overlay mOverlay;
    private boolean mHasOverlay;
    private OverlayDrawer mOverlayDrawer;
    private GlTextureDrawer mTextureDrawer;

    public SnapshotGlPictureRecorder(
            @NonNull PictureResult.Stub stub,
            @Nullable PictureResultListener listener,
            @NonNull RendererCameraPreview preview,
            @NonNull AspectRatio outputRatio,
            @Nullable Overlay overlay) {
        super(stub, listener);
        mPreview = preview;
        mOutputRatio = outputRatio;
        mOverlay = overlay;
        mHasOverlay = mOverlay != null && mOverlay.drawsOn(Overlay.Target.PICTURE_SNAPSHOT);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void take() {
        mPreview.addRendererFrameCallback(new RendererFrameCallback() {

            @RendererThread
            public void onRendererTextureCreated(int textureId) {
                SnapshotGlPictureRecorder.this.onRendererTextureCreated(textureId);
            }

            @RendererThread
            @Override
            public void onRendererFilterChanged(@NonNull Filter filter) {
                SnapshotGlPictureRecorder.this.onRendererFilterChanged(filter);
            }

            @RendererThread
            @Override
            public void onRendererFrame(@NonNull SurfaceTexture surfaceTexture,
                                        int rotation, float scaleX, float scaleY) {
                mPreview.removeRendererFrameCallback(this);
                SnapshotGlPictureRecorder.this.onRendererFrame(surfaceTexture,
                        rotation, scaleX, scaleY);
            }

        });
    }

    @SuppressWarnings("WeakerAccess")
    @RendererThread
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void onRendererTextureCreated(int textureId) {
        mTextureDrawer = new GlTextureDrawer(textureId);
        // Need to crop the size.
        Rect crop = CropHelper.computeCrop(mResult.size, mOutputRatio);
        mResult.size = new Size(crop.width(), crop.height());
        if (mHasOverlay) {
            mOverlayDrawer = new OverlayDrawer(mOverlay, mResult.size);
        }
    }

    @SuppressWarnings("WeakerAccess")
    @RendererThread
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void onRendererFilterChanged(@NonNull Filter filter) {
        mTextureDrawer.setFilter(filter.copy());
    }

    @SuppressWarnings("WeakerAccess")
    @RendererThread
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void onRendererFrame(@SuppressWarnings("unused") @NonNull final SurfaceTexture surfaceTexture,
                                 final int rotation,
                                 final float scaleX,
                                 final float scaleY) {
        // Get egl context from the RendererThread, which is the one in which we have created
        // the textureId and the overlayTextureId, managed by the GlSurfaceView.
        // Next operations can then be performed on different threads using this handle.
        final EGLContext eglContext = EGL14.eglGetCurrentContext();
        WorkerHandler.execute(new Runnable() {
            @Override
            public void run() {
                takeFrame(surfaceTexture, rotation, scaleX, scaleY, eglContext);

            }
        });
    }

    /**
     * The tricky part here is the EGL surface creation.
     *
     * We don't have a real output window for the EGL surface - we will use glReadPixels()
     * and never call swapBuffers(), so what we draw is never published.
     *
     * 1. One option is to use a pbuffer EGL surface. This works, we just have to pass
     *    the correct width and height. However, it is significantly slower than the current
     *    solution.
     *
     * 2. Another option is to create the EGL surface out of a ImageReader.getSurface()
     *    and use the reader to create a JPEG. In this case, we would have to publish
     *    the frame with swapBuffers(). However, currently ImageReader does not support
     *    all formats, it's risky. This is an example error that we get:
     *    "RGBA override BLOB format buffer should have height == width"
     *
     * The third option, which we are using, is to create the EGL surface using whatever
     * {@link Surface} or {@link SurfaceTexture} we have at hand. Since we never call
     * swapBuffers(), the frame will not actually be rendered. This is the fastest.
     *
     * @param scaleX frame scale x in {@link Reference#VIEW}
     * @param scaleY frame scale y in {@link Reference#VIEW}
     */
    @SuppressWarnings("WeakerAccess")
    @WorkerThread
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void takeFrame(@NonNull SurfaceTexture surfaceTexture,
                             int rotation,
                             float scaleX,
                             float scaleY,
                             @NonNull EGLContext eglContext) {

        // 0. EGL window will need an output.
        // We create a fake one as explained in javadocs.
        final int fakeOutputTextureId = 9999;
        SurfaceTexture fakeOutputSurface = new SurfaceTexture(fakeOutputTextureId);
        fakeOutputSurface.setDefaultBufferSize(mResult.size.getWidth(), mResult.size.getHeight());

        // 1. Create an EGL surface
        final EglCore core = new EglCore(eglContext, EglCore.FLAG_RECORDABLE);
        final EglSurface eglSurface = new EglWindowSurface(core, fakeOutputSurface);
        eglSurface.makeCurrent();
        final float[] transform = mTextureDrawer.getTextureTransform();

        // 2. Apply preview transformations
        surfaceTexture.getTransformMatrix(transform);
        float scaleTranslX = (1F - scaleX) / 2F;
        float scaleTranslY = (1F - scaleY) / 2F;
        Matrix.translateM(transform, 0, scaleTranslX, scaleTranslY, 0);
        Matrix.scaleM(transform, 0, scaleX, scaleY, 1);

        // 3. Apply rotation and flip
        // If this doesn't work, rotate "rotation" before scaling, like GlCameraPreview does.
        Matrix.translateM(transform, 0, 0.5F, 0.5F, 0); // Go back to 0,0
        Matrix.rotateM(transform, 0, rotation + mResult.rotation, 0, 0, 1); // Rotate to OUTPUT
        Matrix.scaleM(transform, 0, 1, -1, 1); // Vertical flip because we'll use glReadPixels
        Matrix.translateM(transform, 0, -0.5F, -0.5F, 0); // Go back to old position

        // 4. Do pretty much the same for overlays
        if (mHasOverlay) {
            // 1. First we must draw on the texture and get latest image
            mOverlayDrawer.draw(Overlay.Target.PICTURE_SNAPSHOT);

            // 2. Then we can apply the transformations
            Matrix.translateM(mOverlayDrawer.getTransform(), 0, 0.5F, 0.5F, 0);
            Matrix.rotateM(mOverlayDrawer.getTransform(), 0, mResult.rotation, 0, 0, 1);
            Matrix.scaleM(mOverlayDrawer.getTransform(), 0, 1, -1, 1); // Vertical flip because we'll use glReadPixels
            Matrix.translateM(mOverlayDrawer.getTransform(), 0, -0.5F, -0.5F, 0);
        }
        mResult.rotation = 0;

        // 5. Draw and save
        long timestampUs = surfaceTexture.getTimestamp() / 1000L;
        LOG.i("takeFrame:", "timestampUs:", timestampUs);
        mTextureDrawer.draw(timestampUs);
        if (mHasOverlay) mOverlayDrawer.render(timestampUs);
        mResult.data = eglSurface.toByteArray(Bitmap.CompressFormat.JPEG);

        // 6. Cleanup
        eglSurface.release();
        mTextureDrawer.release();
        fakeOutputSurface.release();
        if (mHasOverlay) mOverlayDrawer.release();
        core.release();
        dispatchResult();
    }

    @Override
    protected void dispatchResult() {
        mOutputRatio = null;
        super.dispatchResult();
    }
}
