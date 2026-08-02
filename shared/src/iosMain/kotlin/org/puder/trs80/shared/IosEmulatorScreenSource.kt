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
 * nothing more. The mask has no color of its own; that is applied when it is
 * drawn, which is what lets the same mask serve any phosphor color.
 *
 * The counterpart on Android is `RenderThread`, doing the same through an
 * `ALPHA_8` `android.graphics.Bitmap`. Neither host rasterizes anything.
 */
@OptIn(ExperimentalForeignApi::class)
class IosEmulatorScreenSource : EmulatorScreenSource {

    private var bitmap = Bitmap()
    private var image: ImageBitmap? = null
    /** Reused every frame; see [IosEmulatorCore.copyPixelsInto]. */
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
        if (metrics.cellWidth == cellWidth && metrics.cellHeight == cellHeight) {
            return
        }
        setCellSize(metrics.cellWidth, metrics.cellHeight)
    }

    /** Tells the core the size a character cell is drawn at, and re-makes the bitmap for it. */
    private fun setCellSize(cellWidth: Int, cellHeight: Int) {
        this.cellWidth = cellWidth
        this.cellHeight = cellHeight
        IosEmulatorCore.setCellSize(cellWidth, cellHeight)
        width = IosEmulatorCore.pixelWidth
        height = IosEmulatorCore.pixelHeight

        bitmap.close()
        // Not Bitmap().apply { ... }: inside that, width and height would resolve
        // to the bitmap's own, which are zero until it has been allocated.
        bitmap = Bitmap()
        bitmap.allocPixels(ImageInfo(width, height, ColorType.ALPHA_8, ColorAlphaType.PREMUL))
        pixels = ByteArray(width * height)
        image = null
        IosEmulatorCore.invalidateRender()
    }

    override fun refresh(): Boolean {
        if (width == 0 || height == 0) {
            // Before the first layout. Rasterizing at the ROM's own cell size
            // costs one frame at the wrong size rather than a blank one.
            setCellSize(IosEmulatorCore.romCellWidth, IosEmulatorCore.romCellHeight)
        }
        if (!IosEmulatorCore.render()) {
            return false
        }
        IosEmulatorCore.copyPixelsInto(pixels)
        bitmap.installPixels(pixels)
        image = bitmap.asComposeImageBitmap()
        return true
    }

    override fun image(): ImageBitmap? = image

    /**
     * Expands the mask into a color bitmap.
     *
     * Only for screenshots, which happen once when a machine is put away —
     * doing this per frame is what made the app burn 75% of a core before, so
     * the drawing path deliberately keeps the mask colorless and tints it.
     */
    override fun snapshot(characterColor: Color, screenColor: Color): ImageBitmap? {
        if (width == 0 || height == 0 || image == null) {
            return null
        }
        val foreground = characterColor.toN32()
        val background = screenColor.toN32()
        val colored = ByteArray(width * height * 4)
        var out = 0
        for (i in pixels.indices) {
            writeN32(colored, out, if (pixels[i] == 0.toByte()) background else foreground)
            out += 4
        }
        val target = Bitmap()
        target.allocPixels(ImageInfo(width, height, ColorType.N32, ColorAlphaType.OPAQUE))
        if (!target.installPixels(colored)) {
            return null
        }
        return target.asComposeImageBitmap()
    }
}

/** A color as a plain ARGB int. */
internal fun Color.toN32(): Int {
    val a = (alpha * 255f).toInt() and 0xFF
    val r = (red * 255f).toInt() and 0xFF
    val g = (green * 255f).toInt() and 0xFF
    val b = (blue * 255f).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

/**
 * Writes [color] into [out] at [at], in the order Skia wants it.
 *
 * N32 is BGRA in memory on Apple platforms — blue first, alpha last. Getting
 * this backwards is invisible in green and in white, since both survive a red
 * and blue swap unchanged; amber was the first color to show it, arriving on
 * screen correctly and in the saved screenshot as blue.
 */
internal fun writeN32(out: ByteArray, at: Int, color: Int) {
    out[at] = (color and 0xFF).toByte()
    out[at + 1] = ((color shr 8) and 0xFF).toByte()
    out[at + 2] = ((color shr 16) and 0xFF).toByte()
    out[at + 3] = ((color ushr 24) and 0xFF).toByte()
}
