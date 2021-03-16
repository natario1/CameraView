---
layout: page
title: "Watermarks and Overlays"
description: "Static and animated overlays"
order: 11
disqus: 1
---

CameraView offers a simple yet powerful framework for watermarks and overlays of any kind.
These overlays can be shown on the live camera preview, plus they appear on the media results
taken with `takePictureSnapshot()` or `takeVideoSnapshot()`.

### Simple Usage

```xml
<com.otaliastudios.cameraview.CameraView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content">
    
    <!-- Watermark in bottom/end corner -->
    <ImageView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:src="@drawable/watermark"
        app:layout_drawOnPreview="true|false"
        app:layout_drawOnPictureSnapshot="true|false"
        app:layout_drawOnVideoSnapshot="true|false"/>
        
    <!-- More overlays here... -->
        
</com.otaliastudios.cameraview.CameraView>
```

As you can see, the overlay system is View-based - each overlay is just a real `View` attached
into the hierarchy. This is a powerful and creative tool. You can, for instance, retrieve the
overlay with `findViewById` and:

- Animate it!
- Change its visibility
- Change its position or appearance
- Do so while video is being recorded

Any changes in the overlay appearance will be recorded in real-time in the picture snapshot
or video snapshot that you are capturing.
 
As you can see in the example, you can also selectively choose, for each overlay, whether it
will draw on the preview (`layout_drawOnPreview`), on picture snapshots (`layout_drawOnPictureSnapshot`), 
on video snapshots (`layout_drawOnVideoSnapshot`).
 
### Advanced Usage

To add an overlay at runtime, simply use `addView()`, but make sure you pass in an instance of
`OverlayLayout.LayoutParams`:

```java
OverlayLayout.LayoutParams() params = new OverlayLayout.LayoutParams();
cameraView.addView(overlay, params);
```

To remove an overlay at runtime, simply use `removeView()`:

```java
cameraView.removeView(overlay);
```

To change the `layout_` flags at runtime, you should cast the overlay `LayoutParams` as follows:

```java
// Cast to OverlayLayout.LayoutParams
View overlay = findViewById(R.id.watermark);
OverlayLayout.LayoutParams params = (OverlayLayout.LayoutParams) overlay.getLayoutParams();

// Perform changes
params.drawOnPreview = true; // draw on preview
params.drawOnPreview = false; // do not draw on preview
params.drawOnPictureSnapshot = true; // draw on picture snapshots
params.drawOnPictureSnapshot = false; // do not draw on picture snapshots
params.drawOnVideoSnapshot = true; // draw on video snapshots
params.drawOnVideoSnapshot = false; // do not draw on video snapshots

// When done, apply
overlay.setLayoutParams(params);
```

To capture a hardware rendered View such as a video rendered to a TextureView, enable the
`cameraDrawHardwareOverlays` flag:

```xml
<com.otaliastudios.cameraview.CameraView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:cameraDrawHardwareOverlays="true"/>
```

Alternatively you can enable it in code with `setDrawHardwareOverlays()`:

```java
cameraView.setDrawHardwareOverlays(true);
```