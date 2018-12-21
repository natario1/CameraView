package com.otaliastudios.cameraview;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class MediaEncoderEngine {

    private final static String TAG = MediaEncoder.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    @SuppressWarnings("WeakerAccess")
    final static int STOP_BY_USER = 0;
    final static int STOP_BY_MAX_DURATION = 1;
    final static int STOP_BY_MAX_SIZE = 2;

    private WorkerHandler mWorker;
    private ArrayList<MediaEncoder> mEncoders;
    private MediaMuxer mMediaMuxer;
    private int mMediaMuxerStartCount;
    private boolean mMediaMuxerStarted;
    private Controller mController;
    private Listener mListener;
    private int mStopReason = STOP_BY_USER;
    private int mPossibleStopReason;
    private final Object mLock = new Object();

    MediaEncoderEngine(@NonNull File file, @NonNull VideoMediaEncoder videoEncoder, @Nullable AudioMediaEncoder audioEncoder,
                       final int maxDuration, final long maxSize, @Nullable Listener listener) {
        mWorker = WorkerHandler.get("EncoderEngine");
        mListener = listener;
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
                // Trying to convert the size constraints to duration constraints,
                // because they are super easy to check.
                // This is really naive & probably not accurate, but...
                int bitRate = 0;
                for (MediaEncoder encoder : mEncoders) {
                    bitRate += encoder.getBitRate();
                }
                int bytePerSecond = bitRate / 8;
                long sizeMaxDuration = (maxSize / bytePerSecond) * 1000L;

                long finalMaxDuration = Long.MAX_VALUE;
                if (maxSize > 0 && maxDuration > 0) {
                    mPossibleStopReason = sizeMaxDuration < maxDuration ? STOP_BY_MAX_SIZE : STOP_BY_MAX_DURATION;
                    finalMaxDuration = Math.min(sizeMaxDuration, maxDuration);
                } else if (maxSize > 0) {
                    mPossibleStopReason = STOP_BY_MAX_SIZE;
                    finalMaxDuration = sizeMaxDuration;
                } else if (maxDuration > 0) {
                    mPossibleStopReason = STOP_BY_MAX_DURATION;
                    finalMaxDuration = maxDuration;
                }
                LOG.i("Computed a max duration of", (finalMaxDuration / 1000F));
                for (MediaEncoder encoder : mEncoders) {
                    encoder.prepare(mController, finalMaxDuration);
                }
            }
        });
    }

    // Stuff here might be called from multiple threads.
    class Controller {

        int start(MediaFormat format) {
            synchronized (mLock) {
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
        }

        boolean isStarted() {
            synchronized (mLock) {
                return mMediaMuxerStarted;
            }
        }

        // Synchronization does not seem needed here.
        void write(int track, ByteBuffer encodedData, MediaCodec.BufferInfo info) {
            if (!mMediaMuxerStarted) {
                throw new IllegalStateException("Trying to write before muxer started");
            }
            mMediaMuxer.writeSampleData(track, encodedData, info);
        }

        void requestStop() {
            synchronized (mLock) {
                mMediaMuxerStartCount--;
                if (mMediaMuxerStartCount == 0) {
                    mStopReason = mPossibleStopReason;
                    stop();
                }
            }
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

    void stop() {
        mWorker.post(new Runnable() {
            @Override
            public void run() {
                for (MediaEncoder encoder : mEncoders) {
                    encoder.stop();
                }
                for (MediaEncoder encoder : mEncoders) {
                    encoder.release();
                }
                Exception error = null;
                if (mMediaMuxer != null) {
                    // stop() throws an exception if you haven't fed it any data.
                    // But also in other occasions. So this is a signal that something
                    // went wrong, and we propagate that to the listener.
                    try {
                        mMediaMuxer.stop();
                        mMediaMuxer.release();
                    } catch (Exception e) {
                        error = e;
                    }
                    mMediaMuxer = null;
                }
                if (mListener != null) mListener.onEncoderStop(mStopReason, error);
                mStopReason = STOP_BY_USER;
                mListener = null;
                mMediaMuxerStartCount = 0;
                mMediaMuxerStarted = false;
            }
        });
    }

    interface Listener {

        @EncoderThread
        void onEncoderStop(int stopReason, @Nullable Exception e);
    }
}
