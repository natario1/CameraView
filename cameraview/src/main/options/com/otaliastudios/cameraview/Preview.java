package com.otaliastudios.cameraview;


/**
 * The preview engine to be used.
 *
 * @see CameraView#setPreview(Preview)
 */
public enum Preview implements Control {

    /**
     * Preview engine based on {@link android.view.SurfaceView}.
     * Not recommended.
     */
    SURFACE(0),

    /**
     * Preview engine based on {@link android.view.TextureView}.
     * Stable, but does not support all features (like video snapshots).
     */
    TEXTURE(1),

    /**
     * Preview engine based on {@link android.opengl.GLSurfaceView}.
     * This is the best engine available.
     */
    GL_SURFACE(2);

    final static Preview DEFAULT = GL_SURFACE;

    private int value;

    Preview(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    static Preview fromValue(int value) {
        Preview[] list = Preview.values();
        for (Preview action : list) {
            if (action.value() == value) {
                return action;
            }
        }
        return null;
    }
}
