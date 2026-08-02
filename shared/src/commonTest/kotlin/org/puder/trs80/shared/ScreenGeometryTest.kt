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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreenGeometryTest {

    @Test
    fun aWideAreaIsLimitedByItsHeight() {
        // 64 cells would each be 31px wide, needing 93px of height per row and
        // 1488 in total -- far more than there is, so the height decides: 800/16
        // is 50, trimmed to 48.
        val metrics = fitCellSize(availableWidth = 2000, availableHeight = 800)

        assertEquals(48, metrics.cellHeight)
        assertEquals(16, metrics.cellWidth)
        assertTrue(metrics.cellHeight * metrics.rows <= 800)
        assertTrue(metrics.cellWidth * metrics.columns <= 2000)
    }

    @Test
    fun aTallAreaIsLimitedByItsWidth() {
        val metrics = fitCellSize(availableWidth = 1206, availableHeight = 2622)

        assertEquals(18, metrics.cellWidth)
        assertEquals(54, metrics.cellHeight)
        assertTrue(metrics.cellWidth * metrics.columns <= 1206)
        assertTrue(metrics.cellHeight * metrics.rows <= 2622)
    }

    /**
     * A cell's three bands have to be equal, or block graphics drawn as a solid
     * block would show seams where one band is a row taller than another.
     *
     * The two halves across are allowed to differ by a pixel; see [fitCellSize]
     * for why that is the cheaper of the two roundings.
     */
    @Test
    fun everyCellSplitsIntoThreeEqualBands() {
        for (width in 200..2600 step 7) {
            for (height in 200..2600 step 103) {
                val metrics = fitCellSize(width, height)
                assertEquals(
                    0, metrics.cellHeight % 3,
                    "cell height ${metrics.cellHeight} at ${width}x$height is not a multiple of 3",
                )
            }
        }
    }

    /**
     * What the odd widths buy: the picture is never more than a cell short of
     * the space it was given.
     */
    @Test
    fun theScreenFillsTheSpaceToWithinOneCell() {
        for (width in 200..2600 step 7) {
            val metrics = fitCellSize(width, availableHeight = 4000)
            val drawn = metrics.cellWidth * metrics.columns
            assertTrue(
                width - drawn < metrics.columns,
                "$drawn of $width leaves ${width - drawn}px unused, more than one cell",
            )
        }
    }

    /** The screen must always fit inside what it was given, never overflow it. */
    @Test
    fun theScreenNeverExceedsTheAreaGiven() {
        for (width in 200..2600 step 7) {
            for (height in 200..2600 step 103) {
                val metrics = fitCellSize(width, height)
                assertTrue(
                    metrics.cellWidth * metrics.columns <= width,
                    "${metrics.cellWidth} x ${metrics.columns} overflows width $width",
                )
                assertTrue(
                    metrics.cellHeight * metrics.rows <= height,
                    "${metrics.cellHeight} x ${metrics.rows} overflows height $height",
                )
            }
        }
    }

    @Test
    fun anUnmeasuredAreaHasNoCellSize() {
        assertEquals(CellMetrics(SCREEN_COLUMNS, SCREEN_ROWS, 0, 0), fitCellSize(0, 0))
        assertEquals(CellMetrics(SCREEN_COLUMNS, SCREEN_ROWS, 0, 0), fitCellSize(1080, 0))
    }
}
