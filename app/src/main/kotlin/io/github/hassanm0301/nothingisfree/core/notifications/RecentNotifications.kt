package io.github.hassanm0301.nothingisfree.core.notifications

import android.app.NotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * When the last alerting notification arrived, and whether any is still showing. Fed by
 * [GlyphNotificationListener], read by the Glyph Toys for the notification pulse addon.
 * Process-scoped: the listener and the toys live in the app process and keep it alive.
 */
object RecentNotifications {
    private val _lastAlertAt = MutableStateFlow<Long?>(null)
    private val showingKeys = mutableSetOf<String>()
    private val _anyShowing = MutableStateFlow(false)

    /** Wall-clock millis of the newest alerting notification, or null if none since start. */
    val lastAlertAt: StateFlow<Long?> = _lastAlertAt.asStateFlow()

    /**
     * Whether a notification that counts (see [counts]) is still in the shade, i.e. not yet
     * dismissed. For the battery-saver pulse's "repeat while showing".
     */
    val anyShowing: StateFlow<Boolean> = _anyShowing.asStateFlow()

    /**
     * Stamp an alert at [atMillis], keeping the newest. Times ahead of [now] are clamped, and a
     * stored time that is now in the future (the clock was set back) gives way to the new one;
     * otherwise it would hold off every pulse until the clock caught up.
     */
    fun record(atMillis: Long, now: Long = System.currentTimeMillis()) {
        val at = minOf(atMillis, now)
        _lastAlertAt.update { previous -> if (previous == null || previous > now || at > previous) at else previous }
    }

    /** Note that the notification [key] is showing, and whether it [counts]. */
    fun showing(key: String, counts: Boolean) = synchronized(showingKeys) {
        if (counts) showingKeys += key else showingKeys -= key
        _anyShowing.value = showingKeys.isNotEmpty()
    }

    /** Note that the notification [key] was dismissed or cancelled. */
    fun removed(key: String) = showing(key, counts = false)

    /** Start over from [keys], the ones that count among those showing now (or none if unknown). */
    fun resetShowing(keys: Collection<String>) = synchronized(showingKeys) {
        showingKeys.clear()
        showingKeys += keys
        _anyShowing.value = showingKeys.isNotEmpty()
    }

    /** Forget the last alert and what is showing. For tests, which share this process-wide state. */
    internal fun clear() {
        _lastAlertAt.value = null
        resetShowing(emptyList())
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

    /**
     * Whether a notification in the shade keeps "repeat while showing" going: one that would
     * alert if it were new, i.e. not ours, not ongoing, not silent and not held back by Do Not
     * Disturb.
     */
    fun counts(ownPackage: Boolean, ongoing: Boolean, importance: Int, passesDnd: Boolean): Boolean =
        isAlerting(ownPackage, ongoing, importance, passesDnd, onlyAlertOnce = false, alreadyShowing = false)
}
