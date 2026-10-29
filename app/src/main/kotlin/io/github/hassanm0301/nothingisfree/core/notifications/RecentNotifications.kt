package io.github.hassanm0301.nothingisfree.core.notifications

import android.app.NotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * When the last alerting notification arrived. Fed by [GlyphNotificationListener], read by the
 * Glyph Toys for the notification pulse addon. Process-scoped: the listener and the toys live in
 * the app process and keep it alive.
 */
object RecentNotifications {
    private val _lastAlertAt = MutableStateFlow<Long?>(null)

    /** Wall-clock millis of the newest alerting notification, or null if none since start. */
    val lastAlertAt: StateFlow<Long?> = _lastAlertAt.asStateFlow()

    /**
     * Stamp an alert at [atMillis], keeping the newest. Times ahead of [now] are clamped, and a
     * stored time that is now in the future (the clock was set back) gives way to the new one;
     * otherwise it would hold off every pulse until the clock caught up.
     */
    fun record(atMillis: Long, now: Long = System.currentTimeMillis()) {
        val at = minOf(atMillis, now)
        _lastAlertAt.update { previous -> if (previous == null || previous > now || at > previous) at else previous }
    }

    /** Forget the last alert. For tests, which share this process-wide state. */
    internal fun clear() {
        _lastAlertAt.value = null
    }

    /**
     * Whether a posted notification should count. Skips our own, ongoing ones (media, downloads,
     * foreground services), silent channels, ones Do Not Disturb holds back, and quiet updates
     * (`FLAG_ONLY_ALERT_ONCE`) to a notification that is already showing.
     */
    fun isAlerting(
        ownPackage: Boolean,
        ongoing: Boolean,
        importance: Int,
        passesDnd: Boolean,
        onlyAlertOnce: Boolean,
        alreadyShowing: Boolean,
    ): Boolean = !ownPackage &&
        !ongoing &&
        importance >= NotificationManager.IMPORTANCE_DEFAULT &&
        passesDnd &&
        !(onlyAlertOnce && alreadyShowing)
}
