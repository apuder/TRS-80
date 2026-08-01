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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The strokes the app draws instead of an icon pack.
 *
 * There are six of them and they are all a line or two, so a dependency would
 * cost more than it saves — and the spec's rule that the accent is stroke only
 * makes drawing them directly the natural fit rather than a workaround.
 */
enum class Trs80Icon {
    Plus, Overflow, Search, Download, Play, Stop, Settings,
    Trash, Eject, ChevronLeft, ChevronRight, DragHandle, Info, Copy, Refresh,
}

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

/**
 * A word that is tapped rather than read — Back, Cancel, Show all.
 *
 * Carries the same [MinimumTouchTarget] rule as [StrokeIcon], and for the same
 * reason: the type scale is sized to be read, so a word set in it is about 11pt
 * tall. Hanging a `clickable` straight on the text gives a target that thin, and
 * a thumb aimed squarely at the word misses it more often than not.
 *
 * [padding] is horizontal breathing room inside the target. Screens that want
 * the label to sit flush with the screen edge should subtract it from their own
 * padding rather than remove it here.
 */
@Composable
fun TextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Trs80Theme.colors.accentText,
    style: androidx.compose.ui.text.TextStyle = Trs80Theme.type.body,
    padding: androidx.compose.ui.unit.Dp = 10.dp,
) {
    Box(
        modifier
            .heightIn(min = MinimumTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = padding),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = style, color = color)
    }
}

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
            // One continuous cog outline: the teeth are part of the silhouette
            // rather than spokes radiating from a hub, which is what separates a
            // gear from a sun. Six teeth rather than eight, cut deep — at 19dp
            // the gaps have to be wide enough to survive being drawn.
            val cx = w / 2
            val cy = h / 2
            val outer = w * 0.47f
            val root = w * 0.32f
            val teeth = 6
            val step = (2.0 * PI / teeth).toFloat()
            // Fractions of a tooth's turn spent on the tip, the falling flank,
            // the valley, and the rising flank.
            val tip = 0.30f
            val valleyStart = 0.45f
            val valleyEnd = 0.85f
            val path = androidx.compose.ui.graphics.Path()
            repeat(teeth) { i ->
                val base = i * step
                listOf(0f to outer, tip to outer, valleyStart to root, valleyEnd to root)
                    .forEach { (fraction, radius) ->
                        val angle = base + fraction * step
                        val x = cx + radius * kotlin.math.cos(angle)
                        val y = cy + radius * kotlin.math.sin(angle)
                        if (i == 0 && fraction == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                    }
            }
            path.close()
            drawPath(path, color, style = stroke)
            drawCircle(color, w * 0.14f, Offset(cx, cy), style = stroke)
        }

        Trs80Icon.Trash -> {
            // Lid, then body: the outline alone reads as a bin at this size,
            // and the spec keeps destructive controls in text colour.
            drawLine(color, Offset(w * 0.2f, h * 0.26f), Offset(w * 0.8f, h * 0.26f), stroke.width)
            drawLine(color, Offset(w * 0.41f, h * 0.26f), Offset(w * 0.41f, h * 0.17f), stroke.width)
            drawLine(color, Offset(w * 0.59f, h * 0.26f), Offset(w * 0.59f, h * 0.17f), stroke.width)
            drawLine(color, Offset(w * 0.41f, h * 0.17f), Offset(w * 0.59f, h * 0.17f), stroke.width)
            drawLine(color, Offset(w * 0.28f, h * 0.26f), Offset(w * 0.33f, h * 0.83f), stroke.width)
            drawLine(color, Offset(w * 0.72f, h * 0.26f), Offset(w * 0.67f, h * 0.83f), stroke.width)
            drawLine(color, Offset(w * 0.33f, h * 0.83f), Offset(w * 0.67f, h * 0.83f), stroke.width)
        }

        Trs80Icon.Eject -> {
            drawLine(color, Offset(w / 2, h * 0.62f), Offset(w / 2, h * 0.16f), stroke.width)
            drawLine(color, Offset(w * 0.29f, h * 0.37f), Offset(w / 2, h * 0.16f), stroke.width)
            drawLine(color, Offset(w * 0.71f, h * 0.37f), Offset(w / 2, h * 0.16f), stroke.width)
            drawLine(color, Offset(w * 0.2f, h * 0.84f), Offset(w * 0.8f, h * 0.84f), stroke.width)
        }

        Trs80Icon.ChevronLeft -> {
            drawLine(color, Offset(w * 0.62f, h * 0.2f), Offset(w * 0.36f, h * 0.5f), stroke.width)
            drawLine(color, Offset(w * 0.36f, h * 0.5f), Offset(w * 0.62f, h * 0.8f), stroke.width)
        }

        Trs80Icon.ChevronRight -> {
            drawLine(color, Offset(w * 0.38f, h * 0.2f), Offset(w * 0.64f, h * 0.5f), stroke.width)
            drawLine(color, Offset(w * 0.64f, h * 0.5f), Offset(w * 0.38f, h * 0.8f), stroke.width)
        }

        Trs80Icon.DragHandle -> {
            drawLine(color, Offset(w * 0.24f, h * 0.4f), Offset(w * 0.76f, h * 0.4f), stroke.width)
            drawLine(color, Offset(w * 0.24f, h * 0.6f), Offset(w * 0.76f, h * 0.6f), stroke.width)
        }

        Trs80Icon.Info -> {
            drawCircle(color, w * 0.42f, Offset(w / 2, h / 2), style = stroke)
            drawLine(color, Offset(w / 2, h * 0.44f), Offset(w / 2, h * 0.7f), stroke.width)
            drawCircle(color, w * 0.055f, Offset(w / 2, h * 0.31f))
        }

        Trs80Icon.Copy -> {
            // Two sheets, the back one showing at the corner.
            drawRect(
                color,
                topLeft = Offset(w * 0.16f, h * 0.16f),
                size = Size(w * 0.5f, h * 0.5f),
                style = stroke,
            )
            drawRect(
                color,
                topLeft = Offset(w * 0.34f, h * 0.34f),
                size = Size(w * 0.5f, h * 0.5f),
                style = stroke,
            )
        }

        Trs80Icon.Refresh -> {
            // The conventional circular arrow: a nearly closed ring with a
            // solid head at the end of the stroke. The head is a filled
            // triangle rather than two barbs, which is what makes it read as
            // the icon everyone already knows -- and Play sets the precedent
            // for a solid triangle sitting on a stroked arc.
            val cx = w / 2
            val cy = h / 2
            val radius = w * 0.33f
            val tail = 350f
            val sweep = 310f
            drawArc(
                color = color,
                startAngle = tail,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2),
                style = stroke,
            )
            val head = (tail + sweep) * PI.toFloat() / 180f
            val onArc = Offset(cx + radius * cos(head), cy + radius * sin(head))
            // Clockwise tangent, which is the way the stroke was travelling.
            val along = Offset(-sin(head), cos(head))
            // Radially outward, which is how the base straddles the stroke.
            val across = Offset(cos(head), sin(head))
            val reach = w * 0.23f
            val halfBase = w * 0.125f
            drawPath(
                androidx.compose.ui.graphics.Path().apply {
                    moveTo(onArc.x + along.x * reach, onArc.y + along.y * reach)
                    lineTo(onArc.x + across.x * halfBase, onArc.y + across.y * halfBase)
                    lineTo(onArc.x - across.x * halfBase, onArc.y - across.y * halfBase)
                    close()
                },
                color,
            )
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
    val focus = remember { FocusRequester() }
    Row(
        modifier
            .background(colors.field)
            .border(Trs80Theme.spacing.hairline, colors.text.copy(alpha = 0.2f))
            // A text field is only as tall as its line of text, so the box drawn
            // around it — border, padding, the space beside the icon — is not
            // part of it and a tap there did nothing. It looked like the target
            // without being one, which reads as a field that cannot be typed in.
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { focus.requestFocus() }
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
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
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
    fill: Boolean = false,
) {
    val colors = Trs80Theme.colors
    Row(modifier.border(Trs80Theme.spacing.hairline, colors.text.copy(alpha = 0.18f))) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier
                    // Sized to their content by default, since most of these sit
                    // at the right-hand end of a row; a control given the full
                    // width shares it out instead.
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .clickable { onSelect(index) }
                    .background(
                        if (isSelected) colors.accent.copy(alpha = 0.16f) else Color.Transparent
                    )
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
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

/** A hairline rule, the app's only divider. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(Trs80Theme.spacing.hairline)
            .background(Trs80Theme.colors.hairline)
    )
}

/**
 * A section heading: the kicker, an optional count, and a hairline running out
 * to the right edge.
 *
 * @param count shown next to the label in muted text — the spec uses it for the
 * number of disks, where the heading is also the tally.
 */
@Composable
fun SectionKicker(
    label: String,
    modifier: Modifier = Modifier,
    count: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Trs80Theme.colors
    Row(
        modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(), style = Trs80Theme.type.kicker, color = colors.accentText)
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(count, style = Trs80Theme.type.kickerSmall, color = colors.muted)
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(Trs80Theme.spacing.hairline).background(colors.hairline))
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/**
 * A settings row: label on the left, value on the right.
 *
 * The spec is firm that controls never get their own card, so this draws no
 * background — only the row's own hairline, which the caller opts into. Rows
 * that lead somewhere pass [onClick] and get a chevron.
 */
@Composable
fun SettingRow(
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    value: (@Composable () -> Unit)? = null,
) {
    val colors = Trs80Theme.colors
    val clickable = if (onClick != null) {
        modifier.heightIn(min = MinimumTouchTarget).clickable(onClick = onClick)
    } else {
        modifier
    }
    Row(
        clickable.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Trs80Theme.type.body, color = colors.text)
            if (subtitle != null) {
                Text(subtitle, style = Trs80Theme.type.bodySmall, color = colors.muted)
            }
        }
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            value()
        }
        if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            StrokeIcon(Trs80Icon.ChevronRight, color = colors.muted, size = 15.dp)
        }
    }
}

