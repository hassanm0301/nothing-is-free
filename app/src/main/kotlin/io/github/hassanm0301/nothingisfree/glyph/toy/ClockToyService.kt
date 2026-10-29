package io.github.hassanm0301.nothingisfree.glyph.toy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The "Digital clock" Glyph Toy: hours over minutes (see [ClockFace]). Named `ClockToyService`
 * because the phone remembers the chosen toy by component name; renaming the class would quietly
 * unselect it.
 */
class ClockToyService : GlyphToyService() {

    override val toyId = ToyId.DIGITAL_CLOCK

    override fun faces(size: Int): Flow<ToyFace> = flowOf(ClockFace(size))
}
