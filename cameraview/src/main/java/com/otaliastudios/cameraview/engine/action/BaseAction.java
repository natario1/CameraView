package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * The base implementation of {@link Action} that should always be subclassed,
 * instead of implementing the root interface itself.
 *
 * It holds a list of callbacks and dispatches events to them, plus it cares about
 * its own lifecycle:
 * - when {@link #start(ActionHolder)} is called, we add ourselves to the holder list
 * - when {@link #STATE_COMPLETED} is reached, we remove ouverselves from the holder list
 *
 * This is very important in all cases.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class BaseAction implements Action {

    private final List<ActionCallback> callbacks = new ArrayList<>();
    private int state;
    private ActionHolder holder;
    private boolean needsOnStart;

    @Override
    public final int getState() {
        return state;
    }

    @Override
    public final void start(@NonNull ActionHolder holder) {
        this.holder = holder;
        holder.addAction(this);
        if (holder.getLastResult(this) != null) {
            onStart(holder);
        } else {
            needsOnStart = true;
        }
    }

    @Override
    public final void abort(@NonNull ActionHolder holder) {
        holder.removeAction(this);
        if (!isCompleted()) {
            onAbort(holder);
            setState(STATE_COMPLETED);
        }
        needsOnStart = false;
    }

    /**
     * Action was started and will soon receive events from the
     * holder stream.
     * @param holder holder
     */
    @CallSuper
    protected void onStart(@NonNull ActionHolder holder) {
        // Repeating holder assignment here (already in start()) because we NEED it in start()
        // but some special actions will not call start() at all for their children.
        this.holder = holder;
        // Overrideable
    }

    /**
     * Action was aborted and will not receive events from the
     * holder stream anymore. It will soon be marked as completed.
     * @param holder holder
     */
    @SuppressWarnings("unused")
    protected void onAbort(@NonNull ActionHolder holder) {
        // Overrideable
    }

    @CallSuper
    @Override
    public void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request) {
        if (needsOnStart) {
            onStart(holder);
            needsOnStart = false;
        }
    }

    @Override
    public void onCaptureProgressed(@NonNull ActionHolder holder,
                                    @NonNull CaptureRequest request,
                                    @NonNull CaptureResult result) {
        // Overrideable
    }

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        // Overrideable
    }

    /**
     * Called by subclasses to notify of their state. If state is {@link #STATE_COMPLETED},
     * this removes this action from the holder.
     * @param newState new state
     */
    protected final void setState(int newState) {
        if (newState != state) {
            state = newState;
            for (ActionCallback callback : callbacks) {
                callback.onActionStateChanged(this, state);
            }
            if (state == STATE_COMPLETED) {
                holder.removeAction(this);
                onCompleted(holder);
            }
        }
    }

    /**
     * Whether this action has reached the completed state.
     * @return true if completed
     */
    public boolean isCompleted() {
        return state == STATE_COMPLETED;
    }

    /**
     * Called when this action has completed (possibly aborted).
     * @param holder holder
     */
    protected void onCompleted(@NonNull ActionHolder holder) {
        // Overrideable
    }

    /**
     * Returns the holder.
     * @return the holder
     */
    @NonNull
    protected ActionHolder getHolder() {
        return holder;
    }


    /**
     * Reads a characteristic with a fallback.
     * @param key key
     * @param fallback fallback
     * @param <T> key type
     * @return value or fallback
     */
    @NonNull
    protected <T> T readCharacteristic(@NonNull CameraCharacteristics.Key<T> key,
                                       @NonNull T fallback) {
        T value = holder.getCharacteristics(this).get(key);
        return value == null ? fallback : value;
    }

    @Override
    public void addCallback(@NonNull ActionCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
            callback.onActionStateChanged(this, getState());
        }
    }

    @Override
    public void removeCallback(@NonNull ActionCallback callback) {
        callbacks.remove(callback);
    }
}
