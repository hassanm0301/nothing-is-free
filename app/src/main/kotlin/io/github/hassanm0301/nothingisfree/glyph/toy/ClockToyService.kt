package io.github.hassanm0301.nothingisfree.glyph.toy

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import android.os.PowerManager
import android.text.format.DateFormat
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy
import io.github.hassanm0301.nothingisfree.core.notifications.RecentNotifications
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The "Pulse clock" Glyph Toy: a digital clock whose outer ring breathes for a while after a
 * notification. The Glyph service binds this when the user picks the toy (on the Phone (4a) Pro:
 * Settings > Glyph Interface > Flip to Glyph > Always-on Glyph Toy) and unbinds it when they leave.
 */
class ClockToyService : Service() {

    private val scope = MainScope()
    private var manager: GlyphMatrixManager? = null
    private var toy: ClockToy? = null
    private var jobs: List<Job> = emptyList()

    /** Keeps the CPU up while the ring pulses, so the animation runs with the screen off. */
    private val wakeLock: PowerManager.WakeLock by lazy {
        getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NothingIsFree:ClockToyPulse")
            .apply { setReferenceCounted(false) }
    }

    /** The Glyph service sends toy events here; on the (4a) Pro, [GlyphToy.EVENT_AOD] arrives each minute. */
    private val messenger = Messenger(
        Handler(Looper.getMainLooper()) { msg ->
            if (msg.what == GlyphToy.MSG_GLYPH_TOY &&
                msg.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA) == GlyphToy.EVENT_AOD
            ) {
                toy?.refresh()
            }
            true
        },
    )

    override fun onBind(intent: Intent): IBinder {
        val gm = GlyphMatrixManager.getInstance(applicationContext)
        manager = gm
        gm.init(
            object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName) {
                    val device = if (Common.is25111p()) Glyph.DEVICE_25111p else Glyph.DEVICE_23112
                    if (!gm.register(device)) Log.w(TAG, "Glyph service did not authorise $device")
                    start(gm)
                }

                override fun onServiceDisconnected(name: ComponentName) = stop()
            },
        )
        return messenger.binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        stop()
        manager?.turnOff()
        manager?.unInit()
        manager = null
        return false
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun start(gm: GlyphMatrixManager) {
        stop()
        val size = Common.getDeviceMatrixLength().takeIf { it > 0 } ?: DEFAULT_SIZE
        val clockToy = ClockToy(
            face = ClockFace(size),
            show = { frame ->
                try {
                    gm.setMatrixFrame(frame.toFrame(max = GLYPH_MAX_BRIGHTNESS))
                } catch (e: GlyphException) {
                    Log.w(TAG, "Frame dropped", e)
                }
            },
            is24Hour = { DateFormat.is24HourFormat(this) },
        )
        toy = clockToy
        val pulses = RecentNotifications.lastAlertAt
        val window = ClockToyPreferences.pulseWindowSeconds
        jobs = listOf(
            scope.launch { clockToy.run(pulses, window) },
            scope.launch {
                combine(pulses, window) { at, seconds -> at?.plus(PulseWave.duration(seconds * 1_000L)) }
                    .collect { until ->
                        val remaining = (until ?: 0L) - System.currentTimeMillis()
                        if (remaining > 0) wakeLock.acquire(remaining)
                    }
            },
        )
    }

    private fun stop() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        toy = null
        if (wakeLock.isHeld) wakeLock.release()
    }

    private companion object {
        const val TAG = "ClockToyService"
        const val DEFAULT_SIZE = 13

        /** Hardware brightness ceiling; the SDK scales its own 0..255 inputs up to this. */
        const val GLYPH_MAX_BRIGHTNESS = 4095
    }
}
