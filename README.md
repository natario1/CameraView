[![Build Status](https://travis-ci.org/natario1/CameraView.svg?branch=master)](https://travis-ci.org/natario1/CameraView)
[![Code Coverage](https://codecov.io/gh/natario1/CameraView/branch/master/graph/badge.svg)](https://codecov.io/gh/natario1/CameraView)
[![Release](https://img.shields.io/github/release/natario1/CameraView.svg)](https://github.com/natario1/CameraView/releases)
[![Issues](https://img.shields.io/github/issues-raw/natario1/CameraView.svg)](https://github.com/natario1/CameraView/issues)
[![Funding](https://img.shields.io/opencollective/all/CameraView.svg?colorB=r)](https://natario1.github.io/CameraView/extra/donate)

&#10240;  <!-- Hack to add whitespace -->

<p align="center">
  <img src="docs/static/banner.png" width="100%">
</p>

# CameraView

CameraView is a well documented, high-level library that makes capturing pictures and videos easy,
addressing most of the common issues and needs, and still leaving you with flexibility where needed.

```groovy
api 'com.otaliastudios:cameraview:2.1.0'
```

- Fast & reliable
- Gestures support [[docs]](https://natario1.github.io/CameraView/docs/gestures.html)
- Real-time filters [[docs]](https://natario1.github.io/CameraView/docs/filters.html)
- Camera1 or Camera2 powered engine [[docs]](https://natario1.github.io/CameraView/docs/previews.html)
- Frame processing support [[docs]](https://natario1.github.io/CameraView/docs/frame-processing.html)
- Watermarks & animated overlays [[docs]](https://natario1.github.io/CameraView/docs/watermarks-and-overlays.html)
- OpenGL powered preview [[docs]](https://natario1.github.io/CameraView/docs/previews.html)
- Take high-quality content with `takePicture` and `takeVideo` [[docs]](https://natario1.github.io/CameraView/docs/capturing-media.html)
- Take super-fast snapshots with `takePictureSnapshot` and `takeVideoSnapshot` [[docs]](https://natario1.github.io/CameraView/docs/capturing-media.html)
- Smart sizing: create a `CameraView` of any size [[docs]](https://natario1.github.io/CameraView/docs/preview-size.html)
- Control HDR, flash, zoom, white balance, exposure, location, grid drawing & more [[docs]](https://natario1.github.io/CameraView/docs/controls.html)
- Lightweight
- Works down to API level 15
- Well tested

Read the [official website](https://natario1.github.io/CameraView) for setup instructions and documentation.
You might also be interested in [changelog](https://natario1.github.io/CameraView/about/changelog.html)
or in the [v1 migration guide](https://natario1.github.io/CameraView/extra/v1-migration-guide.html).

&#10240;  <!-- Hack to add whitespace -->

<p align="center">
  <img src="docs/static/screen1.png" width="250" hspace="5"><img src="docs/static/screen2.png" width="250" hspace="5"><img src="docs/static/screen3.png" width="250" hspace="5">
</p>

&#10240;  <!-- Hack to add whitespace -->

If you like the project, use it with profit, or simply want to thank back, please consider [donating
to the project](https://natario1.github.io/CameraView/extra/donate) now! You can either make a one time
donation or become a sponsor, in which case your company logo will immediately show up here.

Thank you for any contribution - it is a nice reward for what has been done until now, and a 
motivation boost to push the library forward.

```xml
<com.otaliastudios.cameraview.CameraView
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:cameraPictureSizeMinWidth="@integer/picture_min_width"
    app:cameraPictureSizeMinHeight="@integer/picture_min_height"
    app:cameraPictureSizeMaxWidth="@integer/picture_max_width"
    app:cameraPictureSizeMaxHeight="@integer/picture_max_height"
    app:cameraPictureSizeMinArea="@integer/picture_min_area"
    app:cameraPictureSizeMaxArea="@integer/picture_max_area"
    app:cameraPictureSizeSmallest="false|true"
    app:cameraPictureSizeBiggest="false|true"
    app:cameraPictureSizeAspectRatio="@string/video_ratio"
    app:cameraVideoSizeMinWidth="@integer/video_min_width"
    app:cameraVideoSizeMinHeight="@integer/video_min_height"
    app:cameraVideoSizeMaxWidth="@integer/video_max_width"
    app:cameraVideoSizeMaxHeight="@integer/video_max_height"
    app:cameraVideoSizeMinArea="@integer/video_min_area"
    app:cameraVideoSizeMaxArea="@integer/video_max_area"
    app:cameraVideoSizeSmallest="false|true"
    app:cameraVideoSizeBiggest="false|true"
    app:cameraVideoSizeAspectRatio="@string/video_ratio"
    app:cameraSnapshotMaxWidth="@integer/snapshot_max_width"
    app:cameraSnapshotMaxHeight="@integer/snapshot_max_height"
    app:cameraVideoBitRate="@integer/video_bit_rate"
    app:cameraAudioBitRate="@integer/audio_bit_rate"
    app:cameraGestureTap="none|autoFocus|takePicture"
    app:cameraGestureLongTap="none|autoFocus|takePicture"
    app:cameraGesturePinch="none|zoom|exposureCorrection|filterControl1|filterControl2"
    app:cameraGestureScrollHorizontal="none|zoom|exposureCorrection|filterControl1|filterControl2"
    app:cameraGestureScrollVertical="none|zoom|exposureCorrection|filterControl1|filterControl2"
    app:cameraEngine="camera1|camera2"
    app:cameraPreview="glSurface|surface|texture"
    app:cameraFacing="back|front"
    app:cameraHdr="on|off"
    app:cameraFlash="on|auto|torch|off"
    app:cameraWhiteBalance="auto|cloudy|daylight|fluorescent|incandescent"
    app:cameraMode="picture|video"
    app:cameraAudio="on|off|mono|stereo"
    app:cameraGrid="draw3x3|draw4x4|drawPhi|off"
    app:cameraGridColor="@color/grid_color"
    app:cameraPlaySounds="true|false"
    app:cameraVideoMaxSize="@integer/video_max_size"
    app:cameraVideoMaxDuration="@integer/video_max_duration"
    app:cameraVideoCodec="deviceDefault|h264|h263"
    app:cameraAutoFocusResetDelay="@integer/autofocus_delay"
    app:cameraAutoFocusMarker="@string/cameraview_default_autofocus_marker"
    app:cameraUseDeviceOrientation="true|false"
    app:cameraFilter="@string/real_time_filter"
    app:cameraExperimental="false|true">
    
    <!-- Watermark! -->
    <ImageView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:src="@drawable/watermark"
        app:layout_drawOnPreview="true|false"
        app:layout_drawOnPictureSnapshot="true|false"
        app:layout_drawOnVideoSnapshot="true|false"/>
        
</com.otaliastudios.cameraview.CameraView>
```

## Backers

Thanks to all backers! [Become a backer.](https://opencollective.com/cameraview#backer)

<a href="https://opencollective.com/cameraview#backers" target="_blank"><img src="https://opencollective.com/cameraview/backers.svg?width=890"></a>

## Sponsors

Thanks to sponsors! [Become a sponsor](https://opencollective.com/cameraview#sponsor) and have your logo here.

<a href="https://opencollective.com/cameraview/sponsor/0/website" target="_blank"><img src="https://opencollective.com/cameraview/sponsor/0/avatar.svg"></a>
<a href="https://opencollective.com/cameraview/sponsor/1/website" target="_blank"><img src="https://opencollective.com/cameraview/sponsor/1/avatar.svg"></a>
<a href="https://opencollective.com/cameraview/sponsor/2/website" target="_blank"><img src="https://opencollective.com/cameraview/sponsor/2/avatar.svg"></a>
<a href="https://opencollective.com/cameraview/sponsor/3/website" target="_blank"><img src="https://opencollective.com/cameraview/sponsor/3/avatar.svg"></a>
<a href="https://opencollective.com/cameraview/sponsor/4/website" target="_blank"><img src="https://opencollective.com/cameraview/sponsor/4/avatar.svg"></a>
<a href="https://opencollective.com/cameraview/sponsor/5/website" target="_blank"><img src="https://opencollective.com/cameraview/sponsor/5/avatar.svg"></a>
<a href="https://opencollective.com/cameraview/sponsor/6/website" target="_blank"><img src="https://opencollective.com/cameraview/sponsor/6/avatar.svg"></a>
<a href="https://opencollective.com/cameraview/sponsor/7/website" target="_blank"><img src="https://opencollective.com/cameraview/sponsor/7/avatar.svg"></a>
<a href="https://opencollective.com/cameraview/sponsor/8/website" target="_blank"><img src="https://opencollective.com/cameraview/sponsor/8/avatar.svg"></a>
<a href="https://opencollective.com/cameraview/sponsor/9/website" target="_blank"><img src="https://opencollective.com/cameraview/sponsor/9/avatar.svg"></a>

