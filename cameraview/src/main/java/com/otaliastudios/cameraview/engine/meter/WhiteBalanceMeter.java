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
public class WhiteBalanceMeter extends BaseMeter {

    private static final String TAG = WhiteBalanceMeter.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    public WhiteBalanceMeter(@NonNull List<MeteringRectangle> areas, boolean skipIfPossible) {
        super(areas, skipIfPossible);
    }

    @Override
    protected boolean checkIsSupported(@NonNull ActionHolder holder) {
        boolean isNotLegacy = readCharacteristic(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1)
                != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        Integer awbMode = holder.getBuilder(this).get(CaptureRequest.CONTROL_AWB_MODE);
        boolean result = isNotLegacy
                && awbMode != null
                && awbMode == CaptureRequest.CONTROL_AWB_MODE_AUTO;
        LOG.i("checkIsSupported:", result);
        return result;
    }

    @Override
    protected boolean checkShouldSkip(@NonNull ActionHolder holder) {
        CaptureResult lastResult = holder.getLastResult(this);
        if (lastResult != null) {
            Integer awbState = lastResult.get(CaptureResult.CONTROL_AWB_STATE);
            boolean result = awbState != null
                    && awbState == CaptureRequest.CONTROL_AWB_STATE_CONVERGED;
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
        int maxRegions = readCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB,
                0);
        if (!areas.isEmpty() && maxRegions > 0) {
            int max = Math.min(maxRegions, areas.size());
            holder.getBuilder(this).set(CaptureRequest.CONTROL_AWB_REGIONS,
                    areas.subList(0, max).toArray(new MeteringRectangle[]{}));
            holder.applyBuilder(this);
        }
    }

    @Override
    public void onCaptureCompleted(@NonNull ActionHolder holder,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(holder, request, result);
        Integer awbState = result.get(CaptureResult.CONTROL_AWB_STATE);
        LOG.i("onCaptureCompleted:", "awbState:", awbState);
        if (awbState == null) return;

        switch (awbState) {
            case CaptureRequest.CONTROL_AWB_STATE_CONVERGED: {
                setSuccessful(true);
                setState(STATE_COMPLETED);
                break;
            }
            case CaptureRequest.CONTROL_AWB_STATE_LOCKED: {
                // Nothing we can do if AWB was locked.
                setSuccessful(false);
                setState(STATE_COMPLETED);
                break;
            }
            case CaptureRequest.CONTROL_AWB_STATE_INACTIVE:
            case CaptureRequest.CONTROL_AWB_STATE_SEARCHING: {
                // Wait...
                break;
            }
        }
    }
}
