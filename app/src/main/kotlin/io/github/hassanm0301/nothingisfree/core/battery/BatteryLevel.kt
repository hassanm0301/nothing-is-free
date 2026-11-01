package io.github.hassanm0301.nothingisfree.core.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The phone's battery level, 0..100, from the system's sticky `ACTION_BATTERY_CHANGED`
 * broadcast. Battery broadcasts only come from the system, so the receivers here take no export
 * flag (Android 14 asks for none in that case).
 */
object BatteryLevel {

    /**
     * The level now, or null if the system doesn't say. Reads the last broadcast without
     * registering a receiver, so it is cheap enough for every minute's repaint.
     */
    fun read(context: Context): Int? =
        percent(context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))

    /**
     * The level as it changes, starting with the current one, for the app's own screens. The
     * receiver stays registered while this is collected; the toys use [read] instead.
     */
    fun changes(context: Context): Flow<Int> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                percent(intent)?.let { trySend(it) }
            }
        }
        // Sticky, so the current level arrives straight away as well.
        percent(context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))?.let { trySend(it) }
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    /** [level] out of [scale] as a whole percentage, rounded down; null if unknown. */
    fun percent(level: Int, scale: Int): Int? =
        if (level < 0 || scale <= 0) null else (level * 100 / scale).coerceIn(0, 100)

    private fun percent(intent: Intent?): Int? = intent?.let {
        percent(it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), it.getIntExtra(BatteryManager.EXTRA_SCALE, -1))
    }
}
