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
public class AutoExposure extends Parameter {

    private static final String TAG = AutoExposure.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private boolean isStarted;

    @Override
    protected boolean checkSupportsProcessing(@NonNull CameraCharacteristics characteristics,
                                              @NonNull CaptureRequest.Builder builder) {
        // In our case, this means checking if we support the AE precapture trigger.
        boolean isNotLegacy = readCharacteristic(characteristics,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1) !=
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        Integer aeMode = builder.get(CaptureRequest.CONTROL_AE_MODE);
        boolean isAEOn = aeMode != null &&
                (aeMode == CameraCharacteristics.CONTROL_AE_MODE_ON
                        || aeMode == CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                        || aeMode == CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH
                        || aeMode == CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE
                        || aeMode == 5 /* CameraCharacteristics.CONTROL_AE_MODE_ON_EXTERNAL_FLASH, API 28 */);
        boolean result = isNotLegacy && isAEOn;
        LOG.i("checkSupportsProcessing:", result);
        return result;
    }

    @Override
    protected boolean checkShouldSkip(@NonNull CaptureResult lastResult) {
        Integer aeState = lastResult.get(CaptureResult.CONTROL_AE_STATE);
        boolean result = aeState != null && aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED;
        LOG.i("checkShouldSkip:", result);
        return result;
    }

    @Override
    protected void onStartMetering(@NonNull CameraCharacteristics characteristics,
                                   @NonNull CaptureRequest.Builder builder,
                                   @NonNull List<MeteringRectangle> areas,
                                   boolean supportsProcessing) {
        isStarted = false;

        if (supportsProcessing) {
            // Remove any lock. This would make processing be stuck into the process method.
            builder.set(CaptureRequest.CONTROL_AE_LOCK, false);
            // Launch the precapture trigger.
            builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        }

        // Even if precapture is not supported, check the regions anyway.
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 0);
        if (!areas.isEmpty() && maxRegions > 0) {
            int max = Math.min(maxRegions, areas.size());
            builder.set(CaptureRequest.CONTROL_AE_REGIONS,
                    areas.subList(0, max).toArray(new MeteringRectangle[]{}));
        }
    }


    @Override
    public void processCapture(@NonNull CaptureResult result) {
        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
        LOG.i("onCapture:", "aeState:", aeState);
        if (aeState == null) return;

        if (!isStarted) {
            if (aeState == CaptureRequest.CONTROL_AE_STATE_PRECAPTURE) {
                isStarted = true;
            } else if (aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED
                    || aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED) {
                // PRECAPTURE is a transient state, so also check for the final states.
                isMetered = true;
                isSuccessful = true;
            }
        } else {
            if (aeState == CaptureRequest.CONTROL_AE_STATE_CONVERGED
                    || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                isMetered = true;
                isSuccessful = true;
            }
        }
    }

    @Override
    protected void onMetered(@NonNull CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AE_LOCK, true);
    }

    @Override
    protected void onResetMetering(@NonNull CameraCharacteristics characteristics,
                                   @NonNull CaptureRequest.Builder builder,
                                   @Nullable MeteringRectangle area,
                                   boolean supportsProcessing) {
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 0);
        if (area != null && maxRegions > 0) {
            builder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{area});
        }
        if (supportsProcessing) {
            builder.set(CaptureRequest.CONTROL_AE_LOCK, false);
            // Cleanup any precapture sequence.
            if (Build.VERSION.SDK_INT >= 23) {
                builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
            }
        }
    }
}
