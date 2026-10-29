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
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonId
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonPreferences
import io.github.hassanm0301.nothingisfree.glyph.addon.PulseWave
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Base for this app's Glyph Toys. The Glyph service binds a toy when the user picks it (on the
 * Phone (4a) Pro: Settings > Glyph Interface > Flip to Glyph > Always-on Glyph Toy) and unbinds it
 * when they leave; in between the toy draws on the [GlyphMatrixManager], which every toy in the
 * process shares through [arbiter].
 *
 * A toy only names itself ([toyId]) and its face ([faces]). The base runs the [ToyLoop], repaints
 * on each AOD tick, and adds the addons switched on for the toy in [AddonPreferences].
 */
abstract class GlyphToyService : Service() {

    /** Which toy this is: picks its addons in [AddonPreferences]. */
    abstract val toyId: ToyId

    /** The toy's face on a [size]x[size] matrix, emitted again whenever its options change. */
    protected abstract fun faces(size: Int): Flow<ToyFace>

    private val scope = MainScope()
    private val tag = javaClass.simpleName
    private var jobs: List<Job> = emptyList()
    private var loop: ToyLoop? = null

    /** Keeps the CPU up while the notification pulse runs, so it animates with the screen off. */
    private val wakeLock: PowerManager.WakeLock by lazy {
        getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NothingIsFree:NotificationPulse")
            .apply { setReferenceCounted(false) }
    }

    private val client = object : MatrixArbiter.Client<GlyphMatrixManager> {
        override fun start(matrix: GlyphMatrixManager) = this@GlyphToyService.start(matrix)
        override fun stop() = this@GlyphToyService.stop()
    }

    /** The Glyph service sends toy events here; on the (4a) Pro, [GlyphToy.EVENT_AOD] arrives each minute. */
    private val messenger = Messenger(
        Handler(Looper.getMainLooper()) { msg ->
            if (msg.what == GlyphToy.MSG_GLYPH_TOY &&
                msg.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA) == GlyphToy.EVENT_AOD
            ) {
                loop?.refresh()
            }
            true
        },
    )

    override fun onBind(intent: Intent): IBinder {
        arbiter(this).acquire(client)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        arbiter(this).release(client)
        stop()
        return false
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun start(gm: GlyphMatrixManager) {
        stop()
        val size = Common.getDeviceMatrixLength().takeIf { it > 0 } ?: DEFAULT_SIZE
        val toyLoop = ToyLoop(
            show = { frame ->
                try {
                    gm.setMatrixFrame(frame.toFrame(max = GLYPH_MAX_BRIGHTNESS))
                } catch (e: GlyphException) {
                    Log.w(tag, "Frame dropped", e)
                }
            },
            is24Hour = { DateFormat.is24HourFormat(this) },
        )
        loop = toyLoop
        val pulseOn = AddonPreferences.enabledFlow(AddonId.NOTIFICATION_PULSE, toyId)
        val alerts = RecentNotifications.lastAlertAt
        val window = AddonPreferences.pulseWindowSeconds
        jobs = listOf(
            scope.launch { toyLoop.run(faces(size), pulseOn, alerts, window) },
            scope.launch {
                // Held for what is left of a running pulse, and let go early if it is switched off.
                // A pulse that hasn't started (an alert time ahead of a clock set back) holds nothing.
                combine(pulseOn, alerts, window) { on, at, seconds -> Triple(on, at, seconds * 1_000L) }
                    .collect { (on, at, windowMs) ->
                        val elapsed = PulseWave.elapsed(at?.takeIf { on }, windowMs, System.currentTimeMillis())
                        if (elapsed != null) wakeLock.acquire(PulseWave.duration(windowMs) - elapsed) else releaseWakeLock()
                    }
            },
        )
    }

    private fun stop() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        loop = null
        releaseWakeLock()
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) wakeLock.release()
    }

    private companion object {
        const val TAG = "GlyphToyService"
        const val DEFAULT_SIZE = 13

        /** Hardware brightness ceiling; the SDK scales its own 0..255 inputs up to this. */
        const val GLYPH_MAX_BRIGHTNESS = 4095

        private var shared: MatrixArbiter<GlyphMatrixManager>? = null

        /** The process-wide matrix connection: opened for the first bound toy, closed after the last. */
        fun arbiter(service: Service): MatrixArbiter<GlyphMatrixManager> = shared ?: run {
            val gm = GlyphMatrixManager.getInstance(service.applicationContext)
            MatrixArbiter<GlyphMatrixManager>(
                open = { onConnected, onDisconnected ->
                    gm.init(
                        object : GlyphMatrixManager.Callback {
                            override fun onServiceConnected(name: ComponentName) {
                                val device = if (Common.is25111p()) Glyph.DEVICE_25111p else Glyph.DEVICE_23112
                                if (!gm.register(device)) Log.w(TAG, "Glyph service did not authorise $device")
                                onConnected(gm)
                            }

                            override fun onServiceDisconnected(name: ComponentName) = onDisconnected()
                        },
                    )
                },
                close = {
                    gm.turnOff()
                    // A connection the system already dropped is no longer registered; unbinding it throws.
                    runCatching { gm.unInit() }.onFailure { Log.w(TAG, "unInit failed", it) }
                },
            ).also { shared = it }
        }
    }
}
