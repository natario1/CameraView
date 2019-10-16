package com.otaliastudios.cameraview.engine;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.BaseAction;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class LogAction extends BaseAction {

    private final static CameraLogger LOG
            = CameraLogger.create(Camera2Engine.class.getSimpleName());

    private String lastLog;

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(holder, request, result);
        Integer aeMode = result.get(CaptureResult.CONTROL_AE_MODE);
        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
        Boolean aeLock = result.get(CaptureResult.CONTROL_AE_LOCK);
        Integer aeTriggerState = result.get(CaptureResult.CONTROL_AE_PRECAPTURE_TRIGGER);
        Integer afTriggerState = result.get(CaptureResult.CONTROL_AF_TRIGGER);
        String log = "aeMode: " + aeMode + " aeLock: " + aeLock +
                " aeState: " + aeState + " aeTriggerState: " + aeTriggerState +
                " afState: " + afState + " afTriggerState: " + afTriggerState;
        if (!log.equals(lastLog)) {
            lastLog = log;
            LOG.v(log);
        }
    }

    @Override
    protected void onCompleted(@NonNull ActionHolder holder) {
        super.onCompleted(holder);
        setState(0); // set another state.
        start(holder); // restart.
    }
}
