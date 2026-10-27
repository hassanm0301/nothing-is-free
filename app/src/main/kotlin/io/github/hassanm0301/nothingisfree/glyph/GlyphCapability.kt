package io.github.hassanm0301.nothingisfree.glyph

import io.github.hassanm0301.nothingisfree.core.device.NothingModel

/**
 * What the current phone can do with light, as far as the Glyph SDKs expose it.
 *
 * - [Strips]: segmented LED strips (Phone (1), (2), (2a), (3a) family, (4a) bar) driven
 *   by `GlyphManager` from the Glyph Developer Kit.
 * - [Matrix]: a dot-matrix of [width]x[width] LEDs (Phone (3): 25, Phone (4a) Pro: 13)
 *   driven by `GlyphMatrixManager`. On the (4a) Pro only AOD "toys" are supported and
 *   there is no Glyph Touch.
 */
sealed interface GlyphCapability {
    data object None : GlyphCapability

    data class Strips(val deviceId: String) : GlyphCapability

    data class Matrix(
        val deviceId: String,
        val width: Int,
        val aodOnly: Boolean,
        val touch: Boolean,
    ) : GlyphCapability

    companion object {
        fun of(model: NothingModel): GlyphCapability = when (model) {
            NothingModel.PHONE_3 -> Matrix(deviceId = "23112", width = 25, aodOnly = false, touch = true)
            NothingModel.PHONE_4A_PRO -> Matrix(deviceId = "25111p", width = 13, aodOnly = true, touch = false)
            NothingModel.PHONE_1,
            NothingModel.PHONE_2,
            NothingModel.PHONE_2A,
            NothingModel.PHONE_2A_PLUS,
            NothingModel.PHONE_3A,
            NothingModel.PHONE_4A,
            NothingModel.PHONE_4B -> Strips(deviceId = model.glyphDeviceId!!)
            NothingModel.UNKNOWN_NOTHING,
            NothingModel.NOT_NOTHING -> None
        }
    }
}
