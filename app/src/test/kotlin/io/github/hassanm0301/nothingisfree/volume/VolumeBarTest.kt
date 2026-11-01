package io.github.hassanm0301.nothingisfree.volume

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VolumeBarTest {

    private val screen = ScreenRect(0, 0, 1080, 2400)

    /** A popup at the right edge, like AOSP's, with its slider inset from the popup's edges. */
    private val popup = SystemWindow(id = 1, layer = 5, bounds = ScreenRect(920, 800, 1060, 1600), titleSaysVolume = false)
    private val slider = ScreenRect(950, 850, 1030, 1550)

    private val shade = SystemWindow(id = 2, layer = 3, bounds = screen, titleSaysVolume = false)
    private val statusBar = SystemWindow(id = 3, layer = 4, bounds = ScreenRect(0, 0, 1080, 100), titleSaysVolume = false)

    @Test
    fun `candidates are the windows small enough to be the popup, top-most first`() {
        assertEquals(listOf(popup, statusBar), VolumeBar.candidates(listOf(shade, statusBar, popup), screen))
    }

    @Test
    fun `a window titled as the volume controls is the only candidate, whatever its size`() {
        val titled = shade.copy(id = 9, titleSaysVolume = true)
        assertEquals(listOf(titled), VolumeBar.candidates(listOf(popup, titled, statusBar), screen))
    }

    @Test
    fun `no windows, no candidates`() {
        assertEquals(emptyList<SystemWindow>(), VolumeBar.candidates(emptyList(), screen))
        assertEquals(emptyList<SystemWindow>(), VolumeBar.candidates(listOf(shade), screen))
    }

    @Test
    fun `a slider in a narrow popup reaches out to the popup's edges`() {
        assertEquals(Bar(ScreenRect(920, 850, 1060, 1550), vertical = true), VolumeBar.bar(popup, slider, screen))
    }

    @Test
    fun `a horizontal slider reaches up and down to the popup's edges`() {
        val panel = SystemWindow(id = 4, layer = 5, bounds = ScreenRect(100, 1800, 980, 2100), titleSaysVolume = false)
        val across = ScreenRect(140, 1900, 940, 1940)
        assertEquals(Bar(ScreenRect(140, 1800, 940, 2100), vertical = false), VolumeBar.bar(panel, across, screen))
    }

    @Test
    fun `the slider alone in a window too big or too wide to be just the popup`() {
        val titledShade = shade.copy(titleSaysVolume = true)
        assertEquals(Bar(slider, vertical = true), VolumeBar.bar(titledShade, slider, screen))
        val wide = SystemWindow(id = 5, layer = 5, bounds = ScreenRect(0, 1000, 1080, 1400), titleSaysVolume = false)
        val inWide = ScreenRect(950, 1020, 1030, 1380)
        assertEquals(Bar(inWide, vertical = true), VolumeBar.bar(wide, inWide, screen))
    }

    @Test
    fun `a small window titled as the volume controls stands for the bar without a slider`() {
        val titled = popup.copy(titleSaysVolume = true)
        assertEquals(Bar(titled.bounds, vertical = true), VolumeBar.bar(titled, null, screen))
    }

    @Test
    fun `no slider and no volume title means no bar`() {
        assertNull(VolumeBar.bar(popup, null, screen))
        assertNull(VolumeBar.bar(shade.copy(titleSaysVolume = true), null, screen))
        assertNull(VolumeBar.bar(popup, ScreenRect(10, 10, 10, 50), screen))
    }

    @Test
    fun `the label goes on the side of the bar facing the middle, centred on it`() {
        val right = Bar(ScreenRect(920, 800, 1060, 1600), vertical = true)
        assertEquals(784 to 1170, VolumeBar.place(right, screen, width = 120, height = 60, gap = 16, margin = 16))
        val left = Bar(ScreenRect(20, 800, 160, 1600), vertical = true)
        assertEquals(176 to 1170, VolumeBar.place(left, screen, width = 120, height = 60, gap = 16, margin = 16))
    }

    @Test
    fun `beside or above follows the slider, not the outline's shape`() {
        val squat = Bar(ScreenRect(600, 1000, 1060, 1300), vertical = true)
        assertEquals(464 to 1120, VolumeBar.place(squat, screen, width = 120, height = 60, gap = 16, margin = 16))
    }

    @Test
    fun `a horizontal bar gets the label above it, or below with no room above`() {
        val middle = Bar(ScreenRect(100, 1200, 980, 1240), vertical = false)
        assertEquals(480 to 1124, VolumeBar.place(middle, screen, width = 120, height = 60, gap = 16, margin = 16))
        val top = Bar(ScreenRect(100, 20, 980, 60), vertical = false)
        assertEquals(480 to 76, VolumeBar.place(top, screen, width = 120, height = 60, gap = 16, margin = 16))
    }

    @Test
    fun `the label stays inside the screen`() {
        val offTop = Bar(ScreenRect(920, -400, 1060, 100), vertical = true)
        assertEquals(784 to 16, VolumeBar.place(offTop, screen, width = 120, height = 60, gap = 16, margin = 16))
        val offBottom = Bar(ScreenRect(920, 2300, 1060, 3000), vertical = true)
        assertEquals(784 to 2324, VolumeBar.place(offBottom, screen, width = 120, height = 60, gap = 16, margin = 16))
        // Wider than the screen: pinned to the left margin rather than thrown off the right.
        val bar = Bar(popup.bounds, vertical = true)
        assertEquals(16 to 1170, VolumeBar.place(bar, screen, width = 2000, height = 60, gap = 16, margin = 16))
    }

    @Test
    fun `the stand-in bar sits at the right edge, centred`() {
        assertEquals(
            Bar(ScreenRect(910, 900, 1040, 1500), vertical = true),
            VolumeBar.fallback(screen, edge = 40, width = 130, height = 600),
        )
    }
}
