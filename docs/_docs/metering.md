---
layout: page
title: "Metering"
description: "Exposure and metering controls"
order: 4
disqus: 1
---

In CameraView grammar, metering is the act of measuring the scene brightness, colors and focus
distance in order to automatically adapt the camera exposure, focus and white balance (AE, AF and AWB,
often referred as 3A).

We treat three different types on metering: [continuous metering](#continuous-metering), 
[picture metering](#picture-metering) and [touch metering](#touch-metering). You can also apply
adjustment to the metered exposure through the [exposure correction](#exposure-correction) control.

### Continuous Metering

By default, and if the device supports it, all three routines (AE, AF, AWB) are continuously metered
as the device moves or the scene changes.

- For AE, this is always enabled if supported
- For AF, this is always enabled if supported
- For AWB, this is enabled if the `WhiteBalance` parameter is set to `AUTO` [[docs]](controls#camerawhitebalance)

### Picture Metering

> In Camera1, picture metering is always enabled for pictures, and always disabled for picture snapshots. 
The following applies to Camera2 only.

The camera engine will try to trigger metering when a picture is requested, either with `takePicture()`
or `takePictureSnapshot()`. This has two obvious consequences:

- improves the picture quality
- increases the latency, because metering takes time

For these reasons, picture metering is **enabled** by default for HQ pictures and **disabled** by
default for picture snapshots. However, the behavior can be changed with two flags and their
respective XML attributes:

```java
cameraView.setPictureMetering(true); // Meter before takePicture()
cameraView.setPictureMetering(false); // Don't
cameraView.setPictureSnapshotMetering(true); // Meter before takePictureSnapshot()
cameraView.setPictureSnapshotMetering(false); // Don't
```

### Touch Metering

Touch metering is triggered by either a [Gesture](gestures) or by the developer itself, which
can start touch metering on a specific point with the `startAutoFocus()` API.
This action needs the coordinates of a point or region computed with respect to the view width and height.

```java
// Start touch metering at the center:
cameraView.startAutoFocus(cameraView.getWidth() / 2F, cameraView.getHeight() / 2F);
// Start touch metering within a given area,
// like the bounding box of a face.
 cameraView.startAutoFocus(rect);
```

In both cases, the metering callbacks will be triggered:

```java
cameraView.addCameraListener(new CameraListener() {
    
    @Override
    public void onAutoFocusStart(@NonNull PointF point) {
        // Touch metering was started by a gesture or by startAutoFocus(float, float).
        // The camera is currently trying to meter around that area.
        // This can be used to draw things on screen.
    }

    @Override
    public void onAutoFocusEnd(boolean successful, @NonNull PointF point) {
        // Touch metering operation just ended. If successful, the camera will have converged
        // to a new focus point, and possibly new exposure and white balance as well.
        // The point is the same that was passed to onAutoFocusStart.
    }
});
```

Touch metering is not guaranteed to be supported: check the `CameraOptions` to be sure.

##### Touch Metering Markers

You can set a marker for drawing on screen in response to touch metering events.
In XML, you should pass the qualified class name of your marker.

```java
cameraView.setAutoFocusMarker(null);
cameraView.setAutoFocusMarker(marker);
```

We offer a default marker (similar to the old `focusWithMarker` attribute in v1),
which you can set in XML using the `@string/cameraview_default_autofocus_marker` resource,
or programmatically:

```java
cameraView.setAutoFocusMarker(new DefaultAutoFocusMarker());
```

##### Touch Metering Reset Delay

You control how a touch metering operation is reset after being completed.
Setting a negative value (or 0, or `Long.MAX_VALUE`) will not reset the metering values.
This is useful for low end devices that have slow auto-focus capabilities.
Defaults to 3 seconds.

```java
cameraView.setCameraAutoFocusResetDelay(1000);  // 1 second
cameraView.setCameraAutoFocusResetDelay(0);  // NO reset
cameraView.setCameraAutoFocusResetDelay(-1);  // NO reset
cameraView.setCameraAutoFocusResetDelay(Long.MAX_VALUE);  // NO reset
```

### Exposure correction

There are two ways to control the exposure correction value:

- User can change the exposure correction with a [Gesture](gestures)
- The developer can change this value with the `setExposureCorrection(float)` API, passing in the EV
  value, in camera stops. This value should be contained in the minimum and maximum supported values,
  as returned by `CameraOptions`.

Both actions will trigger the exposure correction callback, which can be used, for example, to draw a seek bar:

```java
cameraView.addCameraListener(new CameraListener() {
    
    @UiThread
    public void onExposureCorrectionChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
        // newValue: the new correction value
        // bounds: min and max bounds for newValue, as returned by CameraOptions
        // fingers: finger positions that caused the event, null if not caused by touch
    }
});
```

EV correction is not guaranteed to be supported: check the `CameraOptions` to be sure.

### Related XML Attributes

```xml
<com.otaliastudios.cameraview.CameraView
    app:cameraPictureMetering="true|false"
    app:cameraPictureSnapshotMetering="false|true"
    app:cameraAutoFocusMarker="@string/cameraview_default_autofocus_marker"
    app:cameraAutoFocusResetDelay="3000"/>
```

### Related APIs

|Method|Description|
|------|-----------|
|`setPictureMetering(boolean)`|Whether the engine should trigger 3A metering when a picture is requested. Defaults to true.|
|`setPictureSnapshotMetering(boolean)`|Whether the engine should trigger 3A metering when a picture snapshot is requested. Defaults to false.|
|`startAutoFocus(float, float)`|Starts the 3A touch metering routine at the given coordinates, with respect to the view system.|
|`startAutoFocus(RectF)`|Starts the 3A touch metering routine for the given area, defined with respect to the view system.|
|`CameraOptions.isAutoFocusSupported()`|Whether touch metering (metering with respect to a specific region of the screen) is supported.|
|`setExposureCorrection(float)`|Changes the exposure adjustment, in EV stops. A positive value means a brighter picture.|
|`CameraOptions.getExposureCorrectionMinValue()`|The minimum value of negative exposure correction, in EV stops.|
|`CameraOptions.getExposureCorrectionMaxValue()`|The maximum value of positive exposure correction, in EV stops.|