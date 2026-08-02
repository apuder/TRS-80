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
 * A fixed-size, 8-bit coverage mask that the graphics stack will draw.
 *
 * This is the whole of what turning the emulated screen into a picture needs
 * from a platform. The core hands back one byte per pixel — how much of that
 * pixel a character covers, and nothing about its color — and every host has
 * some way of holding exactly that: `ALPHA_8` on both, as it happens, since both
 * draw through Skia in the end. What differs is only which class owns the
 * bytes, so that is all that is behind this interface.
 *
 * The color arrives when the mask is drawn, not when it is filled, which is what
 * lets the phosphor color change without re-rasterizing anything.
 *
 * An interface with a factory rather than an `expect class`, so a test can watch
 * what the frame loop does to it without a real bitmap on either platform.
 */
interface ScreenMask {

    /**
     * Takes a new frame's coverage bytes, which must be width * height of them.
     *
     * @return the mask as something drawable, or null if it could not be filled.
     */
    fun install(pixels: ByteArray): ImageBitmap?

    /**
     * Expands the same bytes into an opaque color image, for a screenshot.
     *
     * Separate from [install] because the drawing path deliberately keeps the
     * mask colorless: expanding every pixel to four bytes per frame is what once
     * cost three quarters of a core, whereas a screenshot happens once, when a
     * machine is put away, and has to stand on its own afterwards.
     */
    fun colorized(pixels: ByteArray, characterColor: Color, screenColor: Color): ImageBitmap?

    /** Releases whatever the mask holds. It is not used again afterwards. */
    fun close()
}

/** A mask of [width] by [height] pixels, on whichever platform this is. */
expect fun createScreenMask(width: Int, height: Int): ScreenMask
