/*
 * Copyright The TRS-80 App Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.puder.trs80.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.catalog
import trs_80.shared.generated.resources.custom
import trs_80.shared.generated.resources.loading
import trs_80.shared.generated.resources.search_entries
import trs_80.shared.generated.resources.show_all
import trs_80.shared.generated.resources.show_fewer
import trs_80.shared.generated.resources.sort_alphabetical
import trs_80.shared.generated.resources.sort_last_used
import trs_80.shared.generated.resources.store_unreachable
import trs_80.shared.generated.resources.yours
import org.puder.trs80.shared.ui.theme.MinimumTouchTarget
import org.puder.trs80.shared.ui.theme.ProgressRing
import org.puder.trs80.shared.ui.theme.SearchField
import org.puder.trs80.shared.ui.theme.SegmentedToggle
import org.puder.trs80.shared.ui.theme.StrokeIcon
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.Trs80Icon
import org.puder.trs80.shared.ui.theme.Trs80Theme
import org.puder.trs80.shared.ui.theme.scanlines

/** How many of the user's own machines the library shows before it offers the rest. */
/**
 * How large the control at the end of a catalogue row is drawn.
 *
 * Bigger than the icons elsewhere: this is the row's one action, and at the
 * size the rest of the set uses it reads as a status mark rather than a thing
 * to press.
 */
private val ROW_CONTROL = 24.dp

/** One turn of the refresh control while the store is being asked. */
private const val SPIN_MILLIS = 900

private const val COLLAPSED_PLATES = 3

/** One entry in the catalogue, and what the app can do with it. */
data class CatalogueEntry(
    val id: String,
    val title: String,
    val author: String,
    val year: Int,
    val artUrl: String?,
    /**
     * The configuration this entry installed as, or null if it is not on the
     * device — which is also what decides whether it plays or downloads.
     */
    val installedId: Int? = null,
    /** Being downloaded now. */
    val installing: Boolean = false,
) {
    /** Whether the program is on the device. */
    val installed: Boolean get() = installedId != null
}

/** What the library can be asked to do. */
data class LibraryActions(
    val onRun: (Int) -> Unit = {},
    val onOpenEntry: (CatalogueEntry) -> Unit = {},
    val onInstall: (CatalogueEntry) -> Unit = {},
    val onAdd: (() -> Unit)? = null,
    /** Opens the editor for one of the user's machines, from its overflow. */
    val onEdit: ((Int) -> Unit)? = null,
    /** Asks the store for the catalogue again. */
    val onRefresh: (() -> Unit)? = null,
    val onOpenSettings: (() -> Unit)? = null,
)

/** How the user's own machines are ordered. */
enum class LibrarySort { LastUsed, Alphabetical }

/**
 * The library: what the user has, and what the store offers, on one screen.
 *
 * This is the shape the visual spec settles on, and it is a real change from
 * what the app did before — the store used to be somewhere you went, and it is
 * now the second half of the first screen. The user's own machines come first
 * as plates, capped so that the catalogue always starts above the fold.
 */
