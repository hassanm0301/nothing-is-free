package io.github.hassanm0301.nothingisfree.glyph

import android.content.Context
import io.github.hassanm0301.nothingisfree.core.device.NothingDevice
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix

/**
 * The app-facing Glyph API. The UI and future widgets talk to this, never to the SDK directly,
 * so non-Nothing devices and the pre-SDK build get the same code path with a no-op behind it.
 *
 * Implementation plan (docs/GLYPH.md): a `GlyphMatrixController` wrapping `GlyphMatrixManager`
 * (init -> register(deviceId) -> setAppMatrixFrame(frame) -> closeAppMatrix/unInit), plus a
 * `GlyphToy` service for the AOD toy on the Phone (4a) Pro.
 */
interface GlyphController {
    val capability: GlyphCapability

    /** Bind to the Glyph service. Safe to call repeatedly. */
    fun connect()

    /** Push a full frame. [frame] must be `capability.width` square for [GlyphCapability.Matrix]. */
    fun show(frame: DotMatrix)

    /** Turn everything off and release the matrix back to the system. */
    fun clear()

    /** Unbind from the Glyph service. */
    fun disconnect()
}

/** Used everywhere until the SDK is wired in, and permanently on devices without Glyph. */
class NoOpGlyphController(override val capability: GlyphCapability) : GlyphController {
    override fun connect() = Unit
    override fun show(frame: DotMatrix) = Unit
    override fun clear() = Unit
    override fun disconnect() = Unit
}

object GlyphModule {
    /**
     * TODO(glyph): return a real controller when `capability` is [GlyphCapability.Matrix] and the
     * SDK AAR is present. Keep the no-op for everything else.
     */
    fun controller(@Suppress("UNUSED_PARAMETER") context: Context): GlyphController =
        NoOpGlyphController(GlyphCapability.of(NothingDevice.detectedModel))
}
