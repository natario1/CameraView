package com.flurgle.camerakit;

/**
 * Simple pojo containing various camera properties.
 */
public class ExtraProperties {
    public final float verticalViewingAngle;
    public final float horizontalViewingAngle;

    public ExtraProperties(float verticalViewingAngle, float horizontalViewingAngle) {
        this.verticalViewingAngle = verticalViewingAngle;
        this.horizontalViewingAngle = horizontalViewingAngle;
    }
}
