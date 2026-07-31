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

package org.puder.trs80.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * How the emulated screen is fetched and drawn on a given platform.
 *
 * The core rasterizes the picture and hands back one image, so a host has very
 * little to do — which is the point of having moved that work into C. What is
 * left is platform-specific only because turning a byte buffer into something
 * the graphics stack will draw is.
 */
interface EmulatorScreenSource {

    /** The rasterized screen's size, in pixels. */
    val width: Int
    val height: Int

    /**
     * Tells the source how large an area the screen will be drawn into, so the
     * core can rasterize at that size instead of the picture being scaled to it.
     *
     * Called on every layout, so an implementation should do nothing when the
     * size has not actually changed.
     */
    fun resize(availableWidth: Int, availableHeight: Int)

    /**
     * Brings the image up to date with the emulated machine.
     *
     * @return whether anything changed. When nothing has, the frame is skipped
     * entirely: the emulated machine has no frames of its own, so an idle screen
     * should cost nothing.
     */
    fun refresh(): Boolean

    /**
     * The current screen as a coverage mask, ready to draw, or null before the
     * first [refresh]. Valid until the next one.
     *
     * This carries no colour: it is an alpha-only image, tinted when drawn.
     */
    fun image(): ImageBitmap?

    /**
     * A copy of the current screen in colour, for storing as a screenshot.
     *
     * Separate from [image] because that one carries no colour — it is a mask,
     * tinted when drawn — and a screenshot has to stand on its own in a list
     * long after the machine that drew it has stopped.
     *
     * @return null when there is nothing drawn yet.
     */
    fun snapshot(characterColor: Color, screenColor: Color): ImageBitmap?
}

/**
 * Draws the emulated screen, centred.
 *
 * Samples the machine once per display frame, using Compose's frame clock —
 * which is the display's own signal on both platforms. That is the right rate
 * for a machine that has none of its own: a TRS-80 program writes video RAM
 * whenever it likes and the picture follows immediately, so there is nothing to
 * synchronize with and the only thing worth avoiding is sampling out of step
 * with what the user will actually see.
 *
 * A frame where the core reports no change costs nothing at all — no upload, no
 * recomposition, no draw.
 *
 * Note the core has already rasterized at the size this will be drawn at, so
 * nothing here scales the picture beyond fitting it to the box. Changing that
 * size should tell the core, via the source, rather than stretch what it last
 * produced; scaling a one-bit glyph by a fraction is what makes strokes uneven.
 */
@Composable
fun EmulatorScreen(
    source: EmulatorScreenSource,
    characterColor: Color,
    screenColor: Color,
    modifier: Modifier = Modifier,
) {
    // remember, or each recomposition makes a fresh counter at zero and the
    // one the frame loop increments is never the one the canvas reads.
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(source) {
        while (true) {
            withFrameNanos { }
            if (source.refresh()) {
                frame++
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { source.resize(it.width, it.height) },
        ) {
            // Read so a new picture recomposes this; the pixels themselves live
            // in the core's memory, which Compose cannot observe.
            @Suppress("UNUSED_EXPRESSION") frame
            drawEmulatedScreen(source, characterColor, screenColor)
        }
    }
}

/** Fills the background and draws the screen centred, one image pixel per screen pixel. */
private fun DrawScope.drawEmulatedScreen(
    source: EmulatorScreenSource,
    characterColor: Color,
    screenColor: Color,
) {
    drawRect(color = screenColor)

    val image = source.image() ?: return
    if (image.width == 0 || image.height == 0) {
        return
    }

    // One image pixel to one screen pixel. The source was told how much room
    // there is and rasterized to fit it, so stretching the result over the few
    // pixels left spare after the cell size was rounded down would undo exactly
    // what that rounding was for.
    var drawnWidth = image.width
    var drawnHeight = image.height
    if (drawnWidth > size.width || drawnHeight > size.height) {
        // Only reachable in the gap between a resize and the frame that honours
        // it, or from a source that does not resize at all.
        val scale = minOf(size.width / image.width, size.height / image.height)
        drawnWidth = (image.width * scale).toInt()
        drawnHeight = (image.height * scale).toInt()
    }

    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(
            ((size.width - drawnWidth) / 2).toInt(),
            ((size.height - drawnHeight) / 2).toInt(),
        ),
        dstSize = IntSize(drawnWidth, drawnHeight),
        // The core rasterized at exactly this size, so there is nothing to
        // interpolate; smoothing here would only blur it back.
        filterQuality = FilterQuality.None,
        colorFilter = ColorFilter.tint(characterColor, BlendMode.SrcIn),
    )
}
