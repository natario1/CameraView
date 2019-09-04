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
    private static final CameraLogger LOG = CameraLogger.create(TAG + "Metering");

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

        if (changed) {
            notifyBuilderChanged(false);
        }
    }


    @Override
    public void processCapture(@NonNull CaptureResult result) {
        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
        Integer aeTriggerState = result.get(CaptureResult.CONTROL_AE_PRECAPTURE_TRIGGER);
        LOG.i("onCapture:", "aeState:", aeState, "aeTriggerState:", aeTriggerState);
        if (aeState == null) return;

        if (!isStarted) {
            switch (aeState) {
                case CaptureResult.CONTROL_AE_STATE_PRECAPTURE: {
                    isStarted = true;
                    break;
                }
                case CaptureResult.CONTROL_AE_STATE_CONVERGED:
                case CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED: {
                    // PRECAPTURE is a transient state. Being here might mean that precapture run
                    // and was successful, OR that the trigger was not even received yet. To
                    // distinguish, check the trigger state.
                    if (aeTriggerState != null
                            && aeTriggerState == CaptureResult.CONTROL_AE_PRECAPTURE_TRIGGER_START) {
                        notifyMetered(true);
                    }
                    break;
                }
                case CaptureResult.CONTROL_AE_STATE_LOCKED: {
                    // There's nothing we can do, AE was locked, triggers are ignored.
                    notifyMetered(false);
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
                    notifyMetered(true);
                    break;
                }
                case CaptureResult.CONTROL_AE_STATE_LOCKED: {
                    // There's nothing we can do, AE was locked, triggers are ignored.
                    notifyMetered(false);
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
    protected void onMetered(@NonNull CaptureRequest.Builder builder, boolean success) {
        // Undo the trigger.
        /* TODO thinking about it. int newTrigger = Build.VERSION.SDK_INT >= 23
                ? CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL
                : CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE; */
        builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, null);
        // builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
        // notifyBuilderChanged();
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
            Integer trigger = builder.get(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER);
            LOG.w("onResetMetering:", "current precapture trigger is", trigger);
            if (trigger == null || trigger == CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START) {
                // Undo the trigger. This might happen if we can't meter in time / reset called before.
                LOG.w("onResetMetering:", "canceling precapture.");
                int newTrigger = Build.VERSION.SDK_INT >= 23
                        ? CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL
                        : CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE;
                builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, newTrigger);

                builder.set(CaptureRequest.CONTROL_AE_LOCK, true);
                notifyBuilderChanged(true);
                builder.set(CaptureRequest.CONTROL_AE_LOCK, false);
                changed = true;
            }
        }
        if (changed) {
            notifyBuilderChanged(false);
        }
    }
}
