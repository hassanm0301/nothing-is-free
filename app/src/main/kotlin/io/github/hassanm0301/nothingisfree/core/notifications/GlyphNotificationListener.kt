package io.github.hassanm0301.nothingisfree.core.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat

/**
 * Watches posted notifications and stamps [RecentNotifications] with the alerting ones, and with
 * which ones are still showing. Needs notification access, granted by the user in system settings
 * (see [accessSettingsIntent]).
 */
class GlyphNotificationListener : NotificationListenerService() {

    /** Keys currently showing, so quiet updates to them can be told apart from new arrivals. */
    private val showing = mutableSetOf<String>()

    override fun onListenerConnected() {
        showing.clear()
        val active = activeNotifications.orEmpty()
        active.mapTo(showing) { it.key }
        val ranking = currentRanking
        RecentNotifications.resetShowing(active.filter { counts(it, rankingOf(it, ranking)) }.map { it.key })
    }

    override fun onListenerDisconnected() {
        showing.clear()
        RecentNotifications.resetShowing(emptyList())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        val ranking = rankingOf(sbn, rankingMap)
        val alerting = RecentNotifications.isAlerting(
            ownPackage = sbn.packageName == packageName,
            ongoing = sbn.isOngoing,
            importance = ranking?.importance ?: NotificationManager.IMPORTANCE_DEFAULT,
            passesDnd = ranking?.matchesInterruptionFilter() ?: true,
            onlyAlertOnce = sbn.notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0,
            alreadyShowing = sbn.key in showing,
        )
        showing += sbn.key
        RecentNotifications.showing(sbn.key, counts(sbn, ranking))
        if (alerting) RecentNotifications.record(sbn.postTime)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        showing -= sbn.key
        RecentNotifications.removed(sbn.key)
    }

    /** Without a ranking, give the notification the benefit of the doubt. */
    private fun rankingOf(sbn: StatusBarNotification, rankingMap: RankingMap?): Ranking? =
        rankingMap?.let { map -> Ranking().takeIf { map.getRanking(sbn.key, it) } }

    private fun counts(sbn: StatusBarNotification, ranking: Ranking?): Boolean = RecentNotifications.counts(
        ownPackage = sbn.packageName == packageName,
        ongoing = sbn.isOngoing,
        importance = ranking?.importance ?: NotificationManager.IMPORTANCE_DEFAULT,
        passesDnd = ranking?.matchesInterruptionFilter() ?: true,
    )

    companion object {
        fun hasAccess(context: Context): Boolean =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

        /** Opens this listener's own notification-access toggle. */
        fun accessSettingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(context, GlyphNotificationListener::class.java).flattenToString(),
            )
    }
}
