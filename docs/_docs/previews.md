---
layout: page
title: "Engine and Previews"
description: "Camera engine and preview implementations"
order: 7
disqus: 1
---

### Engine

CameraView can interact with the camera sensor through the old Android interface typically referred
as `CAMERA1`, and more recently, also through the more modern interface called `CAMERA2`, for API level 21 (Lollipop).

Being more recent, the latter received less testing and feedback. As such, to enable it, you
are required to also set the experimental flag on: `app:cameraExperimental="true"`. On devices older
than Lollipop, the engine will always be `Engine.CAMERA1`.

|Engine|API Level|Info|
|------|---------|----|
|`Engine.CAMERA1`|All|Highly tested and reliable. Currently supports the full set of features.|
|`Engine.CAMERA2`|API 21+|Experimental, but will be the key focus for the future. New controls might be available only for this engine.|


### Previews

CameraView supports different types of previews, configurable either through the `cameraPreview` 
XML attribute or programmatically with the `Preview` control class.

All previews are supported in all conditions, regardless, for example, of the `Engine` that you
choose.

This parameter defaults to the OpenGL `GL_SURFACE` and it is highly recommended that you do not change this
to use all the features available. However, experienced user might prefer a different solution.

|Preview|Backed by|Info|
|-------|---------|----|
|`Preview.SURFACE`|A `SurfaceView`|Can be good for battery, but will not work well with dynamic layout changes and similar things. No support for video snapshots.|
|`Preview.TEXTURE`|A `TextureView`|Better. Requires hardware acceleration. No support for video snapshots.|
|`Preview.GL_SURFACE`|A `GLSurfaceView`|Recommended. Supports video snapshots. Supports [overlays](watermarks-and-overlays). Supports [real-time filters](filters).|

The GL surface, as an extra benefit, has a much more efficient way of capturing picture snapshots,
that avoids OOM errors, rotating the image on the fly, reading EXIF, and other horrible things belonging to v1.
These picture snapshots will also work while taking videos.


### XML Attributes

```xml
<com.otaliastudios.cameraview.CameraView
    app:cameraEngine="camera1|camera2"
    app:cameraPreview="surface|texture|glSurface"/>
```

### Related APIs

The preview method should only be called once and if the `CameraView` was never added to a window,
for example if you just created it programmatically. Otherwise, it has no effect.

The engine method should only be called when the `CameraView` is closed. Otherwise, it has no effect.

|Method|Description|
|------|-----------|
|`setPreview(Preview)`|Sets the preview implementation.|
|`getPreview()`|Gets the current preview implementation.|
|`setEngine(Engine)`|Sets the engine implementation.|
|`getEngine()`|Gets the current engine implementation.|
