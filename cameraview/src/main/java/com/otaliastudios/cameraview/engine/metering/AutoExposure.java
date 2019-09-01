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

    public AutoExposure(@NonNull MeteringChangeCallback callback) {
        super(callback);
    }

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

        boolean changed = false;
        if (supportsProcessing) {
            // Launch the precapture trigger.
            builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            changed = true;
        }

        // Even if precapture is not supported, check the regions anyway.
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 0);
        if (!areas.isEmpty() && maxRegions > 0) {
            int max = Math.min(maxRegions, areas.size());
            builder.set(CaptureRequest.CONTROL_AE_REGIONS,
                    areas.subList(0, max).toArray(new MeteringRectangle[]{}));
            changed = true;
        }

        // Notify change and remove any problematic control for future request
        if (changed) {
            notifyBuilderChanged();
            builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, null);
        }
    }


    @Override
    public void processCapture(@NonNull CaptureResult result) {
        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
        LOG.i("onCapture:", "aeState:", aeState);
        if (aeState == null) return;

        if (!isStarted) {
            switch (aeState) {
                case CaptureResult.CONTROL_AE_STATE_PRECAPTURE: {
                    isStarted = true;
                    break;
                }
                case CaptureResult.CONTROL_AE_STATE_CONVERGED:
                case CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED: {
                    // PRECAPTURE is a transient state, so also check for the final states.
                    isMetered = true;
                    isSuccessful = true;
                    break;
                }
                case CaptureResult.CONTROL_AE_STATE_LOCKED: {
                    // There's nothing we can do, AE was locked, triggers are ignored.
                    isMetered = true;
                    isSuccessful = false;
                    break;
                }
                case CaptureResult.CONTROL_AE_STATE_INACTIVE:
                case CaptureResult.CONTROL_AE_STATE_SEARCHING: {
                    // Wait...
                    break;
                }
            }
        } else {
            switch (aeState) {
                case CaptureResult.CONTROL_AE_STATE_CONVERGED:
                case CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED: {
                    isMetered = true;
                    isSuccessful = true;
                    break;
                }
                case CaptureResult.CONTROL_AE_STATE_LOCKED: {
                    // There's nothing we can do, AE was locked, triggers are ignored.
                    isMetered = true;
                    isSuccessful = false;
                    break;
                }
                case CaptureResult.CONTROL_AE_STATE_PRECAPTURE:
                case CaptureResult.CONTROL_AE_STATE_INACTIVE:
                case CaptureResult.CONTROL_AE_STATE_SEARCHING: {
                    // Wait...
                    break;
                }
            }
        }
    }

    @Override
    protected void onMetered(@NonNull CaptureRequest.Builder builder) {
        // Do nothing
    }

    @Override
    protected void onResetMetering(@NonNull CameraCharacteristics characteristics,
                                   @NonNull CaptureRequest.Builder builder,
                                   @Nullable MeteringRectangle area,
                                   boolean supportsProcessing) {
        boolean changed = false;
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 0);
        if (area != null && maxRegions > 0) {
            builder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{area});
            changed = true;
        }
        if (supportsProcessing) {
            // Cleanup any precapture sequence.
            if (Build.VERSION.SDK_INT >= 23) {
                builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
                changed = true;
            }
        }
        if (changed) {
            notifyBuilderChanged();
        }
    }
}
