package io.github.hassanm0301.nothingisfree.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * In-memory theme preference. Process-scoped, so it resets on cold start.
 * TODO(persistence): back this with DataStore once settings exist.
 */
object ThemePreferences {
    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        _mode.value = mode
    }
}
