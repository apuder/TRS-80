/*
 * Copyright 2012-2013, Arno Puder
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

package org.puder.trs80

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.AsyncTask
import org.puder.trs80.configuration.Configuration
import org.puder.trs80.shared.CellMetrics
import org.puder.trs80.shared.KeyboardLayout

/** The largest a key "box" may get, in dp. */
private const val MAX_KEY_BOX_SIZE_DP = 55f

/** Increment used when searching for the font size that fills a character cell. */
private const val FONT_SIZE_DELTA = 0.1f

private const val SCREEN_COLS = 64
private const val SCREEN_ROWS = 16
private const val SCREEN_ASPECT_RATIO = 3f

/** Character codes in this range are pseudo-graphics, drawn by hand. */
private val GRAPHICS_CHARS = 128..191

/**
 * Encapsulates the hardware characteristics of the various TRS-80 models. In particular it
 * computes the bitmaps used during rendering. The size of the font is determined by the size
 * of the screen and whether the emulator runs in landscape or portrait mode, so that it
 * scales nicely across screen resolutions.
 *
 * Each bitmap of [font] represents one character of the ASCII code. Alphanumeric characters
 * use the TrueType fonts bundled under `assets/fonts` (see `generateAsciiFont()`); the
 * TRS-80 pseudo-graphics are the 2x3-per-character pseudo pixel graphics computed by
 * `generateGraphicsFont()`.
 */
class Hardware(private val configuration: Configuration) {

    /** The screen geometry of an emulated model, in character cells. */
    internal data class ScreenConfiguration(
        val trsScreenCols: Int,
        val trsScreenRows: Int,
        val aspectRatio: Float
    )

    /** The emulated model, one of the `MODEL*` constants. */
    val model: Int
        get() = configuration.model

    /** The width of the emulated screen, in pixels. */
    var screenWidth = 0
        private set

    /** The height of the emulated screen, in pixels. */
    var screenHeight = 0
        private set

    /** The width of a single character cell, in pixels. */
    var charWidth = 0
        private set

    /** The height of a single character cell, in pixels. */
    var charHeight = 0
        private set

    /** The width of an on-screen keyboard key, in pixels. */
    var keyWidth = 0
        private set

    /** The height of an on-screen keyboard key, in pixels. */
    var keyHeight = 0
        private set

    /** The margin around an on-screen keyboard key, in pixels. */
    var keyMargin = 0
        private set

    /**
     * The glyph of every character code, indexed by code. Entries are `null` until
     * [generateFont] has run, and stay `null` for the codes it did not reach if the task
     * it runs on was cancelled.
     */
    val font: Array<Bitmap?> = arrayOfNulls(256)

    /**
     * The screen geometry of the emulated model.
     *
     * Only [MODEL1] and [MODEL3] have one. Any other model throws, which is what the
     * callers used to do anyway: this returned `null` for them and every caller
     * dereferenced it straight away. `MainActivity` refuses to start the emulator for the
     * other models, so it is unreachable in practice.
     */
    internal val screenConfiguration: ScreenConfiguration
        get() = when (model) {
            MODEL1, MODEL3 -> ScreenConfiguration(SCREEN_COLS, SCREEN_ROWS, SCREEN_ASPECT_RATIO)
            else -> throw IllegalArgumentException("No screen configuration for model $model")
        }

    /** The colour the emulated characters are drawn in. */
    internal val characterColor: Int
        get() = configuration.characterColorAsRGB

    /** The colour behind the emulated characters. */
    internal val screenColor: Int
        get() = configuration.screenColorAsRGB

    /**
     * The emulated screen geometry together with the cell size it is drawn at. Only valid
     * once [generateFont] has computed the cell size for the display.
     */
    internal val cellMetrics: CellMetrics
        get() = screenConfiguration.let {
            CellMetrics(it.trsScreenCols, it.trsScreenRows, charWidth, charHeight)
        }

    /**
     * Computes the screen dimensions that fit into [rect] and renders the font for them.
     *
     * @param task the task this runs on; generation stops as soon as it is cancelled.
     */
    fun generateFont(rect: Rect, task: AsyncTask<*, *, *>) {
        val screenConfig = screenConfiguration
        val contentHeight = rect.bottom - rect.top
        val contentWidth = rect.right
        if (contentWidth / screenConfig.trsScreenCols * screenConfig.aspectRatio >
            contentHeight / screenConfig.trsScreenRows
        ) {
            // Screen height is not sufficient to let the TRS80 screen span the whole width.
            charHeight = contentHeight / screenConfig.trsScreenRows
            // Make sure charHeight is divisible by 3.
            while (charHeight % 3 != 0) {
                charHeight--
            }
            screenHeight = charHeight * screenConfig.trsScreenRows
            charWidth = (charHeight / screenConfig.aspectRatio).toInt()
            screenWidth = charWidth * screenConfig.trsScreenCols
        } else {
            // Screen width is not sufficient to let the TRS80 screen span the whole height.
            charWidth = contentWidth / screenConfig.trsScreenCols
            while (charWidth % 2 != 0) {
                charWidth--
            }
            screenWidth = charWidth * screenConfig.trsScreenCols
            charHeight = (charWidth * screenConfig.aspectRatio).toInt()
            screenHeight = charHeight * screenConfig.trsScreenRows
        }

        generateGraphicsFont(task)
        generateAsciiFont(task)
    }

