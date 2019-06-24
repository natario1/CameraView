---
layout: page
title: "Changelog"
category: about
date: 2018-12-20 17:49:29
order: 3
---

New versions are released through GitHub, so the reference page is the [GitHub Releases](https://github.com/natario1/CameraView/releases) page.

### v2.0.0-beta06 (to be released)

- Improvement: we now choose a video recording profile that is compatible with the chosen size. Should fix some video recording issues. ([#477][477])
- **Breaking change**: some public classes have been moved to different packages. See [table here](../extra/v1-migration-guide.html#repackaging). ([#482][482])

### v2.0.0-beta05

- Fixed `FrameProcessor` freeze and release behavior, was broken ([#431][431])
- New: new api `setAutoFocusResetDelay` to control the delay to reset the focus after autofocus was performed, thanks to [@cneuwirt][cneuwirt] ([#435][435])
- Faster camera preview on layout changes ([#403][403])
- A few bug fixes ([#471][471])

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

[cneuwirt]: https://github.com/cneuwirt

[356]: https://github.com/natario1/CameraView/pull/356
[360]: https://github.com/natario1/CameraView/pull/360
[374]: https://github.com/natario1/CameraView/pull/374
[392]: https://github.com/natario1/CameraView/pull/392
[393]: https://github.com/natario1/CameraView/pull/393
[471]: https://github.com/natario1/CameraView/pull/471
[431]: https://github.com/natario1/CameraView/pull/431
[403]: https://github.com/natario1/CameraView/pull/403
[435]: https://github.com/natario1/CameraView/pull/435
[477]: https://github.com/natario1/CameraView/pull/477
[482]: https://github.com/natario1/CameraView/pull/482