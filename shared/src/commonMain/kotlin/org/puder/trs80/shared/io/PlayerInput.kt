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
 * The two ways of steering a machine that are not a finger on glass.
 *
 * Both report the same thing the on-screen pads do -- a [Direction] -- so what
 * is above them does not know or care which one is running. Both are started
 * only for the keyboard layout that asks for one and stopped on the way out: an
 * accelerometer left running costs battery for a machine nobody is looking at.
 *
 * [start] returns whether there is anything to listen to, so a device with no
 * accelerometer and a device with no controller both say so rather than
 * appearing to work.
 */

/** A connected game controller, as directions and fire. */
expect class GamepadInput(onDirection: (Direction) -> Unit, onFire: (Boolean) -> Unit) {
    fun start(): Boolean
    fun stop()
}

/** The device's own tilt, as a direction. */
expect class MotionInput(onDirection: (Direction) -> Unit) {
    fun start(): Boolean
    fun stop()
}
