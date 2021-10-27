---
layout: page
title: "Changelog"
order: 3
---

New versions are released through GitHub, so the reference page is the [GitHub Releases](https://github.com/natario1/CameraView/releases) page.

> Starting from 2.4.0, you can now [support development](https://github.com/sponsors/natario1) through the GitHub Sponsors program. 
Companies can share a tiny part of their revenue and get private support hours in return. Thanks!

##### v2.7.2

- Fix: fix camera rotation handling for Compose apps and other specific scenarios ([#1117][1117])

<https://github.com/natario1/CameraView/compare/v2.7.1...v2.7.2>

##### v2.7.1

- Fix: fix preview issues on Pixel 4A with certain FPS, thanks to [@honzasmuk][honzasmuk] ([#1089][1089])
- Improvement: don't catch gestures if they're turned off, thanks to [@ObsidianX][ObsidianX] ([#1068][1068])
- New: new flag cameraDrawHardwareOverlays and setDrawHardwareOverlays() to draw overlays on hardware canvas, thanks to [@ObsidianX][ObsidianX] ([#1066][1066])
- Publish on Maven Central instead of JCenter

<https://github.com/natario1/CameraView/compare/v2.7.0...v2.7.1>

##### v2.7.0

- New: onPictureShutter() callback when taking pictures, thanks to [@EzequielAdrianM][EzequielAdrianM] ([#1030][1030])
- New: GestureAction.TAKE_PICTURE_SNAPSHOT lets you take snapshots on gesture, thanks to [@EzequielAdrianM][EzequielAdrianM] ([#1030][1030])
- Improvement: try-catch internal exception when takePicture fails, thanks to [@michaelspecht][michaelspecht] ([#1024][1024])
- Improvement: log errors when file writing fails, thanks to [@bwt][bwt] ([#960][960])
- Fix: Avoid preview deadlocks ([#1020][1020])
- Fix: Workaround for messed-up preview on Pixel 4 ([#1020][1020])
- Fix: Avoid internal StackOverflow errors ([#992][992])

<https://github.com/natario1/CameraView/compare/v2.6.4...v2.7.0>

##### v2.6.4

- Fix: Fix many small bugs ([#953][953])

<https://github.com/natario1/CameraView/compare/v2.6.3...v2.6.4>

##### v2.6.3

- <small>[Video]</small> New: `setAudioCodec` and `app:cameraAudioCodec` to choose the audio encoding format, thanks to [@EverydayPineapple][EverydayPineapple] ([#861][861])
- <small>[Camera1, Frame processing]</small> Fix: frame processing restarts automatically after taking a picture, thanks to [@jeffreyfjohnson][jeffreyfjohnson] ([#877][877])
- <small>[Camera1]</small> Improvement: catch more errors in Camera1 lifecycle to avoid crashes, thanks to [@Namazed][Namazed] ([#851][851] and [#897][897])
- <small>[CameraView]</small> Improvement: `setLifecycleOwner` is now nullable and will unbind the lifecycle, thanks to [@Namazed][Namazed] ([#798][798])
- <small>[Preview]</small> Improvement: the `CameraPreview` APIs are much more friendly for subclassing ([#816][816])

<https://github.com/natario1/CameraView/compare/v2.6.2...v2.6.3>

##### v2.6.2

- <small>[Frame processing]</small> New: `frame.getRotationToUser()` and `frame.getRotationToView()` APIs to help with processing vs. rendering ([#745][745])
- <small>[Camera1, Camera2]</small> New: `cameraPreviewFrameRateExact="true|false"` to tell whether the desired preview frame rate should be as exact as possible, thanks to [@hualong-shen][hualong-shenn] ([#754][754])
- <small>[Logging]</small> Improvement: `CameraLogger` is now thread safe, thanks to [@Namazed][Namazed] ([#779][779])
- <small>[Permissions]</small> Improvement: added runtime API `setRequestPermissions()` that matches the XML attribute, thanks to [@Namazed][Namazed] ([#775][775])

<https://github.com/natario1/CameraView/compare/v2.6.1...v2.6.2>

##### v2.6.1

- <small>[Video]</small> New: `takeVideo(FileDescriptor)` for file descriptors, thanks to [@sewar][sewar] ([#732][732])
- <small>[Video]</small> Improvement: fixed "no encoder found" issues for some devices ([#741][741])
- <small>[Camera2, Metering]</small> Improvement: increased metering timeout for touch metering ([#741][741])
- <small>[Camera2, Metering]</small> Improvement: extended touch metering to LEGACY devices ([#741][741])

<https://github.com/natario1/CameraView/compare/v2.6.0...v2.6.1>

### v2.6.0

- <small>[Metering]</small> New: `startAutoFocus(RectF)` will start 3A metering to a given rect instead of a spot ([#724][724]) 
- <small>[Permissions]</small> New: `app:cameraRequestPermissions` flag to disable the automatic activity permission request ([#718][718])
- <small>[Frame processing]</small> New: `setFrameProcessingPoolSize()` to set the number of Frame instances that can exist at any given time. Useful in conjunction with `setFrameProcessingExecutors()`. Please read docs ([#716][716])
- <small>[Frame processing]</small> New: `setFrameProcessingExecutors()` to set the number of threads involved in frame processing. Useful in conjunction with `setFrameProcessingPoolSize()`. Please read docs ([#716][716])
- <small>[Frame processing, Camera2]</small> Improvement: ensure that slow processing does now slow down the preview ([#716][716])

<https://github.com/natario1/CameraView/compare/v2.5.0...v2.6.0>

### v2.5.0

- <small>[Camera2]</small> New: support for RAW pictures with new APIs `setPictureFormat()` and `CameraOptions.getSupportedPictureFormats()`. Contains a **breaking change**: `PictureResult.getFormat()` is not an integer anymore but rather a `PictureFormat`. This API had no real purpose so this might not affect you ([#691][691])
- <small>[Camera2]</small> New: support for constraining the frame processing size through `setFrameProcessingMaxWidth()` and `setFrameProcessingMaxHeight()`. This can improve processing performance ([#691][691])
- <small>[Camera2]</small> New: support for choosing the frame processing format through `setFrameProcessingFormat()` and `CameraOptions.getSupportedFrameProcessingFormats()` ([#691][691])
- <small>[Camera2]</small> Improvement: Frame processing FPS for Camera2 is now smooth and typically better than Camera1. This required some **breaking changes** (see below) ([#691][691])
- <small>[Camera1, Camera2]</small> Improvement: improved internal threading ([#697][697])
- <small>[Camera1, Camera2]</small> Improvement: improvements to stability and edge cases behavior ([#696][696])
- <small>[Real time filters]</small> Change: filters do not need the experimental flag anymore ([#691][691])

The new frame processing approach will force you to update your code, because `Frame.getData()` is 
not a a byte[] anymore. The class of this object now depends on the engine being used. You can use 
`frame.getDataClass()` (or instanceof) to check.

If you are using the Camera1 engine, you will still receive byte arrays, so you can just cast `frame.getData()` to 
`byte[]`, assuming it's not done already by the compiler.

If you are using the experimental Camera2 engine, you will receive `android.media.Image`s instead.
This object will likely be accepted by frame processing libraries, and also offers access to raw byte data.
This change greatly improved the FPS performance, which is what matters the most at the library level.

<https://github.com/natario1/CameraView/compare/v2.4.0...v2.5.0>

### v2.4.0

- <small>[Camera2]</small> New: support for `previewFrameRate`. Controls preview FPS, snapshot FPS, processor FPS, thanks to [@vaibhavbhandula][vaibhavbhandula] ([#653][653])
- <small>[Camera1]</small> New: support for `previewFrameRate` for Camera1 ([#661][661])
- <small>[Camera2]</small> Fix: fix crashes when taking snapshots very early ([#651][651])
- <small>[Preview]</small> Fix: Fixed preview being upside-down for 180 degrees flips ([#651][651])
- Fix: other bug fixes ([#651][651])

<https://github.com/natario1/CameraView/compare/v2.3.1...v2.4.0>

##### v2.3.1

- <small>[Video]</small> Improvement: better timing for `onVideoRecordingStart()` thanks to [@agrawalsuneet][agrawalsuneet] ([#632][632])
- <small>[Video, Camera1]</small> Fix: fixed video errors when starting on specific devices ([#617][617])
- <small>[Video]</small> Fix: fixed crash when closing the app during video snapshots ([#630][630])
- <small>[Preview]</small> Fix: fixed crash when using `GL_SURFACE` ([#630][630])

<https://github.com/natario1/CameraView/compare/v2.3.0...v2.3.1>

### v2.3.0

- <small>[Camera2, Metering]</small> New: `startAutoFocus` is much more powerful and does 3A metering (AF, AE, AWB) ([#574][574])
- <small>[Camera2, Metering]</small> New: `setPictureMetering(boolean)` decides whether to do metering before `takePicture()`. Defaults to true to improve quality. ([#580][580])
- <small>[Camera2, Metering]</small> New: `setPictureSnapshotMetering(boolean)` decides whether to do metering before `takePictureSnapshot()`. Defaults to false to improve latency. However, you can set this to true to greatly improve the snapshot quality, for example to support `Flash`. ([#580][580])
- <small>[Camera2, Metering]</small> New: metering extended to many more cameras, which can now use `startAutoFocus` or the focus gesture ([#574][574])
- <small>[Camera2, Metering]</small> Improvement: `onAutoFocusEnd` is now guaranteed to be called ([#574][574])
- <small>[Camera2, Metering]</small> Improvement: taking picture does not invalidate the previous focus ([#574][574])
- <small>[Camera2, Metering]</small> Improvement: better metering when zoomed in ([#574][574])
- <small>[Real time filters]</small> **Breaking change**: `Filter` interface signatures now accept timestamps for animations ([#588][588])
- <small>[Overlays]</small> New: you can now use `addView()` and `removeView()` to add or remove overlays at runtime (see docs) ([#588][588])
- <small>[Video]</small> Improvement: better encoder selection ([#588][588])
- Fix: fixed various bugs and improved stability ([#588][588])

<https://github.com/natario1/CameraView/compare/v2.2.0...v2.3.0>

### v2.2.0

- <small>[Real time filters]</small> New: `SimpleFilter` class accepts a fragment shader in the constructor ([#552][552])
- <small>[Real time filters]</small> New: `MultiFilter` to apply more than one filter at the same time ([#559][559])
- <small>[Video]</small> Improvement: query device encoders before configuring them. Should fix issues on multiple devices ([#545][545])
- <small>[Video]</small> Fix: `takeVideoSnapshot` not working unless you set a max duration ([#551][551])
- <small>[Video]</small> Fix: `takeVideo` crashing on Camera2 LEGACY devices ([#551][551])
- <small>[Frame Processing]</small> Fix: fixed dead Frames issues and improved error messages ([#572][572])
- Fix: fixed `CameraView` appearance in the layout editor ([#564][564])

<https://github.com/natario1/CameraView/compare/v2.1.0...v2.2.0>

### v2.1.0

This release adds experimental support for [real-time filters](../docs/filters) thanks to [@agrawalsuneet][agrawalsuneet].
Please read the documentation page for usage instructions.

- New: Real-time filters support ([#527][527])
- New: Add filters through XML ([#535][535])
- New: Map filter controls to scroll/pinch gestures ([#537][537])

<https://github.com/natario1/CameraView/compare/v2.0.0...v2.1.0>

### v2.0.0

- Fix: bug with picture recorder ([#521][521])
- Fix: video snapshots appearing black ([#528][528])
- Fix: video snapshots exceptions and audio issues ([#530][530])

<https://github.com/natario1/CameraView/compare/v2.0.0-rc2...v2.0.0>

##### v2.0.0-rc2

- Fix: crashes when stopping video snapshots ([#513][513])
- Fix: dependencies missing, leading to runtime crashes ([#517][517])

<https://github.com/natario1/CameraView/compare/v2.0.0-rc1...v2.0.0-rc2>

### v2.0.0-rc1

This is likely to be the last release before v2.0.0.

- New: support for watermarks and animated overlays ([docs](../docs/watermarks-and-overlays)), thanks to [@RAN3000][RAN3000] ([#502][502], [#421][421])
- New: added `onVideoRecordingStart()` to be notified when video recording starts, thanks to [@agrawalsuneet][agrawalsuneet] ([#498][498])
- New: added `onVideoRecordingEnd()` to be notified when video recording ends ([#506][506])
- New: added `Audio.MONO` and `Audio.STEREO` to control the channel count for videos and video snapshots ([#506][506])
- New: added `cameraUseDeviceOrientation` to choose whether picture and video outputs should consider the device orientation or not ([#497][497])
- Improvement: improved Camera2 stability and various bugs fixed (e.g. [#501][501])
- Improvement: improved video snapshots speed, quality and stability ([#506][506])

<https://github.com/natario1/CameraView/compare/v2.0.0-beta06...v2.0.0-rc1>

##### v2.0.0-beta06

- New: Full featured Camera2 integration! Use `cameraExperimental="true"` and `cameraEngine="camera2"` to test this out. ([#490][490])
- Improvement: we now choose a video recording profile that is compatible with the chosen size. Should fix some video recording issues. ([#477][477])
- Improvement: most internals are now open to be accessed by subclassing. Feel free to open PRs with more protected methods to be overriden. ([#494][494])
- **Breaking change**: some public classes have been moved to different packages. See [table here](../extra/v1-migration-guide#repackaging). ([#482][482])
- **Breaking change**: the listener methods `onFocusStart` and `onFocusEnd` are now called `onAutoFocusStart` and `onAutoFocusEnd`. ([#484][484])
- **Breaking change**: the gesture actions `focus` and `focusWithMarker` have been removed and replaced by `autoFocus`, which shows no marker. ([#484][484])
- New: new API called `setAutoFocusMarker()` lets you choose your own marker. ([#484][484])

If you were using `focus`, just switch to `autoFocus`.

If you were using `focusWithMarker`, you can [add back the old marker](../docs/metering#touch-metering-markers).

<https://github.com/natario1/CameraView/compare/v2.0.0-beta05...v2.0.0-beta06>

##### v2.0.0-beta05

- Fixed `FrameProcessor` freeze and release behavior, was broken ([#431][431])
- New: new api `setAutoFocusResetDelay` to control the delay to reset the focus after autofocus was performed, thanks to [@cneuwirt][cneuwirt] ([#435][435])
- Faster camera preview on layout changes ([#403][403])
- A few bug fixes ([#471][471])

<https://github.com/natario1/CameraView/compare/v2.0.0-beta04...v2.0.0-beta05>

##### v2.0.0-beta04

- Renames setPreviewSize to setPreviewStreamSize (previewSize suggests it is related to the view size but it's not) ([#393][393])
- Added new APIs `setSnapshotMaxWidth` and `setSnapshotMaxHeight` ([#393][393]). You can now have a good looking preview but still take low-res snapshots using these snapshot constraints. Before this, the two sizes were coupled.

<https://github.com/natario1/CameraView/compare/v2.0.0-beta03...v2.0.0-beta04>

##### v2.0.0-beta03

- Fixed a few bugs ([#392][392])
- Important fixes to video snapshot recording ([#374][374])

<https://github.com/natario1/CameraView/compare/v2.0.0-beta02...v2.0.0-beta03>

##### v2.0.0-beta02

- Fixed important bugs ([#356][356])
- Picture snapshots are now flipped when front camera is used ([#360][360])
- Added `PictureResult.getFacing()` and `VideoResult.getFacing()` ([#360][360])

<https://github.com/natario1/CameraView/compare/v2.0.0-beta01...v2.0.0-beta02>

### v2.0.0-beta01

This is the first beta release. For changes with respect to v1, please take a look at the [migration guide](../extra/v1-migration-guide).

##### v1.6.1

This is the last release before v2.

- Fixed: crash when using TextureView in API 28, thanks to [@Keyrillanskiy][Keyrillanskiy] ([#297][297])
- Fixed: restore Frame Processor callbacks after taking videos, thanks to [@stefanJi][stefanJi] ([#344][344])
- Enhancement: when horizontal, camera now uses the last available orientation, thanks to [@aartikov][aartikov] ([#290][290])
- Changed: we now swallow exceptions during autoFocus that were happening unpredictably on some devices, thanks to [@mahdi-ninja][mahdi-ninja] ([#332][332])

<https://github.com/natario1/CameraView/compare/v1.6.0...v1.6.1>

### v1.6.0

- Lifecycle support. Use `setLifecycleOwner` instead of calling start, stop and destroy ([#265][265])
- Enhancement: provide synchronous version of CameraUtils.decodeBitmap thanks to [@athornz][athornz] ([#224][224])
- Enhancement: prevent possible context leak thanks to [@MatFl][MatFl] ([#245][245])
- Bug: fix crash when using default VideoCodec thanks to [@Namazed][Namazed] ([#264][264])
- Enhancement: CameraException.getReason() gives some insight about the error ([#265][265])
- Enhancement: Common crashes are now being posted to the error callback instead of crashing the app ([#265][265])

<https://github.com/natario1/CameraView/compare/v1.5.1...v1.6.0>

##### v1.5.1

- Bug: byte array length for Frames was incorrect thanks to [@ssakhavi][ssakhavi] ([#205][205])
- Bug: gestures were crashing in some conditions ([#222][222])
- Bug: import correctly the ExifInterface library ([#222][222])
- Updated dependencies thanks to [@caleb-allen][caleb-allen] ([#190][190])

<https://github.com/natario1/CameraView/compare/v1.5.0...v1.5.1>

### v1.5.0

- New: set encoder for video recordings with `cameraVideoCodec` ([#174][174])
- New: set max duration for videos with `cameraVideoMaxDuration` ([#172][172])
- Enhancement: reduced lag with continuous gestures (ev, zoom) ([#170][170])
- Bug: tap to focus was crashing on some devices ([#167][167])
- Bug: capturePicture was breaking if followed by another event soon after ([#173][173])

<https://github.com/natario1/CameraView/compare/v1.4.2...v1.5.0>

##### v1.4.2

- Add prefix to XML resources so they don't collide, thanks to [@RocketRider][RocketRider] ([#162][162])
- Add `videoMaxSize` API and XML attribute, to set max size video in bytes, thanks to [@chaitanyaraghav][chaitanyaraghav] ([#104][104])
- Improved the preview size selection, thanks to [@YeungKC][YeungKC] ([#133][133])
- Improved the playSounds attribute, was playing incorrectly, thanks to [@xp-vit][xp-vit] ([#143][143])

<https://github.com/natario1/CameraView/compare/v1.4.1...v1.4.2>

##### v1.4.1

- Fixed a bug that would flip the front camera preview on some devices ([#112][112])
- Two new `CameraOptions` APIs: `o.getSupportedPictureSizes()` and `o.getSupportedPictureAspectRatios()` ([#101][101])
- Most controls (video quality, hdr, grid, session type, audio, white balance, flash, facing) now inherit
  from a base `Control` class ([#105][105]). This let us add new APIs:

  - `CameraView.set(Control)`: sets the control to the given value, e.g. `set(Flash.AUTO)`
  - `CameraOptions.supports(Control)`: returns true if the control is supported
  - `CameraOptions.getSupportedControls(Class<? extends Control>)`: returns list of supported controls of a given kind

<https://github.com/natario1/CameraView/compare/v1.4.0...v1.4.1>

### v1.4.0

- CameraView is now completely thread-safe. All actions are asynchronous. ([#97][97])
  This has some breaking drawbacks. Specifically, the `get` methods (e.g., `getWhiteBalance`) might
  not return the correct value while it is being changed. So don't trust them right after you have changed the value.
  Instead, always check the `CameraOptions` to see if the value you want is supported.
- Added error handling ([#97][97]) in `CameraListener.onCameraError(CameraException)`.
  At the moment, all exceptions there are unrecoverable. When the method is called, the camera is showing
  a black preview. This is a good moment to show an error dialog to the user.
  You can also try to `start()` again but that is not guaranteed to work.
- Long requested ability to set the picture output size ([#99][99]). Can be done through
  `CameraView.setPictureSize()` or through new XML attributes starting with `cameraPictureSize`.
  Please refer to docs about it.
- Deprecated `toggleFacing`. It was unreliable and will be removed.
- Deprecated `getCaptureSize`. Use `getPictureSize` instead.
- Fixed bugs.

<https://github.com/natario1/CameraView/compare/v1.3.2...v1.4.0>

##### v1.3.2

- Fixed a memory leak thanks to [@andrewmunn][andrewmunn] ([#92][92])
- Reduced memory usage when using cropOutput thanks to [@RobertoMorelos][RobertoMorelos] ([#93][93])
- Improved efficiency for Frame processors, recycle buffers and Frames ([#94][94])

<https://github.com/natario1/CameraView/compare/v1.3.1...v1.3.2>

##### v1.3.1

- Fixed a bug that would make setFacing and other APIs freeze the camera ([#86][86])
- Fixed ConcurrentModificationExceptions during CameraListener callbacks ([#88][88])

<https://github.com/natario1/CameraView/compare/v1.3.0...v1.3.1>

### v1.3.0

- Ability to inject frame processors to do your own visual tasks (barcodes, facial recognition etc.) ([#82][82])
- Ability to inject external loggers (e.g. Crashlytics) to listen for internal logging events ([#80][80])
- Improved CameraUtils.decodeBitmap, you can now pass maxWidth and maxHeight to avoid OOM ([#83][83])
- Updated dependencies thanks to [@v-gar][v-gar] ([#73][73])

<https://github.com/natario1/CameraView/compare/v1.2.3...v1.3.0>

[aartikov]: https://github.com/aartikov
[athornz]: https://github.com/athornz
[bwt]: https://github.com/bwt
[v-gar]: https://github.com/v-gar
[andrewmunn]: https://github.com/andrewmunn
[chaitanyaraghav]: https://github.com/chaitanyaraghav
[YeungKC]: https://github.com/YeungKC
[RobertoMorelos]: https://github.com/RobertoMorelos
[RocketRider]: https://github.com/RocketRider
[xp-vit]: https://github.com/xp-vit
[caleb-allen]: https://github.com/caleb-allen
[ssakhavi]: https://github.com/ssakhavi
[MatFl]: https://github.com/MatFl
[Namazed]: https://github.com/Namazed
[Keyrillanskiy]: https://github.com/Keyrillanskiy
[mahdi-ninja]: https://github.com/mahdi-ninja
[stefanJi]: https://github.com/stefanJi
[cneuwirt]: https://github.com/cneuwirt
[agrawalsuneet]: https://github.com/agrawalsuneet
[RAN3000]: https://github.com/RAN3000
[vaibhavbhandula]: https://github.com/vaibhavbhandula
[sewar]: https://github.com/sewar
[hualong-shen]: https://github.com/hualong-shen
[EverydayPineapple]: https://github.com/EverydayPineapple
[jeffreyfjohnson]: https://github.com/jeffreyfjohnson
[michaelspecht]: https://github.com/michaelspecht
[EzequielAdrianM]: https://github.com/EzequielAdrianM
[honzasmuk]: https://github.com/honzasmuk
[ObsidianX]: https://github.com/ObsidianX


[73]: https://github.com/natario1/CameraView/pull/73
[80]: https://github.com/natario1/CameraView/pull/80
[82]: https://github.com/natario1/CameraView/pull/82
[83]: https://github.com/natario1/CameraView/pull/83
[86]: https://github.com/natario1/CameraView/pull/86
[88]: https://github.com/natario1/CameraView/pull/88
[92]: https://github.com/natario1/CameraView/pull/92
[93]: https://github.com/natario1/CameraView/pull/93
[94]: https://github.com/natario1/CameraView/pull/94
[97]: https://github.com/natario1/CameraView/pull/97
[99]: https://github.com/natario1/CameraView/pull/99
[101]: https://github.com/natario1/CameraView/pull/101
[104]: https://github.com/natario1/CameraView/pull/104
[105]: https://github.com/natario1/CameraView/pull/105
[112]: https://github.com/natario1/CameraView/pull/112
[133]: https://github.com/natario1/CameraView/pull/133
[143]: https://github.com/natario1/CameraView/pull/143
[162]: https://github.com/natario1/CameraView/pull/162
[167]: https://github.com/natario1/CameraView/pull/167
[170]: https://github.com/natario1/CameraView/pull/170
[172]: https://github.com/natario1/CameraView/pull/172
[173]: https://github.com/natario1/CameraView/pull/173
[174]: https://github.com/natario1/CameraView/pull/174
[190]: https://github.com/natario1/CameraView/pull/190
[205]: https://github.com/natario1/CameraView/pull/205
[222]: https://github.com/natario1/CameraView/pull/222
[224]: https://github.com/natario1/CameraView/pull/224
[245]: https://github.com/natario1/CameraView/pull/245
[264]: https://github.com/natario1/CameraView/pull/264
[265]: https://github.com/natario1/CameraView/pull/265
[290]: https://github.com/natario1/CameraView/pull/290
[297]: https://github.com/natario1/CameraView/pull/297
[332]: https://github.com/natario1/CameraView/pull/332
[344]: https://github.com/natario1/CameraView/pull/344
[356]: https://github.com/natario1/CameraView/pull/356
[360]: https://github.com/natario1/CameraView/pull/360
[374]: https://github.com/natario1/CameraView/pull/374
[392]: https://github.com/natario1/CameraView/pull/392
[393]: https://github.com/natario1/CameraView/pull/393
[471]: https://github.com/natario1/CameraView/pull/471
[431]: https://github.com/natario1/CameraView/pull/431
[403]: https://github.com/natario1/CameraView/pull/403
[421]: https://github.com/natario1/CameraView/pull/421
[435]: https://github.com/natario1/CameraView/pull/435
[477]: https://github.com/natario1/CameraView/pull/477
[482]: https://github.com/natario1/CameraView/pull/482
[484]: https://github.com/natario1/CameraView/pull/484
[490]: https://github.com/natario1/CameraView/pull/490
[497]: https://github.com/natario1/CameraView/pull/497
[498]: https://github.com/natario1/CameraView/pull/498
[501]: https://github.com/natario1/CameraView/pull/501
[502]: https://github.com/natario1/CameraView/pull/502
[506]: https://github.com/natario1/CameraView/pull/506
[513]: https://github.com/natario1/CameraView/pull/513
[517]: https://github.com/natario1/CameraView/pull/517
[521]: https://github.com/natario1/CameraView/pull/521
[527]: https://github.com/natario1/CameraView/pull/527
[528]: https://github.com/natario1/CameraView/pull/528
[530]: https://github.com/natario1/CameraView/pull/530
[535]: https://github.com/natario1/CameraView/pull/535
[537]: https://github.com/natario1/CameraView/pull/537
[545]: https://github.com/natario1/CameraView/pull/545
[551]: https://github.com/natario1/CameraView/pull/551
[552]: https://github.com/natario1/CameraView/pull/552
[559]: https://github.com/natario1/CameraView/pull/559
[564]: https://github.com/natario1/CameraView/pull/564
[572]: https://github.com/natario1/CameraView/pull/572
[574]: https://github.com/natario1/CameraView/pull/574
[580]: https://github.com/natario1/CameraView/pull/580
[588]: https://github.com/natario1/CameraView/pull/588
[617]: https://github.com/natario1/CameraView/pull/617
[630]: https://github.com/natario1/CameraView/pull/630
[632]: https://github.com/natario1/CameraView/pull/632
[651]: https://github.com/natario1/CameraView/pull/651
[653]: https://github.com/natario1/CameraView/pull/653
[661]: https://github.com/natario1/CameraView/pull/661
[691]: https://github.com/natario1/CameraView/pull/691
[696]: https://github.com/natario1/CameraView/pull/696
[697]: https://github.com/natario1/CameraView/pull/697
[704]: https://github.com/natario1/CameraView/pull/704
[716]: https://github.com/natario1/CameraView/pull/716
[718]: https://github.com/natario1/CameraView/pull/718
[724]: https://github.com/natario1/CameraView/pull/724
[732]: https://github.com/natario1/CameraView/pull/732
[741]: https://github.com/natario1/CameraView/pull/741
[745]: https://github.com/natario1/CameraView/pull/745
[754]: https://github.com/natario1/CameraView/pull/754
[775]: https://github.com/natario1/CameraView/pull/775
[779]: https://github.com/natario1/CameraView/pull/779
[798]: https://github.com/natario1/CameraView/pull/798
[816]: https://github.com/natario1/CameraView/pull/816
[851]: https://github.com/natario1/CameraView/pull/851
[861]: https://github.com/natario1/CameraView/pull/861
[877]: https://github.com/natario1/CameraView/pull/877
[897]: https://github.com/natario1/CameraView/pull/897
[953]: https://github.com/natario1/CameraView/pull/953
[960]: https://github.com/natario1/CameraView/pull/960
[992]: https://github.com/natario1/CameraView/pull/992
[1020]: https://github.com/natario1/CameraView/pull/1020
[1024]: https://github.com/natario1/CameraView/pull/1024
[1030]: https://github.com/natario1/CameraView/pull/1030
[1089]: https://github.com/natario1/CameraView/pull/1089
[1068]: https://github.com/natario1/CameraView/pull/1068
[1066]: https://github.com/natario1/CameraView/pull/1066
[1117]: https://github.com/natario1/CameraView/pull/1117
