package io.github.hassanm0301.nothingisfree.core.update

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Update settings, persisted. [init] runs from `NothingIsFreeApp.onCreate`. */
object UpdatePreferences {
    private const val KEY_BETA = "beta"

    private var prefs: SharedPreferences? = null
    private val _beta = MutableStateFlow(false)

    /** Whether updates include GitHub pre-releases (the beta channel). */
    val beta: StateFlow<Boolean> = _beta.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
        prefs = p
        _beta.value = p.getBoolean(KEY_BETA, false)
    }

    fun setBeta(on: Boolean) {
        _beta.value = on
        prefs?.edit { putBoolean(KEY_BETA, on) }
    }
}
