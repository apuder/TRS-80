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

/** The screen geometry of a Model I or Model III, in character cells. */
const val SCREEN_COLUMNS = 64
const val SCREEN_ROWS = 16

/** A character cell is three times as tall as it is wide. */
const val SCREEN_ASPECT_RATIO = 3f

/**
 * How wide a whole screen is against its height: four to three.
 *
 * Derived rather than typed, because it is not an independent fact -- it is what
 * the three numbers above come to -- and anywhere a picture of a screen is drawn
 * has to agree with what the core rasterizes into it.
 */
const val SCREEN_PICTURE_RATIO = SCREEN_COLUMNS / (SCREEN_ROWS * SCREEN_ASPECT_RATIO)

/**
 * Picks the size to draw one character cell at, to fill the given area as fully
 * as the screen's proportions allow.
 *
 * This is the one piece of layout the core needs to be told: it rasterizes at
 * whatever cell size it is given, rather than producing a fixed-size picture for
 * the host to scale. Scaling afterwards is what ruins the image — the glyphs are
 * 8x12 one-bit bitmaps, and at a fractional scale each one-pixel stem lands on
 * either one output pixel or two depending where it happens to fall, so strokes
 * come out uneven and some columns vanish altogether.
 *
 * Hence the rounding: the cell is trimmed to a whole number of pixels, and its
 * height to a multiple of three, so that the three bands of the block graphics —
 * which divide a cell into 2x3 quadrants — each get the same number of rows.
 *
 * The width is not rounded to even, though the same argument would suggest it.
 * An odd cell splits into halves of n/2 and n/2+1 pixels, which is a one-pixel
 * asymmetry repeated identically in every cell on screen, and the alternative
 * costs a whole pixel per column: on a 1008-pixel screen, insisting on an even
 * cell means 14 rather than 15 and throws away 64 pixels of picture — 56 pixels
 * of black down each side, against 24. The asymmetry is not visible; the bars
 * are.
 *
 * The result is usually a little smaller than the area given; centering it is
 * the caller's business.
 */
fun fitCellSize(
    availableWidth: Int,
    availableHeight: Int,
    columns: Int = SCREEN_COLUMNS,
    rows: Int = SCREEN_ROWS,
    aspectRatio: Float = SCREEN_ASPECT_RATIO,
): CellMetrics {
    if (availableWidth <= 0 || availableHeight <= 0) {
        return CellMetrics(columns, rows, 0, 0)
    }
    val cellWidth: Int
    val cellHeight: Int
    if (availableWidth / columns * aspectRatio > availableHeight / rows) {
        // Too short to let the screen span the full width, so height decides.
        var height = availableHeight / rows
        while (height % 3 != 0) {
            height--
        }
        cellHeight = height
        cellWidth = (height / aspectRatio).toInt()
    } else {
        // Too narrow to let the screen span the full height, so width decides.
        // The height follows from it and is three times it, so it is a multiple
        // of three whatever the width turns out to be.
        cellWidth = availableWidth / columns
        cellHeight = (cellWidth * aspectRatio).toInt()
    }
    return CellMetrics(columns, rows, cellWidth, cellHeight)
}

/**
 * The same cell, divided down until the whole picture fits in [budget] pixels.
 *
 * Divided by a whole number rather than scaled to fit, so that the picture that
 * comes back is the same shape and every emulated pixel becomes an identical
 * block when it is drawn up to size. Anything else would put some glyph stems
 * a pixel wider than others, which is the thing rasterizing at the drawn size
 * exists to avoid.
 *
 * The height stays a multiple of three, for the same reason it starts as one:
 * the block graphics divide a cell into three bands, and a band that is a row
 * short of its neighbours is visible on any screen full of them.
 */
fun CellMetrics.withinBudget(budget: Int): CellMetrics {
    if (budget <= 0 || cellWidth <= 0 || cellHeight <= 0) {
        return this
    }
    var divisor = 1
    while (true) {
        val width = cellWidth / divisor
        val height = (cellHeight / divisor) / 3 * 3
        if (width < 1 || height < 3) {
            // Smaller than a cell can be; the last one that was not is as far
            // as this goes.
            val last = (divisor - 1).coerceAtLeast(1)
            return copy(cellWidth = cellWidth / last, cellHeight = (cellHeight / last) / 3 * 3)
        }
        if (width * columns * height * rows <= budget) {
            return copy(cellWidth = width, cellHeight = height)
        }
        divisor++
    }
}
