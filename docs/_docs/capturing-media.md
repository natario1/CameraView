---
layout: page
title: "Capturing Media"
description: "Understanding pictures, videos and the snapshot concept"
order: 3
disqus: 1
---

This section introduces some key concepts about media capturing, and about the `Mode` control.

### Mode control

The mode control determines what can be captured with the standard APIs (read below). It can be set through XML
or dynamically changed using `cameraView.setMode()`. The current mode value has a few consequences:

- Sizing: the capture size is chosen among the available picture or video sizes,
  depending on the flag, according to the given size selector.
- Capturing: while in picture mode, `takeVideo` will throw an exception.
- Capturing: while in video mode, `takePicture` will throw an exception.
- Permission behavior: when requesting a `video` session, the record audio permission will be requested.
  If this is needed, the audio permission should be added to your manifest or the app will crash.
  Please read the [permissions page](runtime-permissions).

```java
cameraView.setMode(Mode.PICTURE); // for pictures
cameraView.setMode(Mode.VIDEO); // for video
```

### Capturing media

The library supports 4 capture APIs, two for pictures and two for videos.

- Standard APIs: `takePicture()` and `takeVideo()`. These take a high quality picture or video, depending
  on the configuration values that were used. The standard APIs **must** be called in the appropriate `Mode`.
- Snapshot APIs: `takePictureSnapshot()` and `takeVideoSnapshot()`. These take a super fast, reliable
  snapshot of the camera preview. The snapshot APIs can be called in any `Mode` (you can snap videos in picture mode).

Beyond being extremely fast, and small in size (though low quality), snapshot APIs have the benefit 
that the result is automatically cropped to match the view bounds. This means that, if `CameraView` is square,
resulting snapshots are square as well, no matter what the sensor available sizes are.

|Method|Takes|Quality|Callable in `Mode.PICTURE`|Callable in `Mode.VIDEO`|Auto crop|Output size|
|------|-----|-------|--------------------------|------------------------|---------|-----------|
|`takePicture()`|Pictures|Standard|`yes`|`no`|`no`|That of `setPictureSize`|
|`takeVideo(File)`|Videos|Standard|`no`|`yes`|`no`|That of `setVideoSize`|
|`takePictureSnapshot()`|Pictures|Snapshot|`yes`|`yes`|`yes`|That of the preview stream, [or less](snapshot-size)|
|`takeVideoSnapshot(File)`|Videos|Snapshot|`yes`|`yes`|`yes`|That of the preview stream, [or less](snapshot-size)|

> Please note that the video snapshot features require:
> - API 18. If called on earlier versions, it throws an `IllegalStateException`
> - An OpenGL preview (see [previews](previews)). If not, it throws an `IllegalStateException`

### Capturing pictures while recording

This is allowed at the following conditions:

- `takePictureSnapshot()` is used (no HQ pictures)
- the `GL_SURFACE` preview is used (see [previews](previews))

### Related XML attributes

```xml
<com.otaliastudios.cameraview.CameraView
    app:cameraMode="picture|video"/>
```

### Related callbacks

```java
camera.addCameraListener(new CameraListener() {

    @Override
    public void onPictureShutter() {
        // Picture capture started!
    }
    
    @Override
    public void onPictureTaken(@NonNull PictureResult result) {
        // A Picture was taken!
    }
    
    @Override
    public void onVideoTaken(@NonNull VideoResult result) {
        // A Video was taken!
    }
    
    @Override
    public void onVideoRecordingStart() {
        // Notifies that the actual video recording has started.
        // Can be used to show some UI indicator for video recording or counting time.
    }
    
    @Override
    public void onVideoRecordingEnd() {
        // Notifies that the actual video recording has ended.
        // Can be used to remove UI indicators added in onVideoRecordingStart.
    }
})
```

### Related APIs

|Method|Description|
|------|-----------|
|`setMode()`|Either `Mode.VIDEO` or `Mode.PICTURE`.|
|`isTakingVideo()`|Returns true if the camera is currently recording a video.|
|`isTakingPicture()`|Returns true if the camera is currently capturing a picture.|
|`takePicture()`|Takes a high quality picture.|
|`takeVideo(File)`|Takes a high quality video.|
|`takeVideo(FileDescriptor)`|Takes a high quality video.|
|`takeVideo(File, long)`|Takes a high quality video, stopping after the given duration.|
|`takeVideo(FileDescriptor, long)`|Takes a high quality video, stopping after the given duration.|
|`takePictureSnapshot()`|Takes a picture snapshot.|
|`takeVideoSnapshot(File)`|Takes a video snapshot.|
|`takeVideoSnapshot(File, long)`|Takes a video snapshot, stopping after the given duration.|
|`getPictureSize()`|Returns the output picture size, accounting for any rotation. Null while in `VIDEO` mode.|
|`getVideoSize()`|Returns the output video size, accounting for any rotation. Null while in `PICTURE` mode.|
|`getSnapshotSize()`|Returns the size of pictures taken with `takePictureSnapshot()` or videos taken with `takeVideoSnapshot()`. Accounts for rotation and cropping.|