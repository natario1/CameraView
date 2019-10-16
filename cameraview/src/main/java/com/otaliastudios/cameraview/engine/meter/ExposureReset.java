package com.otaliastudios.cameraview.engine.meter;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
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

    private static final int STATE_WAITING_LOCK = 0;

    @SuppressWarnings("WeakerAccess")
    public ExposureReset() {
        super(true);
    }

    @Override
    protected void onStarted(@NonNull ActionHolder holder, @Nullable MeteringRectangle area) {
        int maxRegions = readCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AE,
                0);
        if (area != null && maxRegions > 0) {
            holder.getBuilder(this).set(CaptureRequest.CONTROL_AE_REGIONS,
                    new MeteringRectangle[]{area});
        }

        // NOTE: precapture might not be supported, in which case I think it will be ignored.
        CaptureResult lastResult = holder.getLastResult(this);
        Integer trigger = lastResult == null ? null
                : lastResult.get(CaptureResult.CONTROL_AE_PRECAPTURE_TRIGGER);
        LOG.i("onStarted:", "last precapture trigger is", trigger);
        if (trigger != null && trigger == CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START) {
            LOG.i("onStarted:", "canceling precapture.");
            int newTrigger = Build.VERSION.SDK_INT >= 23
                    ? CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL
                    : CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE;
            holder.getBuilder(this).set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    newTrigger);
        }

        // Documentation about CONTROL_AE_PRECAPTURE_TRIGGER says that, if it was started but not
        // followed by a CAPTURE_INTENT_STILL_PICTURE request, the internal AE routine might remain
        // locked unless we unlock manually.
        // This is often the case for us, since the snapshot picture recorder does not use the
        // intent and anyway we use the precapture sequence for touch metering as well.
        // To reset docs suggest the use of CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL, which we do above,
        // or the technique used below: locking then unlocking. This proved to be the ONLY method
        // to unlock reliably, unlike the cancel trigger (which we'll run anyway).
        holder.getBuilder(this).set(CaptureRequest.CONTROL_AE_LOCK, true);
        holder.applyBuilder(this);
        setState(STATE_WAITING_LOCK);
    }

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder, @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(holder, request, result);
        if (getState() == STATE_WAITING_LOCK) {
            holder.getBuilder(this).set(CaptureRequest.CONTROL_AE_LOCK, false);
            holder.applyBuilder(this);
            setState(STATE_COMPLETED);
        }
    }
}