    /** Computes [keyWidth], [keyHeight] and [keyMargin] for a keyboard filling [rect]. */
    fun computeKeyDimensions(rect: Rect, keyboardLayout: KeyboardLayout) {
        // The maximum number of key "boxes" per row.
        val maxKeyBoxes = when (keyboardLayout) {
            KeyboardLayout.KEYBOARD_LAYOUT_COMPACT -> 10
            KeyboardLayout.KEYBOARD_LAYOUT_JOYSTICK -> 8
            else -> 15
        }
        val threshold = pxFromDp(MAX_KEY_BOX_SIZE_DP)
        var boxWidth = rect.right / maxKeyBoxes
        if (boxWidth > threshold) {
            boxWidth = threshold.toInt()
        }
        keyWidth = (boxWidth * 0.9f).toInt()
        keyHeight = keyWidth
        keyMargin = (boxWidth - keyWidth) / 2
    }

    private fun generateAsciiFont(task: AsyncTask<*, *, *>) {
        val paint = Paint().apply {
            textAlign = Paint.Align.CENTER
            typeface = Fonts.getTypeface(configuration.model)
            textScaleX = 1.0f
            color = configuration.characterColorAsRGB
            isAntiAlias = true
        }
        setFontSize(paint)
        val xPos = (charWidth / 2).toFloat()
        val yPos = (charHeight / 2 - (paint.descent() + paint.ascent()) / 2).toInt().toFloat()
        for (i in 0..0xff) {
            if (task.isCancelled) {
                return
            }
            if (i in GRAPHICS_CHARS) {
                // Generated by generateGraphicsFont() instead.
                continue
            }
            val bitmap = Bitmap.createBitmap(charWidth, charHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(configuration.screenColorAsRGB)
            // http://www.kreativekorp.com/software/fonts/trs80.shtml
            // TRS font starts at code point 0xe000.
            val chars = Character.toChars(0xe000 + i)
            canvas.drawText(chars, 0, chars.size, xPos, yPos, paint)
            font[i] = bitmap
        }
    }

    /**
     * Sets the correct font size on [paint]. The font size designates the height of the
     * font, and [charHeight] is much bigger than [charWidth] because of the aspect ratio,
     * so [charHeight] cannot be used as the font size. Instead the width of the string "X"
     * is measured while the font size is incrementally increased until it hits [charWidth].
     */
    private fun setFontSize(paint: Paint) {
        var fontSize = charWidth.toFloat()
        var width: Float
        do {
            fontSize += FONT_SIZE_DELTA
            paint.textSize = fontSize
            width = paint.measureText("X")
        } while (width <= charWidth)
        paint.textSize = fontSize - FONT_SIZE_DELTA
    }

    private fun generateGraphicsFont(task: AsyncTask<*, *, *>) {
        val paint = Paint()
        for (i in GRAPHICS_CHARS) {
            if (task.isCancelled) {
                return
            }
            val bitmap = Bitmap.createBitmap(charWidth, charHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(configuration.screenColorAsRGB)
            paint.color = configuration.characterColorAsRGB

            // Each character is a 2x3 grid of pseudo pixels, one per bit of the low six
            // bits of the character code.
            val midX = charWidth / 2
            val upperY = charHeight / 3
            val lowerY = charHeight / 3 * 2
            if (i and 1 != 0) canvas.drawBlock(paint, 0, 0, midX, upperY)
            if (i and 2 != 0) canvas.drawBlock(paint, midX, 0, charWidth, upperY)
            if (i and 4 != 0) canvas.drawBlock(paint, 0, upperY, midX, lowerY)
            if (i and 8 != 0) canvas.drawBlock(paint, midX, upperY, charWidth, lowerY)
            if (i and 16 != 0) canvas.drawBlock(paint, 0, lowerY, midX, charHeight)
            if (i and 32 != 0) canvas.drawBlock(paint, midX, lowerY, charWidth, charHeight)

            font[i] = bitmap
        }
    }

    private fun pxFromDp(dp: Float): Float =
        dp * TRS80Application.getAppContext().resources.displayMetrics.density

    companion object {
        const val MODEL_NONE = 0
        const val MODEL1 = 1
        const val MODEL3 = 3
        const val MODEL4 = 4
        const val MODEL4P = 5
    }
}

private fun Canvas.drawBlock(paint: Paint, left: Int, top: Int, right: Int, bottom: Int) =
    drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
