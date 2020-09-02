package com.otaliastudios.cameraview.demo

import android.graphics.Color
import android.graphics.ImageFormat
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.LayoutParams.MATCH_PARENT

import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraOptions
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.gesture.GestureAction
import com.otaliastudios.cameraview.overlay.OverlayLayout
import kotlin.math.roundToInt
import kotlin.reflect.KClass

/**
 * Controls that we want to display in a ControlView.
 */
abstract class Option<T: Any>(val name: String) {

    abstract fun get(view: CameraView): T

    abstract fun getAll(view: CameraView, options: CameraOptions): Collection<T>

    abstract fun set(view: CameraView, value: T)

    open fun toString(value: T): String {
        return "$value".replace("_", "_").toLowerCase()
    }

    class Width : Option<Int>("Width") {
        override fun get(view: CameraView) = view.layoutParams.width

        override fun set(view: CameraView, value: Int) {
            view.layoutParams.width = value
            view.layoutParams = view.layoutParams
        }

        override fun getAll(view: CameraView, options: CameraOptions): Collection<Int> {
            val root = view.parent as View
            val boundary = root.width.takeIf { it > 0 } ?: 1000
            val step = boundary / 10
            val list = mutableListOf(WRAP_CONTENT, MATCH_PARENT)
            for (i in step until boundary step step) { list.add(i) }
            return list
        }

        override fun toString(value: Int): String {
            if (value == MATCH_PARENT) return "match parent"
            if (value == WRAP_CONTENT) return "wrap content"
            return super.toString(value)
        }
    }

    class Height : Option<Int>("Height") {
        override fun get(view: CameraView) = view.layoutParams.height

        override fun set(view: CameraView, value: Int) {
            view.layoutParams.height = value
            view.layoutParams = view.layoutParams
        }

        override fun getAll(view: CameraView, options: CameraOptions): Collection<Int> {
            val root = view.parent as View
            val boundary = root.height.takeIf { it > 0 } ?: 1000
            val step = boundary / 10
            val list = mutableListOf(WRAP_CONTENT, MATCH_PARENT)
            for (i in step until boundary step step) { list.add(i) }
            return list
        }

        override fun toString(value: Int): String {
            if (value == MATCH_PARENT) return "match parent"
            if (value == WRAP_CONTENT) return "wrap content"
            return super.toString(value)
        }
    }

    abstract class Control<T: com.otaliastudios.cameraview.controls.Control>(private val kclass: KClass<T>, name: String) : Option<T>(name) {
        override fun set(view: CameraView, value: T) = view.set(value)
        override fun get(view: CameraView) = view.get(kclass.java)
        override fun getAll(view: CameraView, options: CameraOptions)
                = options.getSupportedControls(kclass.java)
    }

    class Mode : Control<com.otaliastudios.cameraview.controls.Mode>(com.otaliastudios.cameraview.controls.Mode::class, "Mode")

    class Engine : Control<com.otaliastudios.cameraview.controls.Engine>(com.otaliastudios.cameraview.controls.Engine::class, "Engine") {
        override fun set(view: CameraView, value: com.otaliastudios.cameraview.controls.Engine) {
            if (view.isOpened) {
                view.addCameraListener(object : CameraListener() {
                    override fun onCameraClosed() {
                        super.onCameraClosed()
                        view.removeCameraListener(this)
                        view.engine = value
                        view.open()
                    }
                })
                view.close()
            } else {
                view.engine = value
            }
        }
    }

    class Preview : Control<com.otaliastudios.cameraview.controls.Preview>(com.otaliastudios.cameraview.controls.Preview::class, "Preview Surface") {
        override fun set(view: CameraView, value: com.otaliastudios.cameraview.controls.Preview) {
            if (view.isOpened) {
                view.addCameraListener(object : CameraListener() {
                    override fun onCameraClosed() {
                        super.onCameraClosed()
                        view.removeCameraListener(this)
                        applyPreview(view, value)
                        view.open()
                    }
                })
                view.close()
            } else {
                applyPreview(view, value)
            }
        }

