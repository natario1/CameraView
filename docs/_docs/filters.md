---
layout: page
title: "Real-time Filters"
description: "Apply filters to preview and snapshots"
order: 12
disqus: 1
---

Starting from version `2.1.0`, CameraView experimentally supports real-time filters that can modify
the camera frames before they are shown and recorded. Just like [overlays](watermarks-and-overlays),
these filters are applied to the preview and to any [picture or video snapshots](capturing-media).

Starting from `2.5.0`, this feature is considered to be stable and you do not need the experimental 
flag to use it. The only condition is to use the `Preview.GL_SURFACE` preview.

### Simple usage

```xml
<com.otaliastudios.cameraview.CameraView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:cameraFilter="@string/cameraview_filter_none"/>
```

Real-time filters are applied at creation time, through the `app:cameraFilter` XML attribute,
or anytime during the camera lifecycle using `cameraView.setFilter()`.

We offer a reasonable amount of filters through the `Filters` class, for example:

```java
cameraView.setFilter(Filters.BLACK_AND_WHITE.newInstance());
cameraView.setFilter(Filters.VIGNETTE.newInstance());
cameraView.setFilter(Filters.SEPIA.newInstance());
```

All of the filters stored in the `Filters` class have an XML string resource that you can use
to quickly setup the camera view. For example, `Filters.BLACK_AND_WHITE` can be used by setting
`app:cameraFilter` to `"@string/cameraview_filter_black_and_white"`.

The default filter is called `NoFilter` (`Filters.NONE`) and can be used to clear up any other
filter that was previously set and return back to normal.

|Filter class|Filters value|XML resource value|
|------------|-------------|---------------------|
|`NoFilter`|`Filters.NONE`|`@string/cameraview_filter_none`|
|`AutoFixFilter`|`Filters.AUTO_FIX`|`@string/cameraview_filter_autofix`|
|`BlackAndWhiteFilter`|`Filters.BLACK_AND_WHITE`|`@string/cameraview_filter_black_and_white`|
|`BrightnessFilter`|`Filters.BRIGHTNESS`|`@string/cameraview_filter_brightness`|
|`ContrastFilter`|`Filters.CONTRAST`|`@string/cameraview_filter_contrast`|
|`CrossProcessFilter`|`Filters.CROSS_PROCESS`|`@string/cameraview_filter_cross_process`|
|`DocumentaryFilter`|`Filters.DOCUMENTARY`|`@string/cameraview_filter_documentary`|
|`DuotoneFilter`|`Filters.DUOTONE`|`@string/cameraview_filter_duotone`|
|`FillLightFilter`|`Filters.FILL_LIGHT`|`@string/cameraview_filter_fill_light`|
|`GammaFilter`|`Filters.GAMMA`|`@string/cameraview_filter_gamma`|
|`GrainFilter`|`Filters.GRAIN`|`@string/cameraview_filter_grain`|
|`GrayscaleFilter`|`Filters.GRAYSCALE`|`@string/cameraview_filter_grayscale`|
|`HueFilter`|`Filters.HUE`|`@string/cameraview_filter_hue`|
|`InvertColorsFilter`|`Filters.INVERT_COLORS`|`@string/cameraview_filter_invert_colors`|
|`LomoishFilter`|`Filters.LOMOISH`|`@string/cameraview_filter_lomoish`|
|`PosterizeFilter`|`Filters.POSTERIZE`|`@string/cameraview_filter_posterize`|
|`SaturationFilter`|`Filters.SATURATION`|`@string/cameraview_filter_saturation`|
|`SepiaFilter`|`Filters.SEPIA`|`@string/cameraview_filter_sepia`|
|`SharpnessFilter`|`Filters.SHARPNESS`|`@string/cameraview_filter_sharpness`|
|`TemperatureFilter`|`Filters.TEMPERATURE`|`@string/cameraview_filter_temperature`|
|`TintFilter`|`Filters.TINT`|`@string/cameraview_filter_tint`|
|`VignetteFilter`|`Filters.VIGNETTE`|`@string/cameraview_filter_vignette`|

### Filters controls

Most of the provided filters accept input parameters to tune them. For example, `DuotoneFilter` will
accept two colors to apply the duotone effect.

```java
duotoneFilter.setFirstColor(Color.RED);
duotoneFilter.setSecondColor(Color.GREEN);
```

You can change these values by acting on the filter object, before or after passing it to `CameraView`.
Whenever something is changed, the updated values will be visible immediately in the next frame.

You can also map the first or second filter control to a gesture (like horizontal or vertical scrolling),
as explained in [the gesture documentation](gestures):

```java
camera.mapGesture(Gesture.SCROLL_HORIZONTAL, GestureAction.FILTER_CONTROL_1);
camera.mapGesture(Gesture.SCROLL_VERTICAL, GestureAction.FILTER_CONTROL_2);
```

### MultiFilter

CameraView provides a special filter called `MultiFilter` which can be used to group different filters
together, and apply them in sequence to the input frame, in order to process it more than once.

```java
camera.setFilter(new MultiFilter(firstFilter, secondFilter, thirdFilter));
```

You can even add new filters to the group at any time, using `MultiFilter.addFilter()`.
The `MultiFilter` will also try to dispatch [filter controls](#filters-controls) (e.g. from gestures) 
to its children.

There are some technical caveats when using a `MultiFilter`:

- Using a large number of child filters can consume the available graphic memory
- For [advanced users](#advanced-usage), child filters need to read from `GLES20.GL_TEXTURE_2D`
  instead of the typical `GLES11Ext.GL_TEXTURE_EXTERNAL_OES`. To achieve this, we get your fragment 
  shader String and replace any `"samplerExternalOES"` with `"sampler2D"`. This is a hack
  that might cause issues with specific shader implementations.

### Advanced usage

Advanced users with OpenGL experience can create their own filters by implementing the `Filter` interface
and passing in a fragment shader and a vertex shader that will be used for drawing.

##### Simple filters

For very simple filters that have a static fragment shader, you can create a working filter
implementation by simply creating an instance of `SimpleFilter`:

```java
Filter filter = new SimpleFilter(myFragmentShader);
```

##### More complex filters

We recommend:

- Subclassing `BaseFilter` instead of implementing `Filter`, since that takes care of most of the work
- If accepting parameters, implementing `OneParameterFilter` or `TwoParameterFilter` as well

Most of all, the best way of learning is by looking at the current filters implementations in the
`com.otaliastudios.cameraview.filters` package.

### Related APIs

|Method|Description|
|------|-----------|
|`setFilter(Filter)`|Sets a new real-time filter.|
|`getFilter()`|Returns the current real-time filter.|