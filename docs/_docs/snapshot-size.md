---
layout: page
title: "Snapshot Size"
description: "Sizing the snapshots output"
order: 10
disqus: 1
---

Snapshots are captured from the preview stream instead of using a separate capture channel.
They are extremely fast, small in size, and give you a low-quality output that can be easily
uploaded or processed.

The snapshot size is based on the size of the preview stream, which is described in the [Preview Size](preview-size) document.
Although the preview stream size is customizable, note that this is considered an advanced feature,
as the best preview stream size selector already does a good job for the vast majority of use cases.

When taking snapshots, the preview stream size is then changed to match some constraints.

### Matching the preview ratio

Snapshots will automatically be cropped to match the preview aspect ratio. This means that if your
preview is square, you can finally take a square picture or video, regardless of the available sensor sizes.

Take a look at the [Preview Size](preview-size) document to learn about preview sizing.

### Other constraints

You can refine the size further by applying `maxWidth` and a `maxHeight` constraints:

```java
cameraView.setSnapshotMaxWidth(500);
cameraView.setSnapshotMaxHeight(500);
```

These values apply to both picture and video snapshots. If the snapshot dimensions exceed these values
(both default `Integer.MAX_VALUE`), the snapshot will be scaled down to match the constraints.

This is very useful as it decouples the snapshot size logic from the preview. By using small constraints,
you can have a pleasant, good looking preview stream, while still capturing fast, low-res snapshots
with no issues.

### Video Codec requirements

When taking video snapshots, the video codec that the device provides might require extra constraints,
like

- width / height alignment
- maximum width or height 

CameraView will try to read these requirements and apply them, which can result in video snapshots
that are smaller than you would expect, or with a **very slightly** different aspect ratio.

### XML Attributes

```xml
<com.otaliastudios.cameraview.CameraView
    app:cameraSnapshotMaxWidth="500"
    app:cameraSnapshotMaxHeight="500"/>
```

### Related APIs

|Method|Description|
|------|-----------|
|`setSnapshotMaxWidth(int)`|Sets the max width for snapshots. If out of bounds, the output will be scaled down.|
|`setSnapshotMaxHeight(int)`|Sets the max height for snapshots. If out of bounds, the output will be scaled down.|