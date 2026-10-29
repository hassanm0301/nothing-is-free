package io.github.hassanm0301.nothingisfree.glyph.toy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The "Binary clock" Glyph Toy: hours and minutes in binary-coded decimal (see [BinaryClockFace]),
 * drawn with the options in [BinaryClockPreferences] and redrawn as soon as they change.
 */
class BinaryClockToyService : GlyphToyService() {

    override val toyId = ToyId.BINARY_CLOCK

    override fun faces(size: Int): Flow<ToyFace> =
        BinaryClockPreferences.settings.map { toyId.face(size, it) }
}
