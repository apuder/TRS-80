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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/**
 * The emulated screen, fetched from an [EmulatorCore] and kept ready to draw.
 *
 * The frame loop is the same on both platforms because the core made it so:
 * fitting the picture to the space, telling the core to rasterize at that size,
 * skipping frames it says have not changed, and copying the result into a
 * bitmap. None of that is platform-specific, and having it written twice is how
 * two hosts start drifting apart in ways nobody notices until one of them looks
 * wrong. What genuinely differs — which class holds the bytes — is behind
 * [ScreenMask].
 *
 * Takes the core rather than reaching for a global one, so the screen can be
 * driven by a fake; [newMask] is injectable for the same reason and is otherwise
 * the platform's own.
 */
class CoreScreenSource(
    private val core: EmulatorCore,
    private val newMask: (width: Int, height: Int) -> ScreenMask = ::createScreenMask,
) : EmulatorScreenSource {

    private var mask: ScreenMask? = null
    private var image: ImageBitmap? = null

    /** Reused every frame; see [EmulatorCore.copyPixelsInto]. */
    private var pixels = ByteArray(0)

    private var cellWidth = 0
    private var cellHeight = 0

    override var width: Int = 0
        private set

    override var height: Int = 0
        private set

    override fun resize(availableWidth: Int, availableHeight: Int) {
        val metrics = fitCellSize(availableWidth, availableHeight)
        if (metrics.cellWidth <= 0 || metrics.cellHeight <= 0) {
            return
        }
        // Called on every layout, and most of them leave the cell exactly where
        // it was: the size is rounded down to whole pixels, so a few pixels of
        // difference in the space available do not move it at all.
        if (metrics.cellWidth == cellWidth && metrics.cellHeight == cellHeight) {
            return
        }
        setCellSize(metrics.cellWidth, metrics.cellHeight)
    }

    /** Tells the core what size a character cell is drawn at, and re-makes the mask for it. */
    private fun setCellSize(cellWidth: Int, cellHeight: Int) {
        this.cellWidth = cellWidth
        this.cellHeight = cellHeight
        core.setCellSize(cellWidth, cellHeight)
        width = core.pixelWidth
        height = core.pixelHeight

        mask?.close()
        mask = newMask(width, height)
        pixels = ByteArray(width * height)
        image = null
        // Nothing the core drew at the old size still applies.
        core.invalidateRender()
    }

    override fun refresh(): Boolean {
        if (width == 0 || height == 0) {
            // Before the first layout. Rasterizing at the ROM's own cell size
            // costs one frame at the wrong size rather than a blank one.
            setCellSize(core.romCellWidth, core.romCellHeight)
        }
        val mask = mask ?: return false
        if (!core.render()) {
            return false
        }
        core.copyPixelsInto(pixels)
        image = mask.install(pixels)
        return true
    }

    override fun image(): ImageBitmap? = image

    override fun snapshot(characterColor: Color, screenColor: Color): ImageBitmap? {
        val mask = mask ?: return null
        // image, not width: a mask exists from the first layout, but until a
        // frame has been through it the pixels are all zero, and a screenshot of
        // that is a rectangle of glass.
        if (image == null) {
            return null
        }
        return mask.colorized(pixels, characterColor, screenColor)
    }
}
