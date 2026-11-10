package io.github.hassanm0301.nothingisfree.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Draws the clock widget as a picture: the date, the time and the next alarm, centred, in white.
 *
 * A picture, because a widget's layout is inflated by the launcher in a restricted context that
 * refuses an app's own fonts (`RemoteViews.RemoteViewsContextWrapper.isRestricted`), so a
 * `TextClock` could only ever show a system font.
 */
object ClockWidgetRenderer {
    /** Date and alarm text size, as a share of the time's. */
    private const val LINE_SIZE = 0.24f

    /** Space between two lines, as a share of the time's text size. */
    private const val LINE_GAP = 0.2f

    private val typefaces = HashMap<Int, Typeface>()

    /**
     * The widget for [lines], as large as fits in [maxWidth] x [maxHeight] pixels. The size is
     * worked out as if an alarm were set, so the time doesn't jump when one comes or goes; the
     * picture itself is cropped to what is drawn.
     */
    fun render(context: Context, lines: ClockWidgetLines, font: ClockWidgetFont, maxWidth: Int, maxHeight: Int): Bitmap {
        val timePaint = paint(context, font.timeFont)
        val linePaint = paint(context, font.lineFont)

        val reference = Layout(lines, timePaint, linePaint, timeSize = 100f)
        val scale = min(maxWidth / reference.width, maxHeight / reference.heightWithAlarm)
        // A little under the fit: text sizes don't scale to the exact pixel.
        val layout = Layout(lines, timePaint, linePaint, timeSize = max(100f * scale * 0.98f, 1f))

        val bitmap = Bitmap.createBitmap(
            ceil(layout.width).toInt().coerceAtLeast(1),
            ceil(layout.height).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        layout.draw(Canvas(bitmap))
        return bitmap
    }

    private fun paint(context: Context, fontRes: Int) = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = Color.WHITE
        typeface = typefaces.getOrPut(fontRes) { context.resources.getFont(fontRes) }
        textAlign = Paint.Align.LEFT
    }

    /** Where everything goes for a time drawn at [timeSize] pixels. Sets the paints' text sizes. */
    private class Layout(
        private val lines: ClockWidgetLines,
        private val timePaint: Paint,
        private val linePaint: Paint,
        timeSize: Float,
    ) {
        private val gap = timeSize * LINE_GAP
        private val timeHeight: Float
        private val lineHeight: Float
        private val lineDescent: Float
        private val icon: Float
        private val dateWidth: Float
        private val timeWidth: Float
        private val alarmTextWidth: Float

        init {
            timePaint.textSize = timeSize
            linePaint.textSize = timeSize * LINE_SIZE
            // Digit and capital heights rather than the fonts' line heights, which leave the
            // lines too far apart.
            timeHeight = capHeight(timePaint, "0")
            lineHeight = capHeight(linePaint, "T")
            lineDescent = linePaint.fontMetrics.descent
            icon = lineHeight * 1.15f
            dateWidth = linePaint.measureText(lines.date)
            timeWidth = timePaint.measureText(lines.time)
            alarmTextWidth = lines.alarm?.let(linePaint::measureText) ?: 0f
        }

        private val alarmWidth get() = if (lines.alarm == null) 0f else icon + lineHeight * 0.5f + alarmTextWidth

        val width get() = maxOf(dateWidth, timeWidth, alarmWidth) + 2f

        private val heightWithoutAlarm get() = lineHeight + gap + timeHeight + lineDescent

        val heightWithAlarm get() = heightWithoutAlarm + gap + lineHeight

        val height get() = if (lines.alarm == null) heightWithoutAlarm else heightWithAlarm

        fun draw(canvas: Canvas) {
            val centre = width / 2f
            var baseline = lineHeight
            canvas.drawText(lines.date, centre - dateWidth / 2f, baseline, linePaint)
            baseline += gap + timeHeight
            canvas.drawText(lines.time, centre - timeWidth / 2f, baseline, timePaint)

            val alarm = lines.alarm ?: return
            baseline += gap + lineHeight
            val left = centre - alarmWidth / 2f
            drawAlarmIcon(canvas, left, baseline - (lineHeight + icon) / 2f, icon)
            canvas.drawText(alarm, left + alarmWidth - alarmTextWidth, baseline, linePaint)
        }

        /** An alarm clock in a [size] square at ([left], [top]): a face, two hands and two bells. */
        private fun drawAlarmIcon(canvas: Canvas, left: Float, top: Float, size: Float) {
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = linePaint.color
                style = Paint.Style.STROKE
                strokeWidth = size * 0.1f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val cx = left + size * 0.5f
            val cy = top + size * 0.57f
            val r = size * 0.36f
            canvas.drawCircle(cx, cy, r, stroke)
            canvas.drawLine(cx, cy - r * 0.5f, cx, cy, stroke)
            canvas.drawLine(cx, cy, cx + r * 0.4f, cy + r * 0.3f, stroke)
            canvas.drawLine(left + size * 0.08f, top + size * 0.25f, left + size * 0.25f, top + size * 0.1f, stroke)
            canvas.drawLine(left + size * 0.92f, top + size * 0.25f, left + size * 0.75f, top + size * 0.1f, stroke)
        }

        private fun capHeight(paint: Paint, sample: String): Float {
            val bounds = Rect()
            paint.getTextBounds(sample, 0, sample.length, bounds)
            return -bounds.top.toFloat()
        }
    }
}
