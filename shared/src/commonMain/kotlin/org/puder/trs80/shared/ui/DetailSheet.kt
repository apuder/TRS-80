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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import trs_80.shared.generated.resources.Res
import trs_80.shared.generated.resources.download_failed
import trs_80.shared.generated.resources.downloading
import trs_80.shared.generated.resources.from_retrostore
import trs_80.shared.generated.resources.from_retrostore_plain
import trs_80.shared.generated.resources.machine
import trs_80.shared.generated.resources.media
import trs_80.shared.generated.resources.play
import trs_80.shared.generated.resources.play_fresh
import trs_80.shared.generated.resources.record
import trs_80.shared.generated.resources.screens
import trs_80.shared.generated.resources.source
import trs_80.shared.generated.resources.try_again
import trs_80.shared.generated.resources.your_versions
import org.puder.trs80.shared.ui.theme.Hairline
import org.puder.trs80.shared.ui.theme.MinimumTouchTarget
import org.puder.trs80.shared.ui.theme.ProgressRing
import org.puder.trs80.shared.ui.theme.SectionKicker
import org.puder.trs80.shared.ui.theme.StrokeIcon
import org.puder.trs80.shared.ui.theme.Text
import org.puder.trs80.shared.ui.theme.Trs80Icon
import org.puder.trs80.shared.ui.theme.Trs80Theme
import org.puder.trs80.shared.ui.theme.scanlines

/** How much of the library stays visible above the sheet. */
private val LIST_LEFT_SHOWING = 112.dp

/** Rising is slower than falling: arriving is the part worth watching. */
private const val RISE_MILLIS = 300
private const val FALL_MILLIS = 200

/**
 * How wide the record table stands when it sits beside the description.
 *
 * Narrow on purpose: it is label-and-value pairs, and pairs stretched across a
 * tablet put the two halves so far apart that the eye has to travel to pair
 * them up again.
 */
private val RECORD_COLUMN = 236.dp

/** How far down it has to be dragged before letting go dismisses it. */
private const val DISMISS_FRACTION = 0.33f

/** A downward flick past this, in pixels per second, sends it away regardless. */
private const val FLING_AWAY = 900f

/** Everything the sheet draws about one catalog entry. */
data class DetailContent(
    val title: String,
    val author: String,
    val year: Int,
    val coverUrl: String?,
    val screenshotUrls: List<String>,
    val description: String,
    /** The machine it wants, as the record table names it. */
    val machine: String,
    /** "2 disks · 37.5K", or null while it is not yet known. */
    val media: String?,
    val source: String,
)

/**
 * What the sheet's one primary slot is offering.
 *
 * Three states in the same position rather than three controls: at no point are
 * there two things to press, so there is never a question of which one is meant.
 *
 * There is no Download among them. Downloading is what happens on the way to
 * playing something for the first time, and it is quick — so the slot says what
 * the user came for and shows the wait when there is one. [Failed] is the reason
 * this is still three states and not a boolean: a Play that silently does
 * nothing is worse than a Download that visibly fails, because there is no way
 * to tell it from a missed tap.
 */
enum class DetailAction { Play, Downloading, Failed }

/**
 * A catalog entry, as a sheet over the library.
 *
 * A sheet rather than a screen because the entry is a thing you look into and
 * come back from, not somewhere you go — the list stays visible behind it and
 * is what dismissing returns you to.
 *
 * The controls section the visual spec puts between Screens and Record is
 * deliberately absent: it edits the entry, and editing belongs in the editor.
 *
 * Copying is absent for the same kind of reason. A copy is made of a machine,
 * not of a program, so it belongs on the machine — in the library, where the
 * user's own machines live.
 *
 * @param versions the user's own machines made from this entry, most recently
 * used first. Listed rather than folded into the primary control: choosing one
 * of your own variants is a deliberate act, and the user knows which one.
 */
@Composable
fun DetailSheet(
    content: DetailContent,
    action: DetailAction,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    versions: List<CatalogVersion> = emptyList(),
    onPlayVersion: (Int) -> Unit = {},
) {
    val colors = Trs80Theme.colors
    val spacing = Trs80Theme.spacing
    val scope = rememberCoroutineScope()
    var viewing by remember { mutableStateOf<Int?>(null) }
    // 0 is fully up, 1 is fully off the bottom. Held as a fraction of the
    // sheet's own height so nothing here needs to know how tall it ended up.
    val slide = remember { Animatable(1f) }
    var heightPx by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) { slide.animateTo(0f, tween(RISE_MILLIS, easing = LinearOutSlowInEasing)) }

    fun dismiss() {
        scope.launch {
            slide.animateTo(1f, tween(FALL_MILLIS, easing = FastOutLinearInEasing))
            onDismiss()
        }
    }

    /** Puts the sheet where it was let go: away, or back up. */
    suspend fun settle(velocity: Float) {
        if (slide.value > DISMISS_FRACTION || velocity > FLING_AWAY) {
            slide.animateTo(1f, tween(FALL_MILLIS, easing = FastOutLinearInEasing))
            onDismiss()
        } else {
            slide.animateTo(0f, tween(RISE_MILLIS, easing = LinearOutSlowInEasing))
        }
    }

    /**
     * Lets a drag that starts on the sheet's contents move the sheet.
     *
     * The contents scroll, and a scrolling child takes the whole gesture -- which
     * is why dragging the sheet used to work on the handle and nowhere else. This
     * gives the sheet what the list could not use: a drag downwards once the list
     * is already at its top, and a drag upwards while the sheet is still pushed
     * down. Anything the list can use is still the list's.
     */
    val fromContents = remember(heightPx) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Upwards, and the sheet is not home yet: it goes back before the
                // list moves, or the two would slide at once.
                if (available.y >= 0f || slide.value <= 0f) {
                    return Offset.Zero
                }
                return Offset(0f, moveBy(available.y))
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Downwards, and the list had nothing left to give with it.
                if (available.y <= 0f) {
                    return Offset.Zero
                }
                return Offset(0f, moveBy(available.y))
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (slide.value > 0f) {
                    settle(available.y)
                    return available
                }
                return Velocity.Zero
            }

            private fun moveBy(delta: Float): Float {
                val target = (slide.value + delta / heightPx).coerceIn(0f, 1f)
                val taken = (target - slide.value) * heightPx
                scope.launch { slide.snapTo(target) }
                return taken
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        // The scrim reaches to the top of the sheet only; what is above it is
        // the list, still legible, which is the point of a sheet. It fades with
        // the sheet rather than snapping, or the list would flash.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * (1f - slide.value)))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { dismiss() },
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                // A fixed height, not one that follows its content: the entry
                // arrives from the network a moment after the sheet opens, and a
                // sheet sized to its content would leap to its full height
                // partway through rising.
                .fillMaxSize()
                .padding(top = LIST_LEFT_SHOWING)
                .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
                .graphicsLayer { translationY = slide.value * size.height }
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(colors.ground)
                // Dragging anywhere on the sheet pushes it down; let go past a
                // third of the way and it keeps going rather than snapping back.
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            slide.snapTo((slide.value + delta / heightPx).coerceIn(0f, 1f))
                        }
                    },
                    onDragStopped = { velocity -> settle(velocity) },
                )
                .nestedScroll(fromContents)
                // Taps inside the sheet are the sheet's, not the scrim's.
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            GrabHandle { dismiss() }
            EntryDetail(
                content = content,
                action = action,
                onPrimary = onPrimary,
                versions = versions,
                onPlayVersion = onPlayVersion,
                onOpenScreen = { viewing = it },
                modifier = Modifier
                    .weight(1f)
                    // Only the bottom: the sheet's own top edge is its top.
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            )
        }

        // Over the sheet, because at this size the picture is the whole screen.
        viewing?.let { start ->
            ScreensViewer(
                urls = content.screenshotUrls,
                startIndex = start,
                onDismiss = { viewing = null },
            )
        }
    }
}

/**
 * Everything the app knows about one catalog entry, as a scrolling column.
 *
 * Separated from the sheet because it outgrew it: on a wide window the same
 * thing is a permanent pane beside the list rather than a surface that covers
 * it. Nothing here knows which it is in — the difference is entirely the
 * container, which is what keeps one description of an entry rather than two.
 *
 * The screens viewer is the caller's, not this one's. It wants to cover the
 * whole window, and a viewer opened from inside a scrolling column would be
 * clipped to it.
 */
@Composable
fun EntryDetail(
    content: DetailContent,
    action: DetailAction,
    onPrimary: () -> Unit,
    onOpenScreen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    versions: List<CatalogVersion> = emptyList(),
    onPlayVersion: (Int) -> Unit = {},
    twoColumn: Boolean = false,
) {
    val colors = Trs80Theme.colors
    val body: @Composable ColumnScope.() -> Unit = {
        PrimaryAction(action, onPrimary)
        if (versions.isNotEmpty()) {
            SectionKicker(
                stringResource(Res.string.your_versions),
                count = versions.size.toString(),
            )
            versions.forEach { version ->
                VersionRow(version) { onPlayVersion(version.id) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Hairline()
        if (content.description.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(content.description, style = Trs80Theme.type.body, color = colors.text)
        }
        if (content.screenshotUrls.isNotEmpty()) {
            SectionKicker(stringResource(Res.string.screens))
            Screens(content.screenshotUrls, onOpenScreen)
        }
    }
    val record: @Composable ColumnScope.() -> Unit = {
        SectionKicker(stringResource(Res.string.record))
        RecordRow(stringResource(Res.string.machine), content.machine)
        content.media?.let { RecordRow(stringResource(Res.string.media), it) }
        RecordRow(stringResource(Res.string.source), content.source)
    }

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Trs80Theme.spacing.screenEdge),
    ) {
        Masthead(content)
        Spacer(Modifier.height(14.dp))
        if (twoColumn) {
            // The record goes beside what it describes rather than under it.
            // Stacked, it would leave the pane's whole right half empty and the
            // description running to a width nobody can read a line of; this
            // spends the room on the one part that is a table and reads better
            // narrow than wide.
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), content = body)
                Spacer(Modifier.width(28.dp))
                Column(Modifier.width(RECORD_COLUMN), content = record)
            }
        } else {
            body()
            record()
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun GrabHandle(onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .padding(top = 8.dp)
                .size(width = 34.dp, height = 4.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Trs80Theme.colors.text.copy(alpha = 0.22f))
        )
    }
}

