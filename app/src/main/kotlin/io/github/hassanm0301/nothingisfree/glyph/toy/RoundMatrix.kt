package io.github.hassanm0301.nothingisfree.glyph.toy

/** The LED layout of a round Glyph Matrix, shared by every toy face. */
object RoundMatrix {
    /** Whether ([x], [y]) is a physical LED, assuming the matrix is the circle inscribed in the square. */
    fun isLed(size: Int, x: Int, y: Int): Boolean {
        if (x !in 0 until size || y !in 0 until size) return false
        val c = (size - 1) / 2f
        val r = size / 2f
        return (x - c) * (x - c) + (y - c) * (y - c) <= r * r
    }
}
