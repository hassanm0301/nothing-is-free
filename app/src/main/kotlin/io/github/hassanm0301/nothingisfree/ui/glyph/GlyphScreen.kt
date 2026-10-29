package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.core.device.NothingDevice
import io.github.hassanm0301.nothingisfree.glyph.GlyphCapability
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonPreferences
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
import io.github.hassanm0301.nothingisfree.ui.components.screenHeader
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

/** Glyph tab: the app's Glyph Toys, each with a live preview and its options. */
@Composable
fun GlyphScreen(viewModel: AppViewModel, insets: PaddingValues) {
    val lastAlertAt by viewModel.lastAlertAt.collectAsStateWithLifecycle()
    val pulseWindowSeconds by viewModel.pulseWindowSeconds.collectAsStateWithLifecycle()
    val binaryClock by viewModel.binaryClock.collectAsStateWithLifecycle()

    val model = NothingDevice.detectedModel
    val matrix = GlyphCapability.of(model) as? GlyphCapability.Matrix
    val kicker = if (matrix != null) "${model.marketingName} · ${matrix.width}×${matrix.width} matrix" else "Preview only"
    val body = if (matrix != null) {
        "Always-on Glyph Toys for the matrix on the back. Changes here reach the phone straight away."
    } else {
        "This phone has no Glyph Matrix, so the toys only run as previews here."
    }

    NothingScreen(insets) {
        screenHeader(kicker, "Glyph", body, live = matrix != null)
        item {
            SectionHeader("Toys")
            PulseClockCard(
                lastAlertAt = lastAlertAt,
                pulseWindowSeconds = pulseWindowSeconds,
                pulseWindowChoices = AddonPreferences.pulseWindowChoices,
                onPulseWindowChange = viewModel::setPulseWindowSeconds,
                onTestPulse = viewModel::testPulse,
            )
            Spacer(Modifier.height(Spacing.md))
            BinaryClockCard(binaryClock, onChange = viewModel::updateBinaryClock)
        }
    }
}
