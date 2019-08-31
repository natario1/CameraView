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
public class AutoWhiteBalance extends MeteringParameter {

    private static final String TAG = AutoWhiteBalance.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    @Override
    protected boolean checkSupportsProcessing(@NonNull CameraCharacteristics characteristics,
                                              @NonNull CaptureRequest.Builder builder) {
        boolean isNotLegacy = readCharacteristic(characteristics,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1) !=
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        Integer awbMode = builder.get(CaptureRequest.CONTROL_AWB_MODE);
        boolean result = isNotLegacy && awbMode != null && awbMode == CaptureRequest.CONTROL_AWB_MODE_AUTO;
        LOG.i("checkSupportsProcessing:", result);
        return result;
    }

    @Override
    protected boolean checkShouldSkip(@NonNull CaptureResult lastResult) {
        Integer awbState = lastResult.get(CaptureResult.CONTROL_AWB_STATE);
        boolean result = awbState != null && awbState == CaptureRequest.CONTROL_AWB_STATE_CONVERGED;
        LOG.i("checkShouldSkip:", result);
        return result;
    }

    @Override
    protected void onStartMetering(@NonNull CameraCharacteristics characteristics,
                                   @NonNull CaptureRequest.Builder builder,
                                   @NonNull List<MeteringRectangle> areas,
                                   boolean supportsProcessing) {
        if (supportsProcessing) {
            // Remove any lock. This would make processing be stuck into the process method.
            builder.set(CaptureRequest.CONTROL_AWB_LOCK, false);
        }

        // Even if auto is not supported, change the regions anyway.
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 0);
        if (!areas.isEmpty() && maxRegions > 0) {
            int max = Math.min(maxRegions, areas.size());
            builder.set(CaptureRequest.CONTROL_AWB_REGIONS,
                    areas.subList(0, max).toArray(new MeteringRectangle[]{}));
        }
    }

    @Override
    public void processCapture(@NonNull CaptureResult result) {
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
    protected void onMetered(@NonNull CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AWB_LOCK, true);
    }

    @Override
    protected void onResetMetering(@NonNull CameraCharacteristics characteristics,
                                   @NonNull CaptureRequest.Builder builder,
                                   @Nullable MeteringRectangle area,
                                   boolean supportsProcessing) {
        if (supportsProcessing) {
            builder.set(CaptureRequest.CONTROL_AWB_LOCK, false);
        }
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 0);
        if (area != null && maxRegions > 0) {
            builder.set(CaptureRequest.CONTROL_AWB_REGIONS, new MeteringRectangle[]{area});
        }
    }
}
