---
layout: page
title: "Frame Processing"
description: "Process each frame in real time"
order: 6
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
        long time = frame.getTime();
        Size size = frame.getSize();
        int format = frame.getFormat();
        int userRotation = frame.getRotationToUser();
        int viewRotation = frame.getRotationToView();
        if (frame.getDataClass() == byte[].class) {
            byte[] data = frame.getData();
            // Process byte array...
        } else if (frame.getDataClass() == Image.class) {
            Image data = frame.getData();
            // Process android.media.Image...
        }
    }
});
```

For your convenience, the `FrameProcessor` method is run in a background thread so you can do your job
in a synchronous fashion. Once the process method returns, internally we will re-use the `Frame` instance and
apply new data to it. So:

- you can do your job synchronously in the `process()` method. This is **recommended**.
- if you must hold the `Frame` instance longer, use `frame = frame.freeze()` to get a frozen instance
  that will not be affected. This is **discouraged** because it requires copying the whole array.
  Also, starting from `v2.5.0`, this is not allowed when Camera2 is used.
  
### Process synchronously

Processing synchronously, for the duration of the `process()` method, is the recommended way of using
processors, because it solves different issues:

- avoids the need of calling `frame = frame.freeze()` which is a very expensive operation
- the engine will **automatically drop frames** if the `process()` method is busy, so you'll only 
  receive frames that you can handle
- we have already allocated background threads for you, so there's no need to create another

Some frame consumers might have a built-in asynchronous behavior.
But you can still block the `process()` thread until the consumer has returned.

```java
@Override
@WorkerThread
public void process(@NonNull Frame frame) {
    
    // EXAMPLE 1:
    // Firebase and Google APIs will often return a Task.
    // You can use Tasks.await() to complete the task on the current thread.
    // Read: https://developers.google.com/android/guides/tasks#blocking
    try {
        result = Tasks.await(firebaseDetector.detectInImage(firebaseImage));
    catch (Exception e) {
        // Firebase task failed.
    }
    
    
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

### Frame Data

Starting from `v2.5.0`, the type of data offered by `frame.getData()` depends on the camera engine
that created this frame:
- The Camera1 engine will offer `byte[]` arrays
- The Camera2 engine will offer `android.media.Image` objects

You can check this at runtime by inspecting the data class using `frame.getDataClass()`.

### Frame Size
  
The Camera2 engine offers the option to set size constraints for the incoming frames.

```java
cameraView.setFrameProcessingMaxWidth(maxWidth);
cameraView.setFrameProcessingMaxHeight(maxWidth);
```

With other engines, these API have no effect.

### Frame Format
  
The Camera2 engine offers the option to set the frame format as one of the ImageFormat 
constants. The default is `ImageFormat.YUV_420_888`.

```java
cameraView.setFrameProcessingFormat(ImageFormat.YUV_420_888);
cameraView.setFrameProcessingFormat(ImageFormat.YUV_422_888);
```

With the Camera1 engine, the incoming format will always be `ImageFormat.NV21`.
You can check which formats are available for use through `CameraOptions.getSupportedFrameProcessingFormats()`.

### Advanced: Thread Control

Starting from `v2.5.1`, you can control the number of background threads that are allocated
for frame processing work. This should further push you into performing processing actions synchronously
and can be useful if processing is very slow with respect to the preview frame rate, in order to
avoid dropping too many frames.

You can change the number of threads by calling `setFrameProcessingExecutors()`. Whenever you do,
we recommend that you also change the frame processing pool size to a compatible value.
The frame processing pool size is roughly the number of `Frame` instances that can exist at any given
moment. We recommend that this value is set to the number of executors plus 1. For example:

- Single threaded (default):

```java
cameraView.setFrameProcessingExecutors(1);
cameraView.setFrameProcessingPoolSize(2);
```

- Two threads:

```java
cameraView.setFrameProcessingExecutors(2);
cameraView.setFrameProcessingPoolSize(3);
```

### XML Attributes

```xml
<com.otaliastudios.cameraview.CameraView
    app:cameraFrameProcessingMaxWidth="640"
    app:cameraFrameProcessingMaxHeight="640"
    app:cameraFrameProcessingFormat="0x23"
    app:cameraFrameProcessingPoolSize="2"
    app:cameraFrameProcessingExecutors="1"/>
```

### Related APIs

|Frame API|Type|Description|
|---------|----|-----------|
|`camera.addFrameProcessor(FrameProcessor)`|`-`|Register a `FrameProcessor`.|
|`camera.removeFrameProcessor(FrameProcessor)`|`-`|Removes a `FrameProcessor`.|
|`camera.clearFrameProcessors()`|`-`|Removes all `FrameProcessor`s.|
|`camera.setFrameProcessingMaxWidth(int)`|`-`|Sets the max width for incoming frames.|
|`camera.setFrameProcessingMaxHeight(int)`|`-`|Sets the max height for incoming frames.|
|`camera.getFrameProcessingMaxWidth()`|`int`|Returns the max width for incoming frames.|
|`camera.getFrameProcessingMaxHeight()`|`int`|Returns the max height for incoming frames.|
|`camera.setFrameProcessingFormat(int)`|`-`|Sets the desired format for incoming frames. Should be one of the ImageFormat constants.|
|`camera.getFrameProcessingFormat()`|`-`|Returns the format for incoming frames. One of the ImageFormat constants.|
|`camera.setFrameProcessingPoolSize(int)`|`-`|Sets the frame pool size, roughly the number of Frames that can exist at any given moment. Defaults to 2, which fits all use cases unless you change the executors.|
|`camera.getFrameProcessingPoolSize()`|`-`|Returns the frame pool size.|
|`camera.setFrameProcessingExecutors(int)`|`-`|Sets the processing thread size. Defaults to 1, but can be increased if your processing is slow and you are dropping too many frames. This should always be tuned together with the frame pool size.|
|`camera.getFrameProcessingExecutors()`|`-`|Returns the processing thread size.|
|`frame.getDataClass()`|`Class<T>`|The class of the data returned by `getData()`. Either `byte[]` or `android.media.Image`.|
|`frame.getData()`|`T`|The current preview frame, in its original orientation.|
|`frame.getTime()`|`long`|The preview timestamp, in `System.currentTimeMillis()` reference.|
|`frame.getRotationToUser()`|`int`|The rotation that should be applied to the byte array in order to see what the user sees. Can be useful in the processing phase.|
|`frame.getRotationToView()`|`int`|The rotation that should be applied to the byte array in order to match the View / Activity orientation. Can be useful in the drawing / rendering phase.|
|`frame.getSize()`|`Size`|The frame size, before any rotation is applied, to access data.|
|`frame.getFormat()`|`int`|The frame `ImageFormat`. Defaults to `ImageFormat.NV21` for Camera1 and `ImageFormat.YUV_420_888` for Camera2.|
|`frame.freeze()`|`Frame`|Clones this frame and makes it immutable. Can be expensive because requires copying the byte array.|
|`frame.release()`|`-`|Disposes the content of this frame. Should be used on frozen frames to release memory.|


