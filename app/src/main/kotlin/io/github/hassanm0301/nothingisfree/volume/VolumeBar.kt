package io.github.hassanm0301.nothingisfree.volume

/** A rectangle in screen pixels. Android's `Rect` doesn't work in unit tests, hence this. */
data class ScreenRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2
    val isEmpty: Boolean get() = width <= 0 || height <= 0
    val area: Long get() = if (isEmpty) 0 else width.toLong() * height
}

/**
 * A system window on screen, before its content is looked at.
 *
 * @property id the window's accessibility id, to fetch its content by.
 * @property layer z-order; higher is on top.
 * @property titleSaysVolume the window's title mentions volume (AOSP: "Media volume controls").
 */
data class SystemWindow(val id: Int, val layer: Int, val bounds: ScreenRect, val titleSaysVolume: Boolean)

/**
 * A slider found in a window: where it is, and how full it shows (0..1) if it says.
 */
data class Slider(val bounds: ScreenRect, val fill: Float?)

/**
 * What the label is placed against: beside [outline] if the bar is [vertical], else above it (or
 * below). The outline is the volume slider, stretched across the bar to the edges of the popup
 * window around it where that is known, so the label sits clear of the popup, not on its edge.
 * [fill] is how full the bar shows (0..1), if its slider says.
 */
data class Bar(val outline: ScreenRect, val vertical: Boolean, val fill: Float? = null)

/** Finding the system volume bar on screen, and placing the label beside it. */
object VolumeBar {
    /**
     * A window covering more than this share of the screen is the notification shade or the lock
     * screen, not the volume popup. Both can show sliders of their own (brightness, media).
     */
    const val MAX_POPUP_SHARE = 0.5

    /**
     * The windows worth searching for the volume slider, top-most first: the ones titled as the
     * volume controls or, if none is, every one small enough to be the popup.
     */
    fun candidates(windows: List<SystemWindow>, screen: ScreenRect): List<SystemWindow> {
        val titled = windows.filter { it.titleSaysVolume }
        return titled.ifEmpty { windows.filter { isSmall(it, screen) } }.sortedByDescending { it.layer }
    }

    /** Whether [window] could be the volume popup: titled as the volume controls, or small. */
    fun couldBePopup(window: SystemWindow, screen: ScreenRect): Boolean =
        window.titleSaysVolume || isSmall(window, screen)

    /**
     * The bar in candidate [window], given the first [slider] found in it (null if none). The
     * slider, stretched across to the window's edges if the window is small and, across the bar,
     * no more than half the screen (so it is just the popup). Without a slider, the whole window
     * if it is titled as the volume controls and small.
     */
    fun bar(window: SystemWindow, slider: Slider?, screen: ScreenRect): Bar? {
        val small = isSmall(window, screen)
        val w = window.bounds
        val s = slider?.bounds
        if (s == null || s.isEmpty) {
            return if (window.titleSaysVolume && small) Bar(w, vertical = w.height >= w.width) else null
        }
        val vertical = s.height >= s.width
        val outline = when {
            !small -> s
            vertical && w.width * 2 <= screen.width ->
                ScreenRect(minOf(w.left, s.left), s.top, maxOf(w.right, s.right), s.bottom)
            !vertical && w.height * 2 <= screen.height ->
                ScreenRect(s.left, minOf(w.top, s.top), s.right, maxOf(w.bottom, s.bottom))
            else -> s
        }
        return Bar(outline, vertical, slider.fill)
    }

    /**
     * Where to put a [width] x [height] label against [bar]: [gap] away on the side facing the middle
     * of the screen, centred on it; above a horizontal bar instead (below if there's no room).
     * Returns the top-left corner, kept [margin] inside [screen].
     */
    fun place(bar: Bar, screen: ScreenRect, width: Int, height: Int, gap: Int, margin: Int): Pair<Int, Int> {
        val o = bar.outline
        val (x, y) = if (bar.vertical) {
            val x = if (o.centerX >= screen.centerX) o.left - gap - width else o.right + gap
            x to o.centerY - height / 2
        } else {
            val above = o.top - gap - height
            o.centerX - width / 2 to if (above >= screen.top + margin) above else o.bottom + gap
        }
        return clamp(x, screen.left + margin, screen.right - margin - width) to
            clamp(y, screen.top + margin, screen.bottom - margin - height)
    }

    /**
     * Stands in for the bar when it can't be found: AOSP's default spot for the volume popup, the
     * right edge, centred. [edge] from the edge, [width] x [height].
     */
    fun fallback(screen: ScreenRect, edge: Int, width: Int, height: Int): Bar {
        val right = screen.right - edge
        val top = screen.centerY - height / 2
        return Bar(ScreenRect(right - width, top, right, top + height), vertical = true)
    }

    private fun isSmall(window: SystemWindow, screen: ScreenRect): Boolean =
        !window.bounds.isEmpty && window.bounds.area <= screen.area * MAX_POPUP_SHARE

    /** [value] within [low]..[high]; [low] wins if the label is wider than the room. */
    private fun clamp(value: Int, low: Int, high: Int): Int = maxOf(low, minOf(value, high))
}
