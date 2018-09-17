package com.otaliastudios.cameraview;

import android.graphics.SurfaceTexture;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class MediaEncoderEngine {

    private WorkerHandler mWorker;
    private ArrayList<MediaEncoder> mEncoders;
    private MediaMuxer mMediaMuxer;

    MediaEncoderEngine(@NonNull File file, @NonNull VideoMediaEncoder videoEncoder, @Nullable AudioMediaEncoder audioEncoder) {
        mWorker = WorkerHandler.get("EncoderEngine");
        mEncoders = new ArrayList<>();
        mEncoders.add(videoEncoder);
        if (audioEncoder != null) {
            mEncoders.add(audioEncoder);
        }
        try {
            mMediaMuxer = new MediaMuxer(file.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mWorker.post(new Runnable() {
            @Override
            public void run() {
                for (MediaEncoder encoder : mEncoders) {
                    encoder.prepare(mMediaMuxer);
                }
            }
        });
    }

    void start() {
        mWorker.post(new Runnable() {
            @Override
            public void run() {
                for (MediaEncoder encoder : mEncoders) {
                    encoder.start();
                }
            }
        });
    }

    void notify(final String event, final Object data) {
        mWorker.post(new Runnable() {
            @Override
            public void run() {
                for (MediaEncoder encoder : mEncoders) {
                    encoder.notify(event, data);
                }
            }
        });
    }

    void stop(final Runnable onStop) {
        mWorker.post(new Runnable() {
            @Override
            public void run() {
                for (MediaEncoder encoder : mEncoders) {
                    encoder.stop();
                }
                onStop.run();
                for (MediaEncoder encoder : mEncoders) {
                    encoder.release();
                }
                if (mMediaMuxer != null) {
                    // TODO: stop() throws an exception if you haven't fed it any data.  Keep track
                    //       of frames submitted, and don't call stop() if we haven't written anything.
                    mMediaMuxer.stop();
                    mMediaMuxer.release();
                    mMediaMuxer = null;
                }
            }
        });
    }
}
