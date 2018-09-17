package com.otaliastudios.cameraview;

import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class AudioMediaEncoder extends MediaEncoder {


    static class Config {
        Config() { }
    }

    AudioMediaEncoder(@NonNull Config config) {

    }

    @Override
    void prepare(MediaMuxer muxer) {
        super.prepare(muxer);
    }

    @Override
    void start() {

    }

    @Override
    void notify(String event, Object data) {

    }

    @Override
    void stop() {

    }

    @Override
    void release() {

    }
}
