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

import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class WhiteBalanceReset extends BaseReset {

    private static final String TAG = WhiteBalanceReset.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    @SuppressWarnings("WeakerAccess")
    public WhiteBalanceReset() {
        super(true);
    }

    @Override
    protected void onStarted(@NonNull ActionHolder holder, @Nullable MeteringRectangle area) {
        LOG.w("onStarted:", "with area:", area);
        int maxRegions = readCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB,
                0);
        if (area != null && maxRegions > 0) {
            holder.getBuilder(this).set(CaptureRequest.CONTROL_AWB_REGIONS,
                    new MeteringRectangle[]{area});
            holder.applyBuilder(this);
        }
        setState(STATE_COMPLETED);
    }
}
