package io.github.hassanm0301.nothingisfree.dotmatrix

/**
 * A rectangular grid of brightness values, 0..255, row-major.
 *
 * This is the shared model for everything dot-matrix in the app: the on-screen clock, future
 * widgets, and the Glyph Matrix on Phone (3) / Phone (4a) Pro, whose SDK takes an `int[]` frame
 * of exactly this layout (see [toFrame]).
 */
class DotMatrix(val width: Int, val height: Int) {
    private val cells = IntArray(width * height)

    operator fun get(x: Int, y: Int): Int =
        if (x in 0 until width && y in 0 until height) cells[y * width + x] else 0

    operator fun set(x: Int, y: Int, value: Int) {
        if (x in 0 until width && y in 0 until height) cells[y * width + x] = value.coerceIn(0, 255)
    }

    fun clear() = cells.fill(0)

    fun forEach(block: (x: Int, y: Int, value: Int) -> Unit) {
        for (y in 0 until height) for (x in 0 until width) block(x, y, cells[y * width + x])
    }

    /** Copy of the raw frame, the layout `GlyphMatrixManager.setMatrixFrame(int[])` expects. */
    fun toFrame(): IntArray = cells.copyOf()

    /** Stamp [glyph] with its top-left at ([x], [y]). */
    fun stamp(glyph: DotGlyph, x: Int, y: Int, brightness: Int = 255) {
        for (gy in 0 until glyph.height) for (gx in 0 until glyph.width) {
            if (glyph[gx, gy]) this[x + gx, y + gy] = brightness
        }
    }

    /** Draw a string in [DotFont], left-to-right, with a one-column gap between glyphs. */
    fun text(text: String, x: Int = 0, y: Int = 0, brightness: Int = 255, gap: Int = 1): Int {
        var cursor = x
        for (ch in text) {
            val glyph = DotFont[ch]
            stamp(glyph, cursor, y, brightness)
            cursor += glyph.width + gap
        }
        return cursor - gap - x
    }

    companion object {
        /** Width in dots that [text] will occupy. */
        fun measure(text: String, gap: Int = 1): Int =
            text.sumOf { DotFont[it].width + gap } - gap
    }
}

/** A fixed bitmap glyph. Rows are strings of `#` (on) and `.` (off). */
class DotGlyph(rows: List<String>) {
    val height = rows.size
    val width = rows.maxOf { it.length }
    private val bits = BooleanArray(width * height).also { arr ->
        rows.forEachIndexed { y, row -> row.forEachIndexed { x, c -> arr[y * width + x] = c == '#' } }
    }

    operator fun get(x: Int, y: Int): Boolean = bits[y * width + x]
}
