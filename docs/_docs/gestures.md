---
layout: page
title: "Gestures"
description: "Gestures control"
order: 5
disqus: 1
---

`CameraView` listen to lots of different gestures inside its bounds. You have the chance to map
these gestures to particular actions or camera controls, using the `mapGesture()` method.
This lets you emulate typical behaviors in a single line:

```java
cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM); // Pinch to zoom!
cameraView.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS); // Tap to focus!
cameraView.mapGesture(Gesture.LONG_TAP, GestureAction.TAKE_PICTURE); // Long tap to shoot!
```

Simple as that. There are two things to be noted:

- Not every mapping is valid. For example, you can't control zoom with long taps, or start focusing by pinching.
- Some actions might not be supported by the sensor. Check out `CameraOptions` to know what's legit and what's not.

|Gesture|Description|Can be mapped to|
|-------------|-----------|----------------|
|`PINCH`|Pinch gesture, typically assigned to the zoom control.|`ZOOM` `EXPOSURE_CORRECTION` `FILTER_CONTROL_1` `FILTER_CONTROL_2` `NONE`|
|`TAP`|Single tap gesture, typically assigned to the focus control.|`AUTO_FOCUS` `TAKE_PICTURE` `TAKE_PICTURE_SNAPSHOT` `NONE`|
|`LONG_TAP`|Long tap gesture.|`AUTO_FOCUS` `TAKE_PICTURE` `TAKE_PICTURE_SNAPSHOT` `NONE`|
|`SCROLL_HORIZONTAL`|Horizontal movement gesture.|`ZOOM` `EXPOSURE_CORRECTION` `FILTER_CONTROL_1` `FILTER_CONTROL_2` `NONE`|
|`SCROLL_VERTICAL`|Vertical movement gesture.|`ZOOM` `EXPOSURE_CORRECTION` `FILTER_CONTROL_1` `FILTER_CONTROL_2` `NONE`|

### Gesture Actions

Looking at this from the other side:

|Gesture action|Description|Can be mapped to|
|--------------|-----------|----------------|
|`NONE`|Disables this gesture.|`TAP` `LONG_TAP` `PINCH` `SCROLL_HORIZONTAL` `SCROLL_VERTICAL`|
|`AUTO_FOCUS`|Launches a [touch metering operation](metering#touch-metering) on the finger position.|`TAP` `LONG_TAP`|
|`TAKE_PICTURE`|Takes a picture using [takePicture](capturing-media).|`TAP` `LONG_TAP`|
|`TAKE_PICTURE_SNAPSHOT`|Takes a picture using [takePictureSnapshot](capturing-media).|`TAP` `LONG_TAP`|
|`ZOOM`|[Zooms](controls#zoom) in or out.|`PINCH` `SCROLL_HORIZONTAL` `SCROLL_VERTICAL`|
|`EXPOSURE_CORRECTION`|Controls the [exposure correction](metering#exposure-correction).|`PINCH` `SCROLL_HORIZONTAL` `SCROLL_VERTICAL`|
|`FILTER_CONTROL_1`|Controls the first parameter (if any) of a [real-time filter](filters).|`PINCH` `SCROLL_HORIZONTAL` `SCROLL_VERTICAL`|
|`FILTER_CONTROL_2`|Controls the second parameter (if any) of a [real-time filter](filters).|`PINCH` `SCROLL_HORIZONTAL` `SCROLL_VERTICAL`|

### XML Attributes

```xml
<com.otaliastudios.cameraview.CameraView
    app:cameraGesturePinch="zoom|exposureCorrection|filterControl1|filterControl2|none"
    app:cameraGestureTap="autoFocus|takePicture|takePictureSnapshot|none"
    app:cameraGestureLongTap="autoFocus|takePicture|takePictureSnapshot|none"
    app:cameraGestureScrollHorizontal="zoom|exposureCorrection|filterControl1|filterControl2|none"
    app:cameraGestureScrollVertical="zoom|exposureCorrection|filterControl1|filterControl2|none"/>
```

### Related APIs

|Method|Description|
|------|-----------|
|`mapGesture(Gesture, GestureAction)`|Maps a certain gesture to a certain action. No-op if the action is not supported.|
|`getGestureAction(Gesture)`|Returns the action currently mapped to the given gesture.|
|`clearGesture(Gesture)`|Clears any action mapped to the given gesture.|

