package com.otaliastudios.cameraview.engine.meter;

import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.engine.CameraEngine;
import com.otaliastudios.cameraview.engine.action.ActionHolder;
import com.otaliastudios.cameraview.engine.action.ActionWrapper;
import com.otaliastudios.cameraview.engine.action.Actions;
import com.otaliastudios.cameraview.engine.action.BaseAction;
import com.otaliastudios.cameraview.engine.offset.Axis;
import com.otaliastudios.cameraview.engine.offset.Reference;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class MeterAction extends ActionWrapper {

    private final static String TAG = MeterAction.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    private List<BaseMeter> meters;
    private BaseAction action;
    private ActionHolder holder;
    private final PointF point;
    private final CameraEngine engine;
    private final boolean skipIfPossible;

    public MeterAction(@NonNull CameraEngine engine, @Nullable PointF point,
                       boolean skipIfPossible) {
        this.point = point;
        this.engine = engine;
        this.skipIfPossible = skipIfPossible;
    }

    @NonNull
    @Override
    public BaseAction getAction() {
        return action;
    }

    @Nullable
    public PointF getPoint() {
        return point;
    }

    public boolean isSuccessful() {
        for (BaseMeter meter : meters) {
            if (!meter.isSuccessful()) {
                LOG.i("isSuccessful:", "returning false.");
                return false;
            }
        }
        LOG.i("isSuccessful:", "returning true.");
        return true;
    }

    @Override
    protected void onStart(@NonNull ActionHolder holder) {
        LOG.w("onStart:", "initializing.");
        initialize(holder);
        LOG.w("onStart:", "initialized.");
        super.onStart(holder);
    }

    private void initialize(@NonNull ActionHolder holder) {
        this.holder = holder;
        List<MeteringRectangle> areas = new ArrayList<>();
        if (point != null) {
            // This is a good Q/A. https://stackoverflow.com/a/33181620/4288782
            // At first, the point is relative to the View system and does not account
            // our own cropping. Will keep updating these two below.
            final PointF referencePoint = new PointF(point.x, point.y);
            Size referenceSize = engine.getPreview().getSurfaceSize();

            // 1. Account for cropping.
            // This will enlarge the preview size so that aspect ratio matches.
            referenceSize = applyPreviewCropping(referenceSize, referencePoint);

            // 2. Scale to the preview stream coordinates.
            // This will move to the preview stream coordinates by scaling.
            referenceSize = applyPreviewScale(referenceSize, referencePoint);

            // 3. Rotate to the stream coordinate system.
            // This leaves us with sensor stream coordinates.
            referenceSize = applyPreviewToSensorRotation(referenceSize, referencePoint);

            // 4. Move to the crop region coordinate system.
            // The crop region is the union of all currently active streams.
            referenceSize = applyCropRegionCoordinates(referenceSize, referencePoint);

            // 5. Move to the active array coordinate system.
            referenceSize = applyActiveArrayCoordinates(referenceSize, referencePoint);

            // 6. Now we can compute the metering regions.
            // We want to define them as a fraction of the visible size which (apart from cropping)
            // can be obtained through the SENSOR rotated preview stream size.
            Size visibleSize = engine.getPreviewStreamSize(Reference.SENSOR);
            //noinspection ConstantConditions
            MeteringRectangle area1 = createMeteringRectangle(referenceSize, referencePoint,
                    visibleSize, 0.05F, 1000);
            MeteringRectangle area2 = createMeteringRectangle(referenceSize, referencePoint,
                    visibleSize, 0.1F, 100);
            areas.add(area1);
            areas.add(area2);
        }

        BaseMeter ae = new ExposureMeter(areas, skipIfPossible);
        BaseMeter af = new FocusMeter(areas, skipIfPossible);
        BaseMeter awb = new WhiteBalanceMeter(areas, skipIfPossible);
        meters = Arrays.asList(ae, af, awb);
        action = Actions.together(ae, af, awb);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @NonNull
    private Size applyPreviewCropping(@NonNull Size referenceSize, @NonNull PointF referencePoint) {
        Size previewStreamSize = engine.getPreviewStreamSize(Reference.VIEW);
        Size previewSurfaceSize = referenceSize;
        if (previewStreamSize == null) {
            throw new IllegalStateException("getPreviewStreamSize should not be null here.");
        }
        int referenceWidth = previewSurfaceSize.getWidth();
        int referenceHeight = previewSurfaceSize.getHeight();
        AspectRatio previewStreamAspectRatio = AspectRatio.of(previewStreamSize);
        AspectRatio previewSurfaceAspectRatio = AspectRatio.of(previewSurfaceSize);
        if (engine.getPreview().isCropping()) {
            if (previewStreamAspectRatio.toFloat() > previewSurfaceAspectRatio.toFloat()) {
                // Stream is larger. The x coordinate must be increased: a touch on the left side
                // of the surface is not on the left size of stream (it's more to the right).
                float scale = previewStreamAspectRatio.toFloat()
                        / previewSurfaceAspectRatio.toFloat();
                referencePoint.x += previewSurfaceSize.getWidth() * (scale - 1F) / 2F;
                referenceWidth = Math.round(previewSurfaceSize.getWidth() * scale);
            } else {
                // Stream is taller. The y coordinate must be increased: a touch on the top side
                // of the surface is not on the top size of stream (it's a bit lower).
                float scale = previewSurfaceAspectRatio.toFloat()
                        / previewStreamAspectRatio.toFloat();
                referencePoint.y += previewSurfaceSize.getHeight() * (scale - 1F) / 2F;
                referenceHeight = Math.round(previewSurfaceSize.getHeight() * scale);
            }
        }
        return new Size(referenceWidth, referenceHeight);
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    private Size applyPreviewScale(@NonNull Size referenceSize, @NonNull PointF referencePoint) {
        // The referenceSize how has the same aspect ratio of the previewStreamSize, but they
        // can still have different size (that is, a scale operation is needed).
        Size previewStreamSize = engine.getPreviewStreamSize(Reference.VIEW);
        referencePoint.x *= (float) previewStreamSize.getWidth() / referenceSize.getWidth();
        referencePoint.y *= (float) previewStreamSize.getHeight() / referenceSize.getHeight();
        return previewStreamSize;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @NonNull
    private Size applyPreviewToSensorRotation(@NonNull Size referenceSize,
                                              @NonNull PointF referencePoint) {
        // Not elegant, but the sin/cos way was failing for some reason.
        int angle = engine.getAngles().offset(Reference.SENSOR, Reference.VIEW, Axis.ABSOLUTE);
        boolean flip = angle % 180 != 0;
        float tempX = referencePoint.x;
        float tempY = referencePoint.y;
        if (angle == 0) {
            referencePoint.x = tempX;
            referencePoint.y = tempY;
        } else if (angle == 90) {
            referencePoint.x = tempY;
            referencePoint.y = referenceSize.getWidth() - tempX;
        } else if (angle == 180) {
            referencePoint.x = referenceSize.getWidth() - tempX;
            referencePoint.y = referenceSize.getHeight() - tempY;
        } else if (angle == 270) {
            referencePoint.x = referenceSize.getHeight() - tempY;
            referencePoint.y = tempX;
        } else {
            throw new IllegalStateException("Unexpected angle " + angle);
        }
        return flip ? referenceSize.flip() : referenceSize;
    }

    @NonNull
    private Size applyCropRegionCoordinates(@NonNull Size referenceSize,
                                            @NonNull PointF referencePoint) {
        // The input point and size refer to the stream rect.
        // The stream rect is part of the 'crop region', as described below.
        // https://source.android.com/devices/camera/camera3_crop_reprocess.html
        Rect cropRect = holder.getBuilder(this).get(CaptureRequest.SCALER_CROP_REGION);
        // For now we don't care about x and y position. Rect should not be null, but let's be safe.
        int cropRectWidth = cropRect == null ? referenceSize.getWidth() : cropRect.width();
        int cropRectHeight = cropRect == null ? referenceSize.getHeight() : cropRect.height();
        // The stream is always centered inside the crop region, and one of the dimensions
        // should always match. We just increase the other one.
        referencePoint.x += (cropRectWidth - referenceSize.getWidth()) / 2F;
        referencePoint.y += (cropRectHeight - referenceSize.getHeight()) / 2F;
        return new Size(cropRectWidth, cropRectHeight);
    }

    @NonNull
    private Size applyActiveArrayCoordinates(@NonNull Size referenceSize,
                                             @NonNull PointF referencePoint) {
        // The input point and size refer to the scaler crop region.
        // We can query for the crop region position inside the active array, so this is easy.
        Rect cropRect = holder.getBuilder(this).get(CaptureRequest.SCALER_CROP_REGION);
        referencePoint.x += cropRect == null ? 0 : cropRect.left;
        referencePoint.y += cropRect == null ? 0 : cropRect.top;
        // Finally, get the active rect width and height from characteristics.
        Rect activeRect = holder.getCharacteristics(this)
                .get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (activeRect == null) { // Should never happen
            activeRect = new Rect(0, 0, referenceSize.getWidth(),
                    referenceSize.getHeight());
        }
        return new Size(activeRect.width(), activeRect.height());
    }

    /**
     * Creates a metering rectangle around the center point.
     * The rectangle will have a size that's a factor of the visible width and height.
     * The rectangle will also be constrained to be inside the given boundaries,
     * so we don't exceed them in case the center point is exactly on one side for example.
     * @return a new rectangle
     */
    @NonNull
    private MeteringRectangle createMeteringRectangle(
            @NonNull Size boundaries,
            @NonNull PointF center,
            @NonNull Size visibleSize,
            float factor,
            int weight) {
        float rectangleWidth = factor * visibleSize.getWidth();
        float rectangleHeight = factor * visibleSize.getHeight();
        float rectangleLeft = center.x - rectangleWidth / 2F;
        float rectangleTop = center.y - rectangleHeight / 2F;
        // Respect boundaries
        if (rectangleLeft < 0) rectangleLeft = 0;
        if (rectangleTop < 0) rectangleTop = 0;
        if (rectangleLeft + rectangleWidth > boundaries.getWidth()) {
            rectangleWidth = boundaries.getWidth() - rectangleLeft;
        }
        if (rectangleTop + rectangleHeight > boundaries.getHeight()) {
            rectangleHeight = boundaries.getHeight() - rectangleTop;
        }
        return new MeteringRectangle(
                (int) rectangleLeft,
                (int) rectangleTop,
                (int) rectangleWidth,
                (int) rectangleHeight,
                weight
        );
    }
}