@Composable
private fun Masthead(content: DetailContent) {
    val colors = Trs80Theme.colors
    Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        RemoteImage(
            url = content.coverUrl,
            modifier = Modifier
                .size(64.dp)
                .border(Trs80Theme.spacing.hairline, colors.text.copy(alpha = 0.22f))
                .background(colors.crt)
                .scanlines(),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                content.title,
                style = Trs80Theme.type.title,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                listOfNotNull(
                    content.author.takeIf { it.isNotEmpty() },
                    content.year.takeIf { it > 0 }?.toString(),
                ).joinToString(" · "),
                style = Trs80Theme.type.bodySmall,
                color = colors.muted,
            )
            Spacer(Modifier.height(6.dp))
            // A credit, not a link: it states where this came from and is not
            // something to press.
            Text(
                stringResource(Res.string.from_retrostore),
                style = Trs80Theme.type.kickerSmall,
                color = colors.accentText,
            )
        }
    }
}

/**
 * The one thing this screen is for.
 *
 * Full width, because there is nothing beside it to balance against: whatever
 * else the user might do with the entry, they came here to play it.
 */
@Composable
private fun PrimaryAction(action: DetailAction, onPrimary: () -> Unit) {
    val colors = Trs80Theme.colors
    val failed = action == DetailAction.Failed
    val edge = if (failed) colors.danger else colors.accent
    val label = if (failed) colors.danger else colors.accentText
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = MinimumTouchTarget)
            .border(Trs80Theme.spacing.hairline, edge)
            .background(edge.copy(alpha = 0.08f))
            .clickable(onClick = onPrimary)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (action) {
            DetailAction.Downloading -> ProgressRing(progress = null, size = 22.dp)
            DetailAction.Failed -> StrokeIcon(Trs80Icon.Refresh, color = label, size = 22.dp)
            DetailAction.Play -> StrokeIcon(Trs80Icon.Play, color = label, size = 22.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                when (action) {
                    DetailAction.Downloading -> stringResource(Res.string.downloading)
                    DetailAction.Failed -> stringResource(Res.string.try_again)
                    DetailAction.Play -> stringResource(Res.string.play)
                },
                style = Trs80Theme.type.kicker,
                color = label,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                when (action) {
                    // No total to count towards: the store hands over the whole
                    // program in one response, so there is no progress to report
                    // and saying so beats animating a guess.
                    DetailAction.Downloading -> stringResource(Res.string.from_retrostore_plain)
                    DetailAction.Failed -> stringResource(Res.string.download_failed)
                    DetailAction.Play -> stringResource(Res.string.play_fresh)
                },
                style = Trs80Theme.type.bodySmall,
                color = colors.muted,
            )
        }
    }
}

/** One of the user's own machines from this entry, and a way to start it. */
@Composable
private fun VersionRow(version: CatalogVersion, onPlay: () -> Unit) {
    val colors = Trs80Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = MinimumTouchTarget)
            .clickable(onClick = onPlay)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                version.name,
                style = Trs80Theme.type.body,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(version.model, style = Trs80Theme.type.bodySmall, color = colors.muted)
        }
        Spacer(Modifier.width(10.dp))
        StrokeIcon(Trs80Icon.Play, color = colors.accentText, size = 20.dp)
    }
    Hairline()
}

/** The screens, matted like the library's plates so they read as one thing. */
@Composable
private fun Screens(urls: List<String>, onOpen: (Int) -> Unit) {
    val colors = Trs80Theme.colors
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        urls.forEachIndexed { index, url ->
            Box(
                Modifier
                    .padding(end = Trs80Theme.spacing.gap)
                    .border(Trs80Theme.spacing.hairline, colors.text.copy(alpha = 0.2f))
                    .clickable { onOpen(index) }
                    .padding(Trs80Theme.spacing.mat),
            ) {
                RemoteImage(
                    url = url,
                    modifier = Modifier
                        .size(width = 208.dp, height = 88.dp)
                        .background(colors.crt)
                        .scanlines(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    val colors = Trs80Theme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Trs80Theme.type.body, color = colors.muted)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = Trs80Theme.type.body,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Hairline()
}
