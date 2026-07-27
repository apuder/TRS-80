/*
 * Copyright 2012-2013, Arno Puder
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

package org.puder.trs80.keyboard

/**
 * A single entry of the on-screen keyboard layout, as parsed from `res/xml/keymap_*.xml`.
 *
 * @property label the text drawn on the on-screen key.
 * @property sym the SDL key symbol handed to the emulator core.
 * @property key the SDL key code handed to the emulator core. For printable keys this is the
 * character itself; the others are handed a synthetic code by the parser.
 * @property name the symbolic name the entry is looked up by, e.g. `key_ENTER`.
 * @property value the index of the entry, matching the `Key.TK_*` constants.
 */
data class KeyMap(
        val label: String,
        val sym: Int,
        val key: Int,
        val name: String,
        val value: Int)