/**
 * A switch.
 *
 * Off is a hairline track; on is an accent-tinted track with a solid accent
 * knob. That solid knob is the one place the accent fills, which is why the
 * track stays at a tint rather than matching it.
 */
@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Trs80Theme.colors
    Box(
        modifier
            .size(MinimumTouchTarget)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 42.dp, height = 25.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (checked) colors.accent.copy(alpha = 0.16f) else Color.Transparent)
                .border(
                    Trs80Theme.spacing.hairline,
                    if (checked) colors.accent else colors.text.copy(alpha = 0.22f),
                    RoundedCornerShape(percent = 50),
                )
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .size(19.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (checked) colors.accent else colors.muted)
            )
        }
    }
}

/** A single-line text field: a hairline box and nothing else. */
@Composable
fun Trs80TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val colors = Trs80Theme.colors
    val focus = remember { FocusRequester() }
    Box(
        modifier
            .fillMaxWidth()
            .background(colors.field)
            .border(Trs80Theme.spacing.hairline, colors.text.copy(alpha = 0.2f))
            // As in SearchField: the drawn box has to be the target too.
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { focus.requestFocus() }
            .padding(horizontal = 11.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = Trs80Theme.type.body, color = colors.text.copy(alpha = 0.45f))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = Trs80Theme.type.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
    }
}

