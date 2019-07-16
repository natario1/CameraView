package com.otaliastudios.cameraview.video.encoding;

/**
 * Computes timestamps for audio frames.
 * Video frames do not need this since the timestamp comes from
 * the surface texture.
 */
class AudioTimestamp {

    static long bytesToUs(long bytes, int byteRate) {
        return (1000000L * bytes) / byteRate;
    }

    private long mBaseTimeUs;
    private long mBytesSinceBaseTime;

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
    long increaseUs(int readBytes, int byteRate) {
        long bufferDurationUs = bytesToUs((long) readBytes, byteRate);
        long bufferEndTimeUs = System.nanoTime() / 1000; // now
        long bufferStartTimeUs = bufferEndTimeUs - bufferDurationUs;

        // If this is the first time, the base time is the buffer start time.
        if (mBytesSinceBaseTime == 0) mBaseTimeUs = bufferStartTimeUs;

        // Recompute time assuming that we are respecting the sampling frequency.
        // This puts the time at the end of last read buffer, which means, where we
        // should be if we had no delay / missed buffers.
        long correctedTimeUs = mBaseTimeUs + bytesToUs(mBytesSinceBaseTime, byteRate);
        long correctionUs = bufferStartTimeUs - correctedTimeUs;

        // However, if the correction is too big (> 2*bufferDurationUs), reset to this point.
        // This is triggered if we lose buffers and are recording/encoding at a slower rate.
        if (correctionUs >= 2L * bufferDurationUs) {
            mBaseTimeUs = bufferStartTimeUs;
            mBytesSinceBaseTime = readBytes;
            return mBaseTimeUs;
        } else {
            mBytesSinceBaseTime += readBytes;
            return correctedTimeUs;
        }
    }
}
