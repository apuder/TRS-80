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

package org.puder.trs80.shared

import kotlinx.browser.document
import org.puder.trs80.shared.ui.trs80KeyForCharacter
import org.w3c.dom.events.KeyboardEvent

/**
 * A real keyboard, in a browser.
 *
 * The counterpart of `Trs80Activity.dispatchKeyEvent` and iOS's
 * `KeyForwardingController`: keys are taken above the app rather than inside it,
 * because neither Compose nor a canvas sees them the way a machine needs to --
 * held down, released, and never turned into text on the way.
 *
 * Switched on only while a machine is on screen, which is what the interface is
 * for. Off, the page keeps its own keys: Cmd-R still reloads, and typing in the
 * library's search field goes to the field rather than to a machine that is not
 * there.
 */
class BrowserKeys : HardwareKeys {

    override var enabled: Boolean = false

    init {
        document.addEventListener("keydown", { event ->
            forward(event as KeyboardEvent, down = true)
        })
        document.addEventListener("keyup", { event ->
            forward(event as KeyboardEvent, down = false)
        })
    }

    /** What each key is holding down, so a release finds the same key it pressed. */
    private val held = mutableMapOf<String, KeyMap>()

    private fun forward(event: KeyboardEvent, down: Boolean) {
        if (!enabled) {
            return
        }
        // Cmd and Alt belong to the browser and to the Mac: a machine has
        // neither, and swallowing them would take Cmd-R, Cmd-W and Cmd-Tab
        // with them. The one modifier it does understand is Ctrl, which is
        // BREAK and CLEAR below.
        if (event.metaKey || event.altKey) {
            return
        }
        // A machine's keyboard repeats by being held, not by the host saying so.
        if (down && event.repeat) {
            event.preventDefault()
            return
        }
        val key = if (down) {
            trs80KeyFor(event)?.also { held[event.code] = it }
        } else {
            held.remove(event.code)
        } ?: return

        event.preventDefault()
        if (down) {
            core?.keyDown(key.sym, key.key)
        } else {
            core?.keyUp(key.sym, key.key)
        }
    }

    /** The machine to type at; set once, when there is one. */
    var core: EmulatorCore? = null
}

/**
 * What a browser's key means to a TRS-80.
 *
 * `key` rather than `code`, so a letter is the letter the layout produces and
 * not the one printed on an American keyboard. The named keys are the same set
 * Android maps, for the same reasons: a machine with no Backspace uses its left
 * arrow, and Escape is the only sensible place to put BREAK.
 */
private fun trs80KeyFor(event: KeyboardEvent): KeyMap? = when (event.key) {
    "Enter" -> KeyboardMapping.byName("key_ENTER")
    "Backspace", "ArrowLeft" -> KeyboardMapping.byName("key_LEFT")
    "ArrowRight" -> KeyboardMapping.byName("key_RIGHT")
    "ArrowUp" -> KeyboardMapping.byName("key_UP")
    "ArrowDown" -> KeyboardMapping.byName("key_DOWN")
    "Escape" -> KeyboardMapping.byName("key_BREAK")
    // Ctrl-B and Ctrl-C, the only modified keys the machine knows.
    "b", "B" -> if (event.ctrlKey) KeyboardMapping.byName("key_BREAK") else characterKey(event)
    "c", "C" -> if (event.ctrlKey) KeyboardMapping.byName("key_CLEAR") else characterKey(event)
    else -> characterKey(event)
}

/** A printable key is whatever single character the browser says it produced. */
private fun characterKey(event: KeyboardEvent): KeyMap? =
    event.key.singleOrNull()?.let(::trs80KeyForCharacter)
