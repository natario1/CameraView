package com.otaliastudios.cameraview.engine.locking;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class AutoWhiteBalance extends Parameter {

    private static final String TAG = AutoWhiteBalance.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG + "Locking");

    public AutoWhiteBalance(@NonNull LockingChangeCallback callback) {
        super(callback);
    }

    @Override
    protected boolean checkSupportsLocking(@NonNull CameraCharacteristics characteristics,
                                           @NonNull CaptureRequest.Builder builder) {
        boolean isNotLegacy = readCharacteristic(characteristics,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1) !=
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        Integer awbMode = builder.get(CaptureRequest.CONTROL_AWB_MODE);
        boolean result = isNotLegacy && awbMode != null && awbMode == CaptureRequest.CONTROL_AWB_MODE_AUTO;
        LOG.i("checkSupportsLocking:", result);
        return result;
    }

    @Override
    protected boolean checkShouldSkip(@NonNull CaptureResult lastResult) {
        Integer awbState = lastResult.get(CaptureResult.CONTROL_AWB_STATE);
        boolean result = awbState != null && awbState == CaptureRequest.CONTROL_AWB_STATE_LOCKED;
        LOG.i("checkShouldSkip:", result);
        return result;
    }

    @Override
    protected void onLock(@NonNull CameraCharacteristics characteristics,
                          @NonNull CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AWB_LOCK, true);
        notifyBuilderChanged();
    }

    @Override
    public void processCapture(@NonNull CaptureResult result) {
        Integer awbState = result.get(CaptureResult.CONTROL_AWB_STATE);
        LOG.i("processCapture:", "awbState:", awbState);
        if (awbState == null) return;
        switch (awbState) {
            case CaptureRequest.CONTROL_AWB_STATE_LOCKED: {
                isLocked = true;
                isSuccessful = true;
                break;
            }
            case CaptureRequest.CONTROL_AWB_STATE_CONVERGED:
            case CaptureRequest.CONTROL_AWB_STATE_INACTIVE:
            case CaptureRequest.CONTROL_AWB_STATE_SEARCHING: {
                // Wait...
                break;
            }
        }
    }

    @Override
    protected void onLocked(@NonNull CaptureRequest.Builder builder) {
        // Do nothing
    }
}
