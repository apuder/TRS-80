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

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import java.nio.ByteBuffer

actual fun createScreenMask(width: Int, height: Int): ScreenMask = AndroidScreenMask(width, height)

/**
 * The Android side of [ScreenMask]: an `ALPHA_8` bitmap, straight through.
 *
 * The same thing the old `RenderThread` drew, and for the same reason — the
 * emulated screen is monochrome, so the bytes are coverage and the color is
 * applied when they are drawn.
 */
private class AndroidScreenMask(private val width: Int, private val height: Int) : ScreenMask {

    private val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)

    /**
     * Made once and handed out every frame.
     *
     * Safe to reuse because this wrapper holds no pixels of its own: a draw
     * reads [bitmap] as it stands, and filling it bumps the bitmap's generation
     * id, which is what makes the renderer upload the new content.
     */
    private val image: ImageBitmap = bitmap.asImageBitmap()

    /**
     * The frame's pixels, seen as a buffer.
     *
     * Held rather than wrapped afresh each frame: the source reuses one array
     * until it resizes, and a resize makes a new mask. The identity check is
     * what makes that an observation rather than an assumption.
     */
    private var source: ByteBuffer? = null

    override fun install(pixels: ByteArray): ImageBitmap? {
        var buffer = source
        if (buffer == null || buffer.array() !== pixels) {
            buffer = ByteBuffer.wrap(pixels)
            source = buffer
        }
        // From the start of the array each time; the copy leaves it at the end.
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        return image
    }

    override fun colorized(
        pixels: ByteArray,
        characterColor: Color,
        screenColor: Color,
    ): ImageBitmap? {
        val foreground = characterColor.toArgb()
        val background = screenColor.toArgb()
        // Colors as ints rather than bytes, which is the one place this is
        // simpler than on iOS: Android takes them in ARGB order whatever the
        // byte order underneath turns out to be, so there is nothing to get
        // backwards. Getting it backwards on the other side is what once turned
        // an amber screenshot blue.
        val colors = IntArray(width * height) {
            if (pixels[it] == 0.toByte()) background else foreground
        }
        return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    /**
     * Nothing to do.
     *
     * Bitmap.recycle() is both unnecessary and unsafe here: the pixels have been
     * on the Java heap since Lollipop, so the collector handles them, and a
     * bitmap that a display list still refers to crashes the next frame that
     * draws it.
     */
    override fun close() = Unit
}
