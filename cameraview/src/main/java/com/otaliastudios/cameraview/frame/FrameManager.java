package com.otaliastudios.cameraview.frame;


import android.graphics.ImageFormat;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.size.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class manages the allocation of {@link Frame} objects.
 * The FrameManager keeps a {@link #mPoolSize} integer that defines the number of instances to keep.
 *
 * Main methods are:
 * - {@link #setUp(int, Size)}: to set up with size and allocate buffers
 * - {@link #release()}: to release. After release, a manager can be setUp again.
 * - {@link #getFrame(Object, long, int)}: gets a new {@link Frame}.
 *
 * For frames to get back to the FrameManager pool, all you have to do
 * is call {@link Frame#release()} when done.
 */
public abstract class FrameManager<T> {

    private static final String TAG = FrameManager.class.getSimpleName();
    protected static final CameraLogger LOG = CameraLogger.create(TAG);

    private final int mPoolSize;
    private int mFrameBytes = -1;
    private Size mFrameSize = null;
    private int mFrameFormat = -1;
    private final Class<T> mFrameDataClass;
    private LinkedBlockingQueue<Frame> mFrameQueue;


    /**
     * Construct a new frame manager.
     * The construction must be followed by an {@link #setUp(int, Size)} call
     * as soon as the parameters are known.
     *
     * @param poolSize the size of the backing pool.
     */
    protected FrameManager(int poolSize, @NonNull Class<T> dataClass) {
        mPoolSize = poolSize;
        mFrameDataClass = dataClass;
        mFrameQueue = new LinkedBlockingQueue<>(mPoolSize);
    }

    /**
     * Returns the pool size.
     * @return pool size
     */
    @SuppressWarnings("WeakerAccess")
    public final int getPoolSize() {
        return mPoolSize;
    }

    /**
     * Returns the frame size in bytes.
     * @return frame size in bytes
     */
    @SuppressWarnings("WeakerAccess")
    public final int getFrameBytes() {
        return mFrameBytes;
    }

    /**
     * Allocates a {@link #mPoolSize} number of buffers. Should be called once
     * the preview size and the image format value are known.
     *
     * This method can be called again after {@link #release()} has been called.
     *
     * @param format the image format
     * @param size the frame size
     */
    public void setUp(int format, @NonNull Size size) {
        if (isSetUp()) {
            // TODO throw or just reconfigure?
        }
        mFrameSize = size;
        mFrameFormat = format;
        int bitsPerPixel = ImageFormat.getBitsPerPixel(format);
        long sizeInBits = size.getHeight() * size.getWidth() * bitsPerPixel;
        mFrameBytes = (int) Math.ceil(sizeInBits / 8.0d);
    }

    /**
     * Returns true after {@link #setUp(int, Size)}
     * but before {@link #release()}.
     * Returns false otherwise.
     *
     * @return true if set up
     */
    protected boolean isSetUp() {
        return mFrameSize != null;
    }

    /**
     * Returns a new Frame for the given data. This must be called
     * - after {@link #setUp(int, Size)}, which sets the buffer size
     * - after the byte buffer given by setUp() has been filled.
     *   If this is called X times in a row without releasing frames, it will allocate
     *   X frames and that's bad. Callers must wait for the preview buffer to be available.
     *
     * In Camera1, this is always respected thanks to its internals.
     *
     * @param data data
     * @param time timestamp
     * @param rotation rotation
     * @return a new frame
     */
    @NonNull
    public Frame getFrame(@NonNull T data, long time, int rotation) {
        if (!isSetUp()) {
            throw new IllegalStateException("Can't call getFrame() after releasing " +
                    "or before setUp.");
        }

        Frame frame = mFrameQueue.poll();
        if (frame != null) {
            LOG.v("getFrame for time:", time, "RECYCLING.");
        } else {
            LOG.v("getFrame for time:", time, "CREATING.");
            frame = new Frame(this, mFrameDataClass);
        }
        frame.setContent(data, time, rotation, mFrameSize, mFrameFormat);
        return frame;
    }

    /**
     * Called by child frames when they are released.
     * @param frame the released frame
     */
    void onFrameReleased(@NonNull Frame frame, @NonNull T data) {
        if (!isSetUp()) return;
        // If frame queue is full, let's drop everything.
        // If frame queue accepts this frame, let's recycle the buffer as well.
        if (mFrameQueue.offer(frame)) {
            onFrameDataRecycled(data);
        }
    }

    /**
     * Called when a Frame was recycled and its data is now available.
     * This might be called from old Frames that belong to an old 'setUp'
     * of this FrameManager instance. So the buffer size might be different,
     * for instance.
     * @param data data
     */
    protected abstract void onFrameDataRecycled(@NonNull T data);

    /**
     * Releases all frames controlled by this manager and
     * clears the pool.
     */
    public void release() {
        if (!isSetUp()) {
            LOG.w("release called twice. Ignoring.");
            return;
        }

        LOG.i("release: Clearing the frame and buffer queue.");
        mFrameQueue.clear();
        mFrameBytes = -1;
        mFrameSize = null;
        mFrameFormat = -1;
    }
}
