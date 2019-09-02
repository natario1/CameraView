package com.otaliastudios.cameraview.engine.metering;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;

import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class AutoWhiteBalance extends Parameter {

    private static final String TAG = AutoWhiteBalance.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG + "Metering");

    public AutoWhiteBalance(@NonNull MeteringChangeCallback callback) {
        super(callback);
    }

    @Override
    protected boolean checkSupportsProcessing(@NonNull CameraCharacteristics characteristics,
                                              @NonNull CaptureRequest.Builder builder) {
        boolean isNotLegacy = readCharacteristic(characteristics,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, -1) !=
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        Integer awbMode = builder.get(CaptureRequest.CONTROL_AWB_MODE);
        boolean result = isNotLegacy && awbMode != null && awbMode == CaptureRequest.CONTROL_AWB_MODE_AUTO;
        LOG.i("checkSupportsProcessing:", result);
        return result;
    }

    @Override
    protected boolean checkShouldSkip(@NonNull CaptureResult lastResult) {
        Integer awbState = lastResult.get(CaptureResult.CONTROL_AWB_STATE);
        boolean result = awbState != null && awbState == CaptureRequest.CONTROL_AWB_STATE_CONVERGED;
        LOG.i("checkShouldSkip:", result);
        return result;
    }

    @Override
    protected void onStartMetering(@NonNull CameraCharacteristics characteristics,
                                   @NonNull CaptureRequest.Builder builder,
                                   @NonNull List<MeteringRectangle> areas,
                                   boolean supportsProcessing) {
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 0);
        if (!areas.isEmpty() && maxRegions > 0) {
            int max = Math.min(maxRegions, areas.size());
            builder.set(CaptureRequest.CONTROL_AWB_REGIONS,
                    areas.subList(0, max).toArray(new MeteringRectangle[]{}));
            notifyBuilderChanged();
        }
    }

    @Override
    public void processCapture(@NonNull CaptureResult result) {
        Integer awbState = result.get(CaptureResult.CONTROL_AWB_STATE);
        LOG.i("onCapture:", "awbState:", awbState);
        if (awbState == null) return;

        switch (awbState) {
            case CaptureRequest.CONTROL_AWB_STATE_CONVERGED: {
                isMetered = true;
                isSuccessful = true;
                break;
            }
            case CaptureRequest.CONTROL_AWB_STATE_LOCKED: {
                // Nothing we can do if AWB was locked.
                isMetered = true;
                isSuccessful = false;
                break;
            }
            case CaptureRequest.CONTROL_AWB_STATE_INACTIVE:
            case CaptureRequest.CONTROL_AWB_STATE_SEARCHING: {
                // Wait...
                break;
            }
        }
    }

    @Override
    protected void onMetered(@NonNull CaptureRequest.Builder builder) {
        // Do nothing
    }

    @Override
    protected void onResetMetering(@NonNull CameraCharacteristics characteristics,
                                   @NonNull CaptureRequest.Builder builder,
                                   @Nullable MeteringRectangle area,
                                   boolean supportsProcessing) {
        int maxRegions = readCharacteristic(characteristics,
                CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 0);
        if (area != null && maxRegions > 0) {
            builder.set(CaptureRequest.CONTROL_AWB_REGIONS, new MeteringRectangle[]{area});
            notifyBuilderChanged();
        }
    }
}
