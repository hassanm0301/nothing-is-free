package io.github.hassanm0301.nothingisfree.glyph.toy

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * Base for this app's Glyph Toys. The Glyph service binds a toy when the user picks it and unbinds
 * it when they leave; in between this holds the [GlyphMatrixManager] and pushes frames to it.
 * Subclasses only draw: [onToyStart] once the matrix is ready, [onAod] on each AOD tick.
 */
abstract class GlyphToyService : Service() {

    protected val scope = MainScope()
    private val tag = javaClass.simpleName
    private var manager: GlyphMatrixManager? = null
    private var jobs: List<Job> = emptyList()

    /** The Glyph service sends toy events here; on the (4a) Pro, [GlyphToy.EVENT_AOD] arrives each minute. */
    private val messenger = Messenger(
        Handler(Looper.getMainLooper()) { msg ->
            if (msg.what == GlyphToy.MSG_GLYPH_TOY &&
                msg.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA) == GlyphToy.EVENT_AOD
            ) {
                onAod()
            }
            true
        },
    )

    /**
     * Start drawing on the [size]x[size] matrix, handing each frame to [show]. Launch work in
     * [scope] and return it; the jobs are cancelled when the toy stops, before [onToyStop].
     */
    protected abstract fun onToyStart(size: Int, show: (DotMatrix) -> Unit): List<Job>

    /** The AOD tick. Repaint now rather than waiting for the toy's own timer. */
    protected open fun onAod() {}

    /** The toy stopped or is about to restart: release anything [onToyStart] took. */
    protected open fun onToyStop() {}

    override fun onBind(intent: Intent): IBinder {
        val gm = GlyphMatrixManager.getInstance(applicationContext)
        manager = gm
        gm.init(
            object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName) {
                    val device = if (Common.is25111p()) Glyph.DEVICE_25111p else Glyph.DEVICE_23112
                    if (!gm.register(device)) Log.w(tag, "Glyph service did not authorise $device")
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
        jobs = onToyStart(size) { frame ->
            try {
                gm.setMatrixFrame(frame.toFrame(max = GLYPH_MAX_BRIGHTNESS))
            } catch (e: GlyphException) {
                Log.w(tag, "Frame dropped", e)
            }
        }
    }

    private fun stop() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        onToyStop()
    }

    private companion object {
        const val DEFAULT_SIZE = 13

        /** Hardware brightness ceiling; the SDK scales its own 0..255 inputs up to this. */
        const val GLYPH_MAX_BRIGHTNESS = 4095
    }
}
