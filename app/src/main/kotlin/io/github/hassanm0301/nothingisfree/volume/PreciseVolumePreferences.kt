package io.github.hassanm0301.nothingisfree.volume

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Precise volume settings, shared by the app UI and [PreciseVolumeService] (same process).
 * [init] runs from `NothingIsFreeApp.onCreate`.
 */
object PreciseVolumePreferences {
    private const val KEY_FORMAT = "format"

    private var prefs: SharedPreferences? = null
    private val _format = MutableStateFlow(VolumeFormat.PERCENT)

    /** How the level is written beside the volume bar. */
    val format: StateFlow<VolumeFormat> = _format.asStateFlow()

    fun init(context: Context) {
        val p = context.getSharedPreferences("precise_volume", Context.MODE_PRIVATE)
        prefs = p
        _format.value = p.getString(KEY_FORMAT, null)
            ?.let { name -> VolumeFormat.entries.firstOrNull { it.name == name } }
            ?: VolumeFormat.PERCENT
    }

    fun setFormat(format: VolumeFormat) {
        _format.value = format
        prefs?.edit { putString(KEY_FORMAT, format.name) }
    }
}
