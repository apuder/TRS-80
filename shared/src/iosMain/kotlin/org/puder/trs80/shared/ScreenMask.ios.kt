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
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

actual fun createScreenMask(width: Int, height: Int): ScreenMask = SkiaScreenMask(width, height)

/** The iOS side of [ScreenMask]: a Skia bitmap, straight through. */
private class SkiaScreenMask(private val width: Int, private val height: Int) : ScreenMask {

    // Not Bitmap().apply { ... }: inside that, width and height would resolve to
    // the bitmap's own, which are zero until it has been allocated.
    private val bitmap = Bitmap()

    init {
        bitmap.allocPixels(ImageInfo(width, height, ColorType.ALPHA_8, ColorAlphaType.PREMUL))
    }

    override fun install(pixels: ByteArray): ImageBitmap? {
        if (!bitmap.installPixels(pixels)) {
            return null
        }
        return bitmap.asComposeImageBitmap()
    }

    override fun colorized(
        pixels: ByteArray,
        characterColor: Color,
        screenColor: Color,
    ): ImageBitmap? {
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

    override fun close() = bitmap.close()
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
