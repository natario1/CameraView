package com.otaliastudios.cameraview.video.encoding;

import android.opengl.EGLContext;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.internal.Issue514Workaround;
import com.otaliastudios.cameraview.overlay.Overlay;
import com.otaliastudios.cameraview.overlay.OverlayDrawer;

/**
 * Video configuration to be passed as input to the constructor
 * of a {@link TextureMediaEncoder}.
 */
public class TextureConfig extends VideoConfig {

    public int textureId;
    public Overlay.Target overlayTarget;
    public OverlayDrawer overlayDrawer;
    public int overlayRotation;
    public float scaleX;
    public float scaleY;
    public EGLContext eglContext;

    @NonNull
    TextureConfig copy() {
        TextureConfig copy = new TextureConfig();
        copy(copy);
        copy.textureId = this.textureId;
        copy.overlayDrawer = this.overlayDrawer;
        copy.overlayTarget = this.overlayTarget;
        copy.overlayRotation = this.overlayRotation;
        copy.scaleX = this.scaleX;
        copy.scaleY = this.scaleY;
        copy.eglContext = this.eglContext;
        return copy;
    }

    boolean hasOverlay() {
        return overlayDrawer != null;
    }
}
