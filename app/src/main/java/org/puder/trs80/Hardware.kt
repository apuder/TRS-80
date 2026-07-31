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

package org.puder.trs80

import android.graphics.Rect
import org.puder.trs80.shared.configuration.Configuration
import org.puder.trs80.shared.CellMetrics
import org.puder.trs80.shared.SCREEN_ASPECT_RATIO
import org.puder.trs80.shared.SCREEN_COLUMNS
import org.puder.trs80.shared.SCREEN_ROWS
import org.puder.trs80.shared.fitCellSize
import org.puder.trs80.shared.KeyboardLayout

/** The largest a key "box" may get, in dp. */
private const val MAX_KEY_BOX_SIZE_DP = 55f

/** Increment used when searching for the font size that fills a character cell. */
private const val FONT_SIZE_DELTA = 0.1f

/** Character codes in this range are pseudo-graphics, drawn by hand. */

/**
 * The hardware characteristics of the various TRS-80 models: which screen
 * geometry a model has, and how large a character cell is drawn on this display.
 *
 * It used to rasterize the glyphs too, from TrueType replicas of the TRS-80
 * fonts. The emulator core now rasterizes from the real character generator ROMs
 * instead, so what is left here is the arithmetic deciding how big a cell is —
 * which still matters, because it is what the core's mask gets scaled to, and
 * the integer rounding is what keeps that scaling on whole pixels.
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
     * The screen geometry of the emulated model.
     *
     * Only [MODEL1] and [MODEL3] have one. Any other model throws, which is what the
     * callers used to do anyway: this returned `null` for them and every caller
     * dereferenced it straight away. `MainActivity` refuses to start the emulator for the
     * other models, so it is unreachable in practice.
     */
    internal val screenConfiguration: ScreenConfiguration
        get() = when (model) {
            MODEL1, MODEL3 -> ScreenConfiguration(SCREEN_COLUMNS, SCREEN_ROWS, SCREEN_ASPECT_RATIO)
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
     * Computes the screen dimensions that fit into [rect].
     *
     * The cell size still matters even though the host no longer rasterizes
     * anything: it is what the core's mask is scaled to, and the integer
     * arithmetic here is what keeps that scaling on whole pixels.
     */
    fun computeScreenDimensions(rect: Rect) {
        val screenConfig = screenConfiguration
        val metrics = fitCellSize(
            availableWidth = rect.right,
            availableHeight = rect.bottom - rect.top,
            columns = screenConfig.trsScreenCols,
            rows = screenConfig.trsScreenRows,
            aspectRatio = screenConfig.aspectRatio,
        )
        charWidth = metrics.cellWidth
        charHeight = metrics.cellHeight
        screenWidth = metrics.cellWidth * metrics.columns
        screenHeight = metrics.cellHeight * metrics.rows
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

    private fun pxFromDp(dp: Float): Float =
        dp * TRS80Application.getAppContext().resources.displayMetrics.density

    companion object {
        // Re-exported from `commonMain`, where the configuration code that
        // reads and writes them now lives. Same values, one definition.
        const val MODEL_NONE = org.puder.trs80.shared.MODEL_NONE
        const val MODEL1 = org.puder.trs80.shared.MODEL1
        const val MODEL3 = org.puder.trs80.shared.MODEL3
        const val MODEL4 = org.puder.trs80.shared.MODEL4
        const val MODEL4P = org.puder.trs80.shared.MODEL4P
    }
}

