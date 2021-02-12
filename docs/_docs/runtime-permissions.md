---
layout: page
title: "Runtime Permissions"
description: "Permissions and Manifest setup"
order: 13
disqus: 1
---

CameraView needs two permissions:

- `android.permission.CAMERA` : required for capturing pictures and videos
- `android.permission.RECORD_AUDIO` : required for capturing videos with `Audio.ON` (the default)

### Declaration

The library manifest file declares the `android.permission.CAMERA` permission, but not the audio one.
This means that:

- If you wish to record videos with `Audio.ON` (the default), you should also add
  `android.permission.RECORD_AUDIO` to required permissions

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
```

- If you want your app to be installed only on devices that have a camera, you should add:

```xml
<uses-feature
    android:name="android.hardware.camera"
    android:required="true"/>
```

If you don't request this feature, you can use `CameraUtils.hasCameras()` to detect if current
device has cameras, and then start the camera view.

### Handling

On Marshmallow+, the user must explicitly approve our permissions. You can either:

- handle permissions yourself and then call `open()` or `setLifecycleOwner()` once they are acquired
- let `CameraView` request permissions: we will present a permission request to the user based on
  whether they are needed or not with the current configuration.
  
The automatic request is currently done at the activity level, so the permission request callback 
`onRequestPermissionResults()` will be invoked on the parent activity, not the fragment.

The automatic request can be disabled by setting `app:cameraRequestPermissions="false"` in your
XML declaration or by using this method `setRequestPermissions(boolean requestPermissions)` in your code.