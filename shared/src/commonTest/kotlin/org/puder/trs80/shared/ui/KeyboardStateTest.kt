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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyboardStateTest {

    private val sent = mutableListOf<String>()
    private val state = KeyboardState(
        definition = COMPACT_KEYBOARD,
        onKeyDown = { sent += "down ${it.name}" },
        onKeyUp = { sent += "up ${it.name}" },
    )

    private fun tap(key: KeyboardKey) {
        state.press(key)
        state.release(key)
    }

    private val a = KeyboardKey("key_A")
    private val one = KeyboardKey("key_1", shifted = "key_EXCLAMATION_MARK")
    private val shift = KeyboardKey("key_SHIFT_LEFT")
    private val alt = KeyboardKey("key_ALT")

    @Test
    fun anOrdinaryKeyGoesDownAndUp() {
        tap(a)

        assertEquals(listOf("down key_A", "up key_A"), sent)
    }

    /** Pressing must type; the release must not type a second time. */
    @Test
    fun pressAndReleaseAreDistinct() {
        state.press(a)
        assertEquals(listOf("down key_A"), sent)

        state.release(a)
        assertEquals(listOf("down key_A", "up key_A"), sent)
    }

    @Test
    fun shiftLatchesOnTapRatherThanBeingHeld() {
        tap(shift)

        assertEquals("key_SHIFT_LEFT", state.latchedShift)
        assertEquals(listOf("down key_SHIFT_LEFT"), sent)
    }

    @Test
    fun aLatchedShiftChangesWhatTheNextKeyTypes() {
        tap(shift)
        sent.clear()

        tap(one)

        assertEquals(
            listOf("down key_EXCLAMATION_MARK", "up key_EXCLAMATION_MARK", "up key_SHIFT_LEFT"),
            sent,
        )
    }

    /** Shift applies to exactly one keystroke, then lets go by itself. */
    @Test
    fun theNextKeyReleasesTheLatch() {
        tap(shift)
        tap(one)

        assertNull(state.latchedShift)

        sent.clear()
        tap(one)
        assertEquals(listOf("down key_1", "up key_1"), sent)
    }

    @Test
    fun tappingShiftTwiceLetsGoWithoutTyping() {
        tap(shift)
        sent.clear()

        tap(shift)

        assertNull(state.latchedShift)
        assertEquals(listOf("up key_SHIFT_LEFT"), sent)
    }

    /** A key with no shifted twin types the same thing either way. */
    @Test
    fun shiftDoesNothingToAKeyWithoutAShiftedTwin() {
        tap(shift)
        sent.clear()

        tap(a)

        assertEquals(listOf("down key_A", "up key_A", "up key_SHIFT_LEFT"), sent)
    }

    @Test
    fun theLabelFollowsTheLatch() {
        assertEquals("1", state.labelFor(one))

        tap(shift)

        assertEquals("!", state.labelFor(one))
    }

    @Test
    fun altSwapsPagesAndTypesNothing() {
        assertEquals(0, state.page)

        tap(alt)

        assertEquals(1, state.page)
        assertTrue(sent.isEmpty(), "Alt typed $sent")

        tap(alt)
        assertEquals(0, state.page)
    }

    @Test
    fun heldKeysAreReportedForDrawing() {
        state.press(a)
        assertTrue("key_A" in state.pressed)

        state.release(a)
        assertTrue("key_A" !in state.pressed)
    }

    /** Every key in both grids has to resolve, or it types nothing at all. */
    @Test
    fun everyKeyInEveryLayoutResolves() {
        for (definition in listOf(ORIGINAL_KEYBOARD, COMPACT_KEYBOARD)) {
            val keys = definition.pages.flatMap { it.rows }.flatten()
            for (key in keys) {
                val label: String = KeyboardState(definition, {}, {}).labelFor(key)
                assertTrue(label.isNotEmpty(), "${key.name} has no label")
            }
        }
    }

    @Test
    fun theOriginalKeyboardHasOnePageAndTheCompactHasTwo() {
        assertEquals(1, ORIGINAL_KEYBOARD.pages.size)
        assertEquals(2, COMPACT_KEYBOARD.pages.size)
    }
}
