package com.otaliastudios.cameraview;

import android.support.annotation.NonNull;

/**
 * A preview frame to be processed by {@link FrameProcessor}s.
 */
public class Frame {

    /* for tests */ FrameManager mManager;

    private byte[] mData = null;
    private long mTime = -1;
    private int mRotation = 0;
    private Size mSize = null;
    private int mFormat = -1;

    Frame(@NonNull FrameManager manager) {
        mManager = manager;
    }

    void set(byte[] data, long time, int rotation, Size size, int format) {
        this.mData = data;
        this.mTime = time;
        this.mRotation = rotation;
        this.mSize = size;
        this.mFormat = format;
    }

    @Override
    public boolean equals(Object obj) {
        // We want a super fast implementation here, do not compare arrays.
        return obj instanceof Frame && ((Frame) obj).mTime == mTime;
    }

    /**
     * Clones the frame, returning a frozen content that will not be overwritten.
     * This can be kept or safely passed to other threads.
     * Using freeze without clearing with {@link #release()} can result in memory leaks.
     *
     * @return a frozen Frame
     */
    public Frame freeze() {
        byte[] data = new byte[mData.length];
        System.arraycopy(mData, 0, data, 0, mData.length);
        Frame other = new Frame(mManager);
        other.set(data, mTime, mRotation, mSize, mFormat);
        return other;
    }

    /**
     * Disposes the contents of this frame. Can be useful for frozen frames
     * that are not useful anymore.
     */
    public void release() {
        if (mManager != null) {
            // If needed, the manager will call releaseManager on us.
            mManager.onFrameReleased(this);
        }

        mData = null;
        mRotation = 0;
        mTime = -1;
        mSize = null;
        mFormat = -1;
    }

    // Once this is called, this instance is not usable anymore.
    void releaseManager() {
        mManager = null;
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

    /**
     * Returns the frame size.
     *
     * @return frame size
     */
    public Size getSize() {
        return mSize;
    }

    /**
     * Returns the data format, in one of the
     * {@link android.graphics.ImageFormat} constants.
     * This will always be {@link android.graphics.ImageFormat#NV21} for now.
     *
     * @return the data format
     * @see android.graphics.ImageFormat
     */
    public int getFormat() {
        return mFormat;
    }
}
