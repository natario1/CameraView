package com.otaliastudios.cameraview.video.encoding;

import android.util.Log;

/**
 * Computes timestamps for audio frames.
 * Video frames do not need this since the timestamp comes from
 * the surface texture.
 *
 * This is independent from the channels count, as long as the read bytes include
 * all channels and the byte rate accounts for this as well.
 * If channels is 2, both values will be doubled and we behave the same.
 *
 * This class keeps track of gaps between frames.
 * This can be used, for example, to write zeros instead of nothing.
 */
class AudioTimestamp {

    static long bytesToUs(long bytes, int byteRate) {
        return (1000000L * bytes) / byteRate;
    }

    static long bytesToMillis(long bytes, int byteRate) {
        return (1000L * bytes) / byteRate;
    }

    private int mByteRate;
    private long mBaseTimeUs;
    private long mBytesSinceBaseTime;
    private long mGapUs;

    AudioTimestamp(int byteRate) {
        mByteRate = byteRate;
    }

    /**
     * This method accounts for the current time and proved to be the most reliable among
     * the ones tested.
     *
     * This creates regular timestamps unless we accumulate a lot of delay (greater than
     * twice the buffer duration), in which case it creates a gap and starts again trying
     * to be regular from the new point.
     *
     * Returns timestamps in the {@link System#nanoTime()} reference.
     */
    @SuppressWarnings("SameParameterValue")
    long increaseUs(int readBytes) {
        long bufferDurationUs = bytesToUs((long) readBytes, mByteRate);
        long bufferEndTimeUs = System.nanoTime() / 1000; // now
        long bufferStartTimeUs = bufferEndTimeUs - bufferDurationUs;

        // If this is the first time, the base time is the buffer start time.
        if (mBytesSinceBaseTime == 0) mBaseTimeUs = bufferStartTimeUs;

        // Recompute time assuming that we are respecting the sampling frequency.
        // This puts the time at the end of last read buffer, which means, where we
        // should be if we had no delay / missed buffers.
        long correctedTimeUs = mBaseTimeUs + bytesToUs(mBytesSinceBaseTime, mByteRate);
        long correctionUs = bufferStartTimeUs - correctedTimeUs;

        if (correctionUs >= 2L * bufferDurationUs) {
            // However, if the correction is too big (> 2*bufferDurationUs), reset to this point.
            // This is triggered if we lose buffers and are recording/encoding at a slower rate.
            mBaseTimeUs = bufferStartTimeUs;
            mBytesSinceBaseTime = readBytes;
            mGapUs = correctionUs;
            return mBaseTimeUs;
        } else {
            //noinspection StatementWithEmptyBody
            if (correctionUs < 0) {
                // This means that this method is being called too often, so that the expected start
                // time for this buffer is BEFORE the last buffer end. So, respect the last buffer
                // end instead.
            }
            mGapUs = 0;
            mBytesSinceBaseTime += readBytes;
            return correctedTimeUs;
        }
    }

    /**
     * Returns the number of gaps (meaning, missing frames) assuming that each
     * frame has frameBytes size. Possibly 0.
     *
     * @param frameBytes size of standard frame
     * @return number of gaps
     */
    int getGapCount(int frameBytes) {
        if (mGapUs == 0) return 0;
        long durationUs = bytesToUs((long) frameBytes, mByteRate);
        return (int) (mGapUs / durationUs);
    }

    /**
     * Returns the timestamp of the first missing frame.
     * Should be called only after {@link #getGapCount(int)} returns something
     * greater than zero.
     *
     * @param lastTimeUs the last real frame timestamp
     * @return the first missing frame timestamp
     */
    long getGapStartUs(long lastTimeUs) {
        return lastTimeUs - mGapUs;
    }
}
