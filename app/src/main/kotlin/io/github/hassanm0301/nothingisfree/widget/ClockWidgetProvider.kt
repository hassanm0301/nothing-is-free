package io.github.hassanm0301.nothingisfree.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Receives everything that changes what the clock widget shows: the launcher placing or resizing
 * it, the minute tick, and the system's word that the next alarm, the time, the time zone or the
 * language changed (see the manifest). Each one redraws; [ClockWidget.update] does the work.
 */
class ClockWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // Covered by onUpdate, onAppWidgetOptionsChanged and onDisabled below.
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_DELETED,
            AppWidgetManager.ACTION_APPWIDGET_ENABLED,
            AppWidgetManager.ACTION_APPWIDGET_DISABLED,
            AppWidgetManager.ACTION_APPWIDGET_RESTORED,
            -> super.onReceive(context, intent)

            else -> ClockWidget.update(context)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ClockWidget.update(context)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) =
        ClockWidget.update(context)

    override fun onDisabled(context: Context) = ClockWidget.cancelTick(context)
}
