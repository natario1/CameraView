
*A fork of [Dylan McIntyre's CameraKit-Android library](https://github.com/gogopop/CameraKit-Android), originally a fork of [Google's CameraView library](https://github.com/google/cameraview). Right now this is like CameraKit-Android, but with *a lot* of serious bugs fixed, new sizing behavior, better orientation and EXIF support, new `setLocation` and `setWhiteBalance` APIs. Feel free to open issues with suggestions or contribute. Roadmap:*

- [x] *delete `captureMethod` and `permissionPolicy`, replace with `sessionType` (either picture or video) such that when `sessionType=video`, pictures are captured with the fast 'frame' method*
- [ ] *test video and 'frame' capture behavior, I expect some bugs there*
- [ ] *pass a nullable File to startVideo, so user can choose where to save the file*
- [ ] *simple APIs to draw grid lines*
- [ ] *rethink `adjustViewBounds`, maybe replace with a `scaleType` flag (center crop or center inside)*
- [ ] *add a `sizingMethod` API to choose the capture size? Could be `max`, `4:3`, `16:9`... Right now it's `max`*
- [ ] *pinch to zoom support*
- [ ] *exposure correction APIs*
- [ ] *revisit demo app*
- [ ] *`Camera2` integration*
- [ ] *EXIF support for 'frame' captured pictures, using ExifInterface library, so we can stop rotating it in Java*
- [ ] *add onRequestPermissionResults for easy permission callback*
- [ ] *better error handling, maybe with a onError(e) method in the public listener*

# CameraKit

CameraKit is an easy to use utility to work with the Android Camera APIs. Everything at the moment is work in progress, but it works well for pictures at least.

## Table of Contents

- [Features](#features)
- [Setup](#setup)
- [Usage](#usage)
  - [Capturing Images](#capturing-images)
  - [Capturing Video](#capturing-video)
  - [Other camera events](#other-camera-events)
- [Extra Attributes](#extra-attributes)
  - [cameraSessionType](#camerasessiontype)
  - [cameraFacing](#camerafacing)
  - [cameraFlash](#cameraflash)
  - [cameraFocus](#camerafocus)
  - [cameraZoomMode](#camerazoommode)
  - [cameraCropOutput](#cameracropoutput)
  - [cameraJpegQuality](#camerajpegquality)
  - [cameraWhiteBalance](#camerawhitebalance)
  - [Deprecated: cameraCaptureMethod](#cameracapturemethod)
  - [Deprecated: cameraPermissionsPolicy](#camerapermissionpolicy)
- [Permissions Behavior](#permissions-behavior)
- [Dynamic Sizing Behavior](#dynamic-sizing-behavior)
  - [Center Crop](#center-crop)
  - [Center Inside](#center-inside)

# Features

- Image and video capture seamlessly working with the same preview session. (TODO: remove this, use different sessions)
- System permission handling
- Dynamic sizing behavior
  - Create a `CameraView` of any size (not just presets!)
  - Or let it adapt to the sensor preview size
  - Automatic output cropping to match your `CameraView` bounds
- Multiple capture methods
  - While taking pictures, image is captured normally using the camera APIs.
  - While shooting videos, image is captured as a freeze frame of the `CameraView` preview (similar to SnapChat and Instagram)
- Built-in tap to focus
- EXIF support
  - Automatically detected orientation tag
  - Plug in location tags with `CameraView.setLocation(double, double)`
- TODO: Built-in pinch to zoom

## Setup

For now, clone the repo and add it to your project.
TODO: publish to bintray.

## Usage

To use CameraKit, simply add a `CameraView` to your layout:

```xml
<com.flurgle.camerakit.CameraView
    android:id="@+id/camera"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:adjustViewBounds="true" />
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

To capture an image just call `CameraView.captureImage()`. Make sure you setup a `CameraListener` to handle the image callback.

```java
camera.setCameraListener(new CameraListener() {
    @Override
    public void onPictureTaken(byte[] picture) {
        // Create a bitmap or a file...
        Bitmap result = BitmapFactory.decodeByteArray(picture, 0, picture.length);
    }
});

camera.captureImage();
```

### Capturing Video

TODO: test size and orientation stuff.

To capture video just call `CameraView.startRecordingVideo()` to start, and `CameraView.stopRecordingVideo()` to finish. Make sure you setup a `CameraListener` to handle the video callback.

```java
camera.setCameraListener(new CameraListener() {
    @Override
    public void onVideoTaken(File video) {
        // The File parameter is an MP4 file.
    }
});

camera.startRecordingVideo();
camera.postDelayed(new Runnable() {
    @Override
    public void run() {
        camera.stopRecordingVideo();
    }
}, 2500);
```

### Other camera events

Make sure you can react to different camera events by setting up a `CameraListener` instance.

```java
camera.setCameraListener(new CameraListener() {

    @Override
    public void onCameraOpened() {}

    @Override
    public void onCameraClosed() {}

    @Override
    public void onPictureTaken(byte[] picture) {}

    @Override
    public void onVideoTaken(File video) {}

});
```


## Extra Attributes

```xml
<com.flurgle.camerakit.CameraView xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/camera"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cameraFacing="back"
    app:cameraFlash="off"
    app:cameraFocus="continuous"
    app:cameraZoom="pinch"
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
|[`cameraFocus`](#camerafocus)|`setFocus()`|`off` `continuous` `tap` `tapWithMarker`|`continuous`|
|[`cameraZoomMode`](#camerazoommode)|`setZoom()`|`off` `pinch`|`off`|
|[`cameraCropOutput`](#cameracropoutput)|`setCropOutput()`|`true` `false`|`false`|
|[`cameraJpegQuality`](#camerajpegquality)|`setJpegQuality()`|`0 <= n <= 100`|`100`|
|[`cameraVideoQuality`](#cameravideoquality)|`setVideoQuality()`|`max480p` `max720p` `max1080p` `max2160p` `highest` `lowest`|`max480p`|
|[`cameraWhiteBalance`](#camerawhitebalance)|`setWhiteBalance()`|`auto` `incandescent` `fluorescent` `daylight` `cloudy`|`auto`|
|[`cameraCaptureMethod`](#cameracapturemethod) (Deprecated)|`setCaptureMethod()`|`standard` `frame`|`standard`|
|[`cameraPermissionPolicy`](#camerapermissionpolicy) (Deprecated)|`setPermissionPolicy()`|`picture` `video`|`picture`|

### cameraSessionType

What to capture - either picture or video. This has a couple of consequences:

- Sizing: capture and preview size are chosen among the available picture or video sizes, depending on the flag
- Picture capturing: **you can capture pictures during a `video` session**, though they will be captured as 'screenshots' of preview frames. This is fast and thus works well with slower camera sensors, but the captured image can be blurry or noisy.
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
cameraView.setFocus(CameraKit.Constants.FOCUS_OFF);
cameraView.setFocus(CameraKit.Constants.FOCUS_CONTINUOUS);
cameraView.setFocus(CameraKit.Constants.FOCUS_TAP);
cameraView.setFocus(CameraKit.Constants.FOCUS_TAP_WITH_MARKER);
```

### cameraZoomMode

TODO: work in progress. Right now 'off' is the onlly option.

```java
cameraView.setZoom(CameraKit.Constants.ZOOM_OFF);
cameraView.setZoom(CameraKit.Constants.ZOOM_PINCH);
```


### cameraCropOutput

Wheter the output picture should be cropped to fit the aspect ratio of the preview surface.
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

### cameraCaptureMethod
*Deprecated. Use cameraSessionType instead*

How to capture pictures, either standard or frame. The frame option lets you capture and save a preview frame, which can be better with slower camera sensors, though the captured image can be blurry or noisy.

```java
cameraView.setMethod(CameraKit.Constants.CAPTURE_METHOD_STANDARD);
cameraView.setMethod(CameraKit.Constants.CAPTURE_METHOD_FRAME);
```

### cameraPermissionPolicy
*Deprecated. Use cameraSessionType instead*

Either picture or video. This tells the library which permissions should be asked before starting the camera session. In the case of 'picture', we require the camera permissions. In the case of 'video', the record audio permission is asked as well.

Please note that, if needed, the latter should be added to your manifest file or the app will crash.

```java
cameraView.setPermissionPolicy(CameraKit.Constants.PERMISSIONS_PICTURE);
cameraView.setPermissionPolicy(CameraKit.Constants.PERMISSIONS_VIDEO);
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

If you don't request this feature, you can use `CameraKit.hasCameras()` to detect if current device has cameras, and then start the camera view.

## Dynamic Sizing Behavior

### Center crop

You can setup the `CameraView` dimensions as you wish. Default behavior is that if your dimensions don't match the aspect ratio of the internal preview surface, the surface will be cropped to fill the view, just like `android:scaleType="centerCrop"` on an `ImageView`.

### Center inside

If `android:adjustViewBounds` is set to true the library will try to adapt the view dimension to the chosen preview size. How? All dimensions set to `wrap_content` are streched out to ensure the view holds the whole preview. If both dimensions are `wrap_content`, this is exactly like `android:scaleType="centerInside"` on an `ImageView`.

