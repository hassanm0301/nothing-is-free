package io.github.hassanm0301.nothingisfree.glyph.toy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The "Compact digital clock" Glyph Toy: the hour's second digit beside the minute's first digit
 * (see [CompactClockFace]), drawn with the options in [CompactClockPreferences] and redrawn as
 * soon as they change.
 */
class CompactClockToyService : GlyphToyService() {

    override fun faces(size: Int): Flow<ToyFace> =
        CompactClockPreferences.settings.map { CompactClockFace(size, it) }
}
