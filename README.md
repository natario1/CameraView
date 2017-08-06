
*A fork of [Dylan McIntyre's CameraKit-Android library](https://github.com/gogopop/CameraKit-Android), originally a fork of [Google's CameraView library](https://github.com/google/cameraview). The CameraKit-Android at this point has been completely rewritten and refactored:*

- lots *of serious bugs fixed*
- *decent orientation support for both pictures and videos*
- *EXIF support*
- *real tap-to-focus support*
- *pinch-to-zoom support*
- *simpler APIs, docs and heavily commented code*
- *new `captureSnapshot` API*
- *new `setLocation` and `setWhiteBalance` APIs*
- *new `setGrid` APIs, to draw 3x3, 4x4 or golden ratio grids
- *option to pass a `File` when recording a video*
- *other minor API additions*
- *replacing Method and Permissions stuff with simpler `sessionType`* 
- *smart measuring and sizing behavior, replacing bugged `adjustViewBounds`*
- *measure `CameraView` as center crop or center inside*
- *add multiple `CameraListener`s for events*

*Feel free to open issues with suggestions or contribute.*

# CameraKit

CameraKit is an easy to use utility to work with the Android Camera APIs. Everything at the moment is work in progress, but it works well for pictures at least.

<p>
  <img src="art/screen1.png" width="250" vspace="20" hspace="5">
  <img src="art/screen2.png" width="250" vspace="20" hspace="5">
</p>

## Table of Contents

- [Features](#features)
- [Setup](#setup)
- [Usage](#usage)
  - [Capturing Images](#capturing-images)
  - [Capturing Video](#capturing-video)
  - [Other camera events](#other-camera-events)
- [Dynamic Sizing Behavior](#dynamic-sizing-behavior)
  - [Center Inside](#center-inside)
  - [Center Crop](#center-crop)
- [Extra Attributes](#extra-attributes)
  - [cameraSessionType](#camerasessiontype)
  - [cameraFacing](#camerafacing)
  - [cameraFlash](#cameraflash)
  - [cameraFocus](#camerafocus)
  - [cameraZoomMode](#camerazoommode)
  - [cameraCropOutput](#cameracropoutput)
  - [cameraJpegQuality](#camerajpegquality)
  - [cameraWhiteBalance](#camerawhitebalance)
  - [cameraGrid](#cameragrid)
- [Permissions Behavior](#permissions-behavior)
- [Manifest file](#manifest-file)
- [Roadmap](#roadmap)

# Features

- Seamless image and video capturing, even within the same session
- System permission handling
- Dynamic sizing behavior
  - Create a `CameraView` of any size (not just presets!)
  - Center inside or center crop behaviors
  - Automatic output cropping to match your `CameraView` bounds
- Built-in tap to focus
- Built-in pinch to zoom
- Built-in grid drawing (3x3, 4x4, golden ratio)
- Control the camera parameters via XML or programmatically
- Multiple capture methods
  - Take high-resolution pictures with `capturePicture`
  - Take quick snapshots as a freeze frame of the preview with `captureSnapshot` (similar to SnapChat and Instagram)
- `CameraUtils` to help with Bitmaps and orientations
- EXIF support
  - Automatically detected orientation tag
  - Plug in location tags with `CameraView.setLocation(double, double)` to pictures and videos

## Setup

For now, you must clone the repo and add it to your project.

## Usage

To use CameraKit, simply add a `CameraView` to your layout:

```xml
<com.flurgle.camerakit.CameraView
    android:id="@+id/camera"
    android:keepScreenOn="true"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Make sure you override `onResume`, `onPause` and  `onDestroy` in your activity, and call `CameraView.start()`, `stop()` and `destroy()`.

```java
@Override
protected void onResume() {
    super.onResume();
    cameraView.start();
}

@Override
protected void onPause() {
    super.onPause();
    cameraView.stop();
}

@Override
protected void onDestroy() {
    super.onDestroy();
    cameraView.destroy();
}
```

### Capturing Images

To capture an image just call `CameraView.capturePicture()`. Make sure you setup a `CameraListener` to handle the image callback.

```java
camera.setCameraListener(new CameraListener() {
    @Override
    public void onPictureTaken(byte[] picture) {
        // Create a bitmap or a file...
        // CameraUtils will read EXIF orientation for you, in a worker thread.
        CameraUtils.decodeBitmap(picture, ...);
    }
});

camera.capturePicture();
```

You can also use `camera.captureSnapshot()` to capture a preview frame. This is faster, though will ensure lower quality output.

### Capturing Video

To capture video just call `CameraView.startRecordingVideo(file)` to start, and `CameraView.stopRecordingVideo()` to finish. Make sure you setup a `CameraListener` to handle the video callback.

```java
camera.setCameraListener(new CameraListener() {
    @Override
    public void onVideoTaken(File video) {
        // The File is the same you passed before.
        // Now it holds a MP4 video.
    }
});

File file = ...; // Make sure you have permissions to write here.
camera.startRecordingVideo(file);
camera.postDelayed(new Runnable() {
    @Override
    public void run() {
        // This will trigger onVideoTaken().
        camera.stopRecordingVideo();
    }
}, 2500);

// Shorthand:
camera.startRecordingVideo(file, 2500);
```

### Other camera events

Make sure you can react to different camera events by setting up a `CameraListener` instance.

```java
camera.setCameraListener(new CameraListener() {

    @Override
    public void onCameraOpened(CameraOptions options) {}

    @Override
    public void onCameraClosed() {}

    @Override
    public void onPictureTaken(byte[] picture) {}

    @Override
    public void onVideoTaken(File video) {}
    
    @Override
    public void onFocusStart(float x, float y) {}
    
    @Override
    public void onFocusEnd(boolean successful, float x, float y) {}
    
    @Override
    public void onZoomChanged(float zoomValue, PointF[] fingers) {}

});
```

## Dynamic Sizing Behavior

`CameraView` has a smart measuring behavior that will let you do what you want with a few flags.
Measuring is controlled simply by `layout_width` and `layout_height` attributes, with this meaning:

- `WRAP_CONTENT` : try to stretch this dimension to respect the preview aspect ratio.
- `MATCH_PARENT` : fill this dimension, even if this means ignoring the aspect ratio.
- Fixed values (e.g. `500dp`) : respect this dimension.

You can have previews of all sizes, not just the supported presets. Whaterever you do, the preview will never be distorted. 

### Center inside

You can emulate a **center inside** behavior (like the `ImageView` scaletype) by setting both dimensions to `wrap_content`. The camera will get the biggest possible size that fits into your bounds, just like what happens with image views.


```xml
<com.flurgle.camerakit.CameraView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

This means that the whole preview is visible, and the image output matches what was visible during the capture.

### Center crop

You can emulate a **center crop** behavior by setting both dimensions to fixed values or to `MATCH_PARENT`. The camera view will fill the rect. If your dimensions don't match the aspect ratio of the internal preview surface, the surface will be cropped to fill the view, just like `android:scaleType="centerCrop"` on an `ImageView`.

```xml
<com.flurgle.camerakit.CameraView
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

This means that part of the preview is hidden, and the image output will contain parts of the scene that were not visible during the capture. If this is a problem, see [cameraCropOutput](#cameracropoutput).

## Extra Attributes

```xml
<com.flurgle.camerakit.CameraView xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/camera"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:keepScreenOn="true"
    app:cameraFacing="back"
    app:cameraFlash="off"
    app:cameraFocus="continuous"
    app:cameraZoom="off"
    app:cameraGrid="off"
    app:cameraSessionType="picture"
    app:cameraCropOutput="true"  
    app:cameraJpegQuality="100"
    app:cameraVideoQuality="480p"
    app:cameraWhiteBalance="auto"
    android:adjustViewBounds="true" />
```

|XML Attribute|Method|Values|Default Value|
|-------------|------|------|-------------|
|[`cameraSessionType`](#camerasessiontype)|`setSessionType()`|`picture` `video`|`picture`|
|[`cameraFacing`](#camerafacing)|`setFacing()`|`back` `front`|`back`|
|[`cameraFlash`](#cameraflash)|`setFlash()`|`off` `on` `auto` `torch`|`off`|
|[`cameraFocus`](#camerafocus)|`setFocus()`|`fixed` `continuous` `tap` `tapWithMarker`|`continuous`|
|[`cameraZoomMode`](#camerazoommode)|`setZoom()`|`off` `pinch`|`off`|
|[`cameraGrid`](#cameragrid)|`setGrid()`|`off` `grid3x3` `grid4x4` `phi`|`off`|
|[`cameraCropOutput`](#cameracropoutput)|`setCropOutput()`|`true` `false`|`false`|
|[`cameraJpegQuality`](#camerajpegquality)|`setJpegQuality()`|`0 <= n <= 100`|`100`|
|[`cameraVideoQuality`](#cameravideoquality)|`setVideoQuality()`|`max480p` `max720p` `max1080p` `max2160p` `highest` `lowest`|`max480p`|
|[`cameraWhiteBalance`](#camerawhitebalance)|`setWhiteBalance()`|`auto` `incandescent` `fluorescent` `daylight` `cloudy`|`auto`|

### cameraSessionType

What to capture - either picture or video. This has a couple of consequences:

- Sizing: capture and preview size are chosen among the available picture or video sizes, depending on the flag. When `picture`, we choose the max possible picture size and adapt the preview. When `video`, we respect the `videoQuality` choice and adapt the picture and the preview size.
- Picture capturing: due to sizing behavior, capturing pictures in `video` mode might lead to inconsistent results. In this case it is encouraged to use `captureSnapshot` instead, which will capture preview frames. This is fast and thus works well with slower camera sensors.
- Picture capturing: while recording a video, image capturing might work, but it is not guaranteed (it's device dependent)
- Permission behavior: when requesting a `video` session, the record audio permission will be requested. If this is needed, the audio permission should be added to your manifest or the app will crash.

```java
cameraView.setSessionType(CameraKit.Constants.SESSION_TYPE_PICTURE);
cameraView.setSessionType(CameraKit.Constants.SESSION_TYPE_VIDEO);
```

### cameraFacing

Which camera to use, either back facing or front facing.

```java
cameraView.setFacing(CameraKit.Constants.FACING_BACK);
cameraView.setFacing(CameraKit.Constants.FACING_FRONT);
```

### cameraFlash

Flash mode, either off, on, auto or *torch*.

```java
cameraView.setFlash(CameraKit.Constants.FLASH_OFF);
cameraView.setFlash(CameraKit.Constants.FLASH_ON);
cameraView.setFlash(CameraKit.Constants.FLASH_AUTO);
cameraView.setFlash(CameraKit.Constants.FLASH_TORCH);
```

### cameraFocus

Focus behavior. Can be off, continuous (camera continuously tries to adapt its focus), tap (focus is driven by the user tap) and tapWithMarker (a marker is drawn on screen to indicate focusing).

```java
cameraView.setFocus(CameraKit.Constants.FOCUS_FIXED);
cameraView.setFocus(CameraKit.Constants.FOCUS_CONTINUOUS);
cameraView.setFocus(CameraKit.Constants.FOCUS_TAP);
cameraView.setFocus(CameraKit.Constants.FOCUS_TAP_WITH_MARKER);
```

### cameraZoomMode

Lets you enable built-in pinch-to-zoom behavior. This means that the camera will capture two-finger gestures and move the zoom value accordingly. Nothing is drawn on screen, but you can listen to `onZoomChanged` in your camera listener.

```java
cameraView.setZoomMode(CameraKit.Constants.ZOOM_OFF);
cameraView.setZoomMode(CameraKit.Constants.ZOOM_PINCH);
```

### cameraGrid

Lets you draw grids over the camera preview. Supported values are `off`, `grid3x3` and `grid4x4` for regular grids, and `phi` for a grid based on the golden ratio constant, often used in photography.

```java
cameraView.setZoom(CameraKit.Constants.GRID_OFF);
cameraView.setZoom(CameraKit.Constants.GRID_3X3);
cameraView.setZoom(CameraKit.Constants.GRID_4X4);
cameraView.setZoom(CameraKit.Constants.GRID_PHI);
```

### cameraCropOutput

Whether the output picture should be cropped to fit the aspect ratio of the preview surface.
This can guarantee consistency between what the user sees and the final output, if you fixed the camera view dimensions. This does not support videos.

### cameraJpegQuality

Sets the JPEG quality of pictures.

```java
cameraView.setJpegQuality(100);
cameraView.setJpegQuality(50);
```

### cameraVideoQuality

Sets the desired video quality.

```java
cameraView.setVideoQuality(CameraKit.Constants.VIDEO_QUALITY_480P);
cameraView.setVideoQuality(CameraKit.Constants.VIDEO_QUALITY_720P);
cameraView.setVideoQuality(CameraKit.Constants.VIDEO_QUALITY_1080P);
cameraView.setVideoQuality(CameraKit.Constants.VIDEO_QUALITY_2160P);
cameraView.setVideoQuality(CameraKit.Constants.VIDEO_QUALITY_LOWEST);
cameraView.setVideoQuality(CameraKit.Constants.VIDEO_QUALITY_HIGHEST);
cameraView.setVideoQuality(CameraKit.Constants.VIDEO_QUALITY_QVGA);
```

### cameraWhiteBalance

Sets the desired white balance for the current session.

```java
cameraView.setWhiteBalance(CameraKit.Constants.WHITE_BALANCE_AUTO);
cameraView.setWhiteBalance(CameraKit.Constants.WHITE_BALANCE_INCANDESCENT);
cameraView.setWhiteBalance(CameraKit.Constants.WHITE_BALANCE_FLUORESCENT);
cameraView.setWhiteBalance(CameraKit.Constants.WHITE_BALANCE_DAYLIGHT);
cameraView.setWhiteBalance(CameraKit.Constants.WHITE_BALANCE_CLOUDY);
```

## Permissions behavior

`CameraView` needs two permissions:

- `android.permission.CAMERA` : required for capturing pictures and videos
- `android.permission.RECORD_AUDIO` : required for capturing videos

You can handle permissions yourself and then call `CameraView.start()` once they are acquired. If they are not, `CameraView` will request permissions to the user based on the `sessionType` that was set. In that case, you can restart the camera if you have a successful response from `onRequestPermissionResults()`.

## Manifest file

The library manifest file is not strict and only asks for camera permissions. This means that:

- If you wish to record videos, you should also add `android.permission.RECORD_AUDIO` to required permissions

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
```

- If you want your app to be installed only on devices that have a camera, you should add:

```xml
<uses-feature
    android:name="android.hardware.camera"
    android:required="true"/>
```

If you don't request this feature, you can use `CameraUtils.hasCameras()` to detect if current device has cameras, and then start the camera view.

## Roadmap

These are things that need to be done, off the top of my head:

- [x] fix CropOutput class presumably not working on rotated pictures
- [x] test video and 'frame' capture behavior, I expect some bugs there
- [x] simple APIs to draw grid lines
- [x] check focus, not sure it exposes the right part of the image
- [x] replace setCameraListener() with addCameraListener()
- [x] better threading, for example ensure callbacks are called in the ui thread
- [x] pinch to zoom support
- [ ] exposure correction APIs
- [ ] add a `sizingMethod` API to choose the capture size? Could be `max`, `4:3`, `16:9`... Right now it's `max`
- [ ] `Camera2` integration
- [ ] animate grid lines similar to stock camera app
- [ ] add onRequestPermissionResults for easy permission callback
- [ ] better error handling, maybe with a onError(e) method in the public listener, or have each public method return a boolean

