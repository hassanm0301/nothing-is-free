package io.github.hassanm0301.nothingisfree.ui.glyph

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrixCanvas
import io.github.hassanm0301.nothingisfree.glyph.addon.NotificationPulse
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseWave
import io.github.hassanm0301.nothingisfree.glyph.addon.frame
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyFace
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Matrix size the previews draw at: the Phone (4a) Pro's. */
const val PREVIEW_SIZE = 13

/**
 * A toy's matrix as the phone shows it: [face] at [time] with the notification pulse at
 * [pulseLevel], drawn through the same `frame()` the toy services use.
 */
@Composable
fun ToyMatrix(face: ToyFace, time: LocalTime, pulseLevel: Int, modifier: Modifier = Modifier) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val ring = remember(face) { NotificationPulse.ring(face) }
    val frame = remember(face, time.hour, time.minute, is24Hour, pulseLevel) {
        face.frame(time, is24Hour, pulseLevel, ring)
    }
    DotMatrixCanvas(frame, modifier, isDot = face::isLed)
}

/**
 * Wall-clock time, to the second. Ticks each second so it catches up straight after sleep or a
 * time change; a [ToyMatrix] only redraws when the minute changes.
 */
@Composable
fun rememberNow(): State<LocalTime> = produceState(LocalTime.now().withNano(0)) {
    while (true) {
        value = LocalTime.now().withNano(0)
        delay(1_000L - System.currentTimeMillis() % 1_000L)
    }
}

/** Where a notification pulse is: ring [level] (0..255) and [remainingMs] (0 = not pulsing). */
data class PulseState(val level: Int = 0, val remainingMs: Long = 0) {
    val pulsing: Boolean get() = remainingMs > 0
}

/**
 * The notification pulse from [alertAt], animating every frame for [windowSeconds] (rounded up
 * to whole breaths). Idle when [enabled] is false or no pulse is running.
 */
@Composable
fun rememberPulse(alertAt: Long?, windowSeconds: Int, enabled: Boolean): State<PulseState> =
    produceState(PulseState(), alertAt, windowSeconds, enabled) {
        val windowMs = windowSeconds * 1_000L
        while (enabled) {
            val elapsed = PulseWave.elapsed(alertAt, windowMs, System.currentTimeMillis()) ?: break
            value = PulseState(PulseWave.level(elapsed), PulseWave.duration(windowMs) - elapsed)
            withFrameMillis { }
        }
        value = PulseState()
    }

/** How the phone shows a time on this device: 24-hour or 12-hour, as a toy would. */
@Composable
fun formatTime(time: LocalTime): String {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return time.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "hh:mm a"))
}
