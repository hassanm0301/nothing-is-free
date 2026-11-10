package io.github.hassanm0301.nothingisfree.glyph.toy

import io.github.hassanm0301.nothingisfree.dotmatrix.DotFont
import io.github.hassanm0301.nothingisfree.dotmatrix.DotTypeface

/**
 * The digit fonts the Compact digital clock can draw with, in the order the options list them.
 * Persisted by [name], so don't rename entries; add new fonts at the end.
 */
enum class CompactClockFont(val label: String, val description: String) {
    /** The 5x7 [DotFont], each dot a 2x2 block on a 25x25 matrix: the digits fill the matrix. */
    LARGE("Large", "5x7 digits that fill the matrix"),

    /** The Digital clock's [ClockFont.STANDARD], at the same size: the most room for addons. */
    STANDARD("Standard", "The Digital clock's digits, just as small: 3x5, 5x7 on a 25x25 matrix"),

    /** The Digital clock's [ClockFont.MINIMAL], at the same size. */
    MINIMAL("Minimal", "The fewest LEDs: small digits with open corners and a one-column 1"),
    ;

    /** The typeface for a [size]x[size] matrix. */
    fun typeface(size: Int): DotTypeface = when (this) {
        LARGE -> DotFont
        STANDARD -> ClockFont.STANDARD.typeface(size)
        MINIMAL -> ClockFont.MINIMAL.typeface(size)
    }

    /** How many LEDs wide and tall each dot of the [typeface] is drawn on a [size]x[size] matrix. */
    fun scale(size: Int): Int = if (this == LARGE && size >= 21) 2 else 1
}