        // This is really tricky since the preview can only be changed when not attached to window.
        private fun applyPreview(view: CameraView, value: com.otaliastudios.cameraview.controls.Preview) {
            val params = view.layoutParams
            val parent = view.parent as ViewGroup
            val index = (0 until parent.childCount).first { parent.getChildAt(it) === view }
            parent.removeView(view)
            view.preview = value
            parent.addView(view, index, params)
        }
    }

    class Flash : Control<com.otaliastudios.cameraview.controls.Flash>(com.otaliastudios.cameraview.controls.Flash::class, "Flash")

    class WhiteBalance : Control<com.otaliastudios.cameraview.controls.WhiteBalance>(com.otaliastudios.cameraview.controls.WhiteBalance::class, "White Balance")

    class Hdr : Control<com.otaliastudios.cameraview.controls.Hdr>(com.otaliastudios.cameraview.controls.Hdr::class, "HDR")

    class PictureMetering : Option<Boolean>("Picture Metering") {
        override fun get(view: CameraView) = view.pictureMetering
        override fun getAll(view: CameraView, options: CameraOptions) = listOf(true, false)
        override fun set(view: CameraView, value: Boolean) { view.pictureMetering = value }
    }

    class PictureSnapshotMetering : Option<Boolean>("Picture Snapshot Metering") {
        override fun get(view: CameraView) = view.pictureSnapshotMetering
        override fun getAll(view: CameraView, options: CameraOptions) = listOf(true, false)
        override fun set(view: CameraView, value: Boolean) { view.pictureSnapshotMetering = value }
    }

    class VideoCodec : Control<com.otaliastudios.cameraview.controls.VideoCodec>(com.otaliastudios.cameraview.controls.VideoCodec::class, "Video Codec")

    class AudioCodec : Control<com.otaliastudios.cameraview.controls.AudioCodec>(com.otaliastudios.cameraview.controls.AudioCodec::class, "Audio Codec")

    class Audio : Control<com.otaliastudios.cameraview.controls.Audio>(com.otaliastudios.cameraview.controls.Audio::class, "Audio")

    abstract class Gesture(val gesture: com.otaliastudios.cameraview.gesture.Gesture, name: String) : Option<GestureAction>(name) {
        override fun set(view: CameraView, value: GestureAction) {
            view.mapGesture(gesture, value)
        }

        override fun get(view: CameraView): GestureAction {
            return view.getGestureAction(gesture)
        }

        override fun getAll(view: CameraView, options: CameraOptions): Collection<GestureAction> {
            return GestureAction.values().filter {
                gesture.isAssignableTo(it) && options.supports(it)
            }
        }
    }

    class Pinch : Gesture(com.otaliastudios.cameraview.gesture.Gesture.PINCH, "Pinch")

    class HorizontalScroll : Gesture(com.otaliastudios.cameraview.gesture.Gesture.SCROLL_HORIZONTAL, "Horizontal Scroll")

    class VerticalScroll : Gesture(com.otaliastudios.cameraview.gesture.Gesture.SCROLL_VERTICAL, "Vertical Scroll")

    class Tap : Gesture(com.otaliastudios.cameraview.gesture.Gesture.TAP, "Tap")

    class LongTap : Gesture(com.otaliastudios.cameraview.gesture.Gesture.LONG_TAP, "Long Tap")

    abstract class Overlay(private val overlay: View, private val target: com.otaliastudios.cameraview.overlay.Overlay.Target, name: String) : Option<Boolean>(name) {
        override fun getAll(view: CameraView, options: CameraOptions) = listOf(true, false)
        override fun get(view: CameraView): Boolean {
            val params = overlay.layoutParams as OverlayLayout.LayoutParams
            return when (target) {
                com.otaliastudios.cameraview.overlay.Overlay.Target.PREVIEW -> params.drawOnPreview
                com.otaliastudios.cameraview.overlay.Overlay.Target.PICTURE_SNAPSHOT -> params.drawOnPictureSnapshot
                com.otaliastudios.cameraview.overlay.Overlay.Target.VIDEO_SNAPSHOT -> params.drawOnVideoSnapshot
            }
        }

