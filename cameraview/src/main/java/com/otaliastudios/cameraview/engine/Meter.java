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
        void onMeteringStarted(@NonNull PointF point, @Nullable Gesture gesture);

        /**
         * Notifies that metering has ended. No action is required for implementors.
         * From now on, {@link #isMetering()} will return false so the meter should not
         * be passed capture results anymore.
         * @param point point
         * @param gesture gesture
         * @param success success
         */
        void onMeteringEnd(@NonNull PointF point, @Nullable Gesture gesture, boolean success);

        /**
         * Notifies that metering has been reset. From now on, this meter instance
         * is done, although in theory it could be reused by calling
         * {@link #startMetering(PointF, Gesture)} again.
         * @param point point
         * @param gesture gesture
         */
        void onMeteringReset(@NonNull PointF point, @Nullable Gesture gesture);

        /**
         * Whether metering can be reset. Since it happens at a future time, this should
         * return true if the engine is still in a legit state for this operation.
         * @param point point
         * @param gesture gesture
         * @return true if can reset
         */
        // TODO is this useful? engine could do its checks onMeteringReset()
        boolean canResetMetering(@NonNull PointF point, @Nullable Gesture gesture);
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

    @NonNull
    private <T> T readCharacteristic(@NonNull CameraCharacteristics.Key<T> key, @NonNull T fallback) {
        T value = mCharacteristics.get(key);
        return value == null ? fallback : value;
    }

    /**
     * Starts a metering sequence.
     * @param point point
     * @param gesture gesture
     */
    @SuppressWarnings("WeakerAccess")
    public void startMetering(@NonNull PointF point, @Nullable Gesture gesture) {
        mPoint = point;
        mGesture = gesture;
        mIsMetering = true;

        // This is a good Q/A. https://stackoverflow.com/a/33181620/4288782
        // At first, the point is relative to the View system and does not account our own cropping.
        // Will keep updating these two below.
        PointF referencePoint = new PointF(mPoint.x, mPoint.y);
        Size referenceSize /* = previewSurfaceSize */;

        // 1. Account for cropping.
        Size previewStreamSize = mEngine.getPreviewStreamSize(Reference.VIEW);
        Size previewSurfaceSize = mEngine.mPreview.getSurfaceSize();
        if (previewStreamSize == null) throw new IllegalStateException("getPreviewStreamSize should not be null at this point.");
        AspectRatio previewStreamAspectRatio = AspectRatio.of(previewStreamSize);
        AspectRatio previewSurfaceAspectRatio = AspectRatio.of(previewSurfaceSize);
        if (mEngine.mPreview.isCropping()) {
            if (previewStreamAspectRatio.toFloat() > previewSurfaceAspectRatio.toFloat()) {
                // Stream is larger. The x coordinate must be increased: a touch on the left side
                // of the surface is not on the left size of stream (it's more to the right).
                float scale = previewStreamAspectRatio.toFloat() / previewSurfaceAspectRatio.toFloat();
                referencePoint.x += previewSurfaceSize.getWidth() * (scale - 1F) / 2F;

            } else {
                // Stream is taller. The y coordinate must be increased: a touch on the top side
                // of the surface is not on the top size of stream (it's a bit lower).
                float scale = previewSurfaceAspectRatio.toFloat() / previewStreamAspectRatio.toFloat();
                referencePoint.x += previewSurfaceSize.getHeight() * (scale - 1F) / 2F;
            }
        }

        // 2. Scale to the stream coordinates (not the surface).
        referencePoint.x *= (float) previewStreamSize.getWidth() / previewSurfaceSize.getWidth();
        referencePoint.y *= (float) previewStreamSize.getHeight() / previewSurfaceSize.getHeight();
        referenceSize = previewStreamSize;

        // 3. Rotate to the stream coordinate system.
        // Not elegant, but the sin/cos way was failing.
        int angle = mEngine.getAngles().offset(Reference.SENSOR, Reference.VIEW, Axis.ABSOLUTE);
        boolean flip = angle % 180 != 0;
        float tempX = referencePoint.x; float tempY = referencePoint.y;
        if (angle == 0) {
            referencePoint.x = tempX;
            referencePoint.y = tempY;
        } else if (angle == 90) {
            //noinspection SuspiciousNameCombination
            referencePoint.x = tempY;
            referencePoint.y = referenceSize.getWidth() - tempX;
        } else if (angle == 180) {
            referencePoint.x = referenceSize.getWidth() - tempX;
            referencePoint.y = referenceSize.getHeight() - tempY;
        } else if (angle == 270) {
            referencePoint.x = referenceSize.getHeight() - tempY;
            //noinspection SuspiciousNameCombination
            referencePoint.y = tempX;
        } else {
            throw new IllegalStateException("Unexpected angle " + angle);
        }
        referenceSize = flip ? referenceSize.flip() : referenceSize;

        // These points are now referencing the stream rect on the sensor array.
        // But we still have to figure out how the stream rect is laid on the sensor array.
        // https://source.android.com/devices/camera/camera3_crop_reprocess.html
        // For sanity, let's assume it is centered.
        // For sanity, let's also assume that the crop region is equal to the stream region.

        // 4. Move to the active sensor array coordinate system.
        Rect activeRect = readCharacteristic(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                new Rect(0, 0, referenceSize.getWidth(), referenceSize.getHeight()));
        referencePoint.x += (activeRect.width() - referenceSize.getWidth()) / 2F;
        referencePoint.y += (activeRect.height() - referenceSize.getHeight()) / 2F;
        referenceSize = new Size(activeRect.width(), activeRect.height());

        // 5. Account for zoom! This only works for mZoomValue = 0.
        // We must scale down with respect to the reference size center. If mZoomValue = 1,
        // This must leave everything unchanged.
        float maxZoom = readCharacteristic(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM,
                1F /* no zoom */);
        float currZoom = 1 + mEngine.mZoomValue * (maxZoom - 1); // 1 ... maxZoom
        float currReduction = 1 / currZoom;
        float referenceCenterX = referenceSize.getWidth() / 2F;
        float referenceCenterY = referenceSize.getHeight() / 2F;
        referencePoint.x = referenceCenterX + currReduction * (referencePoint.x - referenceCenterX);
        referencePoint.y = referenceCenterY + currReduction * (referencePoint.y - referenceCenterY);

        // 6. NOW we can compute the metering regions.
        float visibleWidth = referenceSize.getWidth() * currReduction;
        float visibleHeight = referenceSize.getHeight() * currReduction;
        MeteringRectangle area1 = createMeteringRectangle(referencePoint, referenceSize, visibleWidth, visibleHeight, 0.05F, 1000);
        MeteringRectangle area2 = createMeteringRectangle(referencePoint, referenceSize, visibleWidth, visibleHeight, 0.1F, 100);
        List<MeteringRectangle> areas = Arrays.asList(area1, area2);

        // 7. And finally dispatch everything
        mAutoFocus.startMetering(mCharacteristics, mBuilder, areas);
        mAutoWhiteBalance.startMetering(mCharacteristics, mBuilder, areas);
        mAutoExposure.startMetering(mCharacteristics, mBuilder, areas);

        // Dispatch to callback
        mCallback.onMeteringStarted(mPoint, mGesture);
        mMeteringStartTime = System.currentTimeMillis();
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
            @NonNull PointF center, @NonNull Size boundaries,
            float visibleWidth, float visibleHeight,
            float factor, int weight) {
        float halfWidth = factor * visibleWidth / 2F;
        float halfHeight = factor * visibleHeight / 2F;
        return new MeteringRectangle(
                (int) Math.max(0, center.x - halfWidth),
                (int) Math.max(0, center.y - halfHeight),
                (int) Math.min(boundaries.getWidth(), halfWidth * 2F),
                (int) Math.min(boundaries.getHeight(), halfHeight * 2F),
                weight
        );
    }

    /**
     * True if we're metering. False if we're not, for example if we're waiting for
     * a reset call, or if {@link #startMetering(PointF, Gesture)} was never called.
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
            // Use the AF success for dispatching the callback, since the public
            // callback is currently related to AF.
            LOG.i("onCapture:", "all MeteringParameters have converged. Dispatching onMeteringEnd");
            onMeteringEnd(mAutoFocus.isSuccessful());
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
            Rect whole = readCharacteristic(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, new Rect());
            MeteringRectangle rectangle = new MeteringRectangle(whole, MeteringRectangle.METERING_WEIGHT_DONT_CARE);
            mAutoFocus.resetMetering(mCharacteristics, mBuilder, rectangle);
            mAutoWhiteBalance.resetMetering(mCharacteristics, mBuilder, rectangle);
            mAutoExposure.resetMetering(mCharacteristics, mBuilder, rectangle);
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
