package com.otaliastudios.cameraview.engine.meter;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.engine.action.ActionHolder;

import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class FocusMeter extends BaseMeter {

    private static final String TAG = FocusMeter.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    public FocusMeter(@NonNull List<MeteringRectangle> areas, boolean skipIfPossible) {
        super(areas, skipIfPossible);
    }

    @Override
    protected boolean checkIsSupported(@NonNull ActionHolder holder) {
        // Exclude OFF and EDOF as per docs. These do no support the trigger.
        Integer afMode = holder.getBuilder(this).get(CaptureRequest.CONTROL_AF_MODE);
        boolean result = afMode != null &&
                (afMode == CameraCharacteristics.CONTROL_AF_MODE_AUTO
                        || afMode == CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        || afMode == CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                        || afMode == CameraCharacteristics.CONTROL_AF_MODE_MACRO);
        LOG.i("checkIsSupported:", result);
        return result;
    }

    @Override
    protected boolean checkShouldSkip(@NonNull ActionHolder holder) {
        CaptureResult lastResult = holder.getLastResult(this);
        if (lastResult != null) {
            Integer afState = lastResult.get(CaptureResult.CONTROL_AF_STATE);
            boolean result = afState != null &&
                    (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                            afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED);
            LOG.i("checkShouldSkip:", result);
            return result;
        } else {
            LOG.i("checkShouldSkip: false - lastResult is null.");
            return false;
        }
    }

    @Override
    protected void onStarted(@NonNull ActionHolder holder, @NonNull List<MeteringRectangle> areas) {
        LOG.i("onStarted:", "with areas:", areas);
        holder.getBuilder(this).set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        int maxRegions = readCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AF,
                0);
        if (!areas.isEmpty() && maxRegions > 0) {
            int max = Math.min(maxRegions, areas.size());
            holder.getBuilder(this).set(CaptureRequest.CONTROL_AF_REGIONS,
                    areas.subList(0, max).toArray(new MeteringRectangle[]{}));
        }
        holder.applyBuilder(this);
    }

    @Override
    protected void onCompleted(@NonNull ActionHolder holder) {
        super.onCompleted(holder);
        // Remove (but not apply) the risky parameter so it is not included in new requests.
        // Documentation about this key says that this should be allowed.
        holder.getBuilder(this).set(CaptureRequest.CONTROL_AF_TRIGGER, null);
    }

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder, @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(holder, request, result);
        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
        LOG.i("onCaptureCompleted:", "afState:", afState);
        if (afState == null) return;
        switch (afState) {
            case CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED: {
                setSuccessful(true);
                setState(STATE_COMPLETED);
                break;
            }
            case CaptureRequest.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED: {
                setSuccessful(false);
                setState(STATE_COMPLETED);
                break;
            }
            case CaptureRequest.CONTROL_AF_STATE_INACTIVE: break;
            case CaptureRequest.CONTROL_AF_STATE_ACTIVE_SCAN: break;
            default: break;
        }
    }
}
