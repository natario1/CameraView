---
layout: page
title: "Getting Started"
description: "Simple guide to take your first picture"
order: 2
disqus: 1
---

To use the CameraView engine, simply add a `CameraView` to your layout:

```xml
<com.otaliastudios.cameraview.CameraView
    android:id="@+id/camera"
    android:keepScreenOn="true"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

This is the one and only interface to the engine, and is meant to be hosted inside a UI component
like `Fragment` or `Activity`. The camera component is bound to the host lifecycle, so, as soon as possible,
you should register the host:

```java
// For activities
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    CameraView camera = findViewById(R.id.camera);
    camera.setLifecycleOwner(this);
}

// For fragments
@Override
public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    CameraView camera = findViewById(R.id.camera);
    camera.setLifecycleOwner(getViewLifecycleOwner());
}
```

Can't resolve the lifecycle owner interface? Read [below](#without-support-libraries).

### Set up a CameraListener

The next thing to do is to add a new `CameraListener` to be notified about camera events.
You can do this on a per-action basis, but it's easier to just add one when the UI is created:

```java
camera.addCameraListener(new CameraListener() {
    @Override
    public void onPictureTaken(PictureResult result) {
        // A Picture was taken!
    }
    
    @Override
    public void onVideoTaken(VideoResult result) {
        // A Video was taken!
    }
    
    // And much more
})
```

### Taking a picture

To take a picture upon user input, just call `takePicture()`.

```java
camera.addCameraListener(new CameraListener() {
    @Override
    public void onPictureTaken(PictureResult result) {
        // Picture was taken!
        // If planning to show a Bitmap, we will take care of
        // EXIF rotation and background threading for you...
        result.toBitmap(maxWidth, maxHeight, callback);
        
        // If planning to save a file on a background thread,
        // just use toFile. Ensure you have permissions.
        result.toFile(file, callback);
        
        // Access the raw data if needed.
        byte[] data = result.getData();
    }
});
camera.takePicture();
```

Read the docs about `takePictureSnapshot()` for a super fast, lower quality alternative.

### Taking a video

Taking a video is just the same thing, except that you must make sure that camera is in `Mode.VIDEO` mode,
and that you have write permissions to write the file:


```java
camera.addCameraListener(new CameraListener() {
    @Override
    public void onVideoTaken(VideoResult result) {
        // Video was taken!
        // Use result.getFile() to access a file holding
        // the recorded video.
    }
});

// Select output file. Make sure you have write permissions.
camera.setMode(Mode.VIDEO);
camera.takeVideo(file);

// Later... stop recording. This will trigger onVideoTaken().
camera.stopVideo();
```

Read the docs about `takeVideoSnapshot()` for a super fast, lower quality alternative.

### Configuration and more

This was it, but there is a ton of other options available to customize the camera behavior,
to control the sensor, the UI appearance, the quality and size of the output, or to live process
frames. Keep reading the documentation!

> For runtime permissions and Manifest setup, please read the [permissions page](../docs/runtime-permissions).

### Without support libraries

If you are not using support libraries and you can't resolve the LifecycleOwner interface,
make sure you override `onResume`, `onPause` and  `onDestroy` in your activity (`onDestroyView`
in your fragment), and call `open()`, `close()` and `destroy()`.

```java
@Override
protected void onResume() {
    super.onResume();
    cameraView.open();
}

@Override
protected void onPause() {
    super.onPause();
    cameraView.close();
}

@Override
protected void onDestroy() {
    super.onDestroy();
    cameraView.destroy();
}
```

