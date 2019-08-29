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
public class AutoWhiteBalance extends MeteringParameter {

    private static final String TAG = AutoWhiteBalance.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    @Override
    public void startMetering(@NonNull CameraCharacteristics characteristics,
                              @NonNull CaptureRequest.Builder builder,
                              @NonNull List<MeteringRectangle> areas) {
        isSuccessful = false;
        isMetered = false;

        boolean isNotLegacy = readCharacteristic(characteristics,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1) !=
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        Integer awbMode = builder.get(CaptureRequest.CONTROL_AWB_MODE);
        isSupported = isNotLegacy && awbMode != null && awbMode == CaptureRequest.CONTROL_AWB_MODE_AUTO;

        if (isSupported) {
            // Remove any lock. We're not setting any, but just in case.
            builder.set(CaptureRequest.CONTROL_AWB_LOCK, false);
        }

        // Even if auto is not supported, change the regions anyway.
        int maxRegions = readCharacteristic(characteristics, CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 0);
        if (maxRegions > 0) {
            int max = Math.min(maxRegions, areas.size());
            builder.set(CaptureRequest.CONTROL_AWB_REGIONS,
                    areas.subList(0, max).toArray(new MeteringRectangle[]{}));
        }

    }

    @Override
    public void onCapture(@NonNull CaptureResult result) {
        if (isMetered || !isSupported) return;
        Integer awbState = result.get(CaptureResult.CONTROL_AWB_STATE);
        LOG.i("onCapture:", "awbState:", awbState);
        if (awbState == null) return;

        switch (awbState) {
            case CaptureRequest.CONTROL_AWB_STATE_CONVERGED: {
                isMetered = true;
                isSuccessful = true;
                break;
            }
            case CaptureRequest.CONTROL_AWB_STATE_LOCKED: break;
            case CaptureRequest.CONTROL_AWB_STATE_INACTIVE: break;
            case CaptureRequest.CONTROL_AWB_STATE_SEARCHING: break;
            default: break;
        }
    }

    @Override
    public void resetMetering(@NonNull CameraCharacteristics characteristics,
                              @NonNull CaptureRequest.Builder builder,
                              @NonNull MeteringRectangle area) {
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 0);
        if (maxRegions > 0) {
            builder.set(CaptureRequest.CONTROL_AWB_REGIONS, new MeteringRectangle[]{area});
        }
    }
}
