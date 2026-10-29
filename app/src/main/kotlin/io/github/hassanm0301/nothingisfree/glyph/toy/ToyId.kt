package io.github.hassanm0301.nothingisfree.glyph.toy

/**
 * This app's Glyph Toys, in the order the Glyph tab lists them. Persisted by [name] (addon
 * settings), so don't rename entries.
 */
enum class ToyId(val title: String, val summary: String) {
    DIGITAL_CLOCK("Digital clock", "Hours over minutes in a 3x5 dot font"),
    BINARY_CLOCK("Binary clock", "Hours and minutes in binary-coded decimal"),
    ;

    /** The face on a [size]x[size] matrix, with the toy's current options. */
    fun face(size: Int, binary: BinaryClockSettings): ToyFace = when (this) {
        DIGITAL_CLOCK -> ClockFace(size)
        BINARY_CLOCK -> BinaryClockFace(size).with(binary)
    }
}
