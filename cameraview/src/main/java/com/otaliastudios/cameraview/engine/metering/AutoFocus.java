package com.otaliastudios.cameraview.engine.metering;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;

import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class AutoFocus extends Parameter {

    private static final String TAG = AutoFocus.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG + "Metering");

    public AutoFocus(@NonNull MeteringChangeCallback callback) {
        super(callback);
    }

    @Override
    protected boolean checkSupportsProcessing(@NonNull CameraCharacteristics characteristics,
                                              @NonNull CaptureRequest.Builder builder) {
        // Exclude OFF and EDOF as per docs.
        Integer afMode = builder.get(CaptureRequest.CONTROL_AF_MODE);
        boolean result = afMode != null &&
                (afMode == CameraCharacteristics.CONTROL_AF_MODE_AUTO
                        || afMode == CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        || afMode == CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                        || afMode == CameraCharacteristics.CONTROL_AF_MODE_MACRO);
        LOG.i("checkSupportsProcessing:", result);
        return result;
    }

    @Override
    protected boolean checkShouldSkip(@NonNull CaptureResult lastResult) {
        Integer afState = lastResult.get(CaptureResult.CONTROL_AF_STATE);
        boolean result = afState != null &&
                (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED);
        LOG.i("checkShouldSkip:", result);
        return result;
    }

    @Override
    protected void onStartMetering(@NonNull CameraCharacteristics characteristics,
                                   @NonNull CaptureRequest.Builder builder,
                                   @NonNull List<MeteringRectangle> areas,
                                   boolean supportsProcessing) {
        boolean changed = false;
        if (supportsProcessing) {
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            changed = true;
        }

        // Even if auto is not supported, change the regions anyway.
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 0);
        if (!areas.isEmpty() && maxRegions > 0) {
            int max = Math.min(maxRegions, areas.size());
            builder.set(CaptureRequest.CONTROL_AF_REGIONS,
                    areas.subList(0, max).toArray(new MeteringRectangle[]{}));
            changed = true;
        }

        if (changed) {
            notifyBuilderChanged();
            // Remove any problematic control for future requests
            /* builder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_IDLE); */
        }
    }

    @Override
    public void processCapture(@NonNull CaptureResult result) {
        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
        LOG.i("onCapture:", "afState:", afState);
        if (afState == null) return;
        switch (afState) {
            case CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED: {
                isMetered = true;
                isSuccessful = true;
                break;
            }
            case CaptureRequest.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED: {
                isMetered = true;
                isSuccessful = false;
                break;
            }
            case CaptureRequest.CONTROL_AF_STATE_INACTIVE: break;
            case CaptureRequest.CONTROL_AF_STATE_ACTIVE_SCAN: break;
            default: break;
        }
    }

    @Override
    protected void onMetered(@NonNull CaptureRequest.Builder builder) {
        // Do nothing.
    }

    @Override
    protected void onResetMetering(@NonNull CameraCharacteristics characteristics,
                                   @NonNull CaptureRequest.Builder builder,
                                   @Nullable MeteringRectangle area,
                                   boolean supportsProcessing) {
        boolean changed = false;
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 0);
        if (area != null && maxRegions > 0) {
            builder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{area});
            changed = true;
        }

        if (supportsProcessing) { // Cleanup any trigger.
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            changed = true;
        }
        if (changed) {
            notifyBuilderChanged();
        }
    }
}
