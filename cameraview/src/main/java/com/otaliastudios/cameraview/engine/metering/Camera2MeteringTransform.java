package com.otaliastudios.cameraview.engine.metering;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.engine.offset.Angles;
import com.otaliastudios.cameraview.engine.offset.Axis;
import com.otaliastudios.cameraview.engine.offset.Reference;
import com.otaliastudios.cameraview.metering.MeteringTransform;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2MeteringTransform implements MeteringTransform<MeteringRectangle> {

    protected static final String TAG = Camera2MeteringTransform.class.getSimpleName();
    protected static final CameraLogger LOG = CameraLogger.create(TAG);

    private final Angles angles;
    private final Size previewSize;
    private final Size previewStreamSize;
    private final boolean previewIsCropping;
    private final CameraCharacteristics characteristics;
    private final CaptureRequest.Builder builder;

    public Camera2MeteringTransform(@NonNull Angles angles,
                                    @NonNull Size previewSize,
                                    @NonNull Size previewStreamSize,
                                    boolean previewIsCropping,
                                    @NonNull CameraCharacteristics characteristics,
                                    @NonNull CaptureRequest.Builder builder) {
        this.angles = angles;
        this.previewSize = previewSize;
        this.previewStreamSize = previewStreamSize;
        this.previewIsCropping = previewIsCropping;
        this.characteristics = characteristics;
        this.builder = builder;
    }

    @NonNull
    @Override
    public MeteringRectangle transformMeteringRegion(@NonNull RectF region, int weight) {
        Rect round = new Rect();
        region.round(round);
        return new MeteringRectangle(round, weight);
    }

    @NonNull
    @Override
    public PointF transformMeteringPoint(@NonNull PointF point) {
        // This is a good Q/A. https://stackoverflow.com/a/33181620/4288782
        // At first, the point is relative to the View system and does not account
        // our own cropping. Will keep updating these two below.
        final PointF referencePoint = new PointF(point.x, point.y);
        Size referenceSize = previewSize;

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
        LOG.i("input:", point, "output (before clipping):", referencePoint);

        // 6. Probably not needed, but make sure we clip.
        if (referencePoint.x < 0) referencePoint.x = 0;
        if (referencePoint.y < 0) referencePoint.y = 0;
        if (referencePoint.x > referenceSize.getWidth()) referencePoint.x = referenceSize.getWidth();
        if (referencePoint.y > referenceSize.getHeight()) referencePoint.y = referenceSize.getHeight();
        LOG.i("input:", point, "output (after clipping):", referencePoint);
        return referencePoint;
    }


    @SuppressWarnings("UnnecessaryLocalVariable")
    @NonNull
    private Size applyPreviewCropping(@NonNull Size referenceSize, @NonNull PointF referencePoint) {
        Size previewStreamSize = this.previewStreamSize;
        Size previewSurfaceSize = referenceSize;
        int referenceWidth = previewSurfaceSize.getWidth();
        int referenceHeight = previewSurfaceSize.getHeight();
        AspectRatio previewStreamAspectRatio = AspectRatio.of(previewStreamSize);
        AspectRatio previewSurfaceAspectRatio = AspectRatio.of(previewSurfaceSize);
        if (previewIsCropping) {
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

    @NonNull
    private Size applyPreviewScale(@NonNull Size referenceSize, @NonNull PointF referencePoint) {
        // The referenceSize how has the same aspect ratio of the previewStreamSize, but they
        // can still have different size (that is, a scale operation is needed).
        Size previewStreamSize = this.previewStreamSize;
        referencePoint.x *= (float) previewStreamSize.getWidth() / referenceSize.getWidth();
        referencePoint.y *= (float) previewStreamSize.getHeight() / referenceSize.getHeight();
        return previewStreamSize;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @NonNull
    private Size applyPreviewToSensorRotation(@NonNull Size referenceSize,
                                              @NonNull PointF referencePoint) {
        // Not elegant, but the sin/cos way was failing for some reason.
        int angle = angles.offset(Reference.SENSOR, Reference.VIEW, Axis.ABSOLUTE);
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
        Rect cropRect = builder.get(CaptureRequest.SCALER_CROP_REGION);
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
        Rect cropRect = builder.get(CaptureRequest.SCALER_CROP_REGION);
        referencePoint.x += cropRect == null ? 0 : cropRect.left;
        referencePoint.y += cropRect == null ? 0 : cropRect.top;
        // Finally, get the active rect width and height from characteristics.
        Rect activeRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (activeRect == null) { // Should never happen
            activeRect = new Rect(0, 0, referenceSize.getWidth(),
                    referenceSize.getHeight());
        }
        return new Size(activeRect.width(), activeRect.height());
    }
}
