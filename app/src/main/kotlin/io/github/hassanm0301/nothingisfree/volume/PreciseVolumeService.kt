package io.github.hassanm0301.nothingisfree.volume

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import io.github.hassanm0301.nothingisfree.R
import io.github.hassanm0301.nothingisfree.ui.theme.DarkNothingColors
import io.github.hassanm0301.nothingisfree.ui.theme.LightNothingColors
import io.github.hassanm0301.nothingisfree.ui.theme.NothingBlack
import io.github.hassanm0301.nothingisfree.ui.theme.NothingWhite
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Precise volume: writes the volume level as text beside the system volume bar while the volume
 * popup is up. The bar itself is left as it is.
 *
 * It is an accessibility service because only those can both see where the system's volume popup
 * is and draw above it (`TYPE_ACCESSIBILITY_OVERLAY` sits over `TYPE_VOLUME_OVERLAY`). It filters
 * no keys. Two things bring the label up: AudioService's broadcast when the volume changes, and a
 * system window opening that may be the popup, because while nothing plays Android's first press
 * only opens the popup and changes nothing. System windows are looked through for the bar only
 * after one of those, while a label is showing or about to.
 */
class PreciseVolumeService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val timeline = LabelTimeline()
    private val tickRunnable = Runnable { tick() }

    private lateinit var audio: AudioManager
    private lateinit var power: PowerManager
    private lateinit var windowManager: WindowManager

    private var registered = false
    private var ticking = false

    /** The stream that changed last; the label shows its level. */
    private var stream = AudioManager.STREAM_MUSIC

    /** The popup opened without a change, so which stream it shows is read off the bar. */
    private var streamFromBar = false

    /** The label and its window while it is on screen. */
    private var label: TextView? = null
    private var labelParams: WindowManager.LayoutParams? = null

    private val ndot: Typeface? by lazy { ResourcesCompat.getFont(this, R.font.ndot57) }

    private val volumeChanged = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val changed = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
            if (!VolumeStreams.isShown(changed) || !power.isInteractive) return
            stream = changed
            streamFromBar = false
            timeline.changed(SystemClock.uptimeMillis())
            if (!ticking) tick()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        audio = getSystemService(AudioManager::class.java)
        power = getSystemService(PowerManager::class.java)
        // An accessibility service's own window manager carries the token its overlays need.
        windowManager = getSystemService(WindowManager::class.java)
        if (!registered) {
            // A protected broadcast that only the system can send, so exporting the receiver lets
            // no app in, and it arrives whichever system process sends it.
            registerReceiver(volumeChanged, IntentFilter(VOLUME_CHANGED_ACTION), RECEIVER_EXPORTED)
            registered = true
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stop()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }

    /**
     * A window opened. If it is a system window that could be the volume popup, look for the bar:
     * the popup may have opened on a press that changed nothing, so no broadcast is coming.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return
        if ((event.windowChanges and AccessibilityEvent.WINDOWS_CHANGE_ADDED) == 0) return
        if (!registered || ticking || !power.isInteractive) return
        // Not finding the window is no reason to skip it; looking costs a few ticks at most.
        val added = windows.firstOrNull { it.id == event.windowId }
        if (added != null) {
            if (added.type != AccessibilityWindowInfo.TYPE_SYSTEM) return
            if (!VolumeBar.couldBePopup(shapeOf(added), screen())) return
        }
        streamFromBar = true
        timeline.appeared(SystemClock.uptimeMillis())
        tick()
    }

    override fun onInterrupt() = Unit

    private fun stop() {
        if (registered) {
            unregisterReceiver(volumeChanged)
            registered = false
        }
        handler.removeCallbacks(tickRunnable)
        ticking = false
        timeline.cancel()
        removeLabel()
    }

    /** Looks for the bar once, then shows, moves or hides the label to match. */
    private fun tick() {
        handler.removeCallbacks(tickRunnable)
        val screen = screen()
        val awake = power.isInteractive
        val bar = if (awake) findBar(screen) else null
        val step = if (awake) timeline.tick(SystemClock.uptimeMillis(), bar != null) else timeline.cancel()
        when (step) {
            LabelTimeline.Step.WAIT -> Unit
            LabelTimeline.Step.BESIDE_BAR -> {
                if (streamFromBar && bar != null) {
                    stream = streamShownBy(bar)
                    streamFromBar = false
                }
                show(screen, bar)
            }
            LabelTimeline.Step.FALLBACK -> show(screen, VolumeBar.fallback(screen, dp(16), dp(48), dp(240)))
            LabelTimeline.Step.KEEP -> show(screen, bar = null)
            LabelTimeline.Step.HIDE -> {
                ticking = false
                removeLabel()
                return
            }
        }
        ticking = true
        handler.postDelayed(tickRunnable, LabelTimeline.TICK_MS)
    }

    /**
     * The volume bar on screen, or null if it isn't showing or can't be found. Only system windows
     * are looked at, and of those only the ones that could be the volume popup.
     */
    private fun findBar(screen: ScreenRect): Bar? {
        val system = windows.filter { it.type == AccessibilityWindowInfo.TYPE_SYSTEM }
        val byId = system.associateBy { it.id }
        for (candidate in VolumeBar.candidates(system.map(::shapeOf), screen)) {
            val root = byId[candidate.id]?.root ?: continue
            VolumeBar.bar(candidate, firstSlider(root), screen)?.let { return it }
        }
        return null
    }

    /**
     * The stream [bar] shows when the popup opened without a change: the one whose level matches
     * how full the bar is, or else the most likely (media, or the call during a call).
     */
    private fun streamShownBy(bar: Bar): Int {
        val inCall = audio.mode == AudioManager.MODE_IN_CALL || audio.mode == AudioManager.MODE_IN_COMMUNICATION
        val order = if (inCall) VolumeStreams.IN_CALL_ORDER else VolumeStreams.ORDER
        return VolumeStreams.pick(
            bar.fill,
            order.map { s ->
                StreamLevel(s, audio.getStreamVolume(s), audio.getStreamMinVolume(s), audio.getStreamMaxVolume(s))
            },
        )
    }

    private fun shapeOf(window: AccessibilityWindowInfo): SystemWindow = SystemWindow(
        id = window.id,
        layer = window.layer,
        bounds = Rect().also(window::getBoundsInScreen).toScreenRect(),
        titleSaysVolume = window.title?.contains("volume", ignoreCase = true) == true,
    )

    /** The first visible slider (a node with a range) under [root], breadth first. */
    private fun firstSlider(root: AccessibilityNodeInfo): Slider? {
        val nodes = mutableListOf(root)
        var i = 0
        while (i < nodes.size && i < MAX_NODES) {
            val node = nodes[i++]
            val range = node.rangeInfo
            if (range != null && node.isVisibleToUser) {
                val fill = if (range.max > range.min) {
                    ((range.current - range.min) / (range.max - range.min)).coerceIn(0f, 1f)
                } else {
                    null
                }
                return Slider(Rect().also(node::getBoundsInScreen).toScreenRect(), fill)
            }
            for (c in 0 until node.childCount) node.getChild(c)?.let { nodes.add(it) }
        }
        return null
    }

    /**
     * Writes the current level into the label and puts it beside [bar], adding it if it isn't up
     * yet. With no [bar], only the text changes and the label stays where it is.
     */
    private fun show(screen: ScreenRect, bar: Bar?) {
        if (bar == null && label == null) return
        val format = PreciseVolumePreferences.format.value
        val max = audio.getStreamMaxVolume(stream)
        val view = label ?: newLabel()
        val text = format.text(audio.getStreamVolume(stream), max)
        if (view.text.toString() != text) view.text = text
        // As wide as the longest reading, so the label doesn't shift as the digits change.
        val minWidth = ceil(view.paint.measureText(format.widest(max))).toInt() +
            view.paddingLeft + view.paddingRight
        if (view.minWidth != minWidth) view.minWidth = minWidth
        if (bar == null) return

        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(unspecified, unspecified)
        val (x, y) = VolumeBar.place(bar, screen, view.measuredWidth, view.measuredHeight, dp(GAP_DP), dp(MARGIN_DP))
        val params = labelParams
        if (label == null || params == null) {
            val newParams = overlayParams(x, y)
            try {
                windowManager.addView(view, newParams)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Couldn't show the volume label", e)
                return
            }
            label = view
            labelParams = newParams
        } else if (params.x != x || params.y != y) {
            params.x = x
            params.y = y
            windowManager.updateViewLayout(view, params)
        }
    }

    private fun removeLabel() {
        val view = label ?: return
        label = null
        labelParams = null
        try {
            windowManager.removeView(view)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Couldn't remove the volume label", e)
        }
    }

    /** A pill in Ndot, coloured like the system's light or dark theme so it matches the bar. */
    private fun newLabel(): TextView {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val colors = if (night) DarkNothingColors else LightNothingColors
        val ink = if (night) NothingWhite else NothingBlack
        return TextView(this).apply {
            typeface = ndot
            setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
            setTextColor(ink.toArgb())
            gravity = Gravity.CENTER
            maxLines = 1
            includeFontPadding = false
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(100).toFloat()
                setColor(colors.card.toArgb())
                setStroke(dp(1), colors.hairline.toArgb())
            }
            // The volume popup already speaks the level; don't say it twice.
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    /** An untouchable overlay at screen pixel [x], [y], ignoring insets and cutouts. */
    @SuppressLint("RtlHardcoded") // x is measured from the screen's left edge, whatever the language.
    private fun overlayParams(x: Int, y: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        this.x = x
        this.y = y
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        fitInsetsTypes = 0
        // Fades in and out like the system volume popup, which uses the same animation.
        windowAnimations = android.R.style.Animation_Toast
        title = "Precise volume"
    }

    /** The whole display, in the same pixels as window and node bounds. */
    private fun screen(): ScreenRect {
        val bounds = windowManager.maximumWindowMetrics.bounds.toScreenRect()
        if (!bounds.isEmpty) return bounds
        val metrics = resources.displayMetrics
        return ScreenRect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun Rect.toScreenRect() = ScreenRect(left, top, right, bottom)

    companion object {
        private const val TAG = "PreciseVolume"

        /**
         * Hidden in AudioManager, but AudioService sends it for every volume change. It is a
         * protected broadcast, so only the system can send it.
         */
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"

        private const val TEXT_SP = 22f
        private const val GAP_DP = 8
        private const val MARGIN_DP = 8

        /** Enough for the volume popup; stops a big window from being walked node by node. */
        private const val MAX_NODES = 200

        /** Whether the user has switched the service on in Accessibility settings. */
        fun isOn(context: Context): Boolean {
            val me = ComponentName(context, PreciseVolumeService::class.java)
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        /**
         * Accessibility settings, where the service is switched on. Apps can't open its own page
         * there directly (that needs a system permission), so this is the list.
         */
        fun settingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
}
