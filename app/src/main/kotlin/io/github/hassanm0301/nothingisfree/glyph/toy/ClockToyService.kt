package io.github.hassanm0301.nothingisfree.glyph.toy

import android.os.PowerManager
import android.text.format.DateFormat
import io.github.hassanm0301.nothingisfree.core.notifications.RecentNotifications
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonPreferences
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseWave
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The "Pulse clock" Glyph Toy: a digital clock whose outer ring breathes for a while after a
 * notification. The Glyph service binds this when the user picks the toy (on the Phone (4a) Pro:
 * Settings > Glyph Interface > Flip to Glyph > Always-on Glyph Toy) and unbinds it when they leave.
 */
class ClockToyService : GlyphToyService() {

    private var toy: ClockToy? = null

    /** Keeps the CPU up while the ring pulses, so the animation runs with the screen off. */
    private val wakeLock: PowerManager.WakeLock by lazy {
        getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NothingIsFree:ClockToyPulse")
            .apply { setReferenceCounted(false) }
    }

    override fun onToyStart(size: Int, show: (DotMatrix) -> Unit): List<Job> {
        val clockToy = ClockToy(
            face = ClockFace(size),
            show = show,
            is24Hour = { DateFormat.is24HourFormat(this) },
        )
        toy = clockToy
        val pulses = RecentNotifications.lastAlertAt
        val window = AddonPreferences.pulseWindowSeconds
        return listOf(
            scope.launch { clockToy.run(pulses, window) },
            scope.launch {
                combine(pulses, window) { at, seconds -> at?.plus(PulseWave.duration(seconds * 1_000L)) }
                    .collect { until ->
                        val remaining = (until ?: 0L) - System.currentTimeMillis()
                        if (remaining > 0) wakeLock.acquire(remaining)
                    }
            },
        )
    }

    override fun onAod() {
        toy?.refresh()
    }

    override fun onToyStop() {
        toy = null
        if (wakeLock.isHeld) wakeLock.release()
    }
}
