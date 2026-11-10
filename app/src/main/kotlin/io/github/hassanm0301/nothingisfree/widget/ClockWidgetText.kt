package io.github.hassanm0301.nothingisfree.widget

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the clock widget writes: the day and date, the time, and the next alarm if one is set. */
data class ClockWidgetLines(val date: String, val time: String, val alarm: String?) {
    /** The lines as one sentence, for screen readers. */
    val spoken: String get() = listOfNotNull(date, time, alarm?.let { "Alarm $it" }).joinToString(", ")
}

object ClockWidgetText {
    /**
     * The widget's lines at [now]: "Tue 10 Nov", "18:55" and, with a [nextAlarm], "Wed 08:30".
     * Without [is24Hour] the time is "6:55" and the alarm carries am/pm, the one place it matters.
     */
    fun lines(now: LocalDateTime, nextAlarm: LocalDateTime?, is24Hour: Boolean, locale: Locale): ClockWidgetLines {
        fun format(pattern: String, at: LocalDateTime) = DateTimeFormatter.ofPattern(pattern, locale).format(at)
        return ClockWidgetLines(
            date = format("EEE d MMM", now),
            time = format(if (is24Hour) "HH:mm" else "h:mm", now),
            alarm = nextAlarm?.let { format(if (is24Hour) "EEE HH:mm" else "EEE h:mm a", it) },
        )
    }

    /** When the widget next needs redrawing: the start of the minute after [nowMillis]. */
    fun nextMinute(nowMillis: Long): Long = (Math.floorDiv(nowMillis, MINUTE) + 1) * MINUTE

    /** The opaque grey for [shade], 0 (black) to 100 (white), as an ARGB colour. */
    fun grey(shade: Int): Int {
        val level = (shade.coerceIn(0, 100) * 255 + 50) / 100
        return (0xFF shl 24) or (level shl 16) or (level shl 8) or level
    }

    private const val MINUTE = 60_000L
}
