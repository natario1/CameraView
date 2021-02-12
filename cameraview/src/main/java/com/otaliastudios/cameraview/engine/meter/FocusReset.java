package com.otaliastudios.cameraview.engine.meter;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.engine.action.ActionHolder;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class FocusReset extends BaseReset {

    private static final String TAG = FocusReset.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    @SuppressWarnings("WeakerAccess")
    public FocusReset() {
        super(true);
    }

    @Override
    protected void onStarted(@NonNull ActionHolder holder, @Nullable MeteringRectangle area) {
        boolean changed = false;
        int maxRegions = readCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AF,
                0);
        if (area != null && maxRegions > 0) {
            holder.getBuilder(this).set(CaptureRequest.CONTROL_AF_REGIONS,
                    new MeteringRectangle[]{area});
            changed = true;
        }

        // NOTE: trigger might not be supported, in which case I think it will be ignored.
        CaptureResult lastResult = holder.getLastResult(this);
        Integer trigger = lastResult == null ? null
                : lastResult.get(CaptureResult.CONTROL_AF_TRIGGER);
        LOG.w("onStarted:", "last focus trigger is", trigger);
        if (trigger != null && trigger == CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START) {
            holder.getBuilder(this).set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            changed = true;
        }

        if (changed) holder.applyBuilder(this);
        setState(STATE_COMPLETED);
    }
}
