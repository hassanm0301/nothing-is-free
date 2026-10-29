package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.addon.NotificationPulse
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseWave
import io.github.hassanm0301.nothingisfree.glyph.addon.frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class ToyLoopTest {

    /** A clock the test moves by hand; the loop's own waits still run in real time. */
    private class ManualClock(var now: Long) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(now)
    }

    private val clock = ManualClock(Instant.parse("2026-10-28T21:47:10Z").toEpochMilli())
    private val time = LocalTime.of(21, 47, 10)
    private val digital = ClockFace(13)
    private val top = NotificationPulse.ring(digital).first()
    private val frames = Channel<DotMatrix>(Channel.UNLIMITED)
    private var is24Hour = true
    private val loop = ToyLoop(show = { frames.trySend(it) }, is24Hour = { is24Hour }, clock = clock)

    private suspend fun nextFrame() = withTimeout(1_000) { frames.receive() }

    /** Several frame intervals pass without a repaint: the loop is back to waiting for the minute. */
    private suspend fun assertIdle() = assertNull(withTimeoutOrNull(QUIET_MS) { frames.receive() })

    private fun DotMatrix.cells() = toFrame().toList()

    /** Runs [loop] on the digital clock, pulse off and no alert, unless told otherwise. */
    private fun CoroutineScope.runLoop(
        faces: Flow<ToyFace> = flowOf(digital),
        pulseOn: Flow<Boolean> = MutableStateFlow(false),
        alertAt: Flow<Long?> = MutableStateFlow(null),
        windowSeconds: Flow<Int> = MutableStateFlow(30),
    ) = launch { loop.run(faces, pulseOn, alertAt, windowSeconds) }

    @Test
    fun `paints the first frame at once`() = runBlocking {
        val job = runLoop()
        assertEquals(digital.frame(time, is24Hour = true).cells(), nextFrame().cells())
        assertNull(frames.tryReceive().getOrNull())
        job.cancel()
    }

    @Test
    fun `a notification with the pulse off does not repaint`() = runBlocking {
        val alertAt = MutableStateFlow<Long?>(null)
        val job = runLoop(pulseOn = MutableStateFlow(false), alertAt = alertAt)
        nextFrame()
        alertAt.value = clock.now
        assertIdle()
        job.cancel()
    }

    @Test
    fun `waits for the minute when idle but repaints on refresh`() = runBlocking {
        val job = runLoop()
        nextFrame()
        assertIdle()

        // A refresh picks up anything that changed meanwhile, here the 12/24-hour setting.
        is24Hour = false
        loop.refresh()
        assertEquals(digital.frame(time, is24Hour = false).cells(), nextFrame().cells())
        assertIdle()
        job.cancel()
    }

    @Test
    fun `repaints at once when the face changes`() = runBlocking {
        val binary = BinaryClockFace(13)
        val faces = MutableStateFlow(binary.with(BinaryClockSettings(style = BitStyle.SQUARE)))
        val job = runLoop(faces = faces)
        assertEquals(faces.value.frame(time, true).cells(), nextFrame().cells())
        assertNull(frames.tryReceive().getOrNull())

        faces.value = binary.with(BinaryClockSettings(style = BitStyle.DOT, activeBrightness = 128))
        assertEquals(faces.value.frame(time, true).cells(), nextFrame().cells())
        assertNull(frames.tryReceive().getOrNull())
        job.cancel()
    }

    @Test
    fun `pulses only while the addon is on for the toy`() = runBlocking {
        val pulseOn = MutableStateFlow(false)
        // Half a period in is the peak of the breath.
        val alertAt = MutableStateFlow<Long?>(clock.now - PulseWave.PERIOD_MS / 2)
        val job = runLoop(pulseOn = pulseOn, alertAt = alertAt)

        val plain = digital.frame(time, true).cells()
        assertEquals(plain, nextFrame().cells())
        assertIdle()

        pulseOn.value = true
        assertEquals(digital.frame(time, true, pulseLevel = 255).cells(), nextFrame().cells())
        // Still pulsing, so another frame follows within a frame interval.
        assertEquals(255, nextFrame()[top.first, top.second])

        // Off mid-pulse: back to the plain frame, and the animation stops.
        pulseOn.value = false
        val last = withTimeout(1_000) {
            var frame: DotMatrix
            do {
                frame = frames.receive()
            } while (frame[top.first, top.second] != 0)
            frame
        }
        assertEquals(plain, last.cells())
        assertIdle()
        job.cancel()
    }

    @Test
    fun `the pulse ends after the window rounded up to whole breaths`() = runBlocking {
        val alertAt = clock.now
        val duration = PulseWave.duration(25 * 1_000L)
        val job = runLoop(
            pulseOn = MutableStateFlow(true),
            alertAt = MutableStateFlow(alertAt),
            windowSeconds = MutableStateFlow(25),
        )

        // A 25 s window runs to 26 s; the last breath peaks half a period before that.
        clock.now = alertAt + duration - PulseWave.PERIOD_MS / 2
        withTimeout(1_000) {
            do {
                val frame = frames.receive()
            } while (frame[top.first, top.second] != 255)
        }
        assertEquals(255, nextFrame()[top.first, top.second])

        // Frames already queued were painted before the end; the next one is plain, then nothing.
        clock.now = alertAt + duration
        while (frames.tryReceive().isSuccess) {}
        val end = time.plusSeconds(duration / 1_000)
        assertEquals(digital.frame(end, true).cells(), nextFrame().cells())
        assertIdle()
        job.cancel()
    }

    @Test
    fun `binary clock keeps its bits intact under the pulse`() = runBlocking {
        val face = BinaryClockFace(13).with(BinaryClockSettings(style = BitStyle.SQUARE, showUnusedBits = true))
        val alertAt = MutableStateFlow<Long?>(clock.now - PulseWave.PERIOD_MS / 2)
        val job = runLoop(faces = flowOf(face), pulseOn = MutableStateFlow(true), alertAt = alertAt)

        val frame = nextFrame()
        assertEquals(face.frame(time, true, pulseLevel = 255).cells(), frame.cells())
        val bits = face.render(time, true)
        val ring = NotificationPulse.ring(face).toSet()
        assertTrue(ring.isNotEmpty())
        for (y in 0 until 13) for (x in 0 until 13) {
            val expected = if (x to y in ring) 255 else bits[x, y]
            assertEquals("($x,$y)", expected, frame[x, y])
        }
        job.cancel()
    }

    @Test
    fun `works out the ring once per face, not per frame`() = runBlocking {
        var lookups = 0
        val face = object : ToyFace by digital {
            override fun occupies(x: Int, y: Int) = digital.occupies(x, y).also { lookups++ }
        }
        val alertAt = MutableStateFlow<Long?>(clock.now - PulseWave.PERIOD_MS / 2)
        val job = runLoop(faces = flowOf(face), pulseOn = MutableStateFlow(true), alertAt = alertAt)

        nextFrame()
        val afterFirst = lookups
        assertTrue(afterFirst > 0)
        repeat(3) { assertEquals(255, nextFrame()[top.first, top.second]) }
        assertEquals(afterFirst, lookups)
        job.cancel()
    }

    private companion object {
        /** Comfortably more than the loop's 40 ms frame interval. */
        const val QUIET_MS = 200L
    }
}