/**
 * A destructive control: outlined in [Trs80Colors.danger] with a trash glyph.
 *
 * The visual spec asks for text colour rather than red, on the grounds that red
 * would compete with the accent. Overridden deliberately — deleting a machine
 * takes its disks and its saved state with it, and that is worth the collision.
 *
 * @param filled draws it solid instead, for the confirming half of a pair where
 * the outlined version is the one being confirmed.
 */
@Composable
fun DestructiveButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    icon: Trs80Icon? = Trs80Icon.Trash,
) {
    val colors = Trs80Theme.colors
    val content = if (filled) Color.White else colors.danger
    Row(
        modifier
            .heightIn(min = MinimumTouchTarget)
            .background(if (filled) colors.danger else Color.Transparent)
            .border(Trs80Theme.spacing.hairline, colors.danger)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Trs80Theme.type.body, color = content)
        if (icon != null) {
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            StrokeIcon(icon, color = content, size = 17.dp)
        }
    }
}

/**
 * Scanlines over the glass.
 *
 * Every surface showing what the machine drew gets these — the library's
 * plates and the detail sheet's screens alike — so a picture of a TRS-80
 * screen always reads as one.
 */
fun Modifier.scanlines(): Modifier = drawWithContent {
    drawContent()
    var y = 0f
    while (y < size.height) {
        drawRect(
            color = Color.Black.copy(alpha = 0.22f),
            topLeft = Offset(0f, y),
            size = Size(size.width, 1f),
        )
        y += 3f
    }
}
