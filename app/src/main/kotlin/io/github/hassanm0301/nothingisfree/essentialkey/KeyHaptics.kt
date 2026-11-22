// Adapted from Essential Key Tools by KoukeNeko (MIT License, see docs/licenses/EssentialKeyTools.txt).
package io.github.hassanm0301.nothingisfree.essentialkey

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager

/** How the phone vibrates when the Essential Key is pressed. Off unless the user picks one. */
enum class HapticStrength(val title: String) {
    OFF("Off"),
    LIGHT("Light"),
    MEDIUM("Medium"),
    STRONG("Strong"),
    CUSTOM("Custom"),
}

/**
 * Plays the vibration for an Essential Key press, tagged as hardware feedback like a physical
 * button. Every level is an explicit pulse rather than a predefined effect: on Nothing phones the
 * predefined effects fall back to one generic pattern (Phone (3)) or to nothing at all (Phone (4a)),
 * and the Phone (3) motor has no amplitude control, so pulse length is what tells the levels apart.
 */
class KeyHaptics(context: Context) {

    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)

    val hasAmplitudeControl: Boolean get() = vibrator.hasAmplitudeControl()

    /** Plays [custom] for [HapticStrength.CUSTOM]; the other levels are fixed pulses. */
    fun perform(strength: HapticStrength, custom: HapticPattern) {
        val timings = when (strength) {
            HapticStrength.OFF -> return
            HapticStrength.CUSTOM -> return play(custom)
            HapticStrength.LIGHT -> LIGHT
            HapticStrength.MEDIUM -> MEDIUM
            HapticStrength.STRONG -> STRONG
        }
        vibrator.vibrate(VibrationEffect.createWaveform(timings, NO_REPEAT), attributes)
    }

    fun play(pattern: HapticPattern) {
        val waveform = pattern.toWaveform(hasAmplitudeControl)
        vibrator.vibrate(VibrationEffect.createWaveform(waveform.timings, waveform.amplitudes, NO_REPEAT), attributes)
    }

    private companion object {
        const val NO_REPEAT = -1

        // Off/on pairs, starting with the wait before the motor starts.
        val LIGHT = longArrayOf(0, 15)
        // 40ms couldn't be told from Light on the Phone (4a); 50ms clearly can.
        val MEDIUM = longArrayOf(0, 50)
        val STRONG = longArrayOf(0, 80)
    }
}
