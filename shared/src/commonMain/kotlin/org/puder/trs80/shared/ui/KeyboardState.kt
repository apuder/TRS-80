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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.puder.trs80.shared.KeyMap
import org.puder.trs80.shared.KeyboardMapping

/** The mapping name of the key that swaps the compact keyboard's two pages. */
private const val ALT = "key_ALT"

private val SHIFT_KEYS = setOf("key_SHIFT_LEFT", "key_SHIFT_RIGHT")

/**
 * What the on-screen keyboard is doing: which page is showing, whether shift is
 * latched, and which keys are held.
 *
 * Separate from the drawing so the behavior can be tested without a screen —
 * and the behavior is the part worth testing, because it is not what a keyboard
 * usually does. Shift *latches*: tapping it holds it down and lights the shifted
 * labels, and it is released by the next key rather than by lifting a finger.
 * That is how the Android app has always worked, and it is the only way a
 * one-finger keyboard can type a shifted character at all.
 *
 * @param onKeyDown and [onKeyUp] receive the mapping entry to send to the core.
 */
class KeyboardState(
    val definition: KeyboardDefinition,
    private val onKeyDown: (KeyMap) -> Unit,
    private val onKeyUp: (KeyMap) -> Unit,
) {

    /** Which page of [definition] is showing. */
    var page by mutableIntStateOf(0)
        private set

    /** The latched shift key's name, or null when shift is not held. */
    var latchedShift by mutableStateOf<String?>(null)
        private set

    /** The keys currently held down, for drawing them pressed. */
    var pressed by mutableStateOf<Set<String>>(emptySet())
        private set

    /** The label to draw on [key], which changes when shift latches. */
    fun labelFor(key: KeyboardKey): String = when {
        key.name == ALT -> "Alt"
        else -> entryFor(key)?.label.orEmpty()
    }

    /** Whether [key] is a shift key, which is drawn latched rather than pressed. */
    fun isShift(key: KeyboardKey): Boolean = key.name in SHIFT_KEYS

    fun press(key: KeyboardKey) {
        pressed = pressed + key.name
        // Shift and Alt do their work on release, as the Android keys did:
        // pressing them must not type anything.
        if (key.name == ALT || isShift(key)) {
            return
        }
        entryFor(key)?.let(onKeyDown)
    }

    fun release(key: KeyboardKey) {
        pressed = pressed - key.name

        if (key.name == ALT) {
            page = (page + 1) % definition.pages.size
            return
        }

        if (isShift(key)) {
            val entry = KeyboardMapping.byName(key.name) ?: return
            if (latchedShift == key.name) {
                latchedShift = null
                onKeyUp(entry)
            } else {
                latchedShift = key.name
                onKeyDown(entry)
            }
            return
        }

        entryFor(key)?.let(onKeyUp)

        // Any other key releases a latched shift, so shift applies to exactly
        // one keystroke unless it is tapped again.
        latchedShift?.let { shift ->
            latchedShift = null
            KeyboardMapping.byName(shift)?.let(onKeyUp)
        }
    }

    /** @return the mapping entry [key] currently stands for, honouring shift. */
    private fun entryFor(key: KeyboardKey): KeyMap? {
        val name = if (latchedShift != null && key.shifted != null) key.shifted else key.name
        return KeyboardMapping.byName(name)
    }
}
