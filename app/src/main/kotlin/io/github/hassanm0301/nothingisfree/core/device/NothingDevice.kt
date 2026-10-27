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
     * Map of `Build.MODEL` -> model. Nothing uses "A0xx" style model numbers.
     * TODO(device-table): confirm the (3a), (4a) and (4a) Pro model strings on hardware;
     * the gallery's Device card prints the raw values so they can be copied in.
     */
    private val byModel: Map<String, NothingModel> = mapOf(
        "A063" to NothingModel.PHONE_1,
        "A065" to NothingModel.PHONE_2,
        "A142" to NothingModel.PHONE_2A,
        "A059P" to NothingModel.PHONE_2A_PLUS,
        "A024" to NothingModel.PHONE_3,
    )

    val detectedModel: NothingModel
        get() = when {
            !isNothing -> NothingModel.NOT_NOTHING
            else -> byModel[model.uppercase()] ?: NothingModel.UNKNOWN_NOTHING
        }
}
