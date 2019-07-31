package com.otaliastudios.cameraview.internal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.view.Surface;
import com.otaliastudios.cameraview.preview.RendererThread;


/**
 * Fixes an issue for some devices with snapshot picture and video recording.
 * This is so dirty and totally unclear that I wanted to have a separate class.
 *
 * WHAT WE DO
 * What we do here is, assuming that we have overlays, is to create a useless
 * {@link SurfaceTexture} that receives NO input, because we create no Surface out of it.
 * However, it MUST be created using the camera frames texture ID.
 * After creation, and before drawing on the overlay Surface, users can call {@link #start()}:
 * this will call {@link SurfaceTexture#updateTexImage()}. Since this has NO input, this
 * call should be doing NOTHING! However, it fixes the bug #514. Keep reading!
 * After the snapshot has been taken (either picture or video), this can be released
 * using {@link #end()}.
 *
 * WHY IT FIXES THAT ISSUE
 * I have no answer, this is pure magic to me. There is actually no need of this class in some cases:
 * - when we don't have overlays
 * - on the majority of devices
 * But some devices will show the issue #514 and so they need this class to fix it.
 * I believe that this has no performance impact but it could be measured and the use of this
 * class could be restricted to those specific devices that need it.
 *
 * SNAPSHOT PROCEDURE
 * The issue is about picture and video snapshots with overlays. In both cases, we:
 * 1. Take textureId from the camera preview
 * 2. Take EGLContext from the camera preview thread ({@link RendererThread})
 * 3. Create an overlayTextureId
 * 4. Create an overlaySurfaceTexture
 * 5. Create an overlaySurface
 * 6. Move to another thread
 * 7. Create a new EGLContext using the old context as a shared context so we have texture data
 * 8. Create a new EGLWindow using some surface as output
 * 9. For each frame:
 *    9A. Draw overlays on the overlaySurface.lockCanvas() / unlockCanvasAndPost()
 *    9B. Publish overlays to GL texture using overlaySurfaceTexture.updateTexImage()
 *    9C. GLES - draw textureId
 *    9D. GLES - draw overlayTextureId
 * Both textures are drawn on the same EGLWindow and we manage to overlay them with {@link GLES20#GL_BLEND}.
 * This is the whole procedure and it works for the majority of devices and situations.
 *
 * ISSUE DESCRIPTION
 * The #514 issue can be described as follows:
 * - Overlays have no transparency: background is {@link Color#BLACK} and covers the video
 * - Overlays have distorted colors: {@link Color#RED} becomes greenish,
 *                                   {@link Color#GREEN} becomes blueish,
 *                                   {@link Color#BLUE} becomes reddish
 *
 * ISSUE INSIGHTS
 * After painful debugging, we have reached these conclusions:
 * 1. Overlays are drawn on {@link Canvas} with the correct format
 *    This can be checked for example by applying alpha to one overlay. The final color will
 *    be faded out, although on a black background. So the {@link Canvas} drawing step works well.
 * 2. The GLES shader will always receive pixels in RGBA
 *    This seems to be a constant in Android - someone does the conversion for us at a lower level.
 *    This was confirmed for example by forcing A=0.5 and seeing the video frames behind the overlay
 *    black background, or by forcing to 0.0 some of the channels and seeing the output.
 * 3. The {@link Canvas} / {@link Surface} pixels are wrongly treated as YUV!
 *    On problematic devices, some component down there thinks that our overlays RGBA are in YUV,
 *    and will CONVERT THEM TO RGBA. This means:
 *    3A. Original alpha is dropped. The algorithm thinks we have passed YUV.
 *    3B. Original colors are messed up. For example, (255,0,0,255,RGBA) is treated as (255,0,0,YUV)
 *        and converted back to rgb becoming greenish (74,255,27,255,RGBA).
 *        Doing the same conversion for {@link Color#GREEN} and {@link Color#BLUE} confirms what we
 *        were seeing in the issue screenshots.
 *
 * So a pixel format conversion takes place, when it shouldn't happen. We can't solve this:
 * - It is done at a lower level, there's no real way for us to specify the surface format, but
 *   it seems that these devices will prefer a YUV format and misunderstand our {@link Canvas} pixels.
 * - There is also no way to identify which devices will present this issue, it's a bug somewhere
 *   and it is implementation specific.
 *
 * THE MAGIC
 * Impossible to say why, but using this class fixes the described issue.
 * - Creating an useless {@link SurfaceTexture}
 * - Calling a useless {@link SurfaceTexture#updateTexImage()} before locking the overlay canvas
 * - Releasing the useless {@link SurfaceTexture}
 * This must be done using the video frame textureId, not the overlayTextureId nor any other id.
 */
public class Issue514Workaround {

    private SurfaceTexture surfaceTexture = null;
    private final boolean hasOverlay;

    public Issue514Workaround(int cameraTextureId, boolean hasOverlay) {
        this.hasOverlay = hasOverlay;
        if (this.hasOverlay) {
            try {
                surfaceTexture = new SurfaceTexture(cameraTextureId);
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        // Never called! Correctly.
                    }
                });
            } catch (Exception ignore) { }
        }
    }

    public void start() {
        if (hasOverlay) {
            try {
                surfaceTexture.updateTexImage();
            } catch (Exception ignore) {}
        }
    }

    public void end() {
        if (hasOverlay) {
            try {
                surfaceTexture.release();
            } catch (Exception ignore) {}
        }
    }
}
