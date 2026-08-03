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

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers where the light lands.
 *
 * The stroke can only be looked at, but which way round the sweep goes is
 * arithmetic, and it is the whole of the effect: an eighth of a turn out and the
 * highlights slide off the corners onto the edges, where they read as a mistake.
 */
class CornerShineTest {

    private val stops = shineStops(Color.White, alpha = 0.5f).toMap()

    /**
     * The stops at full brightness.
     *
     * By nearness, not equality: a Color packs its alpha into eight bits, so
     * what goes in as 0.5 comes back as 128/255.
     */
    private val lit = stops.filterValues { it.alpha > 0.4f }.keys.sorted()

    /** A fraction of a turn, as an angle from three o'clock going clockwise. */
    private fun degrees(fraction: Float) = fraction * 360f

    /**
     * How bright the sweep is at [angle] degrees, as a fraction of the peak.
     *
     * The stops are what the gradient is made of; this is what it looks like,
     * which is what the tests are actually about. Between two stops a gradient
     * interpolates, so anything asking "is the middle of the top edge dark"
     * has to interpolate too.
     */
    private fun brightnessAt(angle: Float): Float {
        val fraction = angle / 360f
        val ordered = stops.keys.sorted()
        val after = ordered.first { it >= fraction - 0.0001f }
        val before = ordered.last { it <= fraction + 0.0001f }
        if (before == after) {
            return stops.getValue(before).alpha / 0.5f
        }
        val across = (fraction - before) / (after - before)
        val low = stops.getValue(before).alpha
        val high = stops.getValue(after).alpha
        return (low + (high - low) * across) / 0.5f
    }

    @Test
    fun theTwoBrightPointsAreOnACornerDiagonal() {
        assertEquals(2, lit.size, "two peaks, not more: $stops")
        assertEquals(45f, degrees(lit[0]), 0.01f, "bottom-right")
        assertEquals(225f, degrees(lit[1]), 0.01f, "top-left")
    }

    /** The two corners the light does not come from are unlit. */
    @Test
    fun theOtherTwoCornersAreDark() {
        assertEquals(0f, brightnessAt(135f), 0.001f, "bottom-left")
        assertEquals(0f, brightnessAt(315f), 0.001f, "top-right")
    }

    /** And it is brightest exactly on the diagonal, not beside it. */
    @Test
    fun theCornersAreTheBrightestPoints() {
        assertEquals(1f, brightnessAt(45f), 0.01f)
        assertEquals(1f, brightnessAt(225f), 0.01f)
        assertTrue(brightnessAt(45f) > brightnessAt(75f))
        assertTrue(brightnessAt(225f) > brightnessAt(195f))
    }

    /**
     * The light reaches the same distance either side of a corner, and stops.
     *
     * How far is a matter of taste and lives in one constant; that it is
     * symmetric, and that it stops before the next corner, is not.
     */
    @Test
    fun eachCornerIsLitSymmetricallyAndStops() {
        for (away in listOf(10f, 20f, 30f, 40f)) {
            assertEquals(
                brightnessAt(225f - away),
                brightnessAt(225f + away),
                0.01f,
                "$away degrees either side of the top-left corner",
            )
        }
        assertEquals(0f, brightnessAt(135f), 0.001f, "dark again before the next corner")
    }

    /**
     * The light around the bottom-right corner runs past three o'clock and comes
     * out the other side, as far as it would have done had the seam not been
     * there.
     *
     * A sweep gradient's ends do not join, so a corner an eighth of a turn from
     * the start, lit across more than that, has to be cut in two and put back
     * together by hand. What says whether that worked is symmetry about the
     * *corner*, not about the seam: the two points fifty degrees either side of
     * the corner are 95 and 355, and the second one only exists if the tail was
     * put back.
     */
    @Test
    fun aGlowThatReachesPastTheSeamComesOutTheOtherSide() {
        for (away in listOf(20f, 35f, 50f)) {
            assertEquals(
                brightnessAt(45f + away),
                brightnessAt((45f - away + 360f) % 360f),
                0.02f,
                "$away degrees either side of the bottom-right corner",
            )
        }
        assertTrue(brightnessAt(350f) > 0f, "the tail has to be there at all")
    }

    /** The peaks are half a turn apart, or the light comes from two places. */
    @Test
    fun theLightComesFromOneDirection() {
        assertEquals(180f, degrees(lit[1] - lit[0]), 0.01f)
    }

    /**
     * Three o'clock is given the value the wrap would have had.
     *
     * A sweep gradient does not join up: whatever it holds at 1.0 is what it
     * holds at 0.0, so without this there is a seam down the right-hand edge
     * where half-lit meets transparent.
     */
    @Test
    fun theSeamIsClosed() {
        val start = stops.getValue(0f)
        val end = stops.getValue(1f)

        assertEquals(start, end, "the two ends have to agree")
    }

    /**
     * Every stop is a real one, in order.
     *
     * A corner an eighth of a turn from three o'clock lit across more than that
     * puts its first stop below zero, which is not a gradient stop at all -- and
     * that is a legal thing to ask for, since how far the light reaches is meant
     * to be adjustable.
     */
    @Test
    fun theStopsStayInRangeHoweverFarTheLightReaches() {
        val fractions = stops.keys.sorted()

        assertEquals(0f, fractions.first(), 0.001f)
        assertEquals(1f, fractions.last(), 0.001f)
        for (fraction in fractions) {
            assertTrue(fraction in 0f..1f, "$fraction is not a fraction of a turn")
        }
        assertEquals(fractions, fractions.sorted(), "and they have to ascend")
    }

    /** Whatever colour it is asked for, at the brightness it was asked for. */
    @Test
    fun theHighlightIsTheColourItWasGiven() {
        val amber = shineStops(Color(0xFFFFB000), alpha = 0.8f).toMap()
        val peak = amber.getValue(0.125f)

        assertEquals(0.8f, peak.alpha, 0.01f)
        assertTrue(peak.red > peak.blue, "amber, not white")
    }
}
