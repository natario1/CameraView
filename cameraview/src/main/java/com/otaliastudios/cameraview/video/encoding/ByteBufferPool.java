package com.otaliastudios.cameraview.video.encoding;

import com.otaliastudios.cameraview.utils.Pool;

import java.nio.ByteBuffer;

class ByteBufferPool extends Pool<ByteBuffer> {

    ByteBufferPool(final int bufferSize, int maxPoolSize) {
        super(maxPoolSize, new Factory<ByteBuffer>() {
            @Override
            public ByteBuffer create() {
                return ByteBuffer.allocateDirect(bufferSize);
            }
        });
    }
}
