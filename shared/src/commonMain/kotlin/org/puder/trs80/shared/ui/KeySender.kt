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

import org.puder.trs80.shared.KeyMap
import org.puder.trs80.shared.KeyboardMapping

/** The cursor keys and fire, which is all a joystick, a tilt or a gamepad sends. */
internal const val KEY_LEFT = "key_LEFT"
internal const val KEY_RIGHT = "key_RIGHT"
internal const val KEY_UP = "key_UP"
internal const val KEY_DOWN = "key_DOWN"
internal const val KEY_FIRE = "key_SPACE"

/**
 * Sends a key to the machine by name.
 *
 * The on-screen keyboard works in [KeyboardKey]s, which come from a grid. A
 * joystick, a tilt and a gamepad have no grid and nothing to look a key up in,
 * so they say which key they mean and this finds it.
 */
class KeySender(
    private val onKeyDown: (KeyMap) -> Unit,
    private val onKeyUp: (KeyMap) -> Unit,
) {
    fun press(name: String) {
        KeyboardMapping.byName(name)?.let(onKeyDown)
    }

    fun release(name: String) {
        KeyboardMapping.byName(name)?.let(onKeyUp)
    }
}

/**
 * Holds the four cursor keys in a given state, sending only the changes.
 *
 * Every pointing device here works the same way: it decides which directions
 * are being asked for and this turns that into key events. Sending a press for
 * a key already held would be a repeat, and the machine reads repeats as new
 * presses.
 */
class DirectionKeys(private val sender: KeySender) {
    private var left = false
    private var right = false
    private var up = false
    private var down = false

    fun set(left: Boolean, right: Boolean, up: Boolean, down: Boolean) {
        apply(this.left, left, KEY_LEFT) { this.left = it }
        apply(this.right, right, KEY_RIGHT) { this.right = it }
        apply(this.up, up, KEY_UP) { this.up = it }
        apply(this.down, down, KEY_DOWN) { this.down = it }
    }

    /** Lets go of everything, for when the finger leaves or the screen does. */
    fun releaseAll() = set(left = false, right = false, up = false, down = false)

    private inline fun apply(
        was: Boolean,
        now: Boolean,
        key: String,
        store: (Boolean) -> Unit,
    ) {
        if (was == now) {
            return
        }
        store(now)
        if (now) sender.press(key) else sender.release(key)
    }
}
