package io.github.hassanm0301.nothingisfree.glyph.toy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The "Compact digital clock" Glyph Toy: the hour's first digit beside the minute's second digit
 * (see [CompactClockFace]).
 */
class CompactClockToyService : GlyphToyService() {

    override fun faces(size: Int): Flow<ToyFace> = flowOf(CompactClockFace(size))
}
