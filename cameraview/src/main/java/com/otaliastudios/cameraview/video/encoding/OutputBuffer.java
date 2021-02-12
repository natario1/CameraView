package com.otaliastudios.cameraview.video.encoding;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * Represents an output buffer, which means,
 * an encoded buffer of data that should be passed
 * to the muxer.
 */
@SuppressWarnings("WeakerAccess")
public class OutputBuffer {
    public MediaCodec.BufferInfo info;
    public int trackIndex;
    public ByteBuffer data;
}
