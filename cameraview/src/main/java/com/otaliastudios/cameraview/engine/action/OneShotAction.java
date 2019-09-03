package com.otaliastudios.cameraview.engine.action;

import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class OneShotAction extends BaseAction {

    private Runnable runnable;

    public OneShotAction(@NonNull Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void onCaptureStarted(@NonNull ActionHolder holder, @NonNull CaptureRequest request) {
        super.onCaptureStarted(holder, request);
        Object tag = holder.getBuilder(this).build().getTag();
        Object currentTag = request.getTag();
        if (tag == null ? currentTag == null : tag.equals(currentTag)) {
            runnable.run();
            setState(STATE_COMPLETED);
        }
    }
}
