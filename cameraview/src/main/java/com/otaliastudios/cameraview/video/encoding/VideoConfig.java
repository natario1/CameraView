package com.otaliastudios.cameraview.video.encoding;

import androidx.annotation.NonNull;

/**
 * Base video configuration to be passed as input to the constructor
 * of a {@link VideoMediaEncoder}.
 */
public class VideoConfig {
    public int width;
    public int height;
    public int bitRate;
    public int frameRate;
    public int rotation;
    public String mimeType;
    public String encoder;

    protected <C extends VideoConfig> void copy(@NonNull C output) {
        output.width = this.width;
        output.height = this.height;
        output.bitRate = this.bitRate;
        output.frameRate = this.frameRate;
        output.rotation = this.rotation;
        output.mimeType = this.mimeType;
        output.encoder = this.encoder;
    }
}
