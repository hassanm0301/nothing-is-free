package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.addon.NotificationPulse
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseSettings
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

    /** Runs [loop] on the digital clock: pulse off, default settings and no alert, unless told otherwise. */
    private fun CoroutineScope.runLoop(
        faces: Flow<ToyFace> = flowOf(digital),
        pulseOn: Flow<Boolean> = MutableStateFlow(false),
        alertAt: MutableStateFlow<Long?> = MutableStateFlow(null),
        pulse: Flow<PulseSettings> = MutableStateFlow(PulseSettings()),
        showing: MutableStateFlow<Boolean> = MutableStateFlow(false),
        loop: ToyLoop = this@ToyLoopTest.loop,
    ) = launch { loop.run(faces, pulseOn, pulse, alertAt, showing) }

    /** Ring brightness on the next frame. */
    private suspend fun nextRing() = nextFrame()[top.first, top.second]

    /** Skips frames until the ring reaches [level], e.g. past frames painted before the clock moved. */
    private suspend fun awaitRing(level: Int) = withTimeout(1_000) {
        while (frames.receive()[top.first, top.second] != level) {}
    }

    /** The next minute after the test clock's start, 21:48:00. */
    private val nextMinute = clock.now / 60_000 * 60_000 + 60_000

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
        // One rise time in is the peak of the breath.
        val alertAt = MutableStateFlow<Long?>(clock.now - RISE_MS)
        val job = runLoop(pulseOn = pulseOn, alertAt = alertAt)

        val plain = digital.frame(time, true).cells()
        assertEquals(plain, nextFrame().cells())
        assertIdle()

        pulseOn.value = true
        assertEquals(digital.frame(time, true, pulseLevel = 255).cells(), nextFrame().cells())
        // Still pulsing, so another frame follows within a frame interval.
        assertEquals(255, nextRing())

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
    fun `the pulse ends after its number of breaths`() = runBlocking {
        val alertAt = clock.now
        val settings = PulseSettings(count = 3)
        val duration = settings.durationMs
        val job = runLoop(
            pulseOn = MutableStateFlow(true),
            alertAt = MutableStateFlow(alertAt),
            pulse = MutableStateFlow(settings),
        )

        // Three 2 s breaths run to 6 s; the last one peaks a rise time before that.
        clock.now = alertAt + duration - settings.riseMs
        awaitRing(255)
        assertEquals(255, nextRing())

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
        val alertAt = MutableStateFlow<Long?>(clock.now - RISE_MS)
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
        val alertAt = MutableStateFlow<Long?>(clock.now - RISE_MS)
        val job = runLoop(faces = flowOf(face), pulseOn = MutableStateFlow(true), alertAt = alertAt)

        nextFrame()
        val afterFirst = lookups
        assertTrue(afterFirst > 0)
        repeat(3) { assertEquals(255, nextRing()) }
        assertEquals(afterFirst, lookups)
        job.cancel()
    }

    @Test
    fun `rise time and max brightness shape the breath`() = runBlocking {
        val settings = PulseSettings(riseMs = 2_000, maxBrightness = 100)
        val alertAt = clock.now
        val job = runLoop(
            pulseOn = MutableStateFlow(true),
            alertAt = MutableStateFlow(alertAt),
            pulse = MutableStateFlow(settings),
        )

        assertEquals(0, nextRing())
        clock.now = alertAt + 1_000
        awaitRing(settings.level(1_000))
        assertTrue(settings.level(1_000) in 1 until 100)
        clock.now = alertAt + 2_000
        awaitRing(100)
        assertEquals(100, nextRing())
        job.cancel()
    }

    @Test
    fun `keeps the CPU awake for what is left of the pulse, and lets go when it is switched off`() = runBlocking {
        val awake = Channel<Long?>(Channel.UNLIMITED)
        val loop = ToyLoop(
            show = { frames.trySend(it) },
            is24Hour = { true },
            clock = clock,
            keepAwake = { awake.trySend(it) },
        )
        val pulseOn = MutableStateFlow(true)
        val job = runLoop(pulseOn = pulseOn, alertAt = MutableStateFlow(clock.now - RISE_MS), loop = loop)

        assertEquals(PulseSettings().durationMs - RISE_MS, withTimeout(1_000) { awake.receive() })
        // Told once per pulse, not on every frame.
        repeat(3) { nextFrame() }
        assertNull(awake.tryReceive().getOrNull())

        pulseOn.value = false
        assertNull(withTimeout(1_000) { awake.receive() })
        job.cancel()
    }

    @Test
    fun `battery saver holds a notification for the next minute`() = runBlocking {
        val alertAt = MutableStateFlow<Long?>(null)
        val job = runLoop(
            pulseOn = MutableStateFlow(true),
            alertAt = alertAt,
            pulse = MutableStateFlow(PulseSettings(batterySaver = true)),
        )
        assertEquals(digital.frame(time, true).cells(), nextFrame().cells())

        // Not even a repaint when it arrives, nor on a refresh within the same minute.
        clock.now += 5_000
        alertAt.value = clock.now
        assertIdle()
        clock.now += 5_000
        loop.refresh()
        assertEquals(0, nextRing())
        assertIdle()

        // The minute's repaint (the AOD tick here) starts the pulse.
        clock.now = nextMinute
        loop.refresh()
        assertEquals(0, nextRing())
        clock.now = nextMinute + RISE_MS
        awaitRing(255)

        // Once over, the same notification doesn't pulse again at the minute after.
        clock.now = nextMinute + 60_000
        loop.refresh()
        awaitRing(0)
        assertIdle()
        job.cancel()
    }

    @Test
    fun `battery saver can pulse every minute while a notification is still showing`() = runBlocking {
        val showing = MutableStateFlow(true)
        val start = clock.now
        val job = runLoop(
            pulseOn = MutableStateFlow(true),
            alertAt = MutableStateFlow(start - 10 * 60_000),
            pulse = MutableStateFlow(PulseSettings(count = 5, batterySaver = true, repeatWhileShowing = true)),
            showing = showing,
        )
        // The first look is straight away, and the old notification is still in the shade.
        assertEquals(0, nextRing())
        clock.now = start + RISE_MS
        awaitRing(255)
        clock.now = start + 10_000
        awaitRing(0)
        assertIdle()

        clock.now = nextMinute
        loop.refresh()
        assertEquals(0, nextRing())
        clock.now = nextMinute + RISE_MS
        awaitRing(255)
        clock.now = nextMinute + 10_000
        awaitRing(0)
        assertIdle()

        // Cleared: the next minute stays dark.
        showing.value = false
        clock.now = nextMinute + 60_000
        loop.refresh()
        assertEquals(0, nextRing())
        assertIdle()
        job.cancel()
    }

    private companion object {
        /** Comfortably more than the loop's 40 ms frame interval. */
        const val QUIET_MS = 200L

        /** The default rise time: one in, a breath peaks. */
        const val RISE_MS = 1_000L
    }
}
