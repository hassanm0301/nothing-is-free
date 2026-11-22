package io.github.hassanm0301.nothingisfree.essentialkey

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The Essential Key's scan code as Nothing's firmware reports it on the Phone (3a) family. Other
 * models may differ, so the app can learn it (Key setup) and only starts from this.
 */
const val DEFAULT_ESSENTIAL_KEY_SCAN_CODE = 250

/** Vibration on a press: how strong, the custom pattern, and whether only presses that run something buzz. */
data class KeyHapticSettings(
    val strength: HapticStrength = HapticStrength.OFF,
    val pattern: HapticPattern = HapticPattern.DEFAULT,
    val onActionOnly: Boolean = false,
)

/**
 * Essential key settings, shared by the app UI and [EssentialKeyService] (same process).
 * [init] runs from `NothingIsFreeApp.onCreate`.
 */
object EssentialKeyPreferences {
    private const val KEY_SCAN_CODE = "scan_code"
    private const val KEY_ONLY_UNLOCKED = "only_when_unlocked"
    private const val KEY_HAPTIC = "haptic_strength"
    private const val KEY_HAPTIC_ON_ACTION = "haptic_on_action_only"
    private const val KEY_PULSE_COUNT = "haptic_pulse_count"
    private const val KEY_PULSE_MS = "haptic_pulse_ms"
    private const val KEY_PULSE_HZ = "haptic_pulse_hz"
    private const val KEY_PULSE_AMP = "haptic_pulse_amplitude"

    private var prefs: SharedPreferences? = null

    private val _scanCode = MutableStateFlow(DEFAULT_ESSENTIAL_KEY_SCAN_CODE)
    private val _actions = MutableStateFlow(GestureActionMap.EMPTY)
    private val _haptics = MutableStateFlow(KeyHapticSettings())
    private val _onlyWhenUnlocked = MutableStateFlow(false)

    /** Which key is the Essential Key. */
    val scanCode: StateFlow<Int> = _scanCode.asStateFlow()

    /** What each gesture does. */
    val actions: StateFlow<GestureActionMap> = _actions.asStateFlow()

    val haptics: StateFlow<KeyHapticSettings> = _haptics.asStateFlow()

    /** Whether the key does nothing, vibration included, while the lock screen shows. */
    val onlyWhenUnlocked: StateFlow<Boolean> = _onlyWhenUnlocked.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("essential_key", Context.MODE_PRIVATE)
        prefs = p
        _scanCode.value = p.getInt(KEY_SCAN_CODE, DEFAULT_ESSENTIAL_KEY_SCAN_CODE)
        _actions.value = GestureActionMap.decode { key -> p.getString(key, null) }
        _onlyWhenUnlocked.value = p.getBoolean(KEY_ONLY_UNLOCKED, false)
        val d = HapticPattern.DEFAULT
        _haptics.value = KeyHapticSettings(
            strength = p.getString(KEY_HAPTIC, null)
                ?.let { name -> HapticStrength.entries.firstOrNull { it.name == name } }
                ?: HapticStrength.OFF,
            pattern = HapticPattern(
                pulseCount = p.getInt(KEY_PULSE_COUNT, d.pulseCount),
                pulseMillis = p.getInt(KEY_PULSE_MS, d.pulseMillis),
                frequencyHz = p.getInt(KEY_PULSE_HZ, d.frequencyHz),
                amplitudePercent = p.getInt(KEY_PULSE_AMP, d.amplitudePercent),
            ).coerced(),
            onActionOnly = p.getBoolean(KEY_HAPTIC_ON_ACTION, false),
        )
    }

    fun setScanCode(code: Int) {
        _scanCode.value = code
        prefs?.edit { putInt(KEY_SCAN_CODE, code) }
    }

    fun setAction(gesture: KeyGesture, action: KeyAction) {
        _actions.update { it.with(gesture, action) }
        val payload = KeyAction.payloadOf(action)
        prefs?.edit {
            putString(GestureActionMap.idKey(gesture), action.kind.id)
            if (payload == null) remove(GestureActionMap.payloadKey(gesture))
            else putString(GestureActionMap.payloadKey(gesture), payload)
        }
    }

    fun setOnlyWhenUnlocked(on: Boolean) {
        _onlyWhenUnlocked.value = on
        prefs?.edit { putBoolean(KEY_ONLY_UNLOCKED, on) }
    }

    fun updateHaptics(transform: (KeyHapticSettings) -> KeyHapticSettings) {
        val next = transform(_haptics.value).let { it.copy(pattern = it.pattern.coerced()) }
        _haptics.value = next
        prefs?.edit {
            putString(KEY_HAPTIC, next.strength.name)
            putBoolean(KEY_HAPTIC_ON_ACTION, next.onActionOnly)
            putInt(KEY_PULSE_COUNT, next.pattern.pulseCount)
            putInt(KEY_PULSE_MS, next.pattern.pulseMillis)
            putInt(KEY_PULSE_HZ, next.pattern.frequencyHz)
            putInt(KEY_PULSE_AMP, next.pattern.amplitudePercent)
        }
    }
}
