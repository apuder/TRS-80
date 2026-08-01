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
import trs_80.shared.generated.resources.copy
import trs_80.shared.generated.resources.download
import trs_80.shared.generated.resources.downloading
import trs_80.shared.generated.resources.from_retrostore
import trs_80.shared.generated.resources.from_retrostore_plain
import trs_80.shared.generated.resources.machine
import trs_80.shared.generated.resources.media
import trs_80.shared.generated.resources.play
import trs_80.shared.generated.resources.record
import trs_80.shared.generated.resources.screens
import trs_80.shared.generated.resources.source
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
 */
enum class DetailAction { Download, Downloading, Play }

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
 */
@Composable
fun DetailSheet(
    content: DetailContent,
    action: DetailAction,
    onPrimary: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
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
                    onDragStopped = { velocity ->
                        if (slide.value > DISMISS_FRACTION || velocity > FLING_AWAY) {
                            slide.animateTo(1f, tween(FALL_MILLIS, easing = FastOutLinearInEasing))
                            onDismiss()
                        } else {
                            slide.animateTo(0f, tween(RISE_MILLIS, easing = LinearOutSlowInEasing))
                        }
                    },
                )
                // Taps inside the sheet are the sheet's, not the scrim's.
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            GrabHandle { dismiss() }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenEdge)
                    // Only the bottom: the sheet's own top edge is its top.
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            ) {
                Masthead(content)
                Spacer(Modifier.height(14.dp))
                Actions(action, onPrimary, onCopy)
                Spacer(Modifier.height(16.dp))
                Hairline()
                if (content.description.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        content.description,
                        style = Trs80Theme.type.body,
                        color = colors.text,
                    )
                }
                if (content.screenshotUrls.isNotEmpty()) {
                    SectionKicker(stringResource(Res.string.screens))
                    Screens(content.screenshotUrls) { viewing = it }
                }
                SectionKicker(stringResource(Res.string.record))
                RecordRow(stringResource(Res.string.machine), content.machine)
                content.media?.let { RecordRow(stringResource(Res.string.media), it) }
                RecordRow(stringResource(Res.string.source), content.source)
                Spacer(Modifier.height(28.dp))
            }
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
 * The primary slot, and the copy beside it.
 *
 * Copy dims until the program is on the device, because there is nothing to
 * copy before that.
 */
@Composable
private fun Actions(action: DetailAction, onPrimary: () -> Unit, onCopy: () -> Unit) {
    val colors = Trs80Theme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = MinimumTouchTarget)
                .border(Trs80Theme.spacing.hairline, colors.accent)
                .background(colors.accent.copy(alpha = 0.08f))
                .clickable(onClick = onPrimary)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (action) {
                DetailAction.Downloading -> ProgressRing(progress = null, size = 22.dp)
                DetailAction.Play -> StrokeIcon(
                    Trs80Icon.Play,
                    color = colors.accentText,
                    size = 22.dp,
                )

                DetailAction.Download -> StrokeIcon(
                    Trs80Icon.Download,
                    color = colors.accentText,
                    size = 22.dp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    when (action) {
                        DetailAction.Download -> stringResource(Res.string.download)
                        DetailAction.Downloading -> stringResource(Res.string.downloading)
                        DetailAction.Play -> stringResource(Res.string.play)
                    },
                    style = Trs80Theme.type.kicker,
                    color = colors.accentText,
                )
                if (action == DetailAction.Downloading) {
                    Spacer(Modifier.height(2.dp))
                    // No total to count towards: the store hands over the whole
                    // program in one response, so there is no progress to report
                    // and saying so beats animating a guess.
                    Text(
                        stringResource(Res.string.from_retrostore_plain),
                        style = Trs80Theme.type.bodySmall,
                        color = colors.muted,
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        val enabled = action == DetailAction.Play
        Row(
            Modifier
                .heightIn(min = MinimumTouchTarget)
                .border(Trs80Theme.spacing.hairline, colors.text.copy(alpha = 0.22f))
                .then(if (enabled) Modifier.clickable(onClick = onCopy) else Modifier)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val alpha = if (enabled) 1f else 0.45f
            StrokeIcon(
                Trs80Icon.Copy,
                color = colors.text.copy(alpha = alpha),
                size = 18.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.copy), style = Trs80Theme.type.kicker, color = colors.text.copy(alpha = alpha))
        }
    }
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
