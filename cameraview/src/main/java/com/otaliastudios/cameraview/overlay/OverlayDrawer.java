package com.otaliastudios.cameraview.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.internal.Issue514Workaround;
import com.otaliastudios.cameraview.internal.egl.EglViewport;
import com.otaliastudios.cameraview.size.Size;

import java.nio.Buffer;


/**
 * Draws overlays through {@link Overlay}.
 *
 * - Provides a {@link Canvas} to be passed to the Overlay
 * - Lets the overlay draw there: {@link #draw(Overlay.Target)}
 * - Renders this into the current EGL window: {@link #render()}
 * - Applies the {@link Issue514Workaround} the correct way
 *
 * In the future we might want to use a different approach than {@link EglViewport},
 * {@link SurfaceTexture} and {@link GLES11Ext#GL_TEXTURE_EXTERNAL_OES},
 * for example by using a regular {@link GLES20#GL_TEXTURE_2D} that might
 * be filled through {@link GLES20#glTexImage2D(int, int, int, int, int, int, int, int, Buffer)}.
 *
 * The current approach has some issues, for example see {@link Issue514Workaround}.
 */
public class OverlayDrawer {

    private static final String TAG = OverlayDrawer.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private Overlay mOverlay;
    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private float[] mTransform = new float[16];
    private EglViewport mViewport;
    private Issue514Workaround mIssue514Workaround;
    private final Object mIssue514WorkaroundLock = new Object();

    public OverlayDrawer(@NonNull Overlay overlay, @NonNull Size size, int cameraTextureId) {
        mOverlay = overlay;
        mViewport = new EglViewport();
        mTextureId = mViewport.createTexture();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        mSurface = new Surface(mSurfaceTexture);
        mIssue514Workaround = new Issue514Workaround(cameraTextureId);
    }


    public void draw(@NonNull Overlay.Target target) {
        try {
            final Canvas surfaceCanvas = mSurface.lockCanvas(null);
            surfaceCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            mOverlay.drawOn(target, surfaceCanvas);
            mSurface.unlockCanvasAndPost(surfaceCanvas);
        } catch (Surface.OutOfResourcesException e) {
            LOG.w("Got Surface.OutOfResourcesException while drawing video overlays", e);
        }
        synchronized (mIssue514WorkaroundLock) {
            mIssue514Workaround.beforeOverlayUpdateTexImage();
            mSurfaceTexture.updateTexImage();
        }
        mSurfaceTexture.getTransformMatrix(mTransform);
    }

    public float[] getTransform() {
        return mTransform;
    }

    public void render() {
        synchronized (mIssue514WorkaroundLock) {
            mViewport.drawFrame(mTextureId, mTransform);
        }
        // mIssue514Workaround.afterOverlayGlDrawn();
        // mIssue514Workaround.afterOverlayGlDrawn();
    }

    public void release() {
        mIssue514Workaround.end();
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mViewport != null) {
            mViewport.release();
            mViewport = null;
        }
    }
}
