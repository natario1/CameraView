package com.otaliastudios.cameraview.video.encoding;

import com.otaliastudios.cameraview.utils.Pool;

class InputBufferPool extends Pool<InputBuffer> {

    InputBufferPool() {
        super(Integer.MAX_VALUE, new Factory<InputBuffer>() {
            @Override
            public InputBuffer create() {
                return new InputBuffer();
            }
        });
    }
}
