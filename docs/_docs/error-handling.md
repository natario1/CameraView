---
layout: page
title: "Error Handling"
order: 14
disqus: 1
---

Errors are posted to the registered `CameraListener`s callback:

```java
@Override
public void onCameraError(CameraException error) {
    // Got error!
};
```

You are supposed to inspect the `CameraException` object as it contains useful information about
what happened and what should be done, if anything. All things that fail can end up throwing this
exception, which includes temporary actions like taking a picture, or functional actions like 
starting the camera preview.

### Unrecoverable errors

You can exclude unrecoverable errors using `CameraException.isUnrecoverable()`.
If this function returns true, at this point the camera has been released and it is likely showing
a black preview. The operation can't go on.

You can try to call `camera.start()` again, but that's not guaranteed to work. For example, the
camera sensor might be in use by another application, so there's nothing we could do.

### Other errors

For more fine grained control over what happened, inspect the reason using `CameraException.getReason()`.
This will return one of the `CameraException.REASON_` constants:

|Constant|Description|Unrecoverable|
|--------|-----------|-------------|
|`REASON_UNKNOWN`|Unknown error. No other info available.|No|
|`REASON_FAILED_TO_CONNECT`|Failed to connect to the camera service.|Yes|
|`REASON_FAILED_TO_START_PREVIEW`|Failed to start the camera preview.|Yes|
|`REASON_DISCONNECTED`|Camera was forced to disconnect by the system.|Yes|
|`REASON_PICTURE_FAILED`|Could not take a picture or picture snapshot.|No|
|`REASON_VIDEO_FAILED`|Could not take a video or video snapshot.|No|
|`REASON_NO_CAMERA`|Could not find a camera for this `Facing` value. You can try another.|No|




