package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

class ClockToyTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-28T09:41:30Z"), ZoneOffset.UTC)
    private val face = ClockFace(13)
    private val frames = Channel<DotMatrix>(Channel.UNLIMITED)
    private val toy = ClockToy(face, show = { frames.trySend(it) }, is24Hour = { true }, clock = clock)
    private val top = face.ring.first()

    private suspend fun nextFrame() = withTimeout(1_000) { frames.receive() }

    @Test
    fun `pulses inside the window and goes dark when it is cleared`() = runBlocking {
        val alertAt = MutableStateFlow<Long?>(clock.millis() - PulseWave.PERIOD_MS / 2)
        val job = launch { toy.run(alertAt, MutableStateFlow(30)) }

        // Half a period in is the peak of the breath.
        val expected = face.render(LocalTime.of(9, 41, 30), is24Hour = true, ringLevel = 255)
        assertEquals(expected.toFrame().toList(), nextFrame().toFrame().toList())
        // Still pulsing, so another frame follows within a frame interval.
        assertEquals(255, nextFrame()[top.first, top.second])

        alertAt.value = null
        withTimeout(1_000) {
            do {
                val frame = frames.receive()
            } while (frame[top.first, top.second] != 0)
        }
        job.cancel()
    }

    @Test
    fun `waits for the minute when idle but repaints on refresh`() = runBlocking {
        val job = launch { toy.run(MutableStateFlow(null), MutableStateFlow(30)) }
        assertEquals(0, nextFrame()[top.first, top.second])
        assertEquals(null, frames.tryReceive().getOrNull())

        toy.refresh()
        nextFrame()
        job.cancel()
    }
}
