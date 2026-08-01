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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.puder.trs80.shared.ui.theme.ProgressRing
import org.puder.trs80.shared.ui.theme.SearchField
import org.puder.trs80.shared.ui.theme.SegmentedToggle
import org.puder.trs80.shared.ui.theme.StrokeIcon
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.Trs80Icon
import org.puder.trs80.shared.ui.theme.Trs80Theme

/** How many of the user's own machines the library shows before it offers the rest. */
private const val COLLAPSED_PLATES = 3

/** One entry in the catalogue, and what the app can do with it. */
data class CatalogueEntry(
    val id: String,
    val title: String,
    val author: String,
    val year: Int,
    val artUrl: String?,
    /** Already installed, so it plays rather than downloads. */
    val installed: Boolean = false,
    /** Being downloaded now. */
    val installing: Boolean = false,
)

/** What the library can be asked to do. */
data class LibraryActions(
    val onRun: (Int) -> Unit = {},
    val onOpenEntry: (CatalogueEntry) -> Unit = {},
    val onInstall: (CatalogueEntry) -> Unit = {},
    val onAdd: (() -> Unit)? = null,
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
                SectionHeader(label = "Yours", count = yours.size) {
                    SegmentedToggle(
                    options = listOf("LAST USED", "A\u2013Z"),
                    selected = if (sort == LibrarySort.LastUsed) 0 else 1,
                    onSelect = {
                        onSortChange(if (it == 0) LibrarySort.LastUsed else LibrarySort.Alphabetical)
                    },
                )
                }
            }
            items(shown, key = { it.id }) { card ->
                Plate(card, onClick = { actions.onRun(card.id) })
            }
            if (yours.size > COLLAPSED_PLATES) {
                item { ShowAll(expanded, yours.size) { onExpandedChange(!expanded) } }
            }
            item { SectionHeader(label = "Catalogue", count = catalogue.size.takeIf { it > 0 }) }
            when (catalogueState) {
                is StoreState.Loading -> item { CatalogueNote("Loading…") }
                is StoreState.Failed -> item { CatalogueNote("Could not reach the store.") }
                is StoreState.Loaded -> items(catalogue, key = { it.id }) { entry ->
                    CatalogueRow(entry, actions)
                }
            }
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
            placeholder = "Search $total entries",
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
private fun Plate(card: ConfigurationCard, onClick: () -> Unit) {
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
                    Text("CUSTOM", style = Trs80Theme.type.kickerSmall, color = Color.White)
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
                    .padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 7.dp),
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
            if (expanded) "Show fewer" else "Show all $total",
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
        // Always the same position: download, then progress, then play.
        when {
            entry.installing -> ProgressRing(progress = null)
            entry.installed -> StrokeIcon(Trs80Icon.Play, color = colors.accentText, size = 17.dp)
            else -> StrokeIcon(
                Trs80Icon.Download,
                color = colors.accentText,
                size = 17.dp,
                onClick = { actions.onInstall(entry) },
            )
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

/** Scanlines over the glass, as the spec draws them. */
private fun Modifier.scanlines(): Modifier = drawWithContent {
    drawContent()
    var y = 0f
    while (y < size.height) {
        drawRect(
            color = Color.Black.copy(alpha = 0.22f),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(size.width, 1f),
        )
        y += 3f
    }
}