@Composable
fun LibraryScreen(
    yours: List<ConfigurationCard>,
    catalogue: List<CatalogueEntry>,
    catalogueState: StoreState,
    /** Whether the store is being asked again, which the refresh control shows. */
    refreshing: Boolean = false,
    query: String,
    sort: LibrarySort,
    expanded: Boolean,
    onQueryChange: (String) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
) {
    val colors = Trs80Theme.colors
    val spacing = Trs80Theme.spacing
    val shown = if (expanded) yours else yours.take(COLLAPSED_PLATES)

    Column(
        modifier
            .fillMaxSize()
            .background(colors.ground)
            // The ground runs edge to edge; the content does not. Without this
            // the wordmark sits under the status bar.
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        LibraryTopBar(actions)
        SearchRow(query, onQueryChange, yours.size + catalogue.size)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.screenEdge,
                end = spacing.screenEdge,
                bottom = 24.dp,
            ),
        ) {
            item {
                SectionHeader(label = stringResource(Res.string.yours), count = yours.size) {
                    SegmentedToggle(
                    options = listOf(stringResource(Res.string.sort_last_used), stringResource(Res.string.sort_alphabetical)),
                    selected = if (sort == LibrarySort.LastUsed) 0 else 1,
                    onSelect = {
                        onSortChange(if (it == 0) LibrarySort.LastUsed else LibrarySort.Alphabetical)
                    },
                )
                }
            }
            items(shown, key = { it.id }) { card ->
                Plate(
                    card,
                    onClick = { actions.onRun(card.id) },
                    onEdit = actions.onEdit?.let { edit -> { edit(card.id) } },
                )
            }
            if (yours.size > COLLAPSED_PLATES) {
                item { ShowAll(expanded, yours.size) { onExpandedChange(!expanded) } }
            }
            item {
                SectionHeader(
                    label = stringResource(Res.string.catalog),
                    count = catalogue.size.takeIf { it > 0 },
                    trailing = actions.onRefresh?.let {
                        { RefreshControl(refreshing = refreshing, onClick = it) }
                    },
                )
            }
            when (catalogueState) {
                is StoreState.Loading -> item { CatalogueNote(stringResource(Res.string.loading)) }
                is StoreState.Failed -> item { CatalogueNote(stringResource(Res.string.store_unreachable)) }
                is StoreState.Loaded -> items(catalogue, key = { it.id }) { entry ->
                    CatalogueRow(entry, actions)
                }
            }
        }
    }
}

/**
 * Asks the store again.
 *
 * Turns into the ring while it is asking, in the same place and at the same
 * size, so the control does not move or vanish under the finger that pressed it.
 */
@Composable
private fun RefreshControl(refreshing: Boolean, onClick: () -> Unit) {
    val colors = Trs80Theme.colors
    // One reserved slot either way, so the control does not move or resize
    // under the finger that pressed it.
    Box(Modifier.size(MinimumTouchTarget), contentAlignment = Alignment.Center) {
        if (refreshing) {
            // The spin is only composed while it is spinning: an infinite
            // transition redraws every frame for as long as it exists, and this
            // sits on a screen the app spends most of its life on.
            val spin = rememberInfiniteTransition(label = "refresh")
            val angle by spin.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(SPIN_MILLIS, easing = LinearEasing),
                ),
                label = "angle",
            )
            StrokeIcon(
                Trs80Icon.Refresh,
                color = colors.accentText,
                size = 17.dp,
                modifier = Modifier.graphicsLayer { rotationZ = angle },
            )
        } else {
            StrokeIcon(Trs80Icon.Refresh, color = colors.accentText, size = 17.dp, onClick = onClick)
        }
    }
}

@Composable
private fun LibraryTopBar(actions: LibraryActions) {
    val colors = Trs80Theme.colors
    val spacing = Trs80Theme.spacing
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = spacing.screenEdge, end = spacing.screenEdge, top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("TRS-80", style = Trs80Theme.type.wordmark, color = colors.text)
        Spacer(Modifier.weight(1f))
        actions.onAdd?.let {
            StrokeIcon(Trs80Icon.Plus, color = colors.muted, onClick = it)
            Spacer(Modifier.width(14.dp))
        }
        actions.onOpenSettings?.let {
            StrokeIcon(Trs80Icon.Settings, color = colors.muted, onClick = it)
        }
    }
    Divider()
}

@Composable
private fun SearchRow(query: String, onQueryChange: (String) -> Unit, total: Int) {
    val spacing = Trs80Theme.spacing
    Box(Modifier.fillMaxWidth().padding(horizontal = spacing.screenEdge, vertical = 12.dp)) {
        SearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(Res.string.search_entries, total),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SectionHeader(
    label: String,
    count: Int?,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = Trs80Theme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(), style = Trs80Theme.type.kicker, color = colors.accentText)
        count?.let {
            Spacer(Modifier.width(8.dp))
            Text(it.toString(), style = Trs80Theme.type.bodySmall, color = colors.muted)
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(colors.hairline))
        trailing?.let {
            Spacer(Modifier.width(10.dp))
            it()
        }
    }
}

