---
layout: page
title: "FAQs"
description: "Frequently asked questions"
order: 4
disqus: 1
---

### Usage

##### Q: Why is front camera flipped horizontally when using takePicture() or takeVideo() ?

A: It's actually not flipped - if you show your left hand, the person in the picture will show its left hand as well,
so this is the accurate representation of reality.

However, if you want to flip the result horizontally to match the preview,
you can do so by using the [snapshot APIs](../docs/capturing-media), which will respect what is shown in the preview.

##### Q: Can I use filters / overlays / cropping with takePicture() instead of takePictureSnapshot() ?

A: No, these features are only available with the snapshot API.

##### Q: Can I use filters / overlays / cropping with takeVideo() instead of takeVideoSnapshot() ?

A: No, these features are only available with the snapshot API.

##### Q: How can I improve takePictureSnapshot() quality?

A: The picture quality can be controlled in two ways:
- By [changing the snapshot size](../docs/snapshot-size)
- By [enabling metering](../docs/metering#picture-metering).

##### Q: How can I improve takeVideoSnapshot() quality?

A: The video quality can be controlled as follows:
- By changing the [snapshot size](../docs/snapshot-size)
- By changing the [snapshot framerate](../docs/controls#cameraPreviewFrameRate) (carefully: high values can cause dark preview)
- By changing the [video bitrate](../docs/controls#cameraVideoBitRate)
- By changing the [audio bitrate](../docs/controls#cameraAudioBitRate)

##### Q: How can I reduce the picture size?

A: The only control here is the picture size.
- When using `takePicture()`, change the [capture size](../docs/capture-size)
- When using `takePictureSnapshot()`, change the [snapshot size](../docs/snapshot-size)

##### Q: How can I reduce the video size?

A: By using video controls, for instance:
- Change the [video bitrate](../docs/controls#cameraVideoBitRate)
- Change the [audio bitrate](../docs/controls#cameraAudioBitRate)
- When using `takeVideo()`, change the [capture size](../docs/capture-size)
- When using `takeVideoSnapshot()`, change the [snapshot size](../docs/snapshot-size)

##### Q: Why is my preview / snapshot dark?

A: This is often caused by bad [framerate](../docs/controls#cameraPreviewFrameRate). Try using
a lower value, so that there's more time for frame exposure.

### Project Management

##### Q: I have found a bug with Camera1, can you fix it?

A: No, we will not address Camera1 bugs anymore - development is focused on Camera2. However, if you find a solution,
feel free to open a GitHub issue or pull requests to discuss.

##### Q: I have found a bug with my device XYZ, can you fix it?

A: No. Unless it's a device that we physically own, there is very little chance that a device-specific issues
can be solved by the maintainers. We encourage you to investigate on your own and get back to us
with a clear understanding of the problem and the solution.

##### Q: Why don't you review / comment on my GitHub issue?

A: Either because the issue did not respect the provided template, or because I don't have time.
If you are sure about the template, you can get private support by [sponsoring the project](../extra/donate).

##### Q: Why don't you review / comment on my GitHub pull requests?

A: Either because the pull requests did not respect the provided template, or because I don't have time.
If you are sure about the template, you can get private support by [sponsoring the project](../extra/donate).

##### Q: When will you do a new release?

A: We don't have a release schedule. New releases happen when there's enough changes to justify one,
and maintainers have had time to execute and publish the release. You can speed things up by 
[sponsoring the project](../extra/donate) or pull snapshots from [jitpack.io](https://jitpack.io):

```groovy
implementation 'com.github.natario1:CameraView:master-SNAPSHOT'
implementation 'com.github.natario1:CameraView:<commit hash>'
```

Check their website for more information about how to set things up.

