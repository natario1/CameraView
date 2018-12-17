# Migrating to v2.X.X

CameraView v2 introduces various breaking changes that will allow for more flexibility in the future,
removes useless features and makes method names consistent. Upgrading will require addressing these
in your app, plus understanding new concepts.

Until the final v2 release, these things might change, but likely they will not.

## Removals

### Jpeg Quality
Both `cameraJpegQuality` and `setJpegQuality()` have been removed. They were working only with specific
setups and made no real sense. We will use the default quality provided by the camera engine.

### Crop Output
Both `cameraCropOutput` and `setCropOutput()` have been removed. This was an expensive operation that
worked with pictures only. In v2, if you want your output to be cropped to match the view bounds, you
will use the `*snapshot()` APIs (see below).

### Video Quality
This was an opaque option packaging various parameters. It has been removed.
You are expected to control the video quality by choosing the video size and setting video parameters
with new APIs (see below).

### Other removals

- `ExtraProperties`: This has been removed as it was useless.

## CameraUtils

- The `BitmapCallback` has been moved into a separate class.
- The `BitmapCallback` result is now `@Nullable`! This will happen if we encounter an `OutOfMemoryError` during decoding.
  You should consider passing a maxWidth and maxHeight instead of loading the full image.

## CameraOptions

- Methods returning a `Set` now return a `Collection` instead.
- `isVideoSnapshotSupported()` was removed, as we do not rely on internal video snapshot feature anymore. See below.
- In addition to `getSupportedPictureSizes` and `getSupportedPictureAspectRatio`, we now have equivalent methods for video. See below.

## Session type
The `SessionType` has been renamed to `Mode` which has a clearer meaning.

- `setSessionType()` is now `setMode()`
- `cameraSessionType` is now `cameraMode`

## Sizing

- `getPreviewSize()` was removed.
- `getPictureSize()`: now returns the real output picture size. This means that it accounts for rotation.
  It will also return `null` while in `VIDEO` mode: use getVideoSize in that case.
- `getVideoSize()`: added. Returns the real output video size. This means that it accounts for rotation.
  It will return `null` while in `PICTURE` mode.
- `getSnapshotSize()`: This is the size of pictures taken with `takePictureSnapshot()` and videos taken 
  with `takeVideoSnapshot()`. It accounts for rotation and cropping. Read about snapshots below.
  
As you might have guessed, video size is now configurable, with the addition of `setVideoSize(SizeSelector)` method.
It works exactly like the picture one, so please refer to the size selector documentation. Defaults to `SizeSelectors.biggest()`.

The engine will use the video size selector when mode is `VIDEO`, and the picture size selector when mode is `PICTURE`.
  
## Picture and videos

### Take, not capture

- `capturePicture()` is now `takePicture()`
- `captureSnapshot()` is now `takePictureSnapshot()`
- `startCapturingVideo()` is now `takeVideo()`. Signature changed from long to int
- `isCapturingVideo()` is now `isTakingVideo()`

The new `isTakingPicture()` method was added for symmetry with videos.

### Snapshots
This is the major improvement over v1. There are now 4 capture APIs, two for pictures and two for videos.

- Standard APIs: `takePicture()` and `takeVideo()`. These take a high quality picture or video, depending
  on the `SizeSelector` and parameters that were used. The standard APIs **must** be called in the appropriate `Mode`
  (pictures must be taken in `PICTURE` mode, videos must be taken in `VIDEO` mode).
- Snapshot APIs: `takePictureSnapshot()` and `takeVideoSnapshot()`. These take a super fast, reliable
  snapshot of the camera preview. The snapshot APIs can be called in any `Mode` (you can snap videos in picture mode).
  
The good news is that snapshot APIs will **automatically crop the result**, for both video and pictures, 
which means that **square videos** or any other ratio are possible.

|Method|Takes|Quality|Callable in `Mode.PICTURE`|Callable in `Mode.VIDEO`|Auto crop|Output size|
|------|-----|-------|--------------------------|------------------------|---------|-----------|
|`takePicture()`|Pictures|Standard|`yes`|`no`|`no`|That of `setPictureSize`|
|`takeVideo()`|Videos|Standard|`no`|`yes`|`no`|That of `setVideoSize`|
|`takePictureSnapshot()`|Pictures|Snapshot|`yes`|`yes`|`yes`|That of the view|
|`takeVideoSnapshot()`|Videos|Snapshot|`yes`|`yes`|`yes`|That of the view|
  
The video snapshot supports audio and respects the `Audio`, max duration, max size & codec settings,
which makes it a powerful tool. The drawback is that it needs:

- API 18. If called before, it throws
- An OpenGL preview (see below). If not, it throws

### Video capturing
Some new APIs were introduced, which are respected by both standard videos and snapshot videos:

- `setAudioBitRate()` and `cameraAudioBitRate`: sets the audio bit rate in bit/s
- `setVideoBitRate()` and `cameraVideoBitRate`: sets the video bit rate in bit/s

## Camera Preview
The type of preview is now configurable with `cameraPreview` XML attribute and `Preview` control class.
This defaults to the new `GL_SURFACE` and it is highly recommended that you do not change this.

|Preview|Backed by|Info|
|-------|---------|----|
|`Preview.SURFACE`|A `SurfaceView`|This might be better for battery, but will not work well (AFAIR) with dynamic layout changes and similar things. No support for video snapshots.|
|`Preview.TEXTURE`|A `TextureView`|Better. Requires hardware acceleration. No support for video snapshots.|
|`Preview.GL_SURFACE`|A `GLSurfaceView`|Supports video snapshots. Might support GL real time filters in the future.|

The GL surface, as an extra benefit, has a much more efficient way of capturing picture snapshots,
that avoids OOM errors, rotating the image on the fly, reading EXIF, and other horrible things belonging to v1.
These picture snapshots will also work while taking videos.

## CameraListener
The listener interface brings two breaking signature changes:

- `onPictureTaken()` now returns a `PictureResult`. Use `result.getJpeg()` to access the jpeg stream.
  The result class includes rich information about the picture (or picture snapshot) that was taken,
  plus handy utilities (`result.asBitmap()`...)
- `onVideoTaken()` now returns a `VideoResult`. Use `result.getFile()` to access the video file.
  The result class includes rich information about the video (or video snapshot) that was taken.
  
## Experimental mode
The v2 version introduces a `cameraExperimental` XML flag that you can use to enable experimental features.
Might be used in the future to speed up development.

## Other improvements
- Added `@Nullable` and `@NonNull` annotations pretty much everywhere. This might **break** your Kotlin build.
- Added `setGridColor()` and `cameraGridColor` to control the grid color
- Default `Facing` value is not `BACK` anymore but rather a value that guarantees that you have cameras (if possible).
  If device has no `BACK` cameras, defaults to `FRONT`.  
  
TODO: document cameraGridColor
TODO: document setVideoBitRate
TODO: document setAudioBitRate
TODO: document takeVideoSnapshot
TODO: document that takePictureSnaphot works while taking videos, if GL_SURFACE
TODO: document the camera previews
TODO: think bout getPreviewSize() being removed, and think about adding a setPreviewSize().
      If adding getPreviewSize() back, update this doc.
TODO: fix video recording rotation: with front camera, it does not work.
TODO: opencollective
TODO: new docs?


