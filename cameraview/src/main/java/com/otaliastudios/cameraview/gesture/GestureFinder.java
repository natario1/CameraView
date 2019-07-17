package com.otaliastudios.cameraview.gesture;

import android.content.Context;
import android.graphics.PointF;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import android.view.MotionEvent;

/**
 * Base class for gesture finders.
 * Gesture finders are passed down touch events to detect gestures.
 */
public abstract class GestureFinder {

    public interface Controller {
        @NonNull Context getContext();
        int getWidth();
        int getHeight();
    }

    // The number of possible values between minValue and maxValue, for the getValue method.
    // We could make this non-static (e.g. larger granularity for exposure correction).
    private final static int GRANULARITY = 50;

    private boolean mActive;
    @VisibleForTesting Gesture mType;
    private PointF[] mPoints;
    private Controller mController;

    GestureFinder(@NonNull Controller controller, int points) {
        mController = controller;
        mPoints = new PointF[points];
        for (int i = 0; i < points; i++) {
            mPoints[i] = new PointF(0, 0);
        }
    }

    /**
     * Makes this instance active, which means, listening to events.
     * @param active whether this should be active or not
     */
    public void setActive(boolean active) {
        mActive = active;
    }

    /**
     * Whether this instance is active, which means, it is listening
     * to events and identifying new gestures.
     * @return true if active
     */
    public boolean isActive() {
        return mActive;
    }

    /**
     * Called when new events are available.
     * If true is returned, users will call {@link #getGesture()}, {@link #getPoints()}
     * and maybe {@link #getValue(float, float, float)} to know more about the gesture.
     *
     * @param event the new event
     * @return true if a gesture was detected
     */
    public final boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!mActive) return false;
        return handleTouchEvent(event);
    }

    /**
     * Called when new events are available.
     * If true is returned, users will call {@link #getGesture()}, {@link #getPoints()}
     * and maybe {@link #getValue(float, float, float)} to know more about the gesture.
     *
     * @param event the new event
     * @return true if a gesture was detected
     */
    protected abstract boolean handleTouchEvent(@NonNull MotionEvent event);

    /**
     * Returns the gesture that this instance is currently detecting.
     * This is mutable - for instance, a scroll layout can detect both
     * horizontal and vertical scroll gestures.
     *
     * @return the current gesture
     */
    @NonNull
    public final Gesture getGesture() {
        return mType;
    }

    /**
     * Sets the currently detected gesture.
     * @see #getGesture()
     *
     * @param gesture the current gesture
     */
    protected final void setGesture(Gesture gesture) {
        mType = gesture;
    }

    /**
     * Returns an array of points that identify the currently
     * detected gesture. If no gesture was detected, this returns
     * an array of points with x and y set to 0.
     *
     * @return array of gesture points
     */
    @NonNull
    public final PointF[] getPoints() {
        return mPoints;
    }

    /**
     * Utility function to access an item in the
     * {@link #getPoints()} array.
     *
     * @param which the array position
     * @return the point
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected final PointF getPoint(int which) {
        return mPoints[which];
    }

    /**
     * For {@link GestureType#CONTINUOUS} gestures, returns the float value at the current
     * gesture state. This means, for example, scaling the old value with a pinch factor,
     * taking into account the minimum and maximum values.
     *
     * @param currValue the last value
     * @param minValue the min possible value
     * @param maxValue the max possible value
     * @return the new continuous value
     */
    public final float computeValue(float currValue, float minValue, float maxValue) {
        return capValue(currValue, getValue(currValue, minValue, maxValue), minValue, maxValue);
    }

    /**
     * For {@link GestureType#CONTINUOUS} gestures, returns the float value at the current
     * gesture state. This means, for example, scaling the old value with a pinch factor,
     * taking into account the minimum and maximum values.
     *
     * @param currValue the last value
     * @param minValue the min possible value
     * @param maxValue the max possible value
     * @return the new continuous value
     */
    protected abstract float getValue(float currValue, float minValue, float maxValue);

    /**
     * Checks for newValue to be between minValue and maxValue,
     * and checks that it is 'far enough' from the oldValue, in order
     * to reduce useless updates.
     */
    private static float capValue(float oldValue, float newValue, float minValue, float maxValue) {
        if (newValue < minValue) newValue = minValue;
        if (newValue > maxValue) newValue = maxValue;

        float distance = (maxValue - minValue) / (float) GRANULARITY;
        float half = distance / 2;
        if (newValue >= oldValue - half && newValue <= oldValue + half) {
            // Too close! Return the oldValue.
            return oldValue;
        }
        return newValue;
    }

    /**
     * Returns the controller for this finder.
     * @return the controller
     */
    @NonNull
    protected Controller getController() {
        return mController;
    }
}
