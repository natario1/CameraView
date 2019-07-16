package com.otaliastudios.cameraview.video.encoding;

import android.annotation.SuppressLint;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.text.format.DateFormat;

import com.otaliastudios.cameraview.CameraLogger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The entry point for encoding video files.
 *
 * The external API is simple but the internal mechanism is not easy. Basically the engine
 * controls a {@link MediaEncoder} instance for each track (e.g. one for video, one for audio).
 *
 * 1. We prepare the MediaEncoders: {@link MediaEncoder#prepare(Controller, long)}
 *    MediaEncoders can be prepared synchronously or not.
 *
 * 2. Someone calls {@link #start()} from any thread.
 *    As a consequence, we start the MediaEncoders: {@link MediaEncoder#start()}.
 *
 * 3. MediaEncoders do not start synchronously. Instead, they call
 *    {@link Controller#notifyStarted(MediaFormat)} when they have a legit format,
 *    and we keep track of who has started.
 *
 * 4. When all MediaEncoders have started, we actually start the muxer.
 *
 * 5. Someone calls {@link #stop()} from any thread.
 *    As a consequence, we stop the MediaEncoders: {@link MediaEncoder#stop()}.
 *
 * 6. MediaEncoders do not stop synchronously. Instead, they will stop reading but
 *    keep draining the codec until there's no data left. At that point, they can
 *    call {@link Controller#notifyStopped(int)}.
 *
 * 7. When all MediaEncoders have been released, we actually stop the muxer and notify.
 *
 * There is another possibility where MediaEncoders themselves want to stop, for example
 * because they reach some limit or constraint (e.g. max duration). For this, they should
 * call {@link Controller#requestStop(int)}. Once all MediaEncoders have stopped, we will
 * actually call {@link #stop()} on ourselves.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaEncoderEngine {

    /**
     * Receives the stop event callback to know when the video
     * was written (or what went wrong).
     */
    public interface Listener {

        /**
         * Called when encoding started.
         */
        @EncoderThread
        void onEncodingStart();

        /**
         * Called when encoding stopped for some reason.
         * If there's an exception, it failed.
         * @param reason the reason
         * @param e the error, if present
         */
        @EncoderThread
        void onEncodingEnd(int reason, @Nullable Exception e);
    }

    private final static String TAG = MediaEncoderEngine.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);
    private static final boolean DEBUG_PERFORMANCE = true;

    @SuppressWarnings("WeakerAccess")
    public final static int END_BY_USER = 0;
    public final static int END_BY_MAX_DURATION = 1;
    public final static int END_BY_MAX_SIZE = 2;

    private List<MediaEncoder> mEncoders;
    private MediaMuxer mMediaMuxer;
    private int mStartedEncodersCount;
    private int mReleasedEncodersCount;
    private boolean mMediaMuxerStarted;
    @SuppressWarnings("FieldCanBeLocal")
    private Controller mController;
    private Listener mListener;
    private int mEndReason = END_BY_USER;
    private int mPossibleEndReason;
    private final Object mControllerLock = new Object();

    /**
     * Creates a new engine for the given file, with the given encoders and max limits,
     * and listener to receive events.
     *
     * @param file output file
     * @param videoEncoder video encoder to use
     * @param audioEncoder audio encoder to use
     * @param maxDuration max duration in millis
     * @param maxSize max size
     * @param listener a listener
     */
    public MediaEncoderEngine(@NonNull File file,
                              @NonNull VideoMediaEncoder videoEncoder,
                              @Nullable AudioMediaEncoder audioEncoder,
                              final int maxDuration,
                              final long maxSize,
                              @Nullable Listener listener) {
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
        mReleasedEncodersCount = 0;

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
            mPossibleEndReason = sizeMaxDuration < maxDuration ? END_BY_MAX_SIZE : END_BY_MAX_DURATION;
            finalMaxDuration = Math.min(sizeMaxDuration, maxDuration);
        } else if (maxSize > 0) {
            mPossibleEndReason = END_BY_MAX_SIZE;
            finalMaxDuration = sizeMaxDuration;
        } else if (maxDuration > 0) {
            mPossibleEndReason = END_BY_MAX_DURATION;
            finalMaxDuration = maxDuration;
        }
        LOG.w("Computed a max duration of", (finalMaxDuration / 1000F));
        for (MediaEncoder encoder : mEncoders) {
            encoder.prepare(mController, finalMaxDuration);
        }
    }

    /**
     * Asks encoders to start (each one on its own track).
     */
    public final void start() {
        LOG.i("Passing event to encoders:", "START");
        for (MediaEncoder encoder : mEncoders) {
            encoder.start();
        }
    }

    /**
     * Notifies encoders of some event with the given payload.
     * Can be used for example to notify the video encoder of new frame available.
     * @param event an event string
     * @param data an event payload
     */
    @SuppressWarnings("SameParameterValue")
    public final void notify(final String event, final Object data) {
        LOG.i("Passing event to encoders:", event);
        for (MediaEncoder encoder : mEncoders) {
            encoder.notify(event, data);
        }
    }

    /**
     * Asks encoders to stop. This is not sync, of course we will ask for encoders
     * to call {@link Controller#notifyStopped(int)} before actually stop the muxer.
     * When all encoders request a release, {@link #end()} is called to do cleanup
     * and notify the listener.
     */
    public final void stop() {
        LOG.i("Passing event to encoders:", "STOP");
        for (MediaEncoder encoder : mEncoders) {
            encoder.stop();
        }
    }

    /**
     * Called after all encoders have requested a release using {@link Controller#notifyStopped(int)}.
     * At this point we will do cleanup and notify the listener.
     */
    private void end() {
        LOG.i("end:", "Releasing muxer after all encoders have been released.");
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
        LOG.w("end:", "Dispatching end to listener - reason:", mEndReason, "error:", error);
        if (mListener != null) {
            mListener.onEncodingEnd(mEndReason, error);
            mListener = null;
        }
        mEndReason = END_BY_USER;
        mStartedEncodersCount = 0;
        mReleasedEncodersCount = 0;
        mMediaMuxerStarted = false;
        LOG.i("end:", "Completed.");
    }

    /**
     * Returns the current video encoder.
     * @return the current video encoder
     */
    @NonNull
    public VideoMediaEncoder getVideoEncoder() {
        return (VideoMediaEncoder) mEncoders.get(0);
    }

    /**
     * Returns the current audio encoder.
     * @return the current audio encoder
     */
    @SuppressWarnings("unused")
    @Nullable
    public AudioMediaEncoder getAudioEncoder() {
        if (mEncoders.size() > 1) {
            return (AudioMediaEncoder) mEncoders.get(1);
        } else {
            return null;
        }
    }

    /**
     * A handle for {@link MediaEncoder}s to pass information to this engine.
     * All methods here can be called for multiple threads.
     */
    @SuppressWarnings("WeakerAccess")
    public class Controller {

        /**
         * Request that the muxer should start. This is not guaranteed to be executed:
         * we wait for all encoders to call this method, and only then, start the muxer.
         * @param format the media format
         * @return the encoder track index
         */
        public int notifyStarted(@NonNull MediaFormat format) {
            synchronized (mControllerLock) {
                if (mMediaMuxerStarted) {
                    throw new IllegalStateException("Trying to start but muxer started already");
                }
                int track = mMediaMuxer.addTrack(format);
                LOG.w("notifyStarted:", "Assigned track", track, "to format", format.getString(MediaFormat.KEY_MIME));
                if (++mStartedEncodersCount == mEncoders.size()) {
                    LOG.w("notifyStarted:", "All encoders have started. Starting muxer and dispatching onEncodingStart().");
                    mMediaMuxer.start();
                    mMediaMuxerStarted = true;
                    if (mListener != null) {
                        mListener.onEncodingStart();
                    }
                }
                return track;
            }
        }

        /**
         * Whether the muxer is started. MediaEncoders are required to avoid
         * calling {@link #write(OutputBufferPool, OutputBuffer)} until this method returns true.
         *
         * @return true if muxer was started
         */
        public boolean isStarted() {
            synchronized (mControllerLock) {
                return mMediaMuxerStarted;
            }
        }

        @SuppressLint("UseSparseArrays")
        private Map<Integer, Integer> mDebugCount = new HashMap<>();

        /**
         * Writes the given data to the muxer. Should be called after {@link #isStarted()}
         * returns true. Note: this seems to be thread safe, no lock.
         */
        public void write(@NonNull OutputBufferPool pool, @NonNull OutputBuffer buffer) {
            if (!mMediaMuxerStarted) {
                throw new IllegalStateException("Trying to write before muxer started");
            }

            if (DEBUG_PERFORMANCE) {
                // When AUDIO = mono, this is called about twice the time. (200 vs 100 for 5 sec).
                Integer count = mDebugCount.get(buffer.trackIndex);
                mDebugCount.put(buffer.trackIndex, count == null ? 1 : ++count);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(buffer.info.presentationTimeUs / 1000);
                LOG.v("write:", "Writing into muxer -",
                                "track:", buffer.trackIndex,
                        "presentation:", buffer.info.presentationTimeUs,
                        "readable:", calendar.get(Calendar.SECOND) + ":" + calendar.get(Calendar.MILLISECOND),
                        "count:", count);
            } else {
                LOG.v("write:", "Writing into muxer -",
                        "track:", buffer.trackIndex,
                        "presentation:", buffer.info.presentationTimeUs);
            }

            mMediaMuxer.writeSampleData(buffer.trackIndex, buffer.data, buffer.info);
            pool.recycle(buffer);
        }

        /**
         * Requests that the engine stops. This is not executed until all encoders call
         * this method, so it is a kind of soft request, just like {@link #notifyStarted(MediaFormat)}.
         * To be used when maxLength / maxSize constraints are reached, for example.
         *
         * When this succeeds, {@link MediaEncoder#stop()} is called.
         */
        public void requestStop(int track) {
            synchronized (mControllerLock) {
                LOG.w("requestStop:", "Called for track", track);
                if (--mStartedEncodersCount == 0) {
                    LOG.w("requestStop:", "All encoders have requested a stop. Stopping them.");
                    mEndReason = mPossibleEndReason;
                    stop();
                }
            }
        }

        /**
         * Notifies that the encoder was stopped. After this is called by all encoders,
         * we will actually stop the muxer.
         */
        public void notifyStopped(int track) {
            synchronized (mControllerLock) {
                LOG.w("notifyStopped:", "Called for track", track);
                if (++mReleasedEncodersCount == mEncoders.size()) {
                    LOG.w("requestStop:", "All encoders have been released. Stopping the muxer.");
                    end();
                }
            }
        }
    }
}