/**
 * A machine the user has, drawn as a framed screen.
 *
 * The picture is the last thing the machine showed, behind scanlines and under
 * a scrim carrying the name and the model — so the library is a shelf of
 * screens rather than a list of names.
 */
@Composable
private fun Plate(card: ConfigurationCard, onClick: () -> Unit, onEdit: (() -> Unit)?) {
    val colors = Trs80Theme.colors
    val spacing = Trs80Theme.spacing
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.gap)
            .border(spacing.hairline, colors.text.copy(alpha = 0.2f))
            .padding(spacing.mat)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(spacing.plateHeight)
                .background(colors.crt)
                .scanlines(),
        ) {
            val screenshot = card.screenshot
            if (screenshot != null) {
                androidx.compose.foundation.Image(
                    bitmap = screenshot,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                )
            } else {
                // Never run: the machine has drawn nothing, so the plate says so
                // in the machine's own voice rather than showing an empty frame.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        card.name.uppercase(),
                        style = Trs80Theme.type.screen,
                        color = colors.phosphor.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (card.isCustom) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .background(colors.accent.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(stringResource(Res.string.custom), style = Trs80Theme.type.kickerSmall, color = Color.White)
                }
            }

            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x00181917), Color(0xE0181917)),
                        )
                    )
                    .padding(start = 10.dp, end = 4.dp, top = 12.dp),
                // The caption keeps its own baseline while the overflow's touch
                // target centres against it, rather than the target's height
                // dragging the glyph up off the line.
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f).padding(bottom = 7.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        card.name,
                        style = Trs80Theme.type.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        card.model.uppercase(),
                        style = Trs80Theme.type.kickerSmall,
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                if (onEdit != null) {
                    StrokeIcon(
                        Trs80Icon.Overflow,
                        color = Color.White.copy(alpha = 0.72f),
                        size = 17.dp,
                        onClick = onEdit,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowAll(expanded: Boolean, total: Int, onClick: () -> Unit) {
    val colors = Trs80Theme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = Trs80Theme.spacing.gap)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (expanded) stringResource(Res.string.show_fewer) else stringResource(Res.string.show_all, total),
            style = Trs80Theme.type.kicker,
            color = colors.accentText,
            modifier = Modifier.padding(vertical = 9.dp),
        )
    }
}

@Composable
private fun CatalogueRow(entry: CatalogueEntry, actions: LibraryActions) {
    val colors = Trs80Theme.colors
    val spacing = Trs80Theme.spacing
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { actions.onOpenEntry(entry) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteImage(
            url = entry.artUrl,
            modifier = Modifier
                .size(spacing.rowArt)
                .border(spacing.hairline, colors.text.copy(alpha = 0.22f))
                .background(colors.crt),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.title,
                style = Trs80Theme.type.title,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    entry.author.takeIf { it.isNotEmpty() },
                    entry.year.takeIf { it > 0 }?.toString(),
                ).joinToString(" · "),
                style = Trs80Theme.type.bodySmall,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        // One reserved slot for all three states, so the glyph lands in the same
        // place whatever it is. Only the download is tappable, and a tappable
        // icon carries a touch target the others do not — left to size
        // themselves they would each sit at a different distance from the edge.
        Box(Modifier.size(MinimumTouchTarget), contentAlignment = Alignment.Center) {
            when {
                entry.installing -> ProgressRing(progress = null, size = ROW_CONTROL)
                entry.installed ->
                    StrokeIcon(Trs80Icon.Play, color = colors.accentText, size = ROW_CONTROL)

                else -> StrokeIcon(
                    Trs80Icon.Download,
                    color = colors.accentText,
                    size = ROW_CONTROL,
                    onClick = { actions.onInstall(entry) },
                )
            }
        }
    }
    Divider()
}

@Composable
private fun CatalogueNote(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = Trs80Theme.type.body, color = Trs80Theme.colors.muted)
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(Trs80Theme.spacing.hairline)
            .background(Trs80Theme.colors.hairline)
    )
}

