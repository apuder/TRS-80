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

package org.puder.trs80.shared.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DirectionTest {

    private fun at(x: Float, y: Float) = Direction.of(x, y, deadZone = 1f)

    @Test
    fun theFourStraightDirections() {
        assertEquals(Direction(right = true), at(10f, 0f))
        assertEquals(Direction(up = true), at(0f, 10f))
        assertEquals(Direction(left = true), at(-10f, 0f))
        assertEquals(Direction(down = true), at(0f, -10f))
    }

    /** Diagonals hold two keys at once, which is how these games were played. */
    @Test
    fun theDiagonalsHoldTwo() {
        assertEquals(Direction(right = true, up = true), at(10f, 10f))
        assertEquals(Direction(left = true, up = true), at(-10f, 10f))
        assertEquals(Direction(left = true, down = true), at(-10f, -10f))
        assertEquals(Direction(right = true, down = true), at(10f, -10f))
    }

    /** Straight right sits in the middle of its slice, not on its edge. */
    @Test
    fun aSliceIsCentredOnItsDirection() {
        assertEquals(Direction(right = true), at(10f, 2f))
        assertEquals(Direction(right = true), at(10f, -2f))
    }

    @Test
    fun nothingIsPressedInsideTheDeadZone() {
        assertEquals(Direction.NONE, at(0.5f, 0.5f))
        assertEquals(Direction.NONE, at(0f, 0f))
    }
}

class TiltDirectionTest {

    private val tilt = TiltDirection()

    @Test
    fun aSmallTiltIsNotEnough() {
        assertEquals(Direction.NONE, tilt.update(x = 0.5f, y = 0f))
    }

    @Test
    fun tiltingFarEnoughPresses() {
        assertTrue(tilt.update(x = 1.5f, y = 0f).left)
        assertTrue(tilt.update(x = -1.5f, y = 0f).right)
        assertTrue(tilt.update(x = 0f, y = 1.5f).up)
        assertTrue(tilt.update(x = 0f, y = -1.5f).down)
    }

    /**
     * Held past the press point, a direction stays held until it falls well
     * back. Without the gap a device resting near the threshold chatters the
     * key many times a second.
     */
    @Test
    fun aHeldDirectionSurvivesComingPartWayBack() {
        assertTrue(tilt.update(x = 1.5f, y = 0f).left)
        assertTrue(tilt.update(x = 0.5f, y = 0f).left)
        assertEquals(Direction.NONE, tilt.update(x = 0.2f, y = 0f))
    }

    @Test
    fun resettingLetsEverythingGo() {
        assertTrue(tilt.update(x = 1.5f, y = 0f).left)
        tilt.reset()
        assertEquals(Direction.NONE, tilt.update(x = 0.5f, y = 0f))
    }
}
