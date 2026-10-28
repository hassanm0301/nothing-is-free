package io.github.hassanm0301.nothingisfree.core.device

import android.os.Build

/**
 * Nothing phone models, keyed by the identifiers the Glyph SDKs use
 * (`Glyph.DEVICE_xxxxx` constants / `Common.isXxxxx()` checks).
 */
enum class NothingModel(val marketingName: String, val glyphDeviceId: String?) {
    PHONE_1("Phone (1)", "20111"),
    PHONE_2("Phone (2)", "22111"),
    PHONE_2A("Phone (2a)", "23111"),
    PHONE_2A_PLUS("Phone (2a) Plus", "23113"),
    PHONE_3A("Phone (3a) / (3a) Pro", "24111"),
    PHONE_3("Phone (3)", "23112"),
    PHONE_4A("Phone (4a)", "25111"),
    PHONE_4A_PRO("Phone (4a) Pro", "25111p"),
    PHONE_4B("Phone (4b)", "25131"),
    UNKNOWN_NOTHING("Nothing device", null),
    NOT_NOTHING("Not a Nothing device", null),
}

/** Best-effort device identification from [Build] fields. */
object NothingDevice {
    val manufacturer: String get() = Build.MANUFACTURER
    val model: String get() = Build.MODEL
    val device: String get() = Build.DEVICE
    val product: String get() = Build.PRODUCT

    val isNothing: Boolean get() = manufacturer.equals("Nothing", ignoreCase = true)

    /**
     * Map of `Build.MODEL` -> model. Nothing uses "A0xx" style model numbers; these match the
     * `Glyph.DEVICE_xxxxx` strings in the Glyph Matrix SDK 2.0, which checks `Build.MODEL` the same way.
     * TODO(device-table): the (3a) Pro ("A059P") and (4b) strings are not in the SDK; confirm on
     * hardware via the gallery's Device card.
     */
    private val byModel: Map<String, NothingModel> = mapOf(
        "A063" to NothingModel.PHONE_1,
        "A065" to NothingModel.PHONE_2,
        "AIN065" to NothingModel.PHONE_2,
        "A142" to NothingModel.PHONE_2A,
        "A142P" to NothingModel.PHONE_2A_PLUS,
        "A059" to NothingModel.PHONE_3A,
        "A059P" to NothingModel.PHONE_3A,
        "A024" to NothingModel.PHONE_3,
        "A069" to NothingModel.PHONE_4A,
        "A069P" to NothingModel.PHONE_4A_PRO,
    )

    val detectedModel: NothingModel
        get() = when {
            !isNothing -> NothingModel.NOT_NOTHING
            else -> byModel[model.uppercase()] ?: NothingModel.UNKNOWN_NOTHING
        }
}
