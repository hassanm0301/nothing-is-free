package io.github.hassanm0301.nothingisfree.core.notifications

import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.NotificationManager.IMPORTANCE_LOW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecentNotificationsTest {

    @Before
    fun reset() = RecentNotifications.clear()


    private fun alerting(
        ownPackage: Boolean = false,
        ongoing: Boolean = false,
        importance: Int = IMPORTANCE_DEFAULT,
        passesDnd: Boolean = true,
        onlyAlertOnce: Boolean = false,
        alreadyShowing: Boolean = false,
    ) = RecentNotifications.isAlerting(ownPackage, ongoing, importance, passesDnd, onlyAlertOnce, alreadyShowing)

    @Test
    fun `a normal new notification counts`() {
        assertTrue(alerting())
        assertTrue(alerting(importance = IMPORTANCE_HIGH))
    }

    @Test
    fun `quiet or ongoing ones do not`() {
        assertFalse(alerting(ownPackage = true))
        assertFalse(alerting(ongoing = true))
        assertFalse(alerting(importance = IMPORTANCE_LOW))
        assertFalse(alerting(passesDnd = false))
    }

    @Test
    fun `only-alert-once counts the first time, not on updates`() {
        assertTrue(alerting(onlyAlertOnce = true, alreadyShowing = false))
        assertFalse(alerting(onlyAlertOnce = true, alreadyShowing = true))
        assertTrue(alerting(onlyAlertOnce = false, alreadyShowing = true))
    }

    @Test
    fun `record keeps the newest time`() {
        RecentNotifications.record(5_000)
        RecentNotifications.record(4_000)
        assertEquals(5_000L, RecentNotifications.lastAlertAt.value)
        RecentNotifications.record(6_000)
        assertEquals(6_000L, RecentNotifications.lastAlertAt.value)
    }

    @Test
    fun `a time left in the future by a clock set back gives way to the next alert`() {
        RecentNotifications.record(100_000, now = 100_000)
        // The clock goes back an hour; the next notification must still count.
        RecentNotifications.record(40_000, now = 40_000)
        assertEquals(40_000L, RecentNotifications.lastAlertAt.value)
        // And a post time ahead of the clock is clamped to now.
        RecentNotifications.record(90_000, now = 50_000)
        assertEquals(50_000L, RecentNotifications.lastAlertAt.value)
    }
}
