package com.otaliastudios.cameraview.engine.offset;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.controls.Facing;

/**
 * Manages offsets between different {@link Reference} systems.
 *
 * These offsets are computed based on the {@link #setSensorOffset(Facing, int)},
 * {@link #setDisplayOffset(int)} and {@link #setDeviceOrientation(int)} values that are coming
 * from outside.
 *
 * When communicating with the sensor, {@link Axis#RELATIVE_TO_SENSOR} should probably be used.
 * This means inverting the offset when using the front camera.
 * This is often the case when calling offset(SENSOR, OUTPUT), for example when passing a JPEG
 * rotation to the sensor. That is meant to be consumed as relative to the sensor plane.
 *
 * For all other usages, {@link Axis#ABSOLUTE} is probably a better choice.
 */
public class Angles {

    private final static String TAG = Angles.class.getSimpleName();
    private final static CameraLogger LOG = CameraLogger.create(TAG);

    private Facing mSensorFacing;
    @VisibleForTesting int mSensorOffset = 0;
    @VisibleForTesting int mDisplayOffset = 0;
    @VisibleForTesting int mDeviceOrientation = 0;

    /**
     * We want to keep everything in the {@link Axis#ABSOLUTE} reference,
     * so a front facing sensor offset must be inverted.
     *
     * @param sensorFacing sensor facing value
     * @param sensorOffset sensor offset
     */
    public void setSensorOffset(@NonNull Facing sensorFacing, int sensorOffset) {
        sanitizeInput(sensorOffset);
        mSensorFacing = sensorFacing;
        mSensorOffset = sensorOffset;
        if (mSensorFacing == Facing.FRONT) {
            mSensorOffset = sanitizeOutput(360 - mSensorOffset);
        }
        print();
    }

    /**
     * Sets the display offset.
     * @param displayOffset the display offset
     */
    public void setDisplayOffset(int displayOffset) {
        sanitizeInput(displayOffset);
        mDisplayOffset = displayOffset;
        print();
    }

    /**
     * Sets the device orientation.
     * @param deviceOrientation the device orientation
     */
    public void setDeviceOrientation(int deviceOrientation) {
        sanitizeInput(deviceOrientation);
        mDeviceOrientation = deviceOrientation;
        print();
    }

    private void print() {
        LOG.i("Angles changed:",
                "sensorOffset:", mSensorOffset,
                "displayOffset:", mDisplayOffset,
                "deviceOrientation:", mDeviceOrientation);
    }

    /**
     * Returns the offset between two reference systems, computed along the given axis.
     * @param from the source reference system
     * @param to the destination reference system
     * @param axis the axis
     * @return the offset
     */
    public int offset(@NonNull Reference from, @NonNull Reference to, @NonNull Axis axis) {
        int offset = absoluteOffset(from, to);
        if (axis == Axis.RELATIVE_TO_SENSOR) {
            if (mSensorFacing == Facing.FRONT) {
                offset = sanitizeOutput(360 - offset);
            }
        }
        return offset;
    }

    private int absoluteOffset(@NonNull Reference from, @NonNull Reference to) {
        if (from == to) {
            return 0;
        } else if (to == Reference.BASE) {
            return sanitizeOutput(360 - absoluteOffset(to, from));
        } else if (from == Reference.BASE) {
            switch (to) {
                case VIEW: return sanitizeOutput(360 - mDisplayOffset);
                case OUTPUT: return sanitizeOutput(mDeviceOrientation);
                case SENSOR: return sanitizeOutput(360 - mSensorOffset);
                default: throw new RuntimeException("Unknown reference: " + to);
            }
        } else {
            return sanitizeOutput(
                    absoluteOffset(Reference.BASE, to)
                    - absoluteOffset(Reference.BASE, from));
        }
    }

    /**
     * Whether the two references systems are flipped.
     * @param from source
     * @param to destination
     * @return true if flipped
     */
    public boolean flip(@NonNull Reference from, @NonNull Reference to) {
        return offset(from, to, Axis.ABSOLUTE) % 180 != 0;
    }

    private void sanitizeInput(int value) {
        if (value != 0
                && value != 90
                && value != 180
                && value != 270) {
            throw new IllegalStateException("This value is not sanitized: " + value);
        }
    }

    private int sanitizeOutput(int value) {
        return (value + 360) % 360;
    }
}
