/*
 * Copyright 2017, Sascha Haeberling
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

package org.puder.trs80.configuration

/**
 * Various keyboard layouts.
 *
 * @property id the persisted ID of the layout. These IDs end up in the user's saved
 * configurations, so they must never be renumbered.
 */
enum class KeyboardLayout(@JvmField val id: Int) {
    KEYBOARD_LAYOUT_ORIGINAL(0),
    KEYBOARD_LAYOUT_COMPACT(1),
    KEYBOARD_LAYOUT_JOYSTICK(2),
    KEYBOARD_GAME_CONTROLLER(3),
    KEYBOARD_TILT(4),
    KEYBOARD_EXTERNAL(5);

    companion object {
        private val BY_ID: Map<Int, KeyboardLayout> =
            KeyboardLayout.entries.associateBy(KeyboardLayout::id)

        /**
         * @return The layout with the given [id], or null if no layout uses that ID.
         */
        @JvmStatic
        fun fromId(id: Int): KeyboardLayout? = BY_ID[id]
    }
}
