package com.otaliastudios.cameraview.engine.locking;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class AutoFocus extends Parameter {

    private static final String TAG = AutoFocus.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG + "Locking");

    public AutoFocus(@NonNull LockingChangeCallback callback) {
        super(callback);
    }

    @Override
    protected boolean checkSupportsLocking(@NonNull CameraCharacteristics characteristics,
                                           @NonNull CaptureRequest.Builder builder) {
        // We'll lock by changing the AF mode to AUTO.
        // In that mode, AF won't change unless someone starts a trigger operation.
        int[] modes = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, new int[]{});
        for (int mode : modes) {
            if (mode == CameraCharacteristics.CONTROL_AF_MODE_AUTO) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean checkShouldSkip(@NonNull CaptureResult lastResult) {
        Integer afState = lastResult.get(CaptureResult.CONTROL_AF_STATE);
        boolean afStateOk = afState != null &&
                (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                || afState == CaptureResult.CONTROL_AF_STATE_INACTIVE
                || afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED
                || afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED);
        Integer afMode = lastResult.get(CaptureResult.CONTROL_AF_MODE);
        boolean afModeOk = afMode != null && afMode == CaptureResult.CONTROL_AF_MODE_AUTO;
        boolean result = afStateOk && afModeOk;
        LOG.i("checkShouldSkip:", result);
        return result;
    }

    @Override
    protected void onLock(@NonNull CameraCharacteristics characteristics,
                          @NonNull CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        notifyBuilderChanged();
    }

    @Override
    public void processCapture(@NonNull CaptureResult result) {
        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
        Integer afMode = result.get(CaptureResult.CONTROL_AF_MODE);
        LOG.i("onCapture:", "afState:", afState, "afMode:", afMode);
        if (afState == null || afMode == null) return;
        if (afMode != CaptureResult.CONTROL_AF_MODE_AUTO) return;
        switch (afState) {
            case CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED:
            case CaptureRequest.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:
            case CaptureRequest.CONTROL_AF_STATE_INACTIVE:
            case CaptureRequest.CONTROL_AF_STATE_PASSIVE_FOCUSED:
            case CaptureRequest.CONTROL_AF_STATE_PASSIVE_UNFOCUSED: {
                isLocked = true;
                isSuccessful = true;
                break;
            }
            case CaptureRequest.CONTROL_AF_STATE_ACTIVE_SCAN:
            case CaptureRequest.CONTROL_AF_STATE_PASSIVE_SCAN: {
                // Wait...
                break;
            }
        }
    }

    @Override
    protected void onLocked(@NonNull CaptureRequest.Builder builder) {
        // Do nothing.
    }

}
