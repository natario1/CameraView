package com.otaliastudios.cameraview.engine.offset;

/**
 * The axis around which offsets are computed. We have two possibilities:
 * - an axis going out of the device screen towards the user's face
 * - an axis going out of the device screen towards the back
 *
 * We are mostly interested in the first one, but some APIs will require
 * angles in the sensor reference, in which case, for front cameras, we are
 * required to use {@link #RELATIVE_TO_SENSOR}.
 */
public enum Axis {

    /**
     * This rotation axis is the one going out of the device screen
     * towards the user's face.
     */
    ABSOLUTE,

    /**
     * This rotation axis takes into account the current
     * {@link com.otaliastudios.cameraview.controls.Facing} value.
     *
     * - for {@link com.otaliastudios.cameraview.controls.Facing#BACK}, this is {@link #ABSOLUTE}
     * - for {@link com.otaliastudios.cameraview.controls.Facing#FRONT}, this is inverted
     */
    RELATIVE_TO_SENSOR
}
