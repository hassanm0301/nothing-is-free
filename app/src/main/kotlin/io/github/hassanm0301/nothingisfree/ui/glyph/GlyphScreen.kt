package io.github.hassanm0301.nothingisfree.ui.glyph

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.core.device.NothingDevice
import io.github.hassanm0301.nothingisfree.glyph.GlyphCapability
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonId
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyId
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.screenHeader
import io.github.hassanm0301.nothingisfree.ui.theme.NothingTheme
import io.github.hassanm0301.nothingisfree.ui.theme.Spacing

private enum class GlyphSection(val label: String) { TOYS("Toys"), ADDONS("Addons") }

/** A page opened inside the Glyph tab. Stored by [key] so the back stack survives rotation. */
private sealed interface GlyphPage {
    val title: String
    val key: String

    data class Toy(val id: ToyId) : GlyphPage {
        override val title get() = id.title
        override val key get() = "toy/${id.name}"
    }

    data class Addon(val id: AddonId) : GlyphPage {
        override val title get() = id.title
        override val key get() = "addon/${id.name}"
    }

    companion object {
        fun of(key: String): GlyphPage? {
            val (kind, name) = key.split('/', limit = 2).takeIf { it.size == 2 } ?: return null
            return when (kind) {
                "toy" -> ToyId.entries.firstOrNull { it.name == name }?.let(::Toy)
                "addon" -> AddonId.entries.firstOrNull { it.name == name }?.let(::Addon)
                else -> null
            }
        }
    }
}

/**
 * Glyph tab: the toys and the addons as two lists. Tapping one opens its settings; Back walks
 * up from there (a toy's page can open an addon's).
 */
@Composable
fun GlyphScreen(viewModel: AppViewModel, insets: PaddingValues) {
    var section by rememberSaveable { mutableStateOf(GlyphSection.TOYS) }
    val stack = rememberSaveable(saver = listSaver({ it.toList() }, { it.toMutableStateList() })) {
        emptyList<String>().toMutableStateList()
    }
    val pages = stack.mapNotNull(GlyphPage::of)
    // Two back events can land before BackHandler sees the stack shrink; never pop past empty.
    val pop = { stack.removeLastOrNull() }
    // A quick double tap would otherwise open the same page twice.
    val push = { key: String -> if (stack.lastOrNull() != key) stack += key }
    BackHandler(enabled = stack.isNotEmpty()) { pop() }
    // Keeps each page's scroll position while another one is showing.
    val pageState = rememberSaveableStateHolder()

    val page = pages.lastOrNull()
    pageState.SaveableStateProvider(page?.key ?: "list/${section.name}") {
        when (page) {
            is GlyphPage.Toy -> ToyDetailScreen(
                toy = page.id,
                viewModel = viewModel,
                insets = insets,
                onBack = { pop() },
                onOpenAddon = { push(GlyphPage.Addon(it).key) },
            )
            is GlyphPage.Addon -> AddonDetailScreen(
                addon = page.id,
                viewModel = viewModel,
                insets = insets,
                back = pages.getOrNull(pages.size - 2)?.title ?: "Glyph",
                onBack = { pop() },
            )
            null -> GlyphLists(section, { section = it }, viewModel, insets, onOpen = { push(it.key) })
        }
    }
}

@Composable
private fun GlyphLists(
    section: GlyphSection,
    onSection: (GlyphSection) -> Unit,
    viewModel: AppViewModel,
    insets: PaddingValues,
    onOpen: (GlyphPage) -> Unit,
) {
    val addons by viewModel.addons.collectAsStateWithLifecycle()
    val binaryClock by viewModel.binaryClock.collectAsStateWithLifecycle()
    val lastAlertAt by viewModel.lastAlertAt.collectAsStateWithLifecycle()
    val pulseWindowSeconds by viewModel.pulseWindowSeconds.collectAsStateWithLifecycle()
    val now by rememberNow()

    val model = NothingDevice.detectedModel
    val matrix = GlyphCapability.of(model) as? GlyphCapability.Matrix
    val kicker = if (matrix != null) "${model.marketingName} · ${matrix.width}×${matrix.width} matrix" else "Preview only"
    val body = when (section) {
        GlyphSection.TOYS -> if (matrix != null) {
            "Always-on toys for the matrix on the back. Tap one for its settings and a preview."
        } else {
            "This phone has no Glyph Matrix, so toys only run as previews here. Tap one to try it."
        }
        GlyphSection.ADDONS -> "Addons draw on top of any toy. Tap one to choose which toys it's on."
    }

    NothingScreen(insets) {
        screenHeader(kicker, "Glyph", body, live = matrix != null)
        item {
            Spacer(Modifier.height(Spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GlyphSection.entries.forEach { s ->
                    NothingChip(s.label, selected = s == section, onClick = { onSection(s) })
                }
            }
            Spacer(Modifier.height(Spacing.md))
        }
        when (section) {
            GlyphSection.TOYS -> items(ToyId.entries, key = { it.name }) { toy ->
                val face = remember(toy, binaryClock) { toy.face(PREVIEW_SIZE, binaryClock) }
                val pulseOn = toy in addons[AddonId.NOTIFICATION_PULSE].orEmpty()
                val pulse = rememberPulse(lastAlertAt, pulseWindowSeconds, pulseOn)
                val level by remember { derivedStateOf { pulse.value.level } }
                ListCard(
                    title = toy.title,
                    body = toy.summary,
                    tags = AddonId.entries.filter { toy in addons[it].orEmpty() }.map { "+ ${it.title}" },
                    onClick = { onOpen(GlyphPage.Toy(toy)) },
                    leading = { ToyMatrix(face, now, level, Modifier.size(72.dp)) },
                )
            }
            GlyphSection.ADDONS -> items(AddonId.entries, key = { it.name }) { addon ->
                val on = ToyId.entries.filter { it in addons[addon].orEmpty() }
                ListCard(
                    title = addon.title,
                    body = addon.summary,
                    tags = listOf(if (on.isEmpty()) "Off" else "On · " + on.joinToString(" · ") { it.title }),
                    onClick = { onOpen(GlyphPage.Addon(addon)) },
                )
            }
        }
    }
}

/** A tappable list entry: optional thumbnail, title, copy, mono tags, and a chevron. */
@Composable
private fun ListCard(
    title: String,
    body: String,
    tags: List<String>,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    NothingCard(Modifier.padding(bottom = Spacing.sm), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Box(Modifier.size(72.dp)) { leading() }
                Spacer(Modifier.width(Spacing.md))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = NothingTheme.colors.muted)
                tags.forEach { tag ->
                    Spacer(Modifier.height(Spacing.xs))
                    MonoLabel(tag, style = MaterialTheme.typography.labelSmall, color = NothingTheme.colors.accent)
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = NothingTheme.colors.muted,
            )
        }
    }
}
