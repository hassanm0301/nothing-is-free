package io.github.hassanm0301.nothingisfree.core.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The phone's battery as the addons show it.
 *
 * @property percent the charge, 0..100.
 * @property charging plugged in and charging, or full on the charger.
 * @property powerSave battery saver is on.
 */
data class BatteryState(val percent: Int, val charging: Boolean = false, val powerSave: Boolean = false)

/**
 * The phone's battery: level and charging from the system's sticky `ACTION_BATTERY_CHANGED`
 * broadcast, battery saver from [PowerManager].
 */
object BatteryLevel {

    /**
     * The battery now, or null if the system doesn't say. Reads the last broadcast without
     * registering a receiver, so it is cheap enough for every minute's repaint.
     */
    fun read(context: Context): BatteryState? =
        state(context, context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))

    /**
     * The battery as it changes, starting with now, for the app's own screens. The receiver stays
     * registered while this is collected; the toys use [read] instead.
     */
    fun changes(context: Context): Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                read(context)?.let { trySend(it) }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        // Both are system broadcasts. Exported so every sender of them reaches it; it re-reads
        // the state rather than trusting the intent, so a stray broadcast changes nothing.
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        // The battery broadcast is sticky, so it also arrives straight away; this covers a
        // missing one.
        read(context)?.let { trySend(it) }
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    /** [level] out of [scale] as a whole percentage, rounded down; null if unknown. */
    fun percent(level: Int, scale: Int): Int? =
        if (level < 0 || scale <= 0) null else (level * 100 / scale).coerceIn(0, 100)

    /** Whether a battery [status] counts as charging: charging, or full on the charger. */
    fun charging(status: Int): Boolean =
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

    private fun state(context: Context, battery: Intent?): BatteryState? {
        battery ?: return null
        val percent = percent(
            battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
            battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
        ) ?: return null
        return BatteryState(
            percent = percent,
            charging = charging(battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)),
            powerSave = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true,
        )
    }
}
