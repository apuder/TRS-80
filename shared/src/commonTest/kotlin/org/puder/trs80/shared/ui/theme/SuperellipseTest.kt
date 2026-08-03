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

package org.puder.trs80.shared.ui.theme

import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the curve the cover art is cut to.
 *
 * The shape itself can only be looked at, but the equation behind it can be
 * checked: it has to touch the edges where a rectangle would, and sit between a
 * circle and a corner everywhere in between. Get the exponent the wrong way up
 * and it becomes a pincushion, which is a thing that has happened to people.
 */
class SuperellipseTest {

    private val size = Size(100f, 100f)

    private fun point(degrees: Float, exponent: Float = 4f) =
        superellipsePoint((degrees / 180f * PI).toFloat(), size, exponent)

    /**
     * At the axes it is flush with the box, like a rectangle.
     *
     * To a twentieth of a pixel rather than exactly: a right angle in radians is
     * not one in Float, and the leftover cosine of about 4e-8 comes back as 2e-4
     * once it has been through a square root. It is a hundredth of a pixel on a
     * shape drawn at fifty-six.
     */
    @Test
    fun itMeetsTheEdgesAtTheMiddleOfEachSide() {
        assertEquals(100f, point(0f).x, 0.05f)
        assertEquals(50f, point(0f).y, 0.05f)
        assertEquals(50f, point(90f).x, 0.05f)
        assertEquals(100f, point(90f).y, 0.05f)
        assertEquals(0f, point(180f).x, 0.05f)
        assertEquals(0f, point(270f).y, 0.05f)
    }

    /**
     * On the diagonal it is fuller than a circle and short of the corner, which
     * is the whole of what makes it a squircle.
     */
    @Test
    fun theDiagonalSitsBetweenACircleAndACorner() {
        val corner = point(45f)
        val fromCentre = hypot(corner.x - 50f, corner.y - 50f)

        assertTrue(fromCentre > 50f, "a circle would be 50 away; this is $fromCentre")
        assertTrue(fromCentre < 70.71f, "a square's corner is 70.71 away; this is $fromCentre")
    }

    /** The higher the exponent the closer to a rectangle it gets. */
    @Test
    fun aHigherExponentIsASquarerShape() {
        val ellipse = point(45f, exponent = 2f)
        val icon = point(45f, exponent = 4f)
        val nearlySquare = point(45f, exponent = 12f)

        val distances = listOf(ellipse, icon, nearlySquare).map { hypot(it.x - 50f, it.y - 50f) }

        assertEquals(50f, distances[0], 0.01f, "an exponent of two is a circle")
        assertTrue(
            distances[0] < distances[1] && distances[1] < distances[2],
            "the corner should fill out as the exponent rises: $distances",
        )
    }

    /** It is symmetric, or one corner would be fatter than the others. */
    @Test
    fun allFourCornersAreTheSame() {
        val distances = listOf(45f, 135f, 225f, 315f)
            .map { point(it) }
            .map { hypot(it.x - 50f, it.y - 50f) }

        for (distance in distances) {
            assertEquals(distances[0], distance, 0.01f)
        }
    }

    /** A wide box gets a wide shape, not a circle in the middle of one. */
    @Test
    fun itFillsWhateverBoxItIsGiven() {
        val wide = superellipsePoint(0f, Size(200f, 50f), 4f)

        assertEquals(200f, wide.x, 0.01f)
        assertEquals(25f, wide.y, 0.01f)
    }
}
