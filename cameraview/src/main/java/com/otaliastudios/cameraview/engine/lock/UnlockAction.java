package com.otaliastudios.cameraview.engine.lock;

import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.BaseAction;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class UnlockAction extends BaseAction {

    @Override
    protected void onStart(@NonNull ActionHolder holder) {
        super.onStart(holder);
        holder.getBuilder(this).set(CaptureRequest.CONTROL_AE_LOCK, false);
        holder.getBuilder(this).set(CaptureRequest.CONTROL_AWB_LOCK, false);
        holder.applyBuilder(this);
        setState(STATE_COMPLETED);
        // TODO focus is managed by the engine
        // TODO should wait results?
    }
}
