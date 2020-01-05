---
layout: page
title: "Camera Events"
description: "Dealing with the camera lifecycle and callbacks"
order: 1
disqus: 1
---

The camera engine will notify anyone about camera events that took place, either on their own or
after developer action. To access these events, set up one or more `CameraListener` instances.

All actions taken on a `CameraView` instance are asynchronous, which means that the callback can be
executed at any time in the future. For convenience, all of them are executed on the UI thread.

```java
camera.addCameraListener(new CameraListener() {

    public void onCameraOpened(CameraOptions options) {}

    public void onCameraClosed() {}

    public void onCameraError(CameraException error) {}

    public void onPictureTaken(PictureResult result) {}

    public void onVideoTaken(VideoResult result) {}
    
    public void onOrientationChanged(int orientation) {}

    public void onAutoFocusStart(PointF point) {}
    
    public void onAutoFocusEnd(boolean successful, PointF point) {}
    
    public void onZoomChanged(float newValue, float[] bounds, PointF[] fingers) {}
    
    public void onExposureCorrectionChanged(float newValue, float[] bounds, PointF[] fingers) {}

    public void onVideoRecordingStart() {}
    
    public void onVideoRecordingEnd() {}
});
```

### Lifecycle

CameraView has its own lifecycle, which is basically made of an open and a closed state.
You will listen to these events using `onCameraOpened` and `onCameraClosed` callbacks:

```java
camera.addCameraListener(new CameraListener() {

    /**
     * Notifies that the camera was opened.
     * The options object collects all supported options by the current camera.
     */
    @Override
    public void onCameraOpened(CameraOptions options) {}

    /**
     * Notifies that the camera session was closed.
     */
    @Override
    public void onCameraClosed() {}

});
```

The open callback is especially important because the `CameraOptions` includes all the available
options of the current sensor. This can be used to adjust the UI, for example, show a flash icon
if flash is supported.

### Related APIs

|Method|Description|
|------|-----------|
|`open()`|Starts the engine. This will cause a future call to `onCameraOpened()` (or an error)|
|`close()`|Stops the engine. This will cause a future call to `onCameraClosed()`|
|`isOpened()`|Returns true if `open()` was called successfully. This does not mean that camera is showing preview already.|
|`getCameraOptions()`|If camera was opened, returns non-null object with information about what is supported.|

Take a look at public methods in `CameraOptions` to know more.
