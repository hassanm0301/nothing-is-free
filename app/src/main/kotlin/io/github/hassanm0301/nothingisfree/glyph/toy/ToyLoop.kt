package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.addon.MinuteCheck
import io.github.hassanm0301.nothingisfree.glyph.addon.NotificationPulse
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseSettings
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseWave
import io.github.hassanm0301.nothingisfree.glyph.addon.frame
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Drives any Glyph Toy: repaints its face on each minute, on [refresh], at once when an input
 * changes, and every frame while a notification pulse is running for it. Frames are drawn through
 * [frame], like the in-app previews. SDK-free; [show] is where frames leave for the hardware, and
 * [keepAwake] is told how long a pulse that just started still has to run (null once none is), so
 * it can keep the CPU up meanwhile.
 */
class ToyLoop(
    private val show: (DotMatrix) -> Unit,
    private val is24Hour: () -> Boolean,
    private val clock: Clock = PhoneClock,
    private val keepAwake: (forMs: Long?) -> Unit = {},
) {
    private val redraw = Channel<Unit>(Channel.CONFLATED)

    /** Battery saver's once-a-minute look at the notifications. Kept across scenes, like the clock. */
    private val minuteCheck = MinuteCheck()

    /** When the pulse [keepAwake] was last told about ends, or null if it was told none runs. */
    private var awakeUntil: Long? = null

    /** Repaint now rather than at the next minute, e.g. on the AOD tick or a time change. */
    fun refresh() {
        redraw.trySend(Unit)
    }

    /**
     * Runs until cancelled. [faces] re-emits when the toy's options change; [pulseOn] is whether
     * the notification pulse addon is on for this toy. Any new input restarts the loop, so it shows
     * at once and a new alert starts its pulse straight away; except with battery saver, which
     * reads [lastAlertAt] and [anyShowing] only on the first repaint of each minute.
     */
    suspend fun run(
        faces: Flow<ToyFace>,
        pulseOn: Flow<Boolean>,
        pulse: Flow<PulseSettings>,
        lastAlertAt: StateFlow<Long?>,
        anyShowing: StateFlow<Boolean>,
    ) {
        // The ring only depends on the face, so work it out once per face rather than per frame.
        val withRing = faces.map { it to NotificationPulse.ring(it) }
        combine(withRing, pulseOn, pulse, lastAlertAt) { (face, ring), on, settings, alertAt ->
            Scene(face, ring, on, settings, alertAt?.takeIf { on && !settings.batterySaver })
        }
            // With the pulse off or on battery saver, a new alert changes nothing on screen yet;
            // don't repaint for it.
            .distinctUntilChanged()
            .collectLatest { paint(it, lastAlertAt, anyShowing) }
    }

    private suspend fun paint(scene: Scene, lastAlertAt: StateFlow<Long?>, anyShowing: StateFlow<Boolean>) {
        val pulse = scene.pulse
        while (true) {
            val now = clock.millis()
            // Checked on every repaint, so the first one of each minute is battery saver's look.
            val saverStart = minuteCheck.update(now, lastAlertAt.value, pulse.repeatWhileShowing && anyShowing.value)
            val startAt = when {
                !scene.pulseOn -> null
                pulse.batterySaver -> saverStart
                else -> scene.alertAt
            }
            val elapsed = PulseWave.elapsed(startAt, pulse.durationMs, now)
            stayAwake(startAt?.takeIf { elapsed != null }?.plus(pulse.durationMs), now)
            val level = elapsed?.let(pulse::level) ?: 0
            show(scene.face.frame(LocalTime.now(clock), is24Hour(), level, scene.ring))
            val wait = if (elapsed != null) FRAME_MS else MINUTE_MS - now % MINUTE_MS
            withTimeoutOrNull(wait) { redraw.receive() }
        }
    }

    private fun stayAwake(until: Long?, now: Long) {
        if (until == awakeUntil) return
        awakeUntil = until
        keepAwake(until?.let { it - now })
    }

    /**
     * What to paint until an input changes. [alertAt] is the last alert when it starts pulses
     * itself: null while the pulse is off for the toy or on battery saver.
     */
    private data class Scene(
        val face: ToyFace,
        val ring: List<Pair<Int, Int>>,
        val pulseOn: Boolean,
        val pulse: PulseSettings,
        val alertAt: Long?,
    )

    private companion object {
        const val FRAME_MS = 40L
        const val MINUTE_MS = MinuteCheck.MINUTE_MS
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
