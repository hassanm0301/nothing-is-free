package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

class BinaryClockToyTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-28T21:47:30Z"), ZoneOffset.UTC)
    private val time = LocalTime.of(21, 47, 30)
    private val face = BinaryClockFace(13)
    private val frames = Channel<DotMatrix>(Channel.UNLIMITED)
    private var is24Hour = true
    private val toy = BinaryClockToy(face, show = { frames.trySend(it) }, is24Hour = { is24Hour }, clock = clock)

    private suspend fun nextFrame() = withTimeout(1_000) { frames.receive() }

    private fun DotMatrix.cells() = toFrame().toList()

    @Test
    fun `repaints at once when the settings change`() = runBlocking {
        val settings = MutableStateFlow(BinaryClockSettings(style = BitStyle.SQUARE))
        val job = launch { toy.run(settings) }
        assertEquals(face.render(time, true, settings.value).cells(), nextFrame().cells())
        assertNull(frames.tryReceive().getOrNull())

        settings.value = BinaryClockSettings(style = BitStyle.DOT, activeBrightness = 128)
        assertEquals(face.render(time, true, settings.value).cells(), nextFrame().cells())
        assertNull(frames.tryReceive().getOrNull())
        job.cancel()
    }

    @Test
    fun `waits for the minute when idle but repaints on refresh`() = runBlocking {
        val settings = BinaryClockSettings()
        val job = launch { toy.run(MutableStateFlow(settings)) }
        assertEquals(face.render(time, true, settings).cells(), nextFrame().cells())
        assertNull(frames.tryReceive().getOrNull())

        // A refresh picks up anything that changed meanwhile, here the 12/24-hour setting.
        is24Hour = false
        toy.refresh()
        assertEquals(face.render(time, false, settings).cells(), nextFrame().cells())
        job.cancel()
    }
}
