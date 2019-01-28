package com.otaliastudios.cameraview;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class MediaEncoderEngine {

    private final static String TAG = MediaEncoderEngine.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    @SuppressWarnings("WeakerAccess")
    final static int STOP_BY_USER = 0;
    final static int STOP_BY_MAX_DURATION = 1;
    final static int STOP_BY_MAX_SIZE = 2;

    private ArrayList<MediaEncoder> mEncoders;
    private MediaMuxer mMediaMuxer;
    private int mStartedEncodersCount;
    private int mStoppedEncodersCount;
    private boolean mMediaMuxerStarted;
    private Controller mController;
    private Listener mListener;
    private int mStopReason = STOP_BY_USER;
    private int mPossibleStopReason;
    private final Object mControllerLock = new Object();

    MediaEncoderEngine(@NonNull File file, @NonNull VideoMediaEncoder videoEncoder, @Nullable AudioMediaEncoder audioEncoder,
                       final int maxDuration, final long maxSize, @Nullable Listener listener) {
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
        mStartedEncodersCount = 0;
        mMediaMuxerStarted = false;
        mStoppedEncodersCount = 0;

        // Trying to convert the size constraints to duration constraints,
        // because they are super easy to check.
        // This is really naive & probably not accurate, but...
        int bitRate = 0;
        for (MediaEncoder encoder : mEncoders) {
            bitRate += encoder.getEncodedBitRate();
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
        LOG.w("Computed a max duration of", (finalMaxDuration / 1000F));
        for (MediaEncoder encoder : mEncoders) {
            encoder.prepare(mController, finalMaxDuration);
        }
    }

    // Stuff here might be called from multiple threads.
    class Controller {

        /**
         * Request that the muxer should start. This is not guaranteed to be executed:
         * we wait for all encoders to call this method, and only then, start the muxer.
         * @param format the media format
         * @return the encoder track index
         */
        int requestStart(MediaFormat format) {
            synchronized (mControllerLock) {
                if (mMediaMuxerStarted) {
                    throw new IllegalStateException("Trying to start but muxer started already");
                }
                int track = mMediaMuxer.addTrack(format);
                LOG.w("Controller:", "Assigned track", track, "to format", format.getString(MediaFormat.KEY_MIME));
                if (++mStartedEncodersCount == mEncoders.size()) {
                    mMediaMuxer.start();
                    mMediaMuxerStarted = true;
                }
                return track;
            }
        }

        /**
         * Whether the muxer is started.
         * @return true if muxer was started
         */
        boolean isStarted() {
            synchronized (mControllerLock) {
                return mMediaMuxerStarted;
            }
        }

        /**
         * Writes the given data to the muxer. Should be called after {@link #isStarted()}
         * returns true. Note: this seems to be thread safe, no lock.
         * TODO cache values if not started yet, then apply later. Read comments in drain().
         * Currently they are recycled instantly.
         */
        void write(OutputBufferPool pool, OutputBuffer buffer) {
            if (!mMediaMuxerStarted) {
                throw new IllegalStateException("Trying to write before muxer started");
            }
            // This is a bad idea and causes crashes.
            // if (info.presentationTimeUs < mLastTimestampUs) info.presentationTimeUs = mLastTimestampUs;
            // mLastTimestampUs = info.presentationTimeUs;
            LOG.v("Writing for track", buffer.trackIndex, ". Presentation:", buffer.info.presentationTimeUs);
            mMediaMuxer.writeSampleData(buffer.trackIndex, buffer.data, buffer.info);
            pool.recycle(buffer);
        }

        /**
         * Requests that the engine stops. This is not executed until all encoders call
         * this method, so it is a kind of soft request, just like {@link #requestStart(MediaFormat)}.
         * To be used when maxLength / maxSize constraints are reached, for example.
         *
         * When this succeeds, {@link MediaEncoder#stop()} is called.
         */
        void requestStop(int track) {
            LOG.i("RequestStop was called for track", track);
            synchronized (mControllerLock) {
                if (--mStartedEncodersCount == 0) {
                    mStopReason = mPossibleStopReason;
                    stop();
                }
            }
        }

        /**
         * Notifies that the encoder was stopped. After this is called by all encoders,
         * we will actually stop the muxer.
         */
        void requestRelease(int track) {
            LOG.i("requestRelease was called for track", track);
            synchronized (mControllerLock) {
                if (++mStoppedEncodersCount == mEncoders.size()) {
                    release();
                }
            }
        }
    }

    final void start() {
        for (MediaEncoder encoder : mEncoders) {
            encoder.start();
        }
    }

    @SuppressWarnings("SameParameterValue")
    final void notify(final String event, final Object data) {
        for (MediaEncoder encoder : mEncoders) {
            encoder.notify(event, data);
        }
    }

    /**
     * This just asks the encoder to stop. We will wait for them to call {@link Controller#requestRelease(int)}
     * to actually stop the muxer, as there might be async stuff going on.
     */
    final void stop() {
        for (MediaEncoder encoder : mEncoders) {
            encoder.stop();
        }
    }

    private void release() {
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
        if (mListener != null) {
            mListener.onEncoderStop(mStopReason, error);
            mListener = null;
        }
        mStopReason = STOP_BY_USER;
        mStartedEncodersCount = 0;
        mStoppedEncodersCount = 0;
        mMediaMuxerStarted = false;
    }

    @NonNull
    VideoMediaEncoder getVideoEncoder() {
        return (VideoMediaEncoder) mEncoders.get(0);
    }

    @Nullable
    AudioMediaEncoder getAudioEncoder() {
        if (mEncoders.size() > 1) {
            return (AudioMediaEncoder) mEncoders.get(1);
        } else {
            return null;
        }
    }

    interface Listener {

        @EncoderThread
        void onEncoderStop(int stopReason, @Nullable Exception e);
    }
}
