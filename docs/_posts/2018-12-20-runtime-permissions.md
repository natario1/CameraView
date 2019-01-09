---
layout: page
title: "Runtime Permissions"
subtitle: "Permissions and Manifest setup"
description: "Permissions and Manifest setup"
category: docs
order: 8
date: 2018-12-20 20:03:03
disqus: 1
---

`CameraView` needs two permissions:

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

On Marshmallow+, the user must explicitly approve our permissions. You can

- handle permissions yourself and then call `open()` or `setLifecycleOwner()` once they are acquired
- ignore this: `CameraView` will present a permission request to the user based on
  whether they are needed or not with the current configuration.
  
Note however, that this is done at the activity level, so the permission request callback 
`onRequestPermissionResults()` will be invoked on the parent activity, not the fragment.