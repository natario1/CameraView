package com.otaliastudios.cameraview.engine.meter;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.engine.action.ActionHolder;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class ExposureReset extends BaseReset {

    private static final String TAG = ExposureReset.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    public ExposureReset(boolean resetArea) {
        super(resetArea);
    }

    @Override
    protected void onStarted(@NonNull ActionHolder holder, @Nullable MeteringRectangle area) {
        boolean changed = false;
        int maxRegions = readCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 0);
        if (area != null && maxRegions > 0) {
            holder.getBuilder(this).set(CaptureRequest.CONTROL_AE_REGIONS,
                    new MeteringRectangle[]{area});
            changed = true;
        }

        // NOTE: precapture might not be supported, in which case I think it will be ignored.
        Integer trigger = holder.getBuilder(this).get(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER);
        LOG.w("onStarted:", "current precapture trigger is", trigger);
        if (trigger == null || trigger == CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START) {
            LOG.w("onStarted:", "canceling precapture.");
            int newTrigger = Build.VERSION.SDK_INT >= 23
                    ? CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL
                    : CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE;
            holder.getBuilder(this).set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, newTrigger);
            changed = true;
        }

        if (changed) holder.applyBuilder(this);
        setState(STATE_COMPLETED);
    }
}
