package io.github.hassanm0301.nothingisfree.volume

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeFormatTest {

    @Test
    fun `percent rounds to the nearest whole percent`() {
        assertEquals(0, VolumeFormat.percent(0, 15))
        assertEquals(7, VolumeFormat.percent(1, 15))
        assertEquals(47, VolumeFormat.percent(7, 15))
        assertEquals(53, VolumeFormat.percent(8, 15))
        assertEquals(13, VolumeFormat.percent(1, 8))
        assertEquals(100, VolumeFormat.percent(15, 15))
    }

    @Test
    fun `percent is zero without steps`() {
        assertEquals(0, VolumeFormat.percent(3, 0))
        assertEquals(0, VolumeFormat.percent(3, -1))
    }

    @Test
    fun `text writes a percentage or the step out of the steps`() {
        assertEquals("47%", VolumeFormat.PERCENT.text(7, 15))
        assertEquals("7/15", VolumeFormat.STEPS.text(7, 15))
        assertEquals("0%", VolumeFormat.PERCENT.text(0, 15))
        assertEquals("15/15", VolumeFormat.STEPS.text(15, 15))
    }

    @Test
    fun `text keeps the step within the steps`() {
        assertEquals("100%", VolumeFormat.PERCENT.text(20, 15))
        assertEquals("0/15", VolumeFormat.STEPS.text(-1, 15))
        assertEquals("0/0", VolumeFormat.STEPS.text(3, 0))
        assertEquals("0%", VolumeFormat.PERCENT.text(3, 0))
    }

    @Test
    fun `widest is the reading at full volume`() {
        assertEquals("100%", VolumeFormat.PERCENT.widest(15))
        assertEquals("15/15", VolumeFormat.STEPS.widest(15))
        assertEquals("7/7", VolumeFormat.STEPS.widest(7))
    }

    @Test
    fun `only the streams the volume bar shows get a label`() {
        listOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_VOICE_CALL,
        ).forEach { assertTrue("stream $it", VolumeStreams.isShown(it)) }
        listOf(
            AudioManager.STREAM_SYSTEM,
            AudioManager.STREAM_DTMF,
            AudioManager.STREAM_ACCESSIBILITY,
            -1,
        ).forEach { assertFalse("stream $it", VolumeStreams.isShown(it)) }
    }
}
