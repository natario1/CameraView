---
layout: page
title: "Changelog"
category: about
date: 2018-12-20 17:49:29
order: 3
---

New versions are released through GitHub, so the reference page is the [GitHub Releases](https://github.com/natario1/CameraView/releases) page.

### v2.0.0-beta04

- Renames setPreviewSize to setPreviewStreamSize (previewSize suggests it is related to the view size but it's not) ([#393][393])
- Added new APIs `setSnapshotMaxWidth` and `setSnapshotMaxHeight` ([#393][393]). You can now have a good looking preview but still take low-res snapshots using these snapshot constraints. Before this, the two sizes were coupled.

### v2.0.0-beta03

- Fixed a few bugs ([#392][392])
- Important fixes to video snapshot recording ([#374][374])

### v2.0.0-beta02

- Fixed important bugs ([#356][356])
- Picture snapshots are now flipped when front camera is used ([#360][360])
- Added `PictureResult.getFacing()` and `VideoResult.getFacing()` ([#360][360])

### v2.0.0-beta01

This is the first beta release. For changes with respect to v1, please take a look at the [migration guide](../extra/v1-migration-guide.html).

[356]: https://github.com/natario1/CameraView/pull/356
[360]: https://github.com/natario1/CameraView/pull/360
[374]: https://github.com/natario1/CameraView/pull/374
[392]: https://github.com/natario1/CameraView/pull/392
[393]: https://github.com/natario1/CameraView/pull/393