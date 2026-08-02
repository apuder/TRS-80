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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import trs_80.shared.generated.resources.delete_entry
import trs_80.shared.generated.resources.done
import trs_80.shared.generated.resources.duplicate
import trs_80.shared.generated.resources.duplicate_detail
import trs_80.shared.generated.resources.edit_entry
import trs_80.shared.generated.resources.edit_entry_detail
import trs_80.shared.generated.resources.loading
import trs_80.shared.generated.resources.search_entries
import trs_80.shared.generated.resources.share_state
import trs_80.shared.generated.resources.share_state_detail
import trs_80.shared.generated.resources.stop
import trs_80.shared.generated.resources.stop_detail
import trs_80.shared.generated.resources.show_all
import trs_80.shared.generated.resources.show_fewer
import trs_80.shared.generated.resources.sort_alphabetical
import trs_80.shared.generated.resources.sort_last_used
import trs_80.shared.generated.resources.store_unreachable
import trs_80.shared.generated.resources.yours
import org.puder.trs80.shared.ui.theme.DestructiveButton
import org.puder.trs80.shared.ui.theme.Hairline
import org.puder.trs80.shared.ui.theme.MinimumTouchTarget
import org.puder.trs80.shared.ui.theme.ModalPanel
import org.puder.trs80.shared.ui.theme.ProgressRing
import org.puder.trs80.shared.ui.theme.SearchField
import org.puder.trs80.shared.ui.theme.SectionKicker
import org.puder.trs80.shared.ui.theme.SegmentedToggle
import org.puder.trs80.shared.ui.theme.SettingRow
import org.puder.trs80.shared.ui.theme.StrokeIcon
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.TextAction
import org.puder.trs80.shared.ui.theme.Trs80Icon
import org.puder.trs80.shared.ui.theme.Trs80Theme
import org.puder.trs80.shared.ui.theme.scanlines

/** How many of the user's own machines the library shows before it offers the rest. */
/**
 * How large the control at the end of a catalog row is drawn.
 *
 * Bigger than the icons elsewhere: this is the row's one action, and at the
 * size the rest of the set uses it reads as a status mark rather than a thing
 * to press.
 */
private val ROW_CONTROL = 24.dp

/** One turn of the refresh control while the store is being asked. */
private const val SPIN_MILLIS = 900

private const val COLLAPSED_PLATES = 3

/** How long the search field takes to arrive; short enough not to be waited on. */
private const val SEARCH_MILLIS = 140

/**
 * How wide the list stands once there is a pane beside it.
 *
 * Fixed rather than proportional: the rows were drawn for a phone and they are
 * the same rows here. Letting the column grow with the window would stretch a
 * catalog row until its title and its play control were at opposite ends of a
 * tablet with nothing in between.
 */
private val LIST_COLUMN = 380.dp

/** The accent stripe and wash that say which row the pane is showing. */
private val SELECTED_EDGE = 2.dp
private const val SELECTED_WASH = 0.12f

/** One of the user's own machines made from a catalog entry. */
data class CatalogVersion(val id: Int, val name: String, val model: String)

/** One entry in the catalog, and what the app can do with it. */
data class CatalogEntry(
    val id: String,
    val title: String,
    val author: String,
    val year: Int,
    val artUrl: String?,
    /**
     * The unedited machine made from this entry, or null if there is none.
     *
     * What Play starts, and what Play creates when it is missing — which is what
     * makes "play this program" always mean the program as the catalog has it,
     * however much the user has since tinkered.
     */
    val cleanId: Int? = null,
    /** The user's own machines made from this entry, most recently used first. */
    val versions: List<CatalogVersion> = emptyList(),
    /** Being downloaded now. */
    val installing: Boolean = false,
    /** The last attempt to download it failed. */
    val failed: Boolean = false,
)

/** What the library can be asked to do. */
data class LibraryActions(
    val onRun: (Int) -> Unit = {},
    val onOpenEntry: (CatalogEntry) -> Unit = {},
    /**
     * Starts a catalog entry, downloading it first if this is the first time.
     *
     * One action rather than two, because downloading is not something the user
     * wants -- it is what standing between them and playing, and it takes a
     * moment.
     */
    val onPlayEntry: (CatalogEntry) -> Unit = {},
    val onAdd: (() -> Unit)? = null,
    /** Opens the editor for one of the user's machines, from its overflow. */
    val onEdit: ((Int) -> Unit)? = null,
    /** Copies one of the user's machines, from its overflow. */
    val onDuplicate: ((Int) -> Unit)? = null,
    /** Deletes one of the user's machines, once the user has confirmed it. */
    val onDelete: ((Int) -> Unit)? = null,
    /**
     * Throws away a machine's paused session, once the user has confirmed it.
     *
     * Offered only for a machine that has one. This is Android's Stop: the
     * machine is not running in the background -- leaving it wrote its state
     * out -- so what there is to stop is the session waiting to be resumed.
     */
    val onStop: ((Int) -> Unit)? = null,
    /** Uploads a machine's TRS-Xray state to the store. Experimental. */
    val onShare: ((Int) -> Unit)? = null,
    /** Asks the store for the catalog again. */
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
 * as plates, capped so that the catalog always starts above the fold.
 */
@Composable
fun LibraryScreen(
    yours: List<ConfigurationCard>,
    catalog: List<CatalogEntry>,
    catalogState: StoreState,
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
    /** Which catalog row is showing in the pane, so the list can mark it. */
    selectedId: String? = null,
    /**
     * What to draw beside the list where there is room for it.
     *
     * Null on a phone, and on any window too small to split -- there the entry
     * arrives as a sheet over the list instead, which is the caller's business.
     */
    pane: (@Composable () -> Unit)? = null,
) {
    val colors = Trs80Theme.colors
    val spacing = Trs80Theme.spacing
    val shown = if (expanded) yours else yours.take(COLLAPSED_PLATES)
    // Which machine's overflow is open. Held here rather than in the plate so
    // the panel is laid out against the whole screen: a modal opened from inside
    // a list item is scrimmed to that item and nothing else.
    var menuFor by remember { mutableStateOf<ConfigurationCard?>(null) }
    // Whether the menu has given way to the question. Cancelling puts the menu
    // back, since that is where the user was.
    var confirmingDelete by remember { mutableStateOf(false) }
    // Same shape as the delete question, and asked for the same reason: what it
    // throws away cannot be got back.
    var confirmingStop by remember { mutableStateOf(false) }
    // Whether the search field has been asked for. View state, and this screen's
    // own: what is being searched for belongs to the host, but whether there is
    // a field on screen does not outlive the screen.
    //
    // A field is also shown for a query that is already set, whoever set it --
    // a list quietly filtered by something the user cannot see is worse than the
    // space the field takes.
    var searchAsked by rememberSaveable { mutableStateOf(false) }
    val searching = searchAsked || query.isNotEmpty()
    val hasMenu = actions.onEdit != null || actions.onDuplicate != null ||
        actions.onDelete != null || actions.onStop != null || actions.onShare != null

    // The pane is only offered where there is room for it; the caller decides
    // what goes in it, because what an entry says is the host's business and
    // not this screen's.
    val twoPane = pane != null && isWideLayout()

    val list: @Composable (Modifier) -> Unit = { listModifier ->
        Column(listModifier) {
            LibraryTopBar(
                actions = actions,
                searching = searching,
                onToggleSearch = {
                    // Closing clears, so that the list is never filtered by
                    // something with nothing on screen to explain it.
                    if (searching) {
                        onQueryChange("")
                    }
                    searchAsked = !searching
                },
            )
            AnimatedVisibility(
                visible = searching,
                enter = expandVertically(tween(SEARCH_MILLIS)),
                exit = shrinkVertically(tween(SEARCH_MILLIS)),
            ) {
                SearchRow(
                    query = query,
                    onQueryChange = onQueryChange,
                    total = yours.size + catalog.size,
                    focusOnAppear = searchAsked,
                )
            }

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
                        onMenu = if (hasMenu) {
                            { menuFor = card; confirmingDelete = false; confirmingStop = false }
                        } else {
                            null
                        },
                    )
                }
                if (yours.size > COLLAPSED_PLATES) {
                    item { ShowAll(expanded, yours.size) { onExpandedChange(!expanded) } }
                }
                item {
                    SectionHeader(
                        label = stringResource(Res.string.catalog),
                        count = catalog.size.takeIf { it > 0 },
                        trailing = actions.onRefresh?.let {
                            { RefreshControl(refreshing = refreshing, onClick = it) }
                        },
                    )
                }
                when (catalogState) {
                    is StoreState.Loading -> item { CatalogNote(stringResource(Res.string.loading)) }
                    is StoreState.Failed -> item { CatalogNote(stringResource(Res.string.store_unreachable)) }
                    is StoreState.Loaded -> items(catalog, key = { it.id }) { entry ->
                        CatalogRow(entry, actions, selected = entry.id == selectedId)
                    }
                }
            }
        }
    }

    Box(modifier.fillMaxSize().background(colors.ground)) {
    if (twoPane) {
        Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            list(Modifier.width(LIST_COLUMN).fillMaxHeight())
            Box(
                Modifier
                    .width(spacing.hairline)
                    .fillMaxHeight()
                    .background(colors.hairline)
            )
            Box(Modifier.weight(1f).fillMaxHeight()) { pane!!() }
        }
    } else {
        // The ground runs edge to edge; the content does not. Without this
        // the wordmark sits under the status bar.
        list(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing))
    }

        menuFor?.let { card ->
            // One panel at a time: the question replaces the menu rather than
            // stacking on it, so there is never a scrim over a scrim.
            if (confirmingStop) {
                ConfirmStop(
                    name = card.name,
                    onCancel = { confirmingStop = false },
                    onConfirm = {
                        confirmingStop = false
                        menuFor = null
                        actions.onStop?.invoke(card.id)
                    },
                )
            } else if (confirmingDelete) {
                ConfirmDelete(
                    name = card.name,
                    onCancel = { confirmingDelete = false },
                    onConfirm = {
                        confirmingDelete = false
                        menuFor = null
                        actions.onDelete?.invoke(card.id)
                    },
                )
            } else {
                PlateMenu(
                    card = card,
                    actions = actions,
                    onDelete = { confirmingDelete = true },
                    onStop = { confirmingStop = true },
                    onDismiss = { menuFor = null },
                )
            }
        }
    }
}

/**
 * What can be done to one of the user's machines.
 *
 * Copying lives here rather than on the catalog entry, because a copy is made
 * of a machine and not of a program: the thing worth copying is the one that has
 * been set up the way the user wants it.
 */
@Composable
private fun PlateMenu(
    card: ConfigurationCard,
    actions: LibraryActions,
    onDelete: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalPanel(onDismiss = onDismiss) {
        SectionKicker(card.name)
        actions.onEdit?.let { edit ->
            SettingRow(
                label = stringResource(Res.string.edit_entry),
                subtitle = stringResource(Res.string.edit_entry_detail),
                onClick = { onDismiss(); edit(card.id) },
            )
            Hairline()
        }
        actions.onDuplicate?.let { duplicate ->
            SettingRow(
                label = stringResource(Res.string.duplicate),
                subtitle = stringResource(Res.string.duplicate_detail),
                onClick = { onDismiss(); duplicate(card.id) },
            )
            Hairline()
        }
        // Only for a machine with a session waiting: there is nothing to stop
        // otherwise, and a control that does nothing is worse than none.
        if (actions.onStop != null && card.hasSavedState) {
            SettingRow(
                label = stringResource(Res.string.stop),
                subtitle = stringResource(Res.string.stop_detail),
                onClick = { onDismiss(); onStop() },
            )
            Hairline()
        }
        // Likewise the X-ray dump, which only exists once the machine has run.
        if (actions.onShare != null && card.hasXrayState) {
            SettingRow(
                label = stringResource(Res.string.share_state),
                subtitle = stringResource(Res.string.share_state_detail),
                onClick = { onDismiss(); actions.onShare.invoke(card.id) },
            )
            Hairline()
        }
        if (actions.onDelete != null) {
            // Red, and the one control here that is not a row: deleting takes
            // the machine's disks and its saved state with it, so it should not
            // look like the things either side of it.
            Spacer(Modifier.height(14.dp))
            DestructiveButton(
                stringResource(Res.string.delete_entry),
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            TextAction(stringResource(Res.string.done), onClick = onDismiss, padding = 0.dp)
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
private fun LibraryTopBar(
    actions: LibraryActions,
    searching: Boolean,
    onToggleSearch: () -> Unit,
) {
    val colors = Trs80Theme.colors
    val spacing = Trs80Theme.spacing
    // No rule under it, and little space around it. The bar is the app's name
    // and three icons over a plain ground; a line beneath draws more attention
    // to the join than the join deserves, and the height it all takes is height
    // the library does not have for machines.
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = spacing.screenEdge, end = spacing.screenEdge, top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("TRS-80", style = Trs80Theme.type.wordmark, color = colors.text)
        Spacer(Modifier.weight(1f))
        // Lit while the field is open, so the icon says which of its two states
        // it is in rather than only what it does.
        StrokeIcon(
            Trs80Icon.Search,
            color = if (searching) colors.accentText else colors.muted,
            onClick = onToggleSearch,
        )
        Spacer(Modifier.width(14.dp))
        actions.onAdd?.let {
            StrokeIcon(Trs80Icon.Plus, color = colors.muted, onClick = it)
            Spacer(Modifier.width(14.dp))
        }
        actions.onOpenSettings?.let {
            StrokeIcon(Trs80Icon.Settings, color = colors.muted, onClick = it)
        }
    }
}

@Composable
private fun SearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    total: Int,
    focusOnAppear: Boolean,
) {
    val spacing = Trs80Theme.spacing
    Box(Modifier.fillMaxWidth().padding(horizontal = spacing.screenEdge, vertical = 12.dp)) {
        SearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(Res.string.search_entries, total),
            modifier = Modifier.fillMaxWidth(),
            focusOnAppear = focusOnAppear,
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
private fun Plate(card: ConfigurationCard, onClick: () -> Unit, onMenu: (() -> Unit)?) {
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
                // target centers against it, rather than the target's height
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
                if (onMenu != null) {
                    StrokeIcon(
                        Trs80Icon.Overflow,
                        color = Color.White.copy(alpha = 0.72f),
                        size = 17.dp,
                        onClick = onMenu,
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
private fun CatalogRow(
    entry: CatalogEntry,
    actions: LibraryActions,
    selected: Boolean = false,
) {
    val colors = Trs80Theme.colors
    val spacing = Trs80Theme.spacing
    val accentEdge = colors.accent
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { actions.onOpenEntry(entry) }
            // Marked only where a pane exists to show what is selected. On a
            // phone the sheet is the mark, and a row left highlighted under it
            // would still be highlighted after it closed.
            .then(
                if (selected) {
                    Modifier
                        .background(colors.accent.copy(alpha = SELECTED_WASH))
                        .drawBehind {
                            drawRect(
                                color = accentEdge,
                                size = Size(SELECTED_EDGE.toPx(), size.height),
                            )
                        }
                } else {
                    Modifier
                }
            )
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
        // One reserved slot for either state, so the glyph lands in the same
        // place whatever it is. A tappable icon carries a touch target the ring
        // does not; left to size themselves they would sit at different
        // distances from the edge.
        Box(Modifier.size(MinimumTouchTarget), contentAlignment = Alignment.Center) {
            if (entry.installing) {
                ProgressRing(progress = null, size = ROW_CONTROL)
            } else {
                // Play whether or not it is on the device. Downloading is a
                // step on the way, not a thing to ask for: it takes a moment and
                // nobody wants the file, they want the program.
                StrokeIcon(
                    Trs80Icon.Play,
                    color = colors.accentText,
                    size = ROW_CONTROL,
                    onClick = { actions.onPlayEntry(entry) },
                )
            }
        }
    }
    Divider()
}

@Composable
private fun CatalogNote(text: String) {
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

