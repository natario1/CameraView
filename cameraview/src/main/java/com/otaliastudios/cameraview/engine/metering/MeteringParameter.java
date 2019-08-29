package com.otaliastudios.cameraview.engine.metering;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class MeteringParameter {

    @SuppressWarnings("WeakerAccess")
    protected boolean isSupported;

    @SuppressWarnings("WeakerAccess")
    protected boolean isSuccessful;

    @SuppressWarnings("WeakerAccess")
    protected boolean isMetered;

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected <T> T readCharacteristic(@NonNull CameraCharacteristics characteristics,
                                       @NonNull CameraCharacteristics.Key<T> key,
                                       @NonNull T fallback) {
        T value = characteristics.get(key);
        return value == null ? fallback : value;
    }

    public final boolean isMetered() {
        return isMetered || !isSupported;
    }

    public final boolean isSuccessful() {
        return isSuccessful && isSupported;
    }

    public abstract void startMetering(@NonNull CameraCharacteristics characteristics,
                       @NonNull CaptureRequest.Builder builder,
                       @NonNull List<MeteringRectangle> areas);

    public abstract void resetMetering(@NonNull CameraCharacteristics characteristics,
                       @NonNull CaptureRequest.Builder builder,
                       @NonNull MeteringRectangle area);

    public abstract void onCapture(@NonNull CaptureResult result);
}
