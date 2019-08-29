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
    public void process(@NonNull Frame frame) {
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

- you can do your job synchronously in the `process()` method. This is **recommended**.
- if you must hold the `Frame` instance longer, use `frame = frame.freeze()` to get a frozen instance
  that will not be affected. This is **discouraged** because it requires copying the whole array.
  
### Process synchronously

Processing synchronously, for the duration of the `process()` method, is the recommended way of using
processors, because it solves different issues:

- avoids the need of calling `frame = frame.freeze()` which is a very expensive operation
- the engine will **automatically drop frames** if the `process()` method is busy, so you'll only receive frames that you can handle
- we have already allocated a thread for you, so there's no need to create another

Some frame consumers might have a built-in asynchronous behavior.
But you can still block the `process()` thread until the consumer has returned.

```java
@Override
@WorkerThread
public void process(@NonNull Frame frame) {
    
    // EXAMPLE 1:
    // Firebase and Google APIs will often return a Task.
    // You can use Tasks.await() to complete the task on the current thread.
    Tasks.await(firebaseDetector.detectInImage(firebaseImage));
    
    // EXAMPLE 2:
    // For other async consumers, you can use, for example, a CountDownLatch.
    
    // Step 1: create the latch.
    final CountDownLatch latch = new CountDownLatch(1);
    
    // Step 2: launch async processing here...
    // When processing completes or fails, call latch.countDown();
    
    // Step 3: after launching, block the current thread.
    latch.await();
}
```
  
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


