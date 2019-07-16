package com.otaliastudios.cameraview.video.encoding;

import java.nio.ByteBuffer;

/**
 * Represents an input buffer, which means,
 * raw data that should be encoded by MediaCodec.
 */
class InputBuffer {
    ByteBuffer data;
    ByteBuffer source;
    int index;
    int length;
    long timestamp;
    boolean isEndOfStream;
    boolean didReachMaxLength;
}
