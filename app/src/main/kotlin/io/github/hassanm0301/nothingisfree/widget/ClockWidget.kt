package io.github.hassanm0301.nothingisfree.widget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import android.widget.RemoteViews
import io.github.hassanm0301.nothingisfree.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * The home-screen clock: the day and date, the time and the next alarm, in a Nothing font.
 *
 * The app draws it ([ClockWidgetRenderer]) and so has to redraw it each minute. That is one alarm
 * a minute that doesn't wake the phone: asleep, nothing runs, and the widget catches up as the
 * phone wakes. The next alarm costs nothing; the system says when it changes.
 */
object ClockWidget {
    /** The minute tick, sent to [ClockWidgetProvider] by the alarm set in [scheduleTick]. */
    const val ACTION_TICK = "io.github.hassanm0301.nothingisfree.widget.CLOCK_TICK"

    /** The size drawn for before the launcher has said how big the widget is, in dp. */
    private const val DEFAULT_WIDTH_DP = 250
    private const val DEFAULT_HEIGHT_DP = 110

    /** Keeps the picture well under what a widget may carry, however large the widget. */
    private const val MAX_WIDTH_PX = 1200
    private const val MAX_HEIGHT_PX = 800

    /** The layout's padding on each side, in dp. */
    private const val PADDING_DP = 4

    /** Whether a launcher can be asked to place the widget. */
    fun canPin(context: Context): Boolean = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /** Ask the launcher to place the widget; it shows its own confirmation. */
    fun requestPin(context: Context) {
        AppWidgetManager.getInstance(context).requestPinAppWidget(provider(context), null, null)
    }

    /** What the widget writes right now. */
    fun lines(context: Context, now: LocalDateTime = LocalDateTime.now()): ClockWidgetLines {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val nextAlarm = alarms.nextAlarmClock?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it.triggerTime), ZoneId.systemDefault())
        }
        return ClockWidgetText.lines(now, nextAlarm, DateFormat.is24HourFormat(context), Locale.getDefault())
    }

    /** Redraw every placed widget and set the next tick; with none placed, stop ticking. */
    fun update(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(provider(context))
        if (ids.isEmpty()) {
            cancelTick(context)
            return
        }
        // First, so a failed draw doesn't stop the clock.
        scheduleTick(context)

        val lines = lines(context)
        val font = ClockWidgetPreferences.font.value
        val density = context.resources.displayMetrics.density
        for (id in ids) {
            // The portrait size: the narrower width and the taller height of the two the launcher gives.
            val options = manager.getAppWidgetOptions(id)
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: DEFAULT_WIDTH_DP
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 } ?: DEFAULT_HEIGHT_DP
            val width = ((widthDp - 2 * PADDING_DP) * density).toInt().coerceIn(1, MAX_WIDTH_PX)
            val height = ((heightDp - 2 * PADDING_DP) * density).toInt().coerceIn(1, MAX_HEIGHT_PX)

            val views = RemoteViews(context.packageName, R.layout.clock_widget)
            views.setImageViewBitmap(R.id.clock_widget_image, ClockWidgetRenderer.render(context, lines, font, width, height))
            views.setContentDescription(R.id.clock_widget_image, lines.spoken)
            views.setOnClickPendingIntent(R.id.clock_widget_root, showAlarms(context))
            manager.updateAppWidget(id, views)
        }
    }

    fun cancelTick(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(tick(context))
    }

    // Lint only knows SCHEDULE_EXACT_ALARM; the manifest's USE_EXACT_ALARM allows setExact too.
    @SuppressLint("MissingPermission")
    private fun scheduleTick(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val at = ClockWidgetText.nextMinute(System.currentTimeMillis())
        // RTC, not RTC_WAKEUP: a tick never wakes the phone, it waits until something else has.
        if (alarms.canScheduleExactAlarms()) {
            alarms.setExact(AlarmManager.RTC, at, tick(context))
        } else {
            // Only without USE_EXACT_ALARM, which the manifest declares: late beats never.
            alarms.set(AlarmManager.RTC, at, tick(context))
        }
    }

    private fun tick(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ClockWidgetProvider::class.java)
            .setAction(ACTION_TICK)
            // Delivered ahead of background broadcasts, so the minute changes on time.
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** A tap opens the clock app's alarms. */
    private fun showAlarms(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun provider(context: Context) = ComponentName(context, ClockWidgetProvider::class.java)
}
