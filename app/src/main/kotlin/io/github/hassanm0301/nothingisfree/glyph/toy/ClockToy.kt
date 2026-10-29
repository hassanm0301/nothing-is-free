package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseWave
import io.github.hassanm0301.nothingisfree.glyph.addon.frame
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.LocalTime

/**
 * Drives the clock toy: repaints on each minute, and every frame while a notification pulse is
 * running. SDK-free; [show] is where frames leave for the hardware.
 */
class ClockToy(
    private val face: ClockFace,
    private val show: (DotMatrix) -> Unit,
    private val is24Hour: () -> Boolean,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val redraw = Channel<Unit>(Channel.CONFLATED)

    /** Repaint now rather than at the next minute, e.g. on the AOD tick or a time change. */
    fun refresh() {
        redraw.trySend(Unit)
    }

    /** Runs until cancelled. A new alert or window restarts the loop so the pulse starts at once. */
    suspend fun run(lastAlertAt: Flow<Long?>, windowSeconds: Flow<Int>) {
        combine(lastAlertAt, windowSeconds) { at, seconds -> at to seconds * 1_000L }
            .collectLatest { (alertAt, windowMs) -> paint(alertAt, windowMs) }
    }

    private suspend fun paint(alertAt: Long?, windowMs: Long) {
        while (true) {
            val now = clock.millis()
            val elapsed = PulseWave.elapsed(alertAt, windowMs, now)
            val ring = elapsed?.let(PulseWave::level) ?: 0
            show(face.frame(LocalTime.now(clock), is24Hour(), ring))
            val wait = if (elapsed != null) FRAME_MS else MINUTE_MS - now % MINUTE_MS
            withTimeoutOrNull(wait) { redraw.receive() }
        }
    }

    private companion object {
        const val FRAME_MS = 40L
        const val MINUTE_MS = 60_000L
    }
}
