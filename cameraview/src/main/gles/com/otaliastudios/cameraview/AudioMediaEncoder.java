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

    @EncoderThread
    @Override
    void prepare(MediaEncoderEngine.Controller controller) {
        super.prepare(controller);
    }

    @EncoderThread
    @Override
    void start() {

    }

    @EncoderThread
    @Override
    void notify(String event, Object data) {

    }

    @EncoderThread
    @Override
    void stop() {

    }

    @Override
    void release() {
        super.release();
    }
}
