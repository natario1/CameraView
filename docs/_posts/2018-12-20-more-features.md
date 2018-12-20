---
layout: page
title: "More features"
subtitle: "Undocumented features & more"
category: docs
order: 11
date: 2018-12-20 20:41:20
---

### Extra controls

```xml
<com.otaliastudios.cameraview.CameraView
    app:cameraPlaySounds="true|false"
    app:cameraGrid="off|draw3x3|draw4x4|drawPhi"
    app:cameraGridColor="@color/black"/>
```

#### cameraPlaySounds

Controls whether we should play platform-provided sounds during certain events
(shutter click, focus completed). Please note that:

- on API < 16, this flag is always set to `false`
- the Camera1 engine will always play shutter sounds regardless of this flag

Defaults to true.

```java
cameraView.setPlaySounds(true);
cameraView.setPlaySounds(false);
```

#### cameraGrid

Lets you draw grids over the camera preview. Supported values are `off`, `draw3x3` and `draw4x4`
for regular grids, and `drawPhi` for a grid based on the golden ratio constant, often used in photography.
Defaults to `OFF`.

```java
cameraView.setGrid(Grid.OFF);
cameraView.setGrid(Grid.DRAW_3X3);
cameraView.setGrid(Grid.DRAW_4X4);
cameraView.setGrid(Grid.DRAW_PHI);
```

#### cameraGridColor

Lets you choose the color for grid lines. 
Defaults to a shade of grey.

```java
cameraView.setGridColor(Color.WHITE);
cameraView.setGridColor(Color.BLACK);
```

### Undocumented features

Some features and APIs were not documented in this document, including:

- `CameraUtils` utilities
- `CameraOptions` options
- `CameraView.setLocation` APIs

For informations, please take a look at the javadocs or the source code. 

|Method|Description|
|------|-----------|
|`setZoom(float)`, `getZoom()`|Sets a zoom value, where 0 means camera zoomed out and 1 means zoomed in. No-op if zoom is not supported, or camera not started.|
|`setExposureCorrection(float)`, `getExposureCorrection()`|Sets exposure compensation EV value, in camera stops. No-op if this is not supported. Should be between the bounds returned by CameraOptions.|
|`setLocation(Location)`|Sets location data to be appended to picture/video metadata.|
|`setLocation(double, double)`|Sets latitude and longitude to be appended to picture/video metadata.|
|`getLocation()`|Retrieves location data previously applied with setLocation().|
|`startAutoFocus(float, float)`|Starts an autofocus process at the given coordinates, with respect to the view dimensions.|



