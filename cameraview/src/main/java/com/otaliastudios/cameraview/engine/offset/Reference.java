package com.otaliastudios.cameraview.engine.offset;

public enum Reference {

    /**
     * The base reference system has its 'north' aligned with the device natural
     * orientation.
     */
    BASE,

    /**
     * This reference system has its 'north' aligned with the camera sensor.
     */
    SENSOR,

    /**
     * This reference system has its 'north' aligned with the View hierarchy.
     * This can be different than {@link #BASE} if the activity is allowed to rotate
     * (or forced into a non natural position).
     */
    VIEW,

    /**
     * This reference system has its 'north' aligned with the output picture/video.
     * This means that it takes into account the device orientation.
     */
    OUTPUT
}
