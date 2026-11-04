package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotFont
import io.github.hassanm0301.nothingisfree.dotmatrix.DotFontMinimal
import io.github.hassanm0301.nothingisfree.dotmatrix.DotFontSmall
import io.github.hassanm0301.nothingisfree.dotmatrix.DotTypeface

/**
 * The digit fonts the Digital clock can draw with, in the order the options list them.
 * Persisted by [name], so don't rename entries; add new fonts at the end.
 */
enum class ClockFont(val label: String, val description: String) {
    /** Full 3x5 digits on the 13x13 matrix, 5x7 on a 25x25. */
    STANDARD("Standard", "Full 3x5 digits, 5x7 on a 25x25 matrix"),

    /** [DotFontMinimal] at every size: the fewest LEDs, open corners and a narrow `1`. */
    MINIMAL("Minimal", "The fewest LEDs: open corners and a one-column 1"),
    ;

    /** The typeface for a [size]x[size] matrix. */
    fun typeface(size: Int): DotTypeface = when (this) {
        STANDARD -> if (size >= 21) DotFont else DotFontSmall
        MINIMAL -> DotFontMinimal
    }
}
