package com.otaliastudios.cameraview.engine.offset;

import androidx.annotation.NonNull;

import com.otaliastudios.cameraview.controls.Facing;

public class Angles {

    private Facing mSensorFacing;
    private int mSensorOffset = 0;
    private int mDisplayOffset = 0;
    private int mDeviceOrientation = 0;

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
    }

    /**
     * Sets the display offset.
     * @param displayOffset the display offset
     */
    public void setDisplayOffset(int displayOffset) {
        sanitizeInput(displayOffset);
        mDisplayOffset = displayOffset;
    }

    /**
     * Sets the device orientation.
     * @param deviceOrientation the device orientation
     */
    public void setDeviceOrientation(int deviceOrientation) {
        sanitizeInput(deviceOrientation);
        mDeviceOrientation = deviceOrientation;
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

    /**
     * Old results:
     * offset(REF_SENSOR, REF_OUTPUT, Facing.BACK) = mSensorOffset + mDeviceOrientation
     * offset(REF_SENSOR, REF_OUTPUT, Facing.FRONT) = -mSensorOffset - mDeviceOrientation
     * offset(REF_SENSOR, REF_VIEW, Facing.BACK) = mSensorOffset - mDisplayOffset
     * offset(REF_SENSOR, REF_VIEW, Facing.FRONT) = mSensorOffset - mDisplayOffset
     * offset(REF_OUTPUT, REF_VIEW, Facing.BACK) = (mSensorOffset - mDisplayOffset) - (mSensorOffset + mDeviceOrientation) = -mDisplayOffset - mDeviceOrientation
     * offset(REF_OUTPUT, REF_VIEW, Facing.FRONT) = (mSensorOffset - mDisplayOffset) - (-mSensorOffset - mDeviceOrientation) = 2*mSensorOffset + mDisplayOffset + mDeviceOrientation
     */

    /**
     * New results:
     * offset(REF_SENSOR, REF_OUTPUT) = mDeviceOrientation + mSensorOffset
     * -> REF_SENSOR, REF_OUTPUT is typically wanted in the RELATIVE_TO_SENSOR axis.
     *
     * offset(REF_SENSOR, REF_VIEW) = -mDisplayOffset + mSensorOffset
     * -> REF_SENSOR, REF_VIEW is typically wanted in the ABSOLUTE axis.
     */

    private int absoluteOffset(@NonNull Reference from, @NonNull Reference to) {
        if (from == to) {
            return 0;
        } else if (to == Reference.BASE) {
            return 360 - absoluteOffset(to, from);
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
