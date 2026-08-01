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
 * Hence the rounding: the cell is trimmed to a whole number of pixels, and to an
 * even width and a height divisible by three, so the block graphics — which
 * divide a cell into 2x3 quadrants — also land on whole pixels. Since the cell is
 * three times as tall as it is wide, that makes the height a multiple of six
 * either way round. The result is usually a little smaller than the area given;
 * centering it is the caller's business.
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
        // Trimming to a multiple of six, not three, is what keeps the width that
        // follows from it even.
        var height = availableHeight / rows
        while (height % 6 != 0) {
            height--
        }
        cellHeight = height
        cellWidth = (height / aspectRatio).toInt()
    } else {
        // Too narrow to let the screen span the full height, so width decides.
        var width = availableWidth / columns
        while (width % 2 != 0) {
            width--
        }
        cellWidth = width
        cellHeight = (width * aspectRatio).toInt()
    }
    return CellMetrics(columns, rows, cellWidth, cellHeight)
}
