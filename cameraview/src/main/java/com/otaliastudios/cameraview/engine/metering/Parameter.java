package com.otaliastudios.cameraview.engine.metering;

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

    public interface MeteringChangeCallback {
        void onMeteringChange();
    }

    private boolean isSuccessful;
    private boolean isMetered;
    private MeteringChangeCallback callback;
    private boolean shouldSkip;
    private boolean supportsProcessing;

    protected Parameter(@NonNull MeteringChangeCallback callback) {
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
        callback.onMeteringChange();
    }

    @SuppressWarnings("WeakerAccess")
    protected void notifyMetered(boolean success) {
        isMetered = true;
        isSuccessful = success;
    }

    public final boolean isMetered() {
        // A non supported parameter should always appear as metered
        return isMetered || !supportsProcessing || shouldSkip;
    }

    public final boolean isSuccessful() {
        // A non supported parameter should always appear as successful
        return isSuccessful || !supportsProcessing || shouldSkip;
    }

    public final void startMetering(@NonNull CameraCharacteristics characteristics,
                                    @NonNull CaptureRequest.Builder builder,
                                    @NonNull List<MeteringRectangle> areas,
                                    @NonNull CaptureResult lastResult,
                                    boolean skipIfPossible) {
        isSuccessful = false;
        isMetered = false;
        shouldSkip = skipIfPossible && checkShouldSkip(lastResult);
        supportsProcessing = checkSupportsProcessing(characteristics, builder);
        if (!shouldSkip) {
            onStartMetering(characteristics, builder, areas, supportsProcessing);
        }
    }

    public final void onCapture(@NonNull CaptureRequest.Builder builder,
                                @NonNull CaptureResult result) {
        if (!isMetered()) {
            processCapture(result);
            if (isMetered()) onMetered(builder, isSuccessful);
        }
    }

    public final void resetMetering(@NonNull CameraCharacteristics characteristics,
                                    @NonNull CaptureRequest.Builder builder,
                                    @Nullable MeteringRectangle area) {
        onResetMetering(characteristics, builder, area, supportsProcessing);
    }

    protected abstract boolean checkSupportsProcessing(@NonNull CameraCharacteristics characteristics,
                                                       @NonNull CaptureRequest.Builder builder);

    protected abstract boolean checkShouldSkip(@NonNull CaptureResult lastResult);

    protected abstract void onStartMetering(@NonNull CameraCharacteristics characteristics,
                                            @NonNull CaptureRequest.Builder builder,
                                            @NonNull List<MeteringRectangle> areas,
                                            boolean supportsProcessing);

    protected abstract void processCapture(@NonNull CaptureResult result);

    protected abstract void onMetered(@NonNull CaptureRequest.Builder builder, boolean success);

    protected abstract void onResetMetering(@NonNull CameraCharacteristics characteristics,
                                            @NonNull CaptureRequest.Builder builder,
                                            @Nullable MeteringRectangle area,
                                            boolean supportsProcessing);
}