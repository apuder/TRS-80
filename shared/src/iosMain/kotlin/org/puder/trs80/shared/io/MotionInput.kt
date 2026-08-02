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
    private val tilt = org.puder.trs80.shared.ui.TiltDirection()

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
                // Gravity points down, so tipping the top away gives a negative
                // y; the sign here is what makes that read as up.
                //
                // Scaled to m/s^2, which is what the thresholds are written in:
                // Core Motion reports in g and Android's sensor does not, so
                // without this a press wanted a full 1g -- the device on its
                // side -- and tilting did next to nothing.
                onDirection(
                    tilt.update(
                        x = x.toFloat() * GRAVITY,
                        y = -y.toFloat() * GRAVITY,
                    )
                )
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
