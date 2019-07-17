package com.otaliastudios.cameraview.video.encoding;

import android.opengl.EGLContext;

import androidx.annotation.NonNull;

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
        return copy;
    }

    boolean hasOverlay() {
        return overlayTextureId != NO_TEXTURE;
    }
}