        override fun set(view: CameraView, value: Boolean) {
            val params = overlay.layoutParams as OverlayLayout.LayoutParams
            when (target) {
                com.otaliastudios.cameraview.overlay.Overlay.Target.PREVIEW -> params.drawOnPreview = value
                com.otaliastudios.cameraview.overlay.Overlay.Target.PICTURE_SNAPSHOT -> params.drawOnPictureSnapshot = value
                com.otaliastudios.cameraview.overlay.Overlay.Target.VIDEO_SNAPSHOT -> params.drawOnVideoSnapshot = value
            }
            overlay.layoutParams = params
        }
    }

    class OverlayInPreview(overlay: View) : Overlay(overlay, com.otaliastudios.cameraview.overlay.Overlay.Target.PREVIEW, "Overlay in Preview")

    class OverlayInPictureSnapshot(overlay: View) : Overlay(overlay, com.otaliastudios.cameraview.overlay.Overlay.Target.PICTURE_SNAPSHOT, "Overlay in Picture Snapshot")

    class OverlayInVideoSnapshot(overlay: View) : Overlay(overlay, com.otaliastudios.cameraview.overlay.Overlay.Target.VIDEO_SNAPSHOT, "Overlay in Video Snapshot")

    class Grid : Control<com.otaliastudios.cameraview.controls.Grid>(com.otaliastudios.cameraview.controls.Grid::class, "Grid Lines")

    class GridColor : Option<Pair<Int, String>>("Grid Color") {
        private val all = listOf(
                Color.argb(160, 255, 255, 255) to "default",
                Color.WHITE to "white",
                Color.BLACK to "black",
                Color.YELLOW to "yellow"
        )

        override fun getAll(view: CameraView, options: CameraOptions) = all

        override fun get(view: CameraView) = all.first { it.first == view.gridColor }

        override fun set(view: CameraView, value: Pair<Int, String>) {
            view.gridColor = value.first
        }
    }

    class UseDeviceOrientation : Option<Boolean>("Use Device Orientation") {
        override fun getAll(view: CameraView, options: CameraOptions) = listOf(true, false)
        override fun get(view: CameraView) = view.useDeviceOrientation
        override fun set(view: CameraView, value: Boolean) {
            view.useDeviceOrientation = value
        }
    }

    class PreviewFrameRate : Option<Int>("Preview FPS") {
        override fun get(view: CameraView) = view.previewFrameRate.roundToInt()

        override fun set(view: CameraView, value: Int) {
            view.previewFrameRate = value.toFloat()
        }

        override fun getAll(view: CameraView, options: CameraOptions): Collection<Int> {
            val min = options.previewFrameRateMinValue
            val max = options.previewFrameRateMaxValue
            val delta = max - min
            return when {
                min == 0F && max == 0F -> listOf()
                delta < 0.005F -> listOf(min.roundToInt())
                else -> {
                    val results = mutableListOf<Int>()
                    var value = min
                    for (i in 0 until 3) {
                        results.add(value.roundToInt())
                        value += delta / 3
                    }
                    results
                }
            }
        }
    }

    class PictureFormat : Control<com.otaliastudios.cameraview.controls.PictureFormat>(com.otaliastudios.cameraview.controls.PictureFormat::class, "Picture Format")

    class FrameProcessingFormat : Option<Int>("Frame Processing Format") {
        override fun set(view: CameraView, value: Int) {
            view.frameProcessingFormat = value
        }

        override fun get(view: CameraView) = view.frameProcessingFormat

        override fun getAll(view: CameraView, options: CameraOptions)
                = options.supportedFrameProcessingFormats

        override fun toString(value: Int): String {
            return when (value) {
                ImageFormat.NV21 -> "NV21"
                ImageFormat.NV16 -> "NV16"
                ImageFormat.JPEG -> "JPEG"
                ImageFormat.YUY2 -> "YUY2"
                ImageFormat.YUV_420_888 -> "YUV_420_888"
                ImageFormat.YUV_422_888 -> "YUV_422_888"
                ImageFormat.YUV_444_888 -> "YUV_444_888"
                ImageFormat.RAW10 -> "RAW10"
                ImageFormat.RAW12 -> "RAW12"
                ImageFormat.RAW_SENSOR -> "RAW_SENSOR"
                else -> super.toString(value)
            }
        }
    }
}
