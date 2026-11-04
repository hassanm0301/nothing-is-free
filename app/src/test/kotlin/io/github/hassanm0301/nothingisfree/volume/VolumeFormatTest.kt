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

    private val music = StreamLevel(AudioManager.STREAM_MUSIC, index = 7, min = 0, max = 15)
    private val ring = StreamLevel(AudioManager.STREAM_RING, index = 5, min = 0, max = 7)
    private val alarm = StreamLevel(AudioManager.STREAM_ALARM, index = 7, min = 1, max = 7)
    private val call = StreamLevel(AudioManager.STREAM_VOICE_CALL, index = 2, min = 1, max = 5)

    @Test
    fun `the stream is the one whose level the bar shows`() {
        assertEquals(AudioManager.STREAM_RING, VolumeStreams.pick(5f / 7, listOf(music, ring)))
        // A bar that counts in hundredths: 47 of 100 is media's 7 of 15.
        assertEquals(AudioManager.STREAM_MUSIC, VolumeStreams.pick(0.47f, listOf(ring, music)))
    }

    @Test
    fun `when several streams match, the most likely wins`() {
        val fullMusic = music.copy(index = 15)
        assertEquals(AudioManager.STREAM_MUSIC, VolumeStreams.pick(1f, listOf(fullMusic, alarm)))
        assertEquals(AudioManager.STREAM_ALARM, VolumeStreams.pick(1f, listOf(alarm, fullMusic)))
    }

    @Test
    fun `a bar may count from zero or from the stream's lowest step`() {
        val quietMusic = music.copy(index = 3)
        // Call volume 2 in 1..5: a quarter full from its lowest step, 40% from zero.
        assertEquals(AudioManager.STREAM_VOICE_CALL, VolumeStreams.pick(0.25f, listOf(quietMusic, call)))
        assertEquals(AudioManager.STREAM_VOICE_CALL, VolumeStreams.pick(0.4f, listOf(quietMusic, call)))
    }

    @Test
    fun `without a fill or a match, the most likely stream`() {
        assertEquals(AudioManager.STREAM_MUSIC, VolumeStreams.pick(null, listOf(music, ring)))
        assertEquals(AudioManager.STREAM_MUSIC, VolumeStreams.pick(0.9f, listOf(music, ring)))
        assertEquals(AudioManager.STREAM_MUSIC, VolumeStreams.pick(0.5f, emptyList()))
    }

    @Test
    fun `media is most likely, call volume during a call`() {
        assertEquals(AudioManager.STREAM_MUSIC, VolumeStreams.ORDER.first())
        assertEquals(AudioManager.STREAM_VOICE_CALL, VolumeStreams.IN_CALL_ORDER.first())
        assertEquals(VolumeStreams.ORDER.sorted(), VolumeStreams.IN_CALL_ORDER.sorted())
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
