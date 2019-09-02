package com.otaliastudios.cameraview.engine.locking;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class Parameter {

    public interface LockingChangeCallback {
        void onLockingChange();
    }

    private boolean isSuccessful;
    private boolean isLocked;
    private LockingChangeCallback callback;
    private boolean shouldSkip;
    private boolean supportsLocking;

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

    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    protected void notifyLocked(boolean success) {
        isLocked = true;
        isSuccessful = success;
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
        if (isLocked()) onLocked(builder, isSuccessful);
    }

    protected abstract boolean checkSupportsLocking(@NonNull CameraCharacteristics characteristics,
                                                    @NonNull CaptureRequest.Builder builder);

    protected abstract boolean checkShouldSkip(@NonNull CaptureResult lastResult);

    protected abstract void onLock(@NonNull CameraCharacteristics characteristics,
                                   @NonNull CaptureRequest.Builder builder);

    protected abstract void processCapture(@NonNull CaptureResult result);

    protected abstract void onLocked(@NonNull CaptureRequest.Builder builder, boolean success);
}
