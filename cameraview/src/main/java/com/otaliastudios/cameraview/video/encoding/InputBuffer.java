package com.otaliastudios.cameraview.video.encoding;

import java.nio.ByteBuffer;

/**
 * Represents an input buffer, which means,
 * raw data that should be encoded by MediaCodec.
 */
@SuppressWarnings("WeakerAccess")
public class InputBuffer {
    public ByteBuffer data;
    public ByteBuffer source;
    public int index;
    public int length;
    public long timestamp;
    public boolean isEndOfStream;
}
