package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.addon.NotificationPulse
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseWave
import io.github.hassanm0301.nothingisfree.glyph.addon.frame
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Drives any Glyph Toy: repaints its face on each minute, on [refresh], at once when an input
 * changes, and every frame while a notification pulse is running for it. Frames are drawn through
 * [frame], like the in-app previews. SDK-free; [show] is where frames leave for the hardware.
 */
class ToyLoop(
    private val show: (DotMatrix) -> Unit,
    private val is24Hour: () -> Boolean,
    private val clock: Clock = PhoneClock,
) {
    private val redraw = Channel<Unit>(Channel.CONFLATED)

    /** Repaint now rather than at the next minute, e.g. on the AOD tick or a time change. */
    fun refresh() {
        redraw.trySend(Unit)
    }

    /**
     * Runs until cancelled. [faces] re-emits when the toy's options change; [pulseOn] is whether
     * the notification pulse addon is on for this toy. Any new input restarts the loop, so it shows
     * at once and a new alert starts its pulse straight away.
     */
    suspend fun run(
        faces: Flow<ToyFace>,
        pulseOn: Flow<Boolean>,
        lastAlertAt: Flow<Long?>,
        pulseWindowSeconds: Flow<Int>,
    ) {
        // The ring only depends on the face, so work it out once per face rather than per frame.
        val withRing = faces.map { it to NotificationPulse.ring(it) }
        combine(withRing, pulseOn, lastAlertAt, pulseWindowSeconds) { (face, ring), on, alertAt, seconds ->
            Scene(face, ring, alertAt?.takeIf { on }, seconds * 1_000L)
        }.collectLatest(::paint)
    }

    private suspend fun paint(scene: Scene) {
        while (true) {
            val now = clock.millis()
            val elapsed = PulseWave.elapsed(scene.alertAt, scene.windowMs, now)
            val level = elapsed?.let(PulseWave::level) ?: 0
            show(scene.face.frame(LocalTime.now(clock), is24Hour(), level, scene.ring))
            val wait = if (elapsed != null) FRAME_MS else MINUTE_MS - now % MINUTE_MS
            withTimeoutOrNull(wait) { redraw.receive() }
        }
    }

    /** What to paint until an input changes. [alertAt] is null while the pulse is off for the toy. */
    private class Scene(
        val face: ToyFace,
        val ring: List<Pair<Int, Int>>,
        val alertAt: Long?,
        val windowMs: Long,
    )

    private companion object {
        const val FRAME_MS = 40L
        const val MINUTE_MS = 60_000L
    }
}

/**
 * The system clock in the phone's time zone as it is now. `Clock.systemDefaultZone()` would keep
 * the zone from when the toy started; Android resets the process default when the zone changes,
 * so reading it on each call follows the phone (from the next repaint, at most a minute later).
 */
private object PhoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = system(zone)
    override fun instant(): Instant = Instant.now()
}
