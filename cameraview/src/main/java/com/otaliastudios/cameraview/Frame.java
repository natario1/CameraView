package com.otaliastudios.cameraview;

/**
 * A preview frame to be processed by {@link FrameProcessor}s.
 */
public class Frame {

    private byte[] mData = null;
    private long mTime = -1;
    private int mRotation = 0;

    Frame() {}

    void set(byte[] data, long time, int rotation) {
        this.mData = data;
        this.mTime = time;
        this.mRotation = rotation;
    }

    @Override
    public boolean equals(Object obj) {
        // We want a super fast implementation here, do not compare arrays.
        return obj instanceof Frame && ((Frame) obj).mTime == mTime;
    }

    /**
     * Clones the frame, returning a frozen content that will not be overwritten.
     * This can be kept or safely passed to other threads.
     * Using freeze without clearing with {@link #clear()} can result in memory leaks.
     *
     * @return a frozen Frame
     */
    public Frame freeze() {
        byte[] data = new byte[mData.length];
        System.arraycopy(mData, 0, data, 0, mData.length);
        long time = mTime;
        int rotation = mRotation;
        Frame other = new Frame();
        other.set(data, time, rotation);
        return other;
    }

    /**
     * Disposes the contents of this frame. Can be useful for frozen frames
     * that are not useful anymore.
     */
    public void clear() {
        mData = null;
        mRotation = 0;
        mTime = -1;
    }

    /**
     * Returns the frame data.
     * @return the frame data
     */
    public byte[] getData() {
        return mData;
    }

    /**
     * Returns the milliseconds epoch for this frame,
     * in the {@link System#currentTimeMillis()} reference.
     *
     * @return time data
     */
    public long getTime() {
        return mTime;
    }

    /**
     * Returns the clock-wise rotation that should be applied on the data
     * array, such that the resulting frame matches what the user is seeing
     * on screen.
     *
     * @return clock-wise rotation
     */
    public int getRotation() {
        return mRotation;
    }
}
