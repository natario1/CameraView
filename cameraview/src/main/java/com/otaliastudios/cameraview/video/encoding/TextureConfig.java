package com.otaliastudios.cameraview.video.encoding;

import android.opengl.EGLContext;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.internal.Issue514Workaround;

/**
 * Video configuration to be passed as input to the constructor
 * of a {@link TextureMediaEncoder}.
 */
public class TextureConfig extends VideoConfig {

    private final static int NO_TEXTURE = Integer.MIN_VALUE;

    public int textureId = NO_TEXTURE;
    public int overlayTextureId = NO_TEXTURE;
    public int overlayRotation;
    public float scaleX;
    public float scaleY;
    public EGLContext eglContext;
    public Issue514Workaround issue514Workaround;

    @NonNull
    TextureConfig copy() {
        TextureConfig copy = new TextureConfig();
        copy(copy);
        copy.textureId = this.textureId;
        copy.overlayTextureId = this.overlayTextureId;
        copy.overlayRotation = this.overlayRotation;
        copy.scaleX = this.scaleX;
        copy.scaleY = this.scaleY;
        copy.eglContext = this.eglContext;
        copy.issue514Workaround = this.issue514Workaround;
        return copy;
    }

    boolean hasOverlay() {
        return overlayTextureId != NO_TEXTURE;
    }
}
