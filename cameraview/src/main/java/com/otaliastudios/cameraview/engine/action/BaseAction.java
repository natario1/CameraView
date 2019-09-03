package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseAction implements Action {

    private final List<ActionCallback> callbacks = new ArrayList<>();
    private int state;
    private ActionHolder holder;

    @Override
    public int getState() {
        return state;
    }

    @Override
    public final void start(@NonNull ActionHolder holder) {
        this.holder = holder;
        holder.addAction(this);
        onStart(holder);
    }

    protected void onStart(@NonNull ActionHolder holder) {
        // Overrideable
    }

    @Override
    public void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request) {
        // Overrideable
    }

    @Override
    public void onCaptureProgressed(@NonNull ActionHolder holder, @NonNull CaptureRequest request, @NonNull CaptureResult result) {
        // Overrideable
    }

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        // Overrideable
    }

    protected void setState(int newState) {
        if (newState != state) {
            state = newState;
            for (ActionCallback callback : callbacks) {
                callback.onActionStateChanged(this, state);
            }
            if (state == STATE_COMPLETED) {
                holder.removeAction(this);
            }
        }
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
