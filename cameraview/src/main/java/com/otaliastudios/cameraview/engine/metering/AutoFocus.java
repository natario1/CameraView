package com.otaliastudios.cameraview.engine.metering;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;

import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class AutoFocus extends MeteringParameter {

    private static final String TAG = AutoFocus.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    @Override
    public void startMetering(@NonNull CameraCharacteristics characteristics,
                              @NonNull CaptureRequest.Builder builder,
                              @NonNull List<MeteringRectangle> areas) {
        isSuccessful = false;
        isMetered = false;

        Integer afMode = builder.get(CaptureRequest.CONTROL_AF_MODE);
        isSupported = afMode != null && afMode == CaptureRequest.CONTROL_AF_MODE_AUTO;
        if (isSupported) {
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        }

        // Even if auto is not supported, change the regions anyway.
        int maxRegions = readCharacteristic(characteristics, CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 0);
        if (maxRegions > 0) {
            int max = Math.min(maxRegions, areas.size());
            builder.set(CaptureRequest.CONTROL_AF_REGIONS,
                    areas.subList(0, max).toArray(new MeteringRectangle[]{}));
        }

    }

    @Override
    public void onCapture(@NonNull CaptureResult result) {
        if (isMetered || !isSupported) return;
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
    public void resetMetering(@NonNull CameraCharacteristics characteristics,
                              @NonNull CaptureRequest.Builder builder,
                              @NonNull MeteringRectangle area) {
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 0);
        if (maxRegions > 0) {
            builder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{area});
        }

        if (isSupported) { // Cleanup any trigger.
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        }
    }
}
