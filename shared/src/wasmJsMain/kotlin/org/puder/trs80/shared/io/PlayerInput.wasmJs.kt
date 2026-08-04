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

/**
 * The Gamepad API is there for the taking, and this is not it yet.
 *
 * A browser reports pads through navigator.getGamepads(), polled per frame
 * rather than delivered as events, so it wants a frame loop -- which the app
 * will have anyway once there is a machine drawing into a canvas. Until then
 * start() says no, and the on-screen controls are what a player uses.
 */
actual class GamepadInput actual constructor(
    onDirection: (Direction) -> Unit,
    onFire: (Boolean) -> Unit,
) {
    actual fun start(): Boolean = false
    actual fun stop() = Unit
}

/**
 * Tilting a laptop is not a gesture anyone makes.
 *
 * DeviceOrientation exists on a phone browser and needs a permission prompt on
 * iOS Safari; it is worth having when the web app is worth using on a phone,
 * and not before.
 */
actual class MotionInput actual constructor(onDirection: (Direction) -> Unit) {
    actual fun start(): Boolean = false
    actual fun stop() = Unit
}
