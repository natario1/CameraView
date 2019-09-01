package com.otaliastudios.cameraview.engine.locking;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class AutoExposure extends Parameter {

    private static final String TAG = AutoExposure.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    public AutoExposure(@NonNull LockingChangeCallback callback) {
        super(callback);
    }

    @Override
    protected boolean checkSupportsLocking(@NonNull CameraCharacteristics characteristics,
                                           @NonNull CaptureRequest.Builder builder) {
        boolean isNotLegacy = readCharacteristic(characteristics,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1) !=
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        // Not sure we should check aeMode as well, probably all aeModes support locking,
        // but this should not be a big issue since we're not even using different AE modes.
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
        boolean result = aeState != null && aeState == CaptureResult.CONTROL_AE_STATE_LOCKED;
        LOG.i("checkShouldSkip:", result);
        return result;
    }

    @Override
    protected void onLock(@NonNull CameraCharacteristics characteristics,
                          @NonNull CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AE_LOCK, true);
        notifyBuilderChanged();
    }

    @Override
    public void processCapture(@NonNull CaptureResult result) {
        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
        LOG.i("processCapture:", "aeState:", aeState);
        if (aeState == null) return;
        switch (aeState) {
            case CaptureRequest.CONTROL_AE_STATE_LOCKED: {
                isLocked = true;
                isSuccessful = true;
                break;
            }
            case CaptureRequest.CONTROL_AE_STATE_PRECAPTURE:
            case CaptureRequest.CONTROL_AE_STATE_CONVERGED:
            case CaptureRequest.CONTROL_AE_STATE_INACTIVE:
            case CaptureRequest.CONTROL_AE_STATE_SEARCHING:
            case CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED: {
                // Wait...
                break;
            }
        }
    }

    @Override
    protected void onLocked(@NonNull CaptureRequest.Builder builder) {
        // Do nothing
    }

    @Override
    protected void onUnlock(@NonNull CameraCharacteristics characteristics,
                            @NonNull CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AE_LOCK, false);
        notifyBuilderChanged();
    }
}
