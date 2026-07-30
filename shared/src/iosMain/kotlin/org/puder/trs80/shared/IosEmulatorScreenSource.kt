/*
 * Copyright 2025, Arno Puder
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

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

/**
 * The iOS side of [EmulatorScreenSource].
 *
 * The core rasterizes the screen into a coverage mask -- one byte per pixel, zero
 * where the background shows through -- so this is a copy into a Skia bitmap and
 * nothing more. The mask has no colour of its own; that is applied when it is
 * drawn, which is what lets the same mask serve any phosphor colour.
 *
 * The counterpart on Android is `RenderThread`, doing the same through an
 * `ALPHA_8` `android.graphics.Bitmap`. Neither host rasterizes anything.
 */
@OptIn(ExperimentalForeignApi::class)
class IosEmulatorScreenSource : EmulatorScreenSource {

    private var bitmap = Bitmap()
    private var image: ImageBitmap? = null
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
        if (metrics.cellWidth == cellWidth && metrics.cellHeight == cellHeight) {
            return
        }
        setCellSize(metrics.cellWidth, metrics.cellHeight)
    }

    /** Tells the core the size a character cell is drawn at, and re-makes the bitmap for it. */
    private fun setCellSize(cellWidth: Int, cellHeight: Int) {
        this.cellWidth = cellWidth
        this.cellHeight = cellHeight
        EmulatorCore.setCellSize(cellWidth, cellHeight)
        width = EmulatorCore.pixelWidth
        height = EmulatorCore.pixelHeight

        bitmap.close()
        // Not Bitmap().apply { ... }: inside that, width and height would resolve
        // to the bitmap's own, which are zero until it has been allocated.
        bitmap = Bitmap()
        bitmap.allocPixels(ImageInfo(width, height, ColorType.ALPHA_8, ColorAlphaType.PREMUL))
        image = null
        EmulatorCore.invalidateRender()
    }

    override fun refresh(): Boolean {
        if (width == 0 || height == 0) {
            // Before the first layout. Rasterizing at the ROM's own cell size
            // costs one frame at the wrong size rather than a blank one.
            setCellSize(EmulatorCore.romCellWidth, EmulatorCore.romCellHeight)
        }
        if (!EmulatorCore.render()) {
            return false
        }
        bitmap.installPixels(EmulatorCore.pixelBytes())
        image = bitmap.asComposeImageBitmap()
        return true
    }

    override fun image(): ImageBitmap? = image
}
