package com.otaliastudios.cameraview.engine;

import android.graphics.PointF;
import android.graphics.Rect;
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
import com.otaliastudios.cameraview.engine.metering.AutoExposure;
import com.otaliastudios.cameraview.engine.metering.AutoFocus;
import com.otaliastudios.cameraview.engine.metering.AutoWhiteBalance;
import com.otaliastudios.cameraview.engine.metering.MeteringParameter;
import com.otaliastudios.cameraview.engine.offset.Axis;
import com.otaliastudios.cameraview.engine.offset.Reference;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helps Camera2-based engines to perform 3A (auto focus, auto exposure and auto white balance)
 * metering. Users are required to:
 *
 * - Call {@link #startMetering(PointF, Gesture)} to start
 * - Call {@link #onCapture(CaptureResult)} when they have partial or total results, as long as the
 *   meter is still in a metering operation, which can be checked through {@link #isMetering()}
 * - Call {@link #resetMetering()} to reset the metering parameters if needed. This is done automatically
 *   by the meter based on the reset delay configuration in the engine, but can be called explicitly
 *   for example when we have multiple meter requests and want to cancel the old one.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Meter {

    /**
     * The meter callback.
     */
    public interface Callback {

        /**
         * Notifies that metering has started. At this point implementors should apply
         * the builder onto the preview.
         * @param point point
         * @param gesture gesture
         */
        void onMeteringStarted(@Nullable PointF point, @Nullable Gesture gesture);

        /**
         * Notifies that metering has ended. No action is required for implementors.
         * From now on, {@link #isMetering()} will return false so the meter should not
         * be passed capture results anymore.
         * @param point point
         * @param gesture gesture
         * @param success success
         */
        void onMeteringEnd(@Nullable PointF point, @Nullable Gesture gesture, boolean success);

        /**
         * Notifies that metering has been reset. From now on, this meter instance
         * is done, although in theory it could be reused by calling
         * {@link #startMetering(CaptureResult, PointF, Gesture)} again.
         * @param point point
         * @param gesture gesture
         */
        void onMeteringReset(@Nullable PointF point, @Nullable Gesture gesture);

        /**
         * Whether metering can be reset. Since it happens at a future time, this should
         * return true if the engine is still in a legit state for this operation.
         * @param point point
         * @param gesture gesture
         * @return true if can reset
         */
        // TODO is this useful? engine could do its checks onMeteringReset()
        boolean canResetMetering(@Nullable PointF point, @Nullable Gesture gesture);
    }

    private static final String TAG = Meter.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);
    private static final int FORCED_END_DELAY = 2500;

    private final CameraEngine mEngine;
    private final CaptureRequest.Builder mBuilder;
    private final CameraCharacteristics mCharacteristics;
    private final Callback mCallback;
    private PointF mPoint;
    private Gesture mGesture;

    private boolean mIsMetering;
    private long mMeteringStartTime;
    private MeteringParameter mAutoFocus = new AutoFocus();
    private MeteringParameter mAutoWhiteBalance = new AutoWhiteBalance();
    private MeteringParameter mAutoExposure = new AutoExposure();

    /**
     * Creates a new meter.
     * @param engine the engine
     * @param builder a capture builder
     * @param characteristics the camera characteristics
     * @param callback the callback
     */
    @SuppressWarnings("WeakerAccess")
    public Meter(@NonNull CameraEngine engine,
                 @NonNull CaptureRequest.Builder builder,
                 @NonNull CameraCharacteristics characteristics,
                 @NonNull Callback callback) {
        mEngine = engine;
        mBuilder = builder;
        mCharacteristics = characteristics;
        mCallback = callback;
    }

    /**
     * Starts a metering sequence.
     * @param lastResult the last result
     * @param point point
     * @param gesture gesture
     */
    @SuppressWarnings("WeakerAccess")
    public void startMetering(@NonNull CaptureResult lastResult, @Nullable PointF point, @Nullable Gesture gesture) {
        mPoint = point;
        mGesture = gesture;
        mIsMetering = true;

        List<MeteringRectangle> areas = new ArrayList<>();
        if (point != null) {
            // This is a good Q/A. https://stackoverflow.com/a/33181620/4288782
            // At first, the point is relative to the View system and does not account our own cropping.
            // Will keep updating these two below.
            final PointF referencePoint = new PointF(mPoint.x, mPoint.y);
            Size referenceSize = mEngine.mPreview.getSurfaceSize();

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
            Size visibleSize = mEngine.getPreviewStreamSize(Reference.SENSOR);
            //noinspection ConstantConditions
            MeteringRectangle area1 = createMeteringRectangle(referenceSize, referencePoint,
                    visibleSize, 0.05F, 1000);
            MeteringRectangle area2 = createMeteringRectangle(referenceSize, referencePoint,
                    visibleSize, 0.1F, 100);
            areas.add(area1);
            areas.add(area2);
        }

        // 7. And finally dispatch everything
        boolean skipIfPossible = mPoint == null;
        mAutoFocus.startMetering(mCharacteristics, mBuilder, areas, lastResult, skipIfPossible);
        mAutoWhiteBalance.startMetering(mCharacteristics, mBuilder, areas, lastResult, skipIfPossible);
        mAutoExposure.startMetering(mCharacteristics, mBuilder, areas, lastResult, skipIfPossible);

        // Dispatch to callback
        mCallback.onMeteringStarted(mPoint, mGesture);
        mMeteringStartTime = System.currentTimeMillis();
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @NonNull
    private Size applyPreviewCropping(@NonNull Size referenceSize, @NonNull PointF referencePoint) {
        Size previewStreamSize = mEngine.getPreviewStreamSize(Reference.VIEW);
        Size previewSurfaceSize = referenceSize;
        if (previewStreamSize == null) {
            throw new IllegalStateException("getPreviewStreamSize should not be null at this point.");
        }
        int referenceWidth = previewSurfaceSize.getWidth();
        int referenceHeight = previewSurfaceSize.getHeight();
        AspectRatio previewStreamAspectRatio = AspectRatio.of(previewStreamSize);
        AspectRatio previewSurfaceAspectRatio = AspectRatio.of(previewSurfaceSize);
        if (mEngine.mPreview.isCropping()) {
            if (previewStreamAspectRatio.toFloat() > previewSurfaceAspectRatio.toFloat()) {
                // Stream is larger. The x coordinate must be increased: a touch on the left side
                // of the surface is not on the left size of stream (it's more to the right).
                float scale = previewStreamAspectRatio.toFloat() / previewSurfaceAspectRatio.toFloat();
                referencePoint.x += previewSurfaceSize.getWidth() * (scale - 1F) / 2F;
                referenceWidth = Math.round(previewSurfaceSize.getWidth() * scale);
            } else {
                // Stream is taller. The y coordinate must be increased: a touch on the top side
                // of the surface is not on the top size of stream (it's a bit lower).
                float scale = previewSurfaceAspectRatio.toFloat() / previewStreamAspectRatio.toFloat();
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
        Size previewStreamSize = mEngine.getPreviewStreamSize(Reference.VIEW);
        referencePoint.x *= (float) previewStreamSize.getWidth() / referenceSize.getWidth();
        referencePoint.y *= (float) previewStreamSize.getHeight() / referenceSize.getHeight();
        return previewStreamSize;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @NonNull
    private Size applyPreviewToSensorRotation(@NonNull Size referenceSize, @NonNull PointF referencePoint) {
        // Not elegant, but the sin/cos way was failing for some reason.
        int angle = mEngine.getAngles().offset(Reference.SENSOR, Reference.VIEW, Axis.ABSOLUTE);
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
    private Size applyCropRegionCoordinates(@NonNull Size referenceSize, @NonNull PointF referencePoint) {
        // The input point and size refer to the stream rect.
        // The stream rect is part of the 'crop region', as described below.
        // https://source.android.com/devices/camera/camera3_crop_reprocess.html
        Rect cropRect = mBuilder.get(CaptureRequest.SCALER_CROP_REGION);
        // For now, we don't care about x and y position. Rect should be non-null, but let's be safe.
        int cropRectWidth = cropRect == null ? referenceSize.getWidth() : cropRect.width();
        int cropRectHeight = cropRect == null ? referenceSize.getHeight() : cropRect.height();
        // The stream is always centered inside the crop region, and one of the dimensions
        // should always match. We just increase the other one.
        referencePoint.x += (cropRectWidth - referenceSize.getWidth()) / 2F;
        referencePoint.y += (cropRectHeight - referenceSize.getHeight()) / 2F;
        return new Size(cropRectWidth, cropRectHeight);
    }

    @NonNull
    private Size applyActiveArrayCoordinates(@NonNull Size referenceSize, @NonNull PointF referencePoint) {
        // The input point and size refer to the scaler crop region.
        // We can query for the crop region position inside the active array, so this is easy.
        Rect cropRect = mBuilder.get(CaptureRequest.SCALER_CROP_REGION);
        referencePoint.x += cropRect == null ? 0 : cropRect.left;
        referencePoint.y += cropRect == null ? 0 : cropRect.top;
        // Finally, get the active rect width and height from characteristics.
        Rect activeRect = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (activeRect == null) { // Should never happen
            activeRect = new Rect(0, 0, referenceSize.getWidth(), referenceSize.getHeight());
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

    /**
     * True if we're metering. False if we're not, for example if we're waiting for
     * a reset call, or if {@link #startMetering(CaptureResult, PointF, Gesture)} was never called.
     * @return true if metering
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isMetering() {
        return mIsMetering;
    }

    /**
     * Should be called when we have partial or total CaptureResults,
     * but only while {@link #isMetering()} returns true.
     * @param result result
     */
    @SuppressWarnings("WeakerAccess")
    public void onCapture(@NonNull CaptureResult result) {
        if (!mIsMetering) return; // We're not interested in results anymore
        if (!(result instanceof TotalCaptureResult)) return; // Let's ignore these, contents are missing/wrong
        
        if (!mAutoFocus.isMetered()) mAutoFocus.onCapture(result);
        if (!mAutoExposure.isMetered()) mAutoExposure.onCapture(result);
        if (!mAutoWhiteBalance.isMetered()) mAutoWhiteBalance.onCapture(result);
        if (mAutoFocus.isMetered() && mAutoExposure.isMetered() && mAutoWhiteBalance.isMetered()) {
            LOG.i("onCapture:", "all MeteringParameters have converged. Dispatching onMeteringEnd");
            boolean success = mAutoFocus.isSuccessful()
                    && mAutoExposure.isSuccessful()
                    && mAutoWhiteBalance.isSuccessful();
            onMeteringEnd(success);
        } else if (System.currentTimeMillis() - mMeteringStartTime >= FORCED_END_DELAY) {
            LOG.i("onCapture:", "FORCED_END_DELAY was reached. Some MeteringParameter is stuck. Forcing end.");
            onMeteringEnd(false);
        }
    }
    
    private void onMeteringEnd(boolean success) {
        mCallback.onMeteringEnd(mPoint, mGesture, success);
        mIsMetering = false;
        mEngine.mHandler.remove(mResetRunnable);
        if (mEngine.shouldResetAutoFocus()) {
            mEngine.mHandler.post(mEngine.getAutoFocusResetDelay(), mResetRunnable);
        }
    }

    /**
     * Can be called to perform the reset at a time different than the one
     * specified by the {@link CameraEngine} reset delay.
     */
    @SuppressWarnings("WeakerAccess")
    public void resetMetering() {
        mEngine.mHandler.remove(mResetRunnable);
        if (mCallback.canResetMetering(mPoint, mGesture)) {
            LOG.i("Resetting the meter parameters.");
            MeteringRectangle whole = null;
            if (mPoint != null) {
                // If we have a point, we must reset the metering areas.
                Rect wholeRect = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                if (wholeRect == null) wholeRect = new Rect();
                whole = new MeteringRectangle(wholeRect, MeteringRectangle.METERING_WEIGHT_DONT_CARE);
            }
            mAutoFocus.resetMetering(mCharacteristics, mBuilder, whole);
            mAutoWhiteBalance.resetMetering(mCharacteristics, mBuilder, whole);
            mAutoExposure.resetMetering(mCharacteristics, mBuilder, whole);
            mCallback.onMeteringReset(mPoint, mGesture);
        }
    }

    private Runnable mResetRunnable = new Runnable() {
        @Override
        public void run() {
            resetMetering();
        }
    };
}
