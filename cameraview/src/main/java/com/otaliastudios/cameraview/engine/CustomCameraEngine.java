package com.otaliastudios.cameraview.engine;

public abstract class CustomCameraEngine extends CameraBaseEngine {
    protected CustomCameraEngine() {
        super(new CallbackProxy());
    }

    public void setCallbacks(Callback callbacks) {
        ((CallbackProxy) getCallback()).setCallbacks(callbacks);
    }
}
