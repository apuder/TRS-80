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

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Where the light is coming from, as a fraction of a turn from three o'clock,
 * clockwise.
 *
 * An eighth of a turn is 45 degrees: the bottom-right corner. The other peak
 * follows half a turn later at the top-left, so the two brightest points are on
 * one diagonal and the other two corners are dark. Put the peaks on quarters
 * instead and they land in the middle of the edges, which looks like a mistake
 * rather than like light.
 */
private const val FIRST_PEAK = 0.125f
private const val SECOND_PEAK = 0.625f

/**
 * How far the light reaches either side of a peak, as a fraction of a turn.
 *
 * A sixth of a turn is 60 degrees, so each corner is lit across 120 and there
 * is a 60 degree stretch of dark left between them. The iOS original ramps all the way from one
 * corner to the next, which at an app icon's size is a sheen; on a thumbnail it
 * lights most of the perimeter and reads as a frame around the picture rather
 * than as light falling on it.
 */
private const val REACH = 1f / 6f

/**
 * How wide the lit line is.
 *
 * A hairline is what the iOS original uses, and at an app icon's size that is
 * plenty. On a 56dp thumbnail it is a third of a percent of the width and
 * disappears, so this is more. Half of it falls outside the shape and half
 * inside, so the number is the whole thickness of the light, not the part of it
 * over the picture.
 */
private val SHINE_WIDTH = 1.5.dp

/** How bright it gets at the corners. */
private const val SHINE_ALPHA = 0.7f

/**
 * The line that goes all the way round, under the light.
 *
 * Finer and dimmer than the shine: it is there to draw the shape's edge, not to
 * be looked at. Without it the two unlit corners have no outline at all, which
 * is fine over a dark screenshot and leaves a pale one bleeding into the page.
 */
private val RIM_WIDTH = 0.5.dp
private const val RIM_ALPHA = 0.4f

/**
 * The stops of the sweep, from three o'clock clockwise: dark, save for a glow
 * either side of each of the two lit corners.
 *
 * A sweep gradient does not wrap -- whatever it holds at 1.0 is what it holds at
 * 0.0, and a stop outside 0..1 is not a stop at all -- so the first corner's
 * glow, which is only an eighth of a turn from three o'clock, has to be cut in
 * two by hand once [REACH] passes that. Both ends then carry the value the
 * light has there, and the tail that fell off the front is put back on the end.
 */
internal fun shineStops(highlight: Color, alpha: Float = SHINE_ALPHA): Array<Pair<Float, Color>> {
    val lit = highlight.copy(alpha = alpha)
    // How bright three o'clock is: the light reaches it when it comes from
    // further away than the corner is.
    val atSeam = (1f - FIRST_PEAK / REACH).coerceAtLeast(0f)
    val ends = highlight.copy(alpha = alpha * atSeam)
    val stops = mutableListOf(0f to ends)
    if (atSeam <= 0f) {
        stops += FIRST_PEAK - REACH to Color.Transparent
    }
    stops += FIRST_PEAK to lit
    stops += FIRST_PEAK + REACH to Color.Transparent
    stops += SECOND_PEAK - REACH to Color.Transparent
    stops += SECOND_PEAK to lit
    stops += SECOND_PEAK + REACH to Color.Transparent
    if (atSeam > 0f) {
        // What is left of the first corner's glow, on the far side of the seam.
        stops += 1f - (REACH - FIRST_PEAK) to Color.Transparent
    }
    stops += 1f to ends
    return stops.toTypedArray()
}

/**
 * Draws [shape]'s edge as a fine line, and lights two of its corners --
 * top-left and bottom-right -- as if something above and to the left were
 * catching it.
 *
 * One hairline stroke filled with a sweep gradient that goes round the whole
 * shape twice-bright: transparent on one diagonal, half-white on the other. It
 * is what makes a flat rectangle of artwork sit on the page like an object
 * rather than lie on it like a sticker, and it costs no image and no shadow.
 *
 * Stroked along the shape's own outline, straddling it: half the width falls
 * outside the shape and half inside. Apply it before the clip, so that outer
 * half survives.
 *
 * Two earlier attempts and what was wrong with each. Taking the outline at a
 * size one stroke smaller and moving it in by half of one looks like an inset
 * and is not one: scaling a superellipse down is not the same as offsetting it
 * inwards, and the two curves part company hardest at the corners -- which is
 * where the light is -- leaving a wedge of unlit picture outside the stroke.
 * Stroking the true outline and clipping the outer half fixes that but leaves a
 * hairline: the artwork's antialiased edge and the stroke's antialiased edge
 * each cover the boundary pixel only partly, so a sliver of dark picture shows
 * between the page and the light. Straddling the outline covers it, because
 * the stroke is then painted over the join rather than up against it.
 */
fun Modifier.cornerShine(
    shape: Shape,
    highlight: Color = Color.White,
    width: Dp = SHINE_WIDTH,
    rim: Color = Color.White.copy(alpha = RIM_ALPHA),
    rimWidth: Dp = RIM_WIDTH,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this@drawWithCache)
    val path = Path().apply { addOutline(outline) }
    val brush = Brush.sweepGradient(
        *shineStops(highlight),
        center = Offset(size.width / 2f, size.height / 2f),
    )
    // No clips: half of each of these belongs outside the shape.
    val stroke = Stroke(width.toPx())
    val rimStroke = Stroke(rimWidth.toPx())
    onDrawWithContent {
        drawContent()
        // The rim first, so that where the light falls it is the light that is
        // seen: the shine is wider, and covers it at the corners.
        drawPath(path, rim, style = rimStroke)
        drawPath(path, brush, style = stroke)
    }
}
