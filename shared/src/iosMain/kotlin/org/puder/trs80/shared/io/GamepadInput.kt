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

import org.puder.trs80.shared.Log
import org.puder.trs80.shared.ui.Direction
import platform.Foundation.NSNotificationCenter
import platform.GameController.GCController
import platform.GameController.GCControllerDidConnectNotification
import platform.GameController.GCControllerDidDisconnectNotification
import platform.GameController.GCExtendedGamepad
import platform.darwin.NSObjectProtocol

private const val TAG = "GamepadInput"

/**
 * How far a stick has to be pushed before it counts as a direction.
 *
 * A physical stick rarely rests at exactly zero, and a game steered by one that
 * never quite centres is unplayable.
 */
private const val DEAD_ZONE = 0.5f

/**
 * A connected game controller, as directions and fire.
 *
 * Both the d-pad and the left stick steer, because a player will reach for
 * whichever their controller has and neither is wrong. Every face button fires:
 * the machine has one action, so asking which button it is would be a question
 * with no useful answer.
 */
actual class GamepadInput actual constructor(
    private val onDirection: (Direction) -> Unit,
    private val onFire: (Boolean) -> Unit,
) {
    private var connectObserver: NSObjectProtocol? = null
    private var disconnectObserver: NSObjectProtocol? = null

    /** @return whether a controller is connected right now. */
    actual fun start(): Boolean {
        val centre = NSNotificationCenter.defaultCenter
        connectObserver = centre.addObserverForName(
            name = GCControllerDidConnectNotification,
            `object` = null,
            queue = null,
        ) { _ -> attachAll() }
        disconnectObserver = centre.addObserverForName(
            name = GCControllerDidDisconnectNotification,
            `object` = null,
            queue = null,
        ) { _ ->
            onDirection(Direction.NONE)
            onFire(false)
        }
        attachAll()
        return GCController.controllers().isNotEmpty()
    }

    actual fun stop() {
        val centre = NSNotificationCenter.defaultCenter
        connectObserver?.let(centre::removeObserver)
        disconnectObserver?.let(centre::removeObserver)
        connectObserver = null
        disconnectObserver = null
        GCController.controllers().forEach {
            (it as? GCController)?.extendedGamepad?.setValueChangedHandler(null)
        }
        onDirection(Direction.NONE)
        onFire(false)
    }

    private fun attachAll() {
        GCController.controllers().forEach { controller ->
            val pad = (controller as? GCController)?.extendedGamepad
            if (pad == null) {
                Log.i(TAG, "A controller without an extended gamepad profile; ignoring it.")
                return@forEach
            }
            pad.setValueChangedHandler { gamepad, _ -> report(gamepad) }
        }
    }

    private fun report(pad: GCExtendedGamepad?) {
        if (pad == null) {
            return
        }
        // The d-pad reports whole presses; the stick reports how far it is
        // pushed, so it gets a dead zone and the d-pad does not need one.
        val x = pad.dpad.xAxis.value.takeIf { it != 0f } ?: pad.leftThumbstick.xAxis.value
        val y = pad.dpad.yAxis.value.takeIf { it != 0f } ?: pad.leftThumbstick.yAxis.value
        onDirection(Direction.of(x, y, DEAD_ZONE))
        onFire(
            pad.buttonA.pressed || pad.buttonB.pressed ||
                pad.buttonX.pressed || pad.buttonY.pressed
        )
    }
}
