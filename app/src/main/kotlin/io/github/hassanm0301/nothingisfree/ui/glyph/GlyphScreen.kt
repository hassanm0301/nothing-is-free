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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hassanm0301.nothingisfree.core.device.NothingDevice
import io.github.hassanm0301.nothingisfree.glyph.GlyphCapability
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonCategory
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonId
import io.github.hassanm0301.nothingisfree.glyph.addon.AddonPreferences
import io.github.hassanm0301.nothingisfree.glyph.toy.ToyId
import io.github.hassanm0301.nothingisfree.ui.AppViewModel
import io.github.hassanm0301.nothingisfree.ui.components.MonoLabel
import io.github.hassanm0301.nothingisfree.ui.components.NothingCard
import io.github.hassanm0301.nothingisfree.ui.components.NothingChip
import io.github.hassanm0301.nothingisfree.ui.components.NothingScreen
import io.github.hassanm0301.nothingisfree.ui.components.NothingSwitch
import io.github.hassanm0301.nothingisfree.ui.components.SectionHeader
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
 * up from there.
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
    val digitalClock by viewModel.digitalClock.collectAsStateWithLifecycle()
    val binaryClock by viewModel.binaryClock.collectAsStateWithLifecycle()
    val lastAlertAt by viewModel.lastAlertAt.collectAsStateWithLifecycle()
    val pulseSettings by viewModel.pulse.collectAsStateWithLifecycle()
    val notificationsShowing by viewModel.notificationsShowing.collectAsStateWithLifecycle()
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
        GlyphSection.ADDONS ->
            "Addons draw on top of whichever toy is showing. Switch them on here, once for every toy: " +
                "one per category, so switching one on switches off the other of its kind. Tap one " +
                "for its settings."
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
                val face = remember(toy, digitalClock, binaryClock) { toy.face(PREVIEW_SIZE, digitalClock, binaryClock) }
                val pulseOn = AddonId.NOTIFICATION_PULSE in addons
                val pulse = rememberPulse(lastAlertAt, pulseSettings, pulseOn, notificationsShowing)
                val level by remember { derivedStateOf { pulse.value.level } }
                val battery = batteryGauge(viewModel)
                ListCard(
                    title = toy.title,
                    body = toy.summary,
                    tags = AddonId.entries.filter { it in addons }.map { "+ ${it.title}" },
                    onClick = { onOpen(GlyphPage.Toy(toy)) },
                    leading = { ToyMatrix(face, now, level, Modifier.size(72.dp), battery) },
                )
            }
            GlyphSection.ADDONS -> AddonCategory.entries.forEach { category ->
                item(key = "category/${category.name}") { SectionHeader(category.title) }
                items(AddonId.entries.filter { it.category == category }, key = { it.name }) { addon ->
                    val on = addon in addons
                    val other = AddonPreferences.active(addons, category)?.takeIf { it != addon }
                    ListCard(
                        title = addon.title,
                        body = addon.summary,
                        tags = listOf(
                            when {
                                on -> "On · every toy"
                                other != null -> "Off · ${other.title} is on"
                                else -> "Off"
                            },
                        ),
                        onClick = { onOpen(GlyphPage.Addon(addon)) },
                        trailing = { AddonSwitch(addon, on, viewModel) },
                    )
                }
            }
        }
    }
}

/**
 * A tappable list entry: optional thumbnail, title, copy, mono tags, and a chevron, or [trailing]
 * in its place (its own taps don't open the entry).
 */
@Composable
private fun ListCard(
    title: String,
    body: String,
    tags: List<String>,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
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
            if (trailing != null) {
                Spacer(Modifier.width(Spacing.md))
                trailing()
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = NothingTheme.colors.muted,
                )
            }
        }
    }
}

/**
 * The one switch for [addon], [on] or off for every toy at once; on switches off the other addon
 * of its category. Named after the addon, since in a list of them the switch alone doesn't say
 * which it is.
 */
@Composable
fun AddonSwitch(addon: AddonId, on: Boolean, viewModel: AppViewModel, modifier: Modifier = Modifier) {
    NothingSwitch(
        checked = on,
        onCheckedChange = { viewModel.setAddon(addon, it) },
        modifier = modifier.semantics { contentDescription = addon.title },
    )
}
