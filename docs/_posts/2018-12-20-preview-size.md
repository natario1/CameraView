---
layout: page
title: "Preview Size"
subtitle: "Measuring behavior"
category: docs
order: 7
date: 2018-12-20 22:07:17
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