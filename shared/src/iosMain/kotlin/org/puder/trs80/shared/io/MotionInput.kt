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
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

private const val TAG = "MotionInput"

/** How often the accelerometer is read; roughly a frame at 60Hz. */
private const val INTERVAL_SECONDS = 1.0 / 60.0

/**
 * The device's own tilt, as a direction.
 *
 * The accelerometer reports gravity, so a device lying flat reads zero on both
 * axes and tipping it moves them — which is what makes it a stick you steer by
 * moving the whole machine.
 */
@OptIn(ExperimentalForeignApi::class)
class MotionInput(private val onDirection: (Direction) -> Unit) {
    private val manager = CMMotionManager()
    private val tilt = org.puder.trs80.shared.ui.TiltDirection()

    /** @return whether there is an accelerometer to listen to. */
    fun start(): Boolean {
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
                // Gravity points down, so tipping the top away gives a negative
                // y; the sign here is what makes that read as up.
                onDirection(tilt.update(x = x.toFloat(), y = -y.toFloat()))
            }
        }
        return true
    }

    fun stop() {
        manager.stopAccelerometerUpdates()
        tilt.reset()
        onDirection(Direction.NONE)
    }
}
