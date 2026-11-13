package io.github.hassanm0301.nothingisfree.glyph.toy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The "Analog clock" Glyph Toy: a short hour hand and a long minute hand (see [AnalogClockFace]),
 * drawn with the options in [AnalogClockPreferences] and redrawn as soon as they change.
 */
class AnalogClockToyService : GlyphToyService() {

    override fun faces(size: Int): Flow<ToyFace> =
        AnalogClockPreferences.settings.map { AnalogClockFace(size, it) }
}
