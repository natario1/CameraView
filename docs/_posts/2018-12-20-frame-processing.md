---
layout: page
title: "Frame Processing"
subtitle: "Process each frame in real time"
description: "Process each frame in real time"
category: docs
order: 5
date: 2018-12-20 20:45:42
disqus: 1
---

We support frame processors that will receive data from the camera preview stream. This is a useful
feature with a wide range of applications. For example, the frames can be sent to a face detector,
a QR code detector, the
[Firebase Machine Learning Kit](https://firebase.google.com/products/ml-kit/), or any other frame consumer.

```java
cameraView.addFrameProcessor(new FrameProcessor() {
    @Override
    @WorkerThread
    public void process(Frame frame) {
        byte[] data = frame.getData();
        int rotation = frame.getRotation();
        long time = frame.getTime();
        Size size = frame.getSize();
        int format = frame.getFormat();
        // Process...
    }
}
```

For your convenience, the `FrameProcessor` method is run in a background thread so you can do your job
in a synchronous fashion. Once the process method returns, internally we will re-use the `Frame` instance and
apply new data to it. So:

- you can do your job synchronously in the `process()` method
- if you must hold the `Frame` instance longer, use `frame = frame.freeze()` to get a frozen instance
  that will not be affected
  
### Related APIs

|Frame API|Type|Description|
|---------|----|-----------|
|`camera.addFrameProcessor(FrameProcessor)`|`-`|Register a `FrameProcessor`.|
|`frame.getData()`|`byte[]`|The current preview frame, in its original orientation.|
|`frame.getTime()`|`long`|The preview timestamp, in `System.currentTimeMillis()` reference.|
|`frame.getRotation()`|`int`|The rotation that should be applied to the byte array in order to see what the user sees.|
|`frame.getSize()`|`Size`|The frame size, before any rotation is applied, to access data.|
|`frame.getFormat()`|`int`|The frame `ImageFormat`. This will always be `ImageFormat.NV21` for now.|
|`frame.freeze()`|`Frame`|Clones this frame and makes it immutable. Can be expensive because requires copying the byte array.|
|`frame.release()`|`-`|Disposes the content of this frame. Should be used on frozen frames to release memory.|


