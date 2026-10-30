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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.puder.trs80.shared.Log
import org.puder.trs80.shared.ui.Direction
import org.puder.trs80.shared.ui.TiltDirection
import org.puder.trs80.shared.ui.screenTilt
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown

private const val TAG = "MotionInput"

/** How often the accelerometer is read; roughly a frame at 60Hz. */
private const val INTERVAL_SECONDS = 1.0 / 60.0

/** One g in m/s^2, the units the shared thresholds are in. */
private const val GRAVITY = 9.80665f

/**
 * The device's own tilt, as a direction.
 *
 * The accelerometer reports gravity, so a device lying flat reads zero on both
 * axes and tipping it moves them — which is what makes it a stick you steer by
 * moving the whole machine.
 */
@OptIn(ExperimentalForeignApi::class)
actual class MotionInput actual constructor(private val onDirection: (Direction) -> Unit) {
    private val manager = CMMotionManager()
    private val tilt = TiltDirection()

    /** @return whether there is an accelerometer to listen to. */
    actual fun start(): Boolean {
        if (!manager.accelerometerAvailable) {
            Log.i(TAG, "No accelerometer on this device.")
            return false
        }
        manager.accelerometerUpdateInterval = INTERVAL_SECONDS
        manager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { data, error ->
            if (error != null || data == null) {
                return@startAccelerometerUpdatesToQueue
            }
            data.acceleration.useContents {
                val (screenX, screenY) = coreMotionTilt(x, y, screenQuarterTurns())
                onDirection(tilt.update(screenX, screenY))
            }
        }
        return true
    }

    actual fun stop() {
        manager.stopAccelerometerUpdates()
        tilt.reset()
        onDirection(Direction.NONE)
    }
}

/**
 * A Core Motion reading, put into the terms [TiltDirection] takes.
 *
 * Core Motion reports the pull of gravity, in g; the shared mapping takes
 * Android's reading, the push holding the device up, in m/s^2. Same axes,
 * opposite sign -- flat on a table this reads z = -1 where Android reads
 * +9.81. Passing the reading through with Android's signs, as this once did,
 * steered every direction backwards.
 *
 * Scaled as well as turned round: the thresholds are in m/s^2, and in g a
 * press wanted the device on its side.
 */
internal fun coreMotionTilt(x: Double, y: Double, quarterTurns: Int): Pair<Float, Float> =
    screenTilt(-x.toFloat() * GRAVITY, -y.toFloat() * GRAVITY, quarterTurns)

/**
 * How far the screen is turned, counted the way [screenTilt] counts it.
 *
 * Asked at every reading rather than once at the start: the device can be
 * turned from one landscape to the other while the machine runs, and with the
 * same keyboard both ways nothing else would notice.
 */
private fun screenQuarterTurns(): Int =
    when (UIApplication.sharedApplication.keyWindow?.windowScene?.interfaceOrientation) {
        // UIKit names these for where the home button would be, so its
        // landscape right is the device turned anticlockwise, top to the left.
        UIInterfaceOrientationLandscapeRight -> 1
        UIInterfaceOrientationPortraitUpsideDown -> 2
        UIInterfaceOrientationLandscapeLeft -> 3
        else -> 0
    }
