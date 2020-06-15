---
layout: page
title: "Controls"
description: "Configuring output parameters and capture options"
order: 2
disqus: 1
---

CameraView supports a wide range of controls that will control the behavior of the sensor or the
quality of the output.

Everything can be controlled through XML parameters or programmatically. For convenience, most options
are represented by `enum` classes extending the `Control` class. This makes it possible to use 
`CameraView.set(Control)` to set the given control, `CameraView.get(Class<Control>)` to get it,
or `CameraOptions.supports(Control)` to see if it is supported.

### XML Attributes

```xml
<com.otaliastudios.cameraview.CameraView
    app:cameraFacing="front|back"
    app:cameraFlash="off|on|auto|torch"
    app:cameraWhiteBalance="auto|incandescent|fluorescent|daylight|cloudy"
    app:cameraHdr="off|on"
    app:cameraPictureFormat="jpeg|dng"
    app:cameraAudio="on|off|mono|stereo"
    app:cameraAudioBitRate="0"
    app:cameraVideoCodec="deviceDefault|h263|h264"
    app:cameraVideoMaxSize="0"
    app:cameraVideoMaxDuration="0"
    app:cameraVideoBitRate="0"
    app:cameraPreviewFrameRate="30"
    app:cameraPreviewFrameRateExact="false|true"/>
```

### APIs

##### cameraFacing

Which camera to use, either back facing or front facing.
Defaults to the first available value (tries `BACK` first).
The available values are exposed through the `CameraOptions` object.

```java
cameraView.setFacing(Facing.BACK);
cameraView.setFacing(Facing.FRONT);
```

##### cameraFlash

Flash mode, either off, on, auto or torch. Defaults to `OFF`.
The available values are exposed through the `CameraOptions` object.

```java
cameraView.setFlash(Flash.OFF);
cameraView.setFlash(Flash.ON);
cameraView.setFlash(Flash.AUTO);
cameraView.setFlash(Flash.TORCH);
```

##### cameraVideoCodec

Sets the encoder for video recordings. Defaults to `DEVICE_DEFAULT`,
which should typically be H_264.
The available values are exposed through the `CameraOptions` object.

```java
cameraView.setVideoCodec(VideoCodec.DEVICE_DEFAULT);
cameraView.setVideoCodec(VideoCodec.H_263);
cameraView.setVideoCodec(VideoCodec.H_264);
```

##### cameraAudioCodec

Sets the audio encoder for video recordings. Defaults to `DEVICE_DEFAULT`,
which should typically be AAC.
The available values are exposed through the `CameraOptions` object.

`AudioCodec.HE_AAC` and `AudioCodec.AAC_ELD` require at least JellyBean.

The library will safely fall back to device default if the min API requirements
are not met.

```java
cameraView.setAudioCodec(AudioCodec.DEVICE_DEFAULT);
cameraView.setAudioCodec(AudioCodec.AAC);
cameraView.setAudioCodec(AudioCodec.HE_AAC);
cameraView.setAudioCodec(AudioCodec.AAC_ELD);
```

##### cameraWhiteBalance

Sets the desired white balance for the current session.
Defaults to `AUTO`.
The available values are exposed through the `CameraOptions` object.

```java
cameraView.setWhiteBalance(WhiteBalance.AUTO);
cameraView.setWhiteBalance(WhiteBalance.INCANDESCENT);
cameraView.setWhiteBalance(WhiteBalance.FLUORESCENT);
cameraView.setWhiteBalance(WhiteBalance.DAYLIGHT);
cameraView.setWhiteBalance(WhiteBalance.CLOUDY);
```

##### cameraHdr

Turns on or off HDR captures. Defaults to `OFF`.
The available values are exposed through the `CameraOptions` object.

```java
cameraView.setHdr(Hdr.OFF);
cameraView.setHdr(Hdr.ON);
```

##### cameraPictureFormat

The format for pictures taken with `takePicture()`. Does not apply to picture snapshots taken
with `takePictureSnapshot()`. The `JPEG` value is always supported, while for other values
support might change depending on the engine and the device sensor.
The available values are exposed through the `CameraOptions` object.

```java
cameraView.setPictureFormat(PictureFormat.JPEG);
cameraView.setPictureFormat(PictureFormat.DNG);
```

##### cameraAudio

Turns on or off audio stream while recording videos.
Defaults to `ON`.
The available values are exposed through the `CameraOptions` object.

```java
cameraView.setAudio(Audio.OFF);
cameraView.setAudio(Audio.ON); // on but depends on video config
cameraView.setAudio(Audio.MONO); // force mono
cameraView.setAudio(Audio.STEREO); // force stereo
```

##### cameraAudioBitRate

Controls the audio bit rate in bits per second.
Use 0 or a negative value to fallback to the encoder default. Defaults to 0.

```java
cameraView.setAudioBitRate(0);
cameraView.setAudioBitRate(64000);
```

##### cameraVideoMaxSize

Defines the maximum size in bytes for recorded video files.
Once this size is reached, the recording will automatically stop.
Defaults to unlimited size. Use 0 or negatives to disable.

```java
cameraView.setVideoMaxSize(100000);
cameraView.setVideoMaxSize(0); // Disable
```

##### cameraVideoMaxDuration

Defines the maximum duration in milliseconds for video recordings.
Once this duration is reached, the recording will automatically stop.
Defaults to unlimited duration. Use 0 or negatives to disable.

```java
cameraView.setVideoMaxDuration(100000);
cameraView.setVideoMaxDuration(0); // Disable
```

##### cameraVideoBitRate

Controls the video bit rate in bits per second.
Use 0 or a negative value to fallback to the encoder default. Defaults to 0.

```java
cameraView.setVideoBitRate(0);
cameraView.setVideoBitRate(4000000);
```

##### cameraPreviewFrameRate

Controls the preview frame rate, in frames per second.
Use a value of 0F to restore the camera default value.

```java
cameraView.setPreviewFrameRate(30F);
cameraView.setPreviewFrameRate(0F);
```

The preview frame rate is an important parameter because it will also
control (broadly) the rate at which frame processor frames are dispatched, 
the video snapshots frame rate, and the rate at which real-time filters are invoked.
The available values are exposed through the `CameraOptions` object:

```java
float min = options.getPreviewFrameRateMinValue();
float max = options.getPreviewFrameRateMaxValue();
```

##### cameraPreviewFrameRateExact
Controls the behavior of `cameraPreviewFrameRate`. If this option is set to `true`, the narrowest
range containing the new preview fps will be used. If this option is set to `false` the broadest
range containing the new preview fps will be used. Note: If set this option to true, it will give as
exact preview fps as you want, but the sensor will have less freedom when adapting the exposure to
the environment, which may lead to dark preview.

```java
cameraView.setPreviewFrameRateExact(true);
cameraView.setPreviewFrameRageExact(false);
```

### Zoom

There are two ways to control the zoom value:

- User can zoom in or out with a [Gesture](gestures)
- The developer can start manual zoom with the `setZoom(float)` API, passing in a value between 0 and 1.

Both actions will trigger the zoom callback, which can be used, for example, to draw a seek bar:

```java
cameraView.addCameraListener(new CameraListener() {
    
    @Override
    public void onZoomChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
        // newValue: the new zoom value
        // bounds: this is always [0, 1]
        // fingers: if caused by touch gestures, these is the fingers position
    }
});
```

Zoom is not guaranteed to be supported: check the `CameraOptions` to be sure.
