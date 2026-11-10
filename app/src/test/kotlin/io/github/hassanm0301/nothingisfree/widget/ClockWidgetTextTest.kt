package io.github.hassanm0301.nothingisfree.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

class ClockWidgetTextTest {
    private val now = LocalDateTime.of(2026, 11, 10, 18, 55, 40)
    private val alarm = LocalDateTime.of(2026, 11, 11, 8, 30)

    @Test
    fun `writes the day and date, the time and the next alarm`() {
        val lines = ClockWidgetText.lines(now, alarm, is24Hour = true, locale = Locale.UK)
        assertEquals("Tue 10 Nov", lines.date)
        assertEquals("18:55", lines.time)
        assertEquals("Wed 08:30", lines.alarm)
    }

    @Test
    fun `has no alarm line without an alarm`() {
        val lines = ClockWidgetText.lines(now, null, is24Hour = true, locale = Locale.UK)
        assertNull(lines.alarm)
        assertEquals("Tue 10 Nov, 18:55", lines.spoken)
    }

    @Test
    fun `the 12 hour clock drops the leading zero and marks only the alarm am or pm`() {
        val lines = ClockWidgetText.lines(now, alarm, is24Hour = false, locale = Locale.UK)
        assertEquals("6:55", lines.time)
        assertEquals("Wed 8:30 am", lines.alarm)
    }

    @Test
    fun `midnight is 00 on the 24 hour clock and 12 on the 12 hour one`() {
        val midnight = LocalDateTime.of(2026, 1, 1, 0, 5)
        assertEquals("00:05", ClockWidgetText.lines(midnight, null, true, Locale.UK).time)
        assertEquals("12:05", ClockWidgetText.lines(midnight, null, false, Locale.UK).time)
    }

    @Test
    fun `says the lines in order for screen readers`() {
        val lines = ClockWidgetText.lines(now, alarm, is24Hour = true, locale = Locale.UK)
        assertEquals("Tue 10 Nov, 18:55, Alarm Wed 08:30", lines.spoken)
    }

    @Test
    fun `a shade is an opaque grey from black to white`() {
        assertEquals(0xFF000000.toInt(), ClockWidgetText.grey(0))
        assertEquals(0xFF808080.toInt(), ClockWidgetText.grey(50))
        assertEquals(0xFFFFFFFF.toInt(), ClockWidgetText.grey(100))
        // Out of range is the nearest end.
        assertEquals(0xFF000000.toInt(), ClockWidgetText.grey(-5))
        assertEquals(0xFFFFFFFF.toInt(), ClockWidgetText.grey(140))
    }

    @Test
    fun `the next redraw is the start of the next minute`() {
        assertEquals(120_000L, ClockWidgetText.nextMinute(60_001L))
        assertEquals(120_000L, ClockWidgetText.nextMinute(119_999L))
        // On the minute, the one after: this minute is already drawn.
        assertEquals(120_000L, ClockWidgetText.nextMinute(60_000L))
    }
}
