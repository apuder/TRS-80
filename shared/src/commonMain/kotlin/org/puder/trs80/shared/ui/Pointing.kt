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

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/** The eight directions a stick can point, each 45 degrees wide. */
private const val SLICE = 45.0

/** Half a slice, so that straight right sits in the middle of its own slice. */
private const val START = SLICE / 2

/**
 * Which way a stick is being pushed.
 *
 * @param x grows to the right and [y] grows upwards, in whatever units the
 * caller has — only the direction and whether it clears [deadZone] matter.
 */
data class Direction(
    val left: Boolean = false,
    val right: Boolean = false,
    val up: Boolean = false,
    val down: Boolean = false,
) {
    companion object {
        val NONE = Direction()

        /**
         * Reads an offset from the centre as a direction.
         *
         * Eight slices rather than four, so the diagonals hold two keys at once
         * — which is how these games were played, and what the Android app did.
         */
        fun of(x: Float, y: Float, deadZone: Float): Direction {
            if (sqrt(x * x + y * y) < deadZone) {
                return NONE
            }
            var degrees = atan2(y.toDouble(), x.toDouble()) * 180.0 / PI
            if (degrees < 0) {
                degrees += 360.0
            }
            return when {
                degrees < START || degrees >= START + 7 * SLICE -> Direction(right = true)
                degrees < START + SLICE -> Direction(right = true, up = true)
                degrees < START + 2 * SLICE -> Direction(up = true)
                degrees < START + 3 * SLICE -> Direction(left = true, up = true)
                degrees < START + 4 * SLICE -> Direction(left = true)
                degrees < START + 5 * SLICE -> Direction(left = true, down = true)
                degrees < START + 6 * SLICE -> Direction(down = true)
                else -> Direction(right = true, down = true)
            }
        }
    }
}

/** How far the device has to be tilted before a direction is taken as meant. */
private const val TILT_PRESS = 1.0f

/**
 * How far it has to come back before that direction is let go.
 *
 * Lower than [TILT_PRESS] on purpose: without the gap, a device held near the
 * threshold would chatter the key on and off many times a second.
 */
private const val TILT_RELEASE = 0.3f

/**
 * Turns the accelerometer into cursor keys.
 *
 * The thresholds and the hysteresis are the Android app's, so a game plays the
 * same on both. Tilting the top of the device away presses up.
 */
class TiltDirection {
    private var current = Direction.NONE

    /** @param x tilt to the right, [y] tilt away from the holder. */
    fun update(x: Float, y: Float): Direction {
        current = Direction(
            left = hold(current.left, x, TILT_PRESS, TILT_RELEASE),
            right = hold(current.right, -x, TILT_PRESS, TILT_RELEASE),
            up = hold(current.up, y, TILT_PRESS, TILT_RELEASE),
            down = hold(current.down, -y, TILT_PRESS, TILT_RELEASE),
        )
        return current
    }

    fun reset() {
        current = Direction.NONE
    }

    /** Once held, a direction stays held until it falls back past [release]. */
    private fun hold(held: Boolean, value: Float, press: Float, release: Float): Boolean =
        if (held) value > release else value > press
}
