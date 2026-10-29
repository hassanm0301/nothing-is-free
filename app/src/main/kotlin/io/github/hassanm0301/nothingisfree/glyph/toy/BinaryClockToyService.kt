package io.github.hassanm0301.nothingisfree.glyph.toy

import android.text.format.DateFormat
import io.github.hassanm0301.nothingisfree.dotmatrix.DotMatrix
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The "Binary clock" Glyph Toy: hours and minutes in binary-coded decimal, drawn with the options
 * in [BinaryClockPreferences]. Picked the same way as the Pulse clock. No wake lock: the frame
 * only changes once a minute, and on the (4a) Pro the AOD tick arrives each minute anyway.
 */
class BinaryClockToyService : GlyphToyService() {

    private var toy: BinaryClockToy? = null

    override fun onToyStart(size: Int, show: (DotMatrix) -> Unit): List<Job> {
        val binaryToy = BinaryClockToy(
            face = BinaryClockFace(size),
            show = show,
            is24Hour = { DateFormat.is24HourFormat(this) },
        )
        toy = binaryToy
        return listOf(scope.launch { binaryToy.run(BinaryClockPreferences.settings) })
    }

    override fun onAod() {
        toy?.refresh()
    }

    override fun onToyStop() {
        toy = null
    }
}
