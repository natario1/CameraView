package com.otaliastudios.cameraview.frame;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.otaliastudios.cameraview.engine.offset.Angles;
import com.otaliastudios.cameraview.size.Size;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class manages the allocation of byte buffers and {@link Frame} objects.
 * We are interested in recycling both of them, especially byte[] buffers which can create a lot
 * of overhead.
 *
 * The pool size applies to both the {@link Frame} pool and the byte[] pool - it makes sense to use
 * the same number since they are consumed at the same time.
 *
 * We can work in two modes, depending on whether a
 * {@link BufferCallback} is passed to the constructor. The modes changes the buffer behavior.
 *
 * 1. {@link #BUFFER_MODE_DISPATCH}: in this mode, as soon as we have a buffer, it is dispatched to
 *    the {@link BufferCallback}. The callback should then fill the buffer, and finally call
 *    {@link FrameManager#getFrame(Object, long)} to receive a frame.
 *    This is used for Camera1.
 *
 * 2. {@link #BUFFER_MODE_ENQUEUE}: in this mode, the manager internally keeps a queue of byte
 *    buffers, instead of handing them to the callback. The users can ask for buffers through
 *    {@link #getBuffer()}.
 *    This buffer can be filled with data and used to get a frame
 *    {@link FrameManager#getFrame(Object, long)}, or, in case it was not filled, returned to
 *    the queue using {@link #onBufferUnused(byte[])}.
 *    This is used for Camera2.
 */
public class ByteBufferFrameManager extends FrameManager<byte[]> {

    /**
     * Receives callbacks on buffer availability
     * (when a Frame is released, we reuse its buffer).
     */
    public interface BufferCallback {
        void onBufferAvailable(@NonNull byte[] buffer);
    }

    /**
     * In this mode, we have a {@link #mBufferCallback} and dispatch
     * new buffers to the callback.
     */
    private final static int BUFFER_MODE_DISPATCH = 0;

    /**
     * In this mode, we have a {@link #mBufferQueue} where we store
     * buffers and only dispatch when requested.
     */
    private final static int BUFFER_MODE_ENQUEUE = 1;

    private LinkedBlockingQueue<byte[]> mBufferQueue;
    private BufferCallback mBufferCallback;
    private final int mBufferMode;

    /**
     * Construct a new frame manager.
     * The construction must be followed by an {@link FrameManager#setUp(int, Size, Angles)} call
     * as soon as the parameters are known.
     *
     * @param poolSize the size of the backing pool.
     * @param callback a callback
     */
    public ByteBufferFrameManager(int poolSize, @Nullable BufferCallback callback) {
        super(poolSize, byte[].class);
        if (callback != null) {
            mBufferCallback = callback;
            mBufferMode = BUFFER_MODE_DISPATCH;
        } else {
            mBufferQueue = new LinkedBlockingQueue<>(poolSize);
            mBufferMode = BUFFER_MODE_ENQUEUE;
        }
    }


    @Override
    public void setUp(int format, @NonNull Size size, @NonNull Angles angles) {
        super.setUp(format, size, angles);
        int bytes = getFrameBytes();
        for (int i = 0; i < getPoolSize(); i++) {
            if (mBufferMode == BUFFER_MODE_DISPATCH) {
                mBufferCallback.onBufferAvailable(new byte[bytes]);
            } else {
                mBufferQueue.offer(new byte[bytes]);
            }
        }
    }

    /**
     * Returns a new byte buffer than can be filled.
     * This can only be called in {@link #BUFFER_MODE_ENQUEUE} mode! Where the frame
     * manager also holds a queue of the byte buffers.
     *
     * If not null, the buffer returned by this method can be filled and used to get
     * a new frame through {@link FrameManager#getFrame(Object, long)}.
     *
     * @return a buffer, or null
     */
    @Nullable
    public byte[] getBuffer() {
        if (mBufferMode != BUFFER_MODE_ENQUEUE) {
            throw new IllegalStateException("Can't call getBuffer() " +
                    "when not in BUFFER_MODE_ENQUEUE.");
        }
        return mBufferQueue.poll();
    }

    /**
     * Can be called if the buffer obtained by {@link #getBuffer()}
     * was not used to construct a frame, so it can be put back into the queue.
     * @param buffer a buffer
     */
    public void onBufferUnused(@NonNull byte[] buffer) {
        if (mBufferMode != BUFFER_MODE_ENQUEUE) {
            throw new IllegalStateException("Can't call onBufferUnused() " +
                    "when not in BUFFER_MODE_ENQUEUE.");
        }

        if (isSetUp()) {
            mBufferQueue.offer(buffer);
        } else {
            LOG.w("onBufferUnused: buffer was returned but we're not set up anymore.");
        }
    }

    @Override
    protected void onFrameDataReleased(@NonNull byte[] data, boolean recycled) {
        if (recycled && data.length == getFrameBytes()) {
            if (mBufferMode == BUFFER_MODE_DISPATCH) {
                mBufferCallback.onBufferAvailable(data);
            } else {
                mBufferQueue.offer(data);
            }
        }
    }

    @NonNull
    @Override
    protected byte[] onCloneFrameData(@NonNull byte[] data) {
        byte[] clone = new byte[data.length];
        System.arraycopy(data, 0, clone, 0, data.length);
        return clone;
    }

    /**
     * Releases all frames controlled by this manager and
     * clears the pool.
     * In BUFFER_MODE_ENQUEUE, releases also all the buffers.
     */
    @Override
    public void release() {
        super.release();
        if (mBufferMode == BUFFER_MODE_ENQUEUE) {
            mBufferQueue.clear();
        }
    }
}
