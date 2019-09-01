package com.otaliastudios.cameraview.engine.locking;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class Parameter {

    public interface LockingChangeCallback {
        void onLockingChange();
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean isSuccessful;

    @SuppressWarnings("WeakerAccess")
    protected boolean isLocked;

    private LockingChangeCallback callback;
    private boolean shouldSkip;
    private boolean supportsLocking;

    @SuppressWarnings("WeakerAccess")
    protected Parameter(@NonNull LockingChangeCallback callback) {
        this.callback = callback;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected <T> T readCharacteristic(@NonNull CameraCharacteristics characteristics,
                                       @NonNull CameraCharacteristics.Key<T> key,
                                       @NonNull T fallback) {
        T value = characteristics.get(key);
        return value == null ? fallback : value;
    }

    @SuppressWarnings("WeakerAccess")
    protected void notifyBuilderChanged() {
        callback.onLockingChange();
    }

    public final boolean isLocked() {
        // A non supported parameter should always appear as metered
        return isLocked || !supportsLocking || shouldSkip;
    }

    public final boolean isSuccessful() {
        // A non supported parameter should always appear as successful
        return isSuccessful || !supportsLocking || shouldSkip;
    }

    public final void lock(@NonNull CameraCharacteristics characteristics,
                           @NonNull CaptureRequest.Builder builder,
                           @NonNull CaptureResult lastResult) {
        isSuccessful = false;
        isLocked = false;
        shouldSkip = checkShouldSkip(lastResult);
        supportsLocking = checkSupportsLocking(characteristics, builder);
        if (!shouldSkip && supportsLocking) {
            onLock(characteristics, builder);
        }
    }

    public final void onCapture(@NonNull CaptureRequest.Builder builder,
                                @NonNull CaptureResult result) {
        if (isLocked()) return;
        processCapture(result);
        if (isLocked()) onLocked(builder);
    }

    public final void unlock(@NonNull CameraCharacteristics characteristics,
                             @NonNull CaptureRequest.Builder builder) {
        if (supportsLocking) {
            // Not checking shouldSkip. Though we skipped locking, we're
            // now asked to unlock, so do it.
            onUnlock(characteristics, builder);
        }
    }

    protected abstract boolean checkSupportsLocking(@NonNull CameraCharacteristics characteristics,
                                                    @NonNull CaptureRequest.Builder builder);

    protected abstract boolean checkShouldSkip(@NonNull CaptureResult lastResult);

    protected abstract void onLock(@NonNull CameraCharacteristics characteristics,
                                   @NonNull CaptureRequest.Builder builder);

    protected abstract void processCapture(@NonNull CaptureResult result);

    protected abstract void onLocked(@NonNull CaptureRequest.Builder builder);

    protected abstract void onUnlock(@NonNull CameraCharacteristics characteristics,
                                     @NonNull CaptureRequest.Builder builder);
}
