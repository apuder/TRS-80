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

package org.puder.trs80.shared.io

import org.puder.trs80.shared.ui.Direction
import org.puder.trs80.shared.ui.TiltDirection
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers which way a tilt steers, from readings as Core Motion gives them.
 *
 * Each case is a gesture: an edge of the screen tipped down, and what Core
 * Motion reads for it -- the pull of gravity in the device's own frame, in g,
 * so the edge that goes down is the axis that goes positive. Getting the sign
 * of that wrong is how every direction once came out backwards.
 */
class MotionInputTest {

    /** A tip of about 17 degrees: well past the press point. */
    private val tip = 0.3

    private fun steer(x: Double, y: Double, quarterTurns: Int): Direction {
        val (screenX, screenY) = coreMotionTilt(x, y, quarterTurns)
        return TiltDirection().update(screenX, screenY)
    }

    @Test
    fun lyingFlatPressesNothing() {
        assertEquals(Direction.NONE, steer(0.0, 0.0, quarterTurns = 0))
    }

    @Test
    fun uprightTheEdgeTippedDownIsTheWayItGoes() {
        assertEquals(Direction(right = true), steer(x = tip, y = 0.0, quarterTurns = 0))
        assertEquals(Direction(left = true), steer(x = -tip, y = 0.0, quarterTurns = 0))
        // The top tipped away from the holder, as a stick is pushed forward.
        assertEquals(Direction(up = true), steer(x = 0.0, y = tip, quarterTurns = 0))
        assertEquals(Direction(down = true), steer(x = 0.0, y = -tip, quarterTurns = 0))
    }

    /** The device's top to the left: the screen's right edge is its bottom. */
    @Test
    fun turnedAnticlockwiseTheScreensEdgesStillSteer() {
        assertEquals(Direction(right = true), steer(x = 0.0, y = -tip, quarterTurns = 1))
        assertEquals(Direction(up = true), steer(x = tip, y = 0.0, quarterTurns = 1))
    }

    /** The device's top to the right: the screen's right edge is its top. */
    @Test
    fun turnedClockwiseTheScreensEdgesStillSteer() {
        assertEquals(Direction(right = true), steer(x = 0.0, y = tip, quarterTurns = 3))
        assertEquals(Direction(up = true), steer(x = -tip, y = 0.0, quarterTurns = 3))
    }

    @Test
    fun upsideDownTheScreensEdgesStillSteer() {
        assertEquals(Direction(right = true), steer(x = -tip, y = 0.0, quarterTurns = 2))
        assertEquals(Direction(up = true), steer(x = 0.0, y = -tip, quarterTurns = 2))
    }
}
