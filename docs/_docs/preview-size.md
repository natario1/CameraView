---
layout: page
title: "Preview Size"
category: docs
order: 8
disqus: 1
---

`CameraView` has a smart measuring behavior that will let you do what you want with a few flags.
Measuring is controlled simply by `layout_width` and `layout_height` attributes, with this meaning:

|Value|Meaning|
|-----|-------|
|`WRAP_CONTENT`|CameraView will choose this dimension, in order to show the whole preview without cropping. The aspect ratio will be respected.|
|`MATCH_PARENT`|CameraView will fill this dimension. Part of the content *might* be cropped.
|Fixed values (e.g. `500dp`)|Same as `MATCH_PARENT`|

This means that your visible preview can be of any size, not just the presets.
Whatever you do, the preview will never be distorted - it can only be cropped
if needed.

### Examples

##### Center Inside

By setting both dimensions to `WRAP_CONTENT`, you can emulate a **center inside** behavior.
The view will try to fill the available space, but respecting the stream aspect ratio.


```xml
<com.otaliastudios.cameraview.CameraView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

This means that the whole preview is visible, and the image output matches what was visible
during the preview.

##### Center Crop

By setting both dimensions to `MATCH_PARENT` or fixed values, you can emulate a **center crop** 
behavior. The camera view will fill the rect. If your dimensions don't match the aspect ratio
of the internal preview surface, the surface will be cropped to fill the view.

```xml
<com.otaliastudios.cameraview.CameraView
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

This means that part of the preview might be hidden, and the output might contain parts of the scene
that were not visible during the capture, **unless it is taken as a snapshot, since snapshots account for cropping**.


### Advanced feature: Preview Stream Size Selection

> Only do this if you know what you are doing. This is typically not needed - prefer picture/video size selectors,
as they will drive the preview stream size selection and, eventually, the view size. If what you want is just
choose an aspect ratio, do so with [Capture Size](capture-size) selection.

As said, `WRAP_CONTENT` adapts the view boundaries to the preview stream size. The preview stream size must be determined
based on the sizes that the device sensor & hardware actually support. This operation is done automatically
by the engine. The default selector will do the following:

- Constraint 1: match the picture/video output aspect ratio (so you get what you see)
- Constraint 2: match sizes a bit bigger than the View (so there is no upscaling)
- Try to match both, or just one, or fallback to the biggest available size

There are not so many reasons why you would replace this, other than to control the frame processor size
or, indirectly, the snapshot size. You can, however, hook into the process using `setPreviewStreamSize(SizeSelector)`:

```java
cameraView.setPreviewStreamSize(new SizeSelector() {
    @Override
    public List<Size> select(List<Size> source) {
        // Receives a list of available sizes.
        // Must return a list of acceptable sizes.
    }
});
```

After the preview stream size is determined, if it has changed since last time, the `CameraView` will receive
another call to `onMeasure` so the `WRAP_CONTENT` magic can take place.

To understand how SizeSelectors work and the available utilities, please read the [Capture Size](capture-size) document.

