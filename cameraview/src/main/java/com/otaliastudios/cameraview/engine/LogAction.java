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

    private final static CameraLogger LOG = CameraLogger.create(Camera2Engine.class.getSimpleName());

    private String lastLog;

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder, @NonNull CaptureRequest request,
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
            LOG.w(log);
        }

        // START
        // aeMode: 3 aeLock: false aeState: 4 aeTriggerState: 0 afState: 2 afTriggerState: 0
        //
        // DURING metering (focus skips)
        // aeMode: 3 aeLock: false aeState: 4 aeTriggerState: 0 afState: 0 afTriggerState: 0
        // aeMode: 3 aeLock: false aeState: 5 aeTriggerState: 1 afState: 0 afTriggerState: 0
        //
        // DURING locking (focus skips)
        // aeMode: 3 aeLock: false aeState: 4 aeTriggerState: 1 afState: 0 afTriggerState: 0
        // aeMode: 3 aeLock: true aeState: 5 aeTriggerState: 1 afState: 0 afTriggerState: 0
        //
        // AFTER locked
        // aeMode: 3 aeLock: true aeState: 3 aeTriggerState: 1 afState: 0 afTriggerState: 0
        //
        // AFTER super.take() called
        // aeMode: 1 aeLock: true aeState: 5 aeTriggerState: 1 afState: 0 afTriggerState: 0
        // aeMode: 1 aeLock: true aeState: 3 aeTriggerState: 1 afState: 0 afTriggerState: 0
        //
        // Reverting flash changes + reset lock + reset metering
        // aeMode: 3 aeLock: false aeState: 4 aeTriggerState: 2(1 now) afState: 2 afTriggerState: 0
        // aeMode: 3 aeLock: false aeState: 1 aeTriggerState: 2(1 now) afState: 2 afTriggerState: 0
    }
}
