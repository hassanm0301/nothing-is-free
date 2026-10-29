package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.LocalTime

/**
 * Drives the binary clock toy: repaints on each minute, on [refresh], and as soon as the settings
 * change. SDK-free; [show] is where frames leave for the hardware.
 */
class BinaryClockToy(
    private val face: BinaryClockFace,
    private val show: (DotMatrix) -> Unit,
    private val is24Hour: () -> Boolean,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val redraw = Channel<Unit>(Channel.CONFLATED)

    /** Repaint now rather than at the next minute, e.g. on the AOD tick or a time change. */
    fun refresh() {
        redraw.trySend(Unit)
    }

    /** Runs until cancelled. New settings restart the loop, so they show at once. */
    suspend fun run(settings: Flow<BinaryClockSettings>) {
        settings.collectLatest(::paint)
    }

    private suspend fun paint(settings: BinaryClockSettings) {
        while (true) {
            val now = clock.millis()
            show(face.render(LocalTime.now(clock), is24Hour(), settings))
            withTimeoutOrNull(MINUTE_MS - now % MINUTE_MS) { redraw.receive() }
        }
    }

    private companion object {
        const val MINUTE_MS = 60_000L
    }
}
