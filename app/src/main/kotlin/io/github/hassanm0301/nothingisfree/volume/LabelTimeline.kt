package io.github.hassanm0301.nothingisfree.volume

/**
 * When the label shows and what it sits next to. The service calls [changed] on each volume
 * change, then [tick] every [TICK_MS] with whether the volume bar is on screen, until a tick
 * returns [Step.HIDE].
 *
 * The label goes with the bar: it shows once the bar does and hides soon after the bar goes. Once
 * the bar has been found, a change without the bar (an app setting the volume quietly) shows
 * nothing. Until then, finding it may simply not work on this phone, so the label shows at a
 * stand-in spot for [FALLBACK_MS] after the last change.
 */
class LabelTimeline {

    enum class Step {
        /** Not showing yet: the bar may still be on its way. */
        WAIT,

        /** Show the label beside the bar, which is on screen now. */
        BESIDE_BAR,

        /** Show the label at the stand-in spot. */
        FALLBACK,

        /** The bar dropped out for a moment; leave the label where it is. */
        KEEP,

        /** Take the label away and stop ticking. */
        HIDE,
    }

    private enum class Mode { HIDDEN, BAR, FALLBACK }

    private var changedAt: Long? = null
    private var mode = Mode.HIDDEN
    private var missed = 0

    /** The bar has been found at least once, so finding it works on this phone. */
    var barSeen: Boolean = false
        private set

    /** The volume changed at [now] (milliseconds, any monotonic clock). */
    fun changed(now: Long) {
        changedAt = now
    }

    /** Ticks happen [TICK_MS] apart at [now]; [barFound] says whether the bar is on screen. */
    fun tick(now: Long, barFound: Boolean): Step {
        val since = now - (changedAt ?: return hide())
        if (since >= MAX_MS) return hide()
        if (barFound) {
            barSeen = true
            mode = Mode.BAR
            missed = 0
            return Step.BESIDE_BAR
        }
        return when (mode) {
            Mode.BAR -> if (++missed >= BAR_GONE_TICKS) hide() else Step.KEEP
            Mode.FALLBACK -> if (since >= FALLBACK_MS) hide() else Step.FALLBACK
            Mode.HIDDEN -> when {
                !barSeen -> {
                    mode = Mode.FALLBACK
                    Step.FALLBACK
                }
                since < WAIT_FOR_BAR_MS -> Step.WAIT
                else -> hide()
            }
        }
    }

    /** Hide now, whatever the state: the screen went off or the service is stopping. */
    fun cancel(): Step = hide()

    private fun hide(): Step {
        changedAt = null
        mode = Mode.HIDDEN
        missed = 0
        return Step.HIDE
    }

    companion object {
        /** How often the service looks for the bar while the label is showing or waiting. */
        const val TICK_MS = 100L

        /** How long after a change the bar may take to appear before the change is ignored. */
        const val WAIT_FOR_BAR_MS = 600L

        /** How long the label stays at the stand-in spot after the last change. */
        const val FALLBACK_MS = 3_000L

        /** Ticks in a row without the bar before the label beside it goes. */
        const val BAR_GONE_TICKS = 2

        /** The label never outstays the last change by more than this, bar or not. */
        const val MAX_MS = 10_000L
    }
}
