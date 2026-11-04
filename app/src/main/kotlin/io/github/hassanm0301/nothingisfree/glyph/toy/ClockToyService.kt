package io.github.hassanm0301.nothingisfree.glyph.toy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The "Digital clock" Glyph Toy: hours over minutes (see [ClockFace]), in the font chosen in
 * [DigitalClockPreferences] and redrawn as soon as it changes. Named `ClockToyService` because
 * the phone remembers the chosen toy by component name; renaming the class would quietly unselect
 * it.
 */
class ClockToyService : GlyphToyService() {

    override val toyId = ToyId.DIGITAL_CLOCK

    override fun faces(size: Int): Flow<ToyFace> =
        DigitalClockPreferences.settings.map { ClockFace(size, it.font) }
}
