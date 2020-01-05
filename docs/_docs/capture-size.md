---
layout: page
title: "Capture Size"
description: "Set size of output media"
order: 9
disqus: 1
---

If you are planning to use the snapshot APIs, the size of the media output is that of the preview stream,
accounting for any cropping made when [measuring the view](preview-size) and other constraints.
Please read the [Snapshot Size](snapshot-size) document.

If you are planning to use the standard APIs, then what follows applies.

### Controlling Size

Size is controlled using `setPictureSize` and `setVideoSize` for, respectively, picture and video
output. These method will accept a `SizeSelector`. The point of a `SizeSelector` is to analyze the
available sizes that the sensor offers, and choose the ones it prefers.

```java
// This will be the size of pictures taken with takePicture().
cameraView.setPictureSize(new SizeSelector() {
    @Override
    public List<Size> select(List<Size> source) {
        // Receives a list of available sizes.
        // Must return a list of acceptable sizes.
    }
});

// This will be the size of videos taken with takeVideo().
cameraView.setVideoSize(new SizeSelector() {
    @Override
    public List<Size> select(List<Size> source) {
        // Same here.
    }
});
```

In practice, this is way easier using XML attributes or leveraging the `SizeSelectors` utilities.

### XML attributes

```xml
<com.otaliastudios.cameraview.CameraView
    app:cameraPictureSizeMinWidth="100"
    app:cameraPictureSizeMinHeight="100"
    app:cameraPictureSizeMaxWidth="3000"
    app:cameraPictureSizeMaxHeight="3000"
    app:cameraPictureSizeMinArea="10000000"
    app:cameraPictureSizeMaxArea="50000000"
    app:cameraPictureSizeAspectRatio="1:1"
    app:cameraPictureSizeSmallest="true|false"
    app:cameraPictureSizeBiggest="true|false"
    
    app:cameraVideoSizeMinWidth="100"
    app:cameraVideoSizeMinHeight="100"
    app:cameraVideoSizeMaxWidth="3000"
    app:cameraVideoSizeMaxHeight="3000"
    app:cameraVideoSizeMinArea="10000000"
    app:cameraVideoSizeMaxArea="50000000"
    app:cameraVideoSizeAspectRatio="1:1"
    app:cameraVideoSizeSmallest="true|false"
    app:cameraVideoSizeBiggest="true|false"
    />
```

The `cameraPicture*` attributes are used in picture mode, while the `cameraVideo*` attributes are used in video mode.
Note that, for each mode, if you declare more than one XML constraint, the resulting selector will try
to match **all** the constraints. Be careful - it is very likely that applying lots of constraints will give empty results.

### SizeSelectors utilities

All these XML attrs are actually shorthands to some `SizeSelectors` utility method.
For more versatility, or to address selection issues with multiple constraints,
we encourage you to use `SizeSelectors` to get a selector, and then apply it to the `CameraView` as seen.

The utilities will even let you merge different selectors with `or` or `and` logic, in a very 
intuitive way. For example:

```java
SizeSelector width = SizeSelectors.minWidth(1000);
SizeSelector height = SizeSelectors.minHeight(2000);
SizeSelector dimensions = SizeSelectors.and(width, height); // Matches sizes bigger than 1000x2000.
SizeSelector ratio = SizeSelectors.aspectRatio(AspectRatio.of(1, 1), 0); // Matches 1:1 sizes.

SizeSelector result = SizeSelectors.or(
    SizeSelectors.and(ratio, dimensions), // Try to match both constraints
    ratio, // If none is found, at least try to match the aspect ratio
    SizeSelectors.biggest() // If none is found, take the biggest
);
camera.setPictureSize(result);
camera.setVideoSize(result);
```

This selector will try to find square sizes bigger than 1000x2000. If none is found, it falls back
to just square sizes.

### Related APIs

|Method|Description|
|------|-----------|
|`setPictureSize(SizeSelector)`|Provides a size selector for the capture size in `PICTURE` mode.|
|`setVideoSize(SizeSelector)`|Provides a size selector for the capture size in `VIDEO` mode.|
|`getPictureSize()`|Returns the size of the output picture, including any rotation. Returns null in `VIDEO` mode.|
|`getVideoSize()`|Returns the size of the output video, including any rotation. Returns null in `PICTURE` mode.|
