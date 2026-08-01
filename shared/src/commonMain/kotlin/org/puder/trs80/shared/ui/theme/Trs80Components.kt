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

package org.puder.trs80.shared.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp

/**
 * The strokes the app draws instead of an icon pack.
 *
 * There are six of them and they are all a line or two, so a dependency would
 * cost more than it saves — and the spec's rule that the accent is stroke only
 * makes drawing them directly the natural fit rather than a workaround.
 */
enum class Trs80Icon { Plus, Overflow, Search, Download, Play, Stop, Settings }

/**
 * One of [Trs80Icon], stroked in [color].
 *
 * A tappable icon gets a touch target of at least [MinimumTouchTarget]
 * regardless of how large the glyph is drawn. The spec's icons are 17-19px,
 * which is a fine size to look at and far too small to hit.
 */
@Composable
fun StrokeIcon(
    icon: Trs80Icon,
    modifier: Modifier = Modifier,
    color: Color = Trs80Theme.colors.text,
    size: androidx.compose.ui.unit.Dp = 19.dp,
    onClick: (() -> Unit)? = null,
) {
    if (onClick == null) {
        Canvas(modifier.size(size)) { drawIcon(icon, color) }
        return
    }
    Box(
        modifier
            .size(MinimumTouchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) { drawIcon(icon, color) }
    }
}

/** The smallest thing a finger should be asked to hit. */
val MinimumTouchTarget = 44.dp

private fun DrawScope.drawIcon(icon: Trs80Icon, color: Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = w * 0.075f)
    when (icon) {
        Trs80Icon.Plus -> {
            drawLine(color, Offset(w / 2, h * 0.2f), Offset(w / 2, h * 0.8f), stroke.width)
            drawLine(color, Offset(w * 0.2f, h / 2), Offset(w * 0.8f, h / 2), stroke.width)
        }

        Trs80Icon.Overflow -> {
            val r = w * 0.05f
            for (y in listOf(0.22f, 0.5f, 0.78f)) {
                drawCircle(color, r, Offset(w / 2, h * y))
            }
        }

        Trs80Icon.Search -> {
            drawCircle(color, w * 0.29f, Offset(w * 0.45f, h * 0.45f), style = stroke)
            drawLine(color, Offset(w * 0.66f, h * 0.66f), Offset(w * 0.85f, h * 0.85f), stroke.width)
        }

        Trs80Icon.Download -> {
            drawLine(color, Offset(w / 2, h * 0.14f), Offset(w / 2, h * 0.62f), stroke.width)
            drawLine(color, Offset(w * 0.29f, h * 0.45f), Offset(w / 2, h * 0.66f), stroke.width)
            drawLine(color, Offset(w * 0.71f, h * 0.45f), Offset(w / 2, h * 0.66f), stroke.width)
            drawLine(color, Offset(w * 0.2f, h * 0.87f), Offset(w * 0.8f, h * 0.87f), stroke.width)
        }

        Trs80Icon.Play -> {
            // A ring rather than a filled disc: the accent never fills.
            drawCircle(color, w * 0.42f, Offset(w / 2, h / 2), style = stroke)
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.42f, h * 0.34f)
                lineTo(w * 0.68f, h * 0.5f)
                lineTo(w * 0.42f, h * 0.66f)
                close()
            }
            drawPath(path, color)
        }

        Trs80Icon.Settings -> {
            // A gear as a ring with teeth: filling it would break the spec's
            // stroke-only rule, and an outline reads at this size.
            drawCircle(color, w * 0.19f, Offset(w / 2, h / 2), style = stroke)
            val teeth = 8
            repeat(teeth) { i ->
                val angle = (i * 2f * 3.14159f / teeth)
                val inner = w * 0.29f
                val outer = w * 0.42f
                val cx = w / 2
                val cy = h / 2
                drawLine(
                    color,
                    Offset(cx + inner * kotlin.math.cos(angle), cy + inner * kotlin.math.sin(angle)),
                    Offset(cx + outer * kotlin.math.cos(angle), cy + outer * kotlin.math.sin(angle)),
                    stroke.width,
                )
            }
        }

        Trs80Icon.Stop -> {
            drawCircle(color, w * 0.42f, Offset(w / 2, h / 2), style = stroke)
            drawRect(
                color,
                topLeft = Offset(w * 0.38f, h * 0.38f),
                size = Size(w * 0.24f, h * 0.24f),
            )
        }
    }
}

/**
 * A ring showing how far a download has got.
 *
 * @param progress 0..1, or null while the size is not yet known, in which case
 * the ring is drawn whole and faint — honest about not knowing rather than
 * animating a guess.
 */
@Composable
fun ProgressRing(
    progress: Float?,
    modifier: Modifier = Modifier,
    color: Color = Trs80Theme.colors.accentText,
    size: androidx.compose.ui.unit.Dp = 19.dp,
) {
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = this.size.width * 0.09f)
        drawCircle(
            color.copy(alpha = 0.25f),
            this.size.width * 0.42f,
            Offset(this.size.width / 2, this.size.height / 2),
            style = stroke,
        )
        if (progress != null) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(this.size.width * 0.08f, this.size.height * 0.08f),
                size = Size(this.size.width * 0.84f, this.size.height * 0.84f),
                style = stroke,
            )
        }
    }
}

/** The search field: a hairline box, an accent magnifier, and plain text. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = Trs80Theme.colors
    Row(
        modifier
            .background(colors.field)
            .border(Trs80Theme.spacing.hairline, colors.text.copy(alpha = 0.2f))
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StrokeIcon(Trs80Icon.Search, color = colors.accentText, size = 15.dp)
        Spacer(Modifier.width(9.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, style = Trs80Theme.type.body, color = colors.text.copy(alpha = 0.45f))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = Trs80Theme.type.body.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
            )
        }
    }
}

/**
 * A two-option segmented control, stroked.
 *
 * @param options label per option; [selected] is an index into it.
 */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Trs80Theme.colors
    Row(modifier.border(Trs80Theme.spacing.hairline, colors.text.copy(alpha = 0.18f))) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier
                    .clickable { onSelect(index) }
                    .background(
                        if (isSelected) colors.accent.copy(alpha = 0.16f) else Color.Transparent
                    )
                    .padding(horizontal = 7.dp, vertical = 5.dp),
            ) {
                Text(
                    label,
                    style = Trs80Theme.type.kickerSmall,
                    color = if (isSelected) colors.accentText else colors.muted,
                )
            }
        }
    }
}

/**
 * Text, in the app's own type scale.
 *
 * A thin wrapper so screens never reach for Material's typography by accident —
 * the two scales look close enough that a slip would not be obvious, and this
 * is the design system's front door.
 */
@Composable
fun Text(
    text: String,
    style: androidx.compose.ui.text.TextStyle = Trs80Theme.type.body,
    color: Color = Trs80Theme.colors.text,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.text.BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(SpanStyle(color = color)),
        maxLines = maxLines,
        overflow = overflow,
    )
}
