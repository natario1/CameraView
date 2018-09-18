package com.otaliastudios.cameraview;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class MediaEncoderEngine {

    private WorkerHandler mWorker;
    private ArrayList<MediaEncoder> mEncoders;
    private MediaMuxer mMediaMuxer;
    private int mMediaMuxerStartCount;
    private boolean mMediaMuxerStarted;
    private Controller mController;

    MediaEncoderEngine(@NonNull File file, @NonNull VideoMediaEncoder videoEncoder, @Nullable AudioMediaEncoder audioEncoder) {
        mWorker = WorkerHandler.get("EncoderEngine");
        mController = new Controller();
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
        mMediaMuxerStartCount = 0;
        mMediaMuxerStarted = false;
        mWorker.post(new Runnable() {
            @Override
            public void run() {
                for (MediaEncoder encoder : mEncoders) {
                    encoder.prepare(mController);
                }
            }
        });
    }

    class Controller {

        int start(MediaFormat format) {
            if (mMediaMuxerStarted) {
                throw new IllegalStateException("Trying to start but muxer started already");
            }
            int track = mMediaMuxer.addTrack(format);
            mMediaMuxerStartCount++;
            if (mMediaMuxerStartCount == mEncoders.size()) {
                mMediaMuxer.start();
                mMediaMuxerStarted = true;
            }
            return track;
        }

        boolean isStarted() {
            return mMediaMuxerStarted;
        }

        void write(int track, ByteBuffer encodedData, MediaCodec.BufferInfo info) {
            if (!mMediaMuxerStarted) {
                throw new IllegalStateException("Trying to write before muxer started");
            }
            mMediaMuxer.writeSampleData(track, encodedData, info);
        }
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
                    // stop() throws an exception if you haven't fed it any data.
                    // We can just swallow I think.
                    mMediaMuxer.stop();
                    mMediaMuxer.release();
                    mMediaMuxer = null;
                }
                mMediaMuxerStartCount = 0;
                mMediaMuxerStarted = false;
            }
        });
    }
}
