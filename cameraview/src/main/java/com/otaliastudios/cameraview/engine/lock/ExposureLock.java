package com.otaliastudios.cameraview.engine.lock;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.engine.action.ActionHolder;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class ExposureLock extends BaseLock {

    private final static String TAG = ExposureLock.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    @Override
    protected boolean checkIsSupported(@NonNull ActionHolder holder) {
        boolean isNotLegacy = readCharacteristic(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1)
                != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        // Not sure we should check aeMode as well, probably all aeModes support locking,
        // but this should not be a big issue since we're not even using different AE modes.
        Integer aeMode = holder.getBuilder(this).get(CaptureRequest.CONTROL_AE_MODE);
        boolean isAEOn = aeMode != null &&
                (aeMode == CameraCharacteristics.CONTROL_AE_MODE_ON
                        || aeMode == CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                        || aeMode == CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH
                        || aeMode == CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE
                        || aeMode == 5
                        /* CameraCharacteristics.CONTROL_AE_MODE_ON_EXTERNAL_FLASH, API 28 */);
        boolean result = isNotLegacy && isAEOn;
        LOG.i("checkIsSupported:", result);
        return result;
    }

    @Override
    protected boolean checkShouldSkip(@NonNull ActionHolder holder) {
        CaptureResult lastResult = holder.getLastResult(this);
        if (lastResult != null) {
            Integer aeState = lastResult.get(CaptureResult.CONTROL_AE_STATE);
            boolean result = aeState != null && aeState == CaptureResult.CONTROL_AE_STATE_LOCKED;
            LOG.i("checkShouldSkip:", result);
            return result;
        } else {
            LOG.i("checkShouldSkip: false - lastResult is null.");
            return false;
        }
    }

    @Override
    protected void onStarted(@NonNull ActionHolder holder) {
        int cancelTrigger = Build.VERSION.SDK_INT >= 23
                ? CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL
                : CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE;
        holder.getBuilder(this).set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                cancelTrigger);
        holder.getBuilder(this).set(CaptureRequest.CONTROL_AE_LOCK, true);
        holder.applyBuilder(this);
    }

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(holder, request, result);
        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
        LOG.i("processCapture:", "aeState:", aeState);
        if (aeState == null) return;
        switch (aeState) {
            case CaptureRequest.CONTROL_AE_STATE_LOCKED: {
                setState(STATE_COMPLETED);
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
}
