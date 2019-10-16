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
public class FocusLock extends BaseLock {

    private final static String TAG = FocusLock.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    @Override
    protected boolean checkIsSupported(@NonNull ActionHolder holder) {
        // We'll lock by changing the AF mode to AUTO.
        // In that mode, AF won't change unless someone starts a trigger operation.
        int[] modes = readCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES,
                new int[]{});
        for (int mode : modes) {
            if (mode == CameraCharacteristics.CONTROL_AF_MODE_AUTO) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean checkShouldSkip(@NonNull ActionHolder holder) {
        CaptureResult lastResult = holder.getLastResult(this);
        if (lastResult != null) {
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
        } else {
            LOG.i("checkShouldSkip: false - lastResult is null.");
            return false;
        }
    }

    @Override
    protected void onStarted(@NonNull ActionHolder holder) {
        holder.getBuilder(this).set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_AUTO);
        holder.getBuilder(this).set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        holder.applyBuilder(this);
    }

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(holder, request, result);
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
                setState(STATE_COMPLETED);
                break;
            }
            case CaptureRequest.CONTROL_AF_STATE_ACTIVE_SCAN:
            case CaptureRequest.CONTROL_AF_STATE_PASSIVE_SCAN: {
                // Wait...
                break;
            }
        }
    }
}
