// Adapted from Essential Key Tools by KoukeNeko (MIT License, see docs/licenses/EssentialKeyTools.txt).
package io.github.hassanm0301.nothingisfree.essentialkey

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Essential key: watches hardware keys for the Essential Key (by its scan code), works out the
 * gesture and runs what the user picked for it. It sees key events only, never screen content.
 *
 * Taking the event doesn't stop Nothing OS's own handling of the key. Double, triple and long
 * press are free anyway; the single press is Essential Space's until the user turns the default
 * functions off (the Default functions page).
 */
class EssentialKeyService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var executor: ActionExecutor
    private lateinit var haptics: KeyHaptics
    private lateinit var keyguard: KeyguardManager

    private var classifier: KeyGestureClassifier? = null
    private var scanCode = DEFAULT_ESSENTIAL_KEY_SCAN_CODE
    private var actions = GestureActionMap.EMPTY

    /** Which mode the classifier was built for: testing recognises every gesture. */
    private var builtForTesting = false

    /** The gesture in progress began on the test page, so it runs nothing even if the page closes first. */
    private var suppressNext = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = true
        executor = ActionExecutor(this)
        haptics = KeyHaptics(this)
        keyguard = getSystemService(KeyguardManager::class.java)
        scope.launch {
            combine(EssentialKeyPreferences.scanCode, EssentialKeyPreferences.actions, ::Pair)
                .collect { (code, map) ->
                    scanCode = code
                    actions = map
                    rebuildClassifier()
                }
        }
    }

    private fun rebuildClassifier() {
        classifier?.reset()
        suppressNext = false
        builtForTesting = KeyEventStream.testing
        classifier = KeyGestureClassifier(
            // Only mapped gestures need recognising, so a lone single press needn't wait for a second.
            enabledGestures = if (builtForTesting) KeyGesture.entries.toSet() else actions.activeGestures(),
            scheduler = HandlerGestureScheduler(handler),
            onGesture = ::onGesture,
            onPress = ::onPress,
        )
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (KeyEventStream.learning) {
            // Key setup: show every key but the volume rocker, and let them all through.
            if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP && event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
                KeyEventStream.publish(event.observed())
            }
            return false
        }
        if (event.scanCode != scanCode) return false
        // Entering or leaving Key test changes which gestures count. Only at the start of a press,
        // so a multi-tap already under way isn't cut short.
        if (event.action == KeyEvent.ACTION_DOWN && builtForTesting != KeyEventStream.testing) rebuildClassifier()
        if (KeyEventStream.testing) suppressNext = true
        KeyEventStream.publish(event.observed())
        val c = classifier ?: return true
        when (event.action) {
            KeyEvent.ACTION_DOWN -> c.onKeyDown(event.downTime)
            KeyEvent.ACTION_UP -> c.onKeyUp(event.downTime, event.eventTime)
        }
        return true
    }

    /** Like a physical button, the buzz confirms the press itself, unless it's set to wait for an action. */
    private fun onPress() {
        val settings = EssentialKeyPreferences.haptics.value
        if (!settings.onActionOnly && actions.activeGestures().isNotEmpty() && !ignoredWhileLocked()) {
            haptics.perform(settings.strength, settings.pattern)
        }
    }

    private fun onGesture(gesture: KeyGesture) {
        KeyEventStream.publish(ObservedGesture(gesture, System.currentTimeMillis()))
        val suppressed = KeyEventStream.testing || suppressNext
        suppressNext = false
        if (suppressed || ignoredWhileLocked()) return
        val action = actions.actionFor(gesture)
        val settings = EssentialKeyPreferences.haptics.value
        if (settings.onActionOnly && action != KeyAction.None) haptics.perform(settings.strength, settings.pattern)
        executor.execute(action)
    }

    /**
     * The lock screen showing, not the screen being off: a press with the screen off wakes the phone,
     * so by the time the gesture is known the lock screen is up. Checked as the action is about to
     * run, so a press that straddles the phone locking can't slip through.
     */
    private fun ignoredWhileLocked(): Boolean =
        EssentialKeyPreferences.onlyWhenUnlocked.value && keyguard.isKeyguardLocked

    private fun KeyEvent.observed() = ObservedKeyEvent(keyCode, scanCode, action == KeyEvent.ACTION_DOWN, eventTime)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        classifier?.reset()
        suppressNext = false
    }

    override fun onUnbind(intent: Intent?): Boolean {
        tearDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        tearDown()
        super.onDestroy()
    }

    private fun tearDown() {
        if (!running) return
        running = false
        classifier?.reset()
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        if (::executor.isInitialized) executor.release()
    }

    companion object {
        /** Whether the service is connected right now. */
        @Volatile
        var running: Boolean = false
            private set

        /** Whether the user has switched the service on in Accessibility settings. */
        fun isOn(context: Context): Boolean {
            val me = ComponentName(context, EssentialKeyService::class.java)
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}

/** Posts the classifier's timers to the service's main looper. */
private class HandlerGestureScheduler(private val handler: Handler) : GestureScheduler {
    override fun schedule(delayMs: Long, action: () -> Unit): Cancellable {
        val runnable = Runnable(action)
        handler.postDelayed(runnable, delayMs)
        return object : Cancellable {
            override fun cancel() = handler.removeCallbacks(runnable)
        }
    }
}
