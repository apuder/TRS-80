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

import org.puder.trs80.shared.KeyboardLayout

/**
 * One key: which entry of the keyboard mapping it stands for, what it becomes
 * when shift is down, and how wide it is.
 *
 * @property name the mapping entry's name, e.g. `key_ENTER`.
 * @property shifted what it stands for while shift is latched, if anything.
 * @property size the key's width, in units of one ordinary key.
 */
data class KeyboardKey(
    val name: String,
    val shifted: String? = null,
    val size: Int = 1,
)

/** One page of keys. The compact keyboard has two; the original has one. */
data class KeyboardPage(val rows: List<List<KeyboardKey>>)

/** A whole on-screen keyboard. */
data class KeyboardDefinition(val pages: List<KeyboardPage>)

private fun k(name: String, shifted: String? = null, size: Int = 1) =
    KeyboardKey(name, shifted, size)

/**
 * @return the keys for [layout], or null for the layouts that are not key grids.
 *
 * The joystick and tilt layouts are gesture surfaces rather than keys — the
 * Android originals contain no key at all — and the game controller and external
 * layouts are hardware. They are not ported here.
 */
fun keyboardFor(layout: KeyboardLayout?): KeyboardDefinition? = when (layout) {
    KeyboardLayout.KEYBOARD_LAYOUT_ORIGINAL -> ORIGINAL_KEYBOARD
    KeyboardLayout.KEYBOARD_LAYOUT_COMPACT -> COMPACT_KEYBOARD
    else -> null
}

/**
 * The layouts worth offering, which is the ones this app can draw.
 *
 * Derived from [keyboardFor] rather than listed again, so a layout starts
 * being offered the moment it can be drawn and never before. Offering the rest
 * meant picking Joystick or Tilt and getting the original keyboard anyway --
 * a choice that read as though it did something.
 *
 * External is not among them and should not be: on Android it is never a
 * choice either, it is what an attached hardware keyboard makes it.
 *
 * Deliberately lazy. The definitions it asks about are declared further down
 * this file, and a top-level val is initialized in declaration order -- so
 * computing this eagerly asked [keyboardFor] about keyboards that did not exist
 * yet and got an empty list, leaving the editor offering nothing.
 */
val offeredKeyboardLayouts: List<KeyboardLayout> by lazy {
    KeyboardLayout.entries.filter { keyboardFor(it) != null }
}

/*
 * The two grids below were extracted from the Android layout XML rather than
 * retyped: 123 keys, each with its shifted twin and its width, and a mistake in
 * any one of them would be a key that types the wrong character on a machine
 * nobody has to check it against. Every name was verified to resolve against
 * KeyboardMapping at extraction time.
 */

/**
 * The full TRS-80 keyboard, as the machine had it. One page.
 */
val ORIGINAL_KEYBOARD: KeyboardDefinition = KeyboardDefinition(
    listOf(
        KeyboardPage(
            listOf(
                listOf(k("key_1", shifted = "key_EXCLAMATION_MARK"), k("key_2", shifted = "key_QUOT"), k("key_3", shifted = "key_HASH"), k("key_4", shifted = "key_DOLLAR"), k("key_5", shifted = "key_PERCENT"), k("key_6", shifted = "key_AMP"), k("key_7", shifted = "key_APOS"), k("key_8", shifted = "key_BR_OPEN"), k("key_9", shifted = "key_BR_CLOSE"), k("key_0"), k("key_COLON", shifted = "key_ASTERIX"), k("key_MINUS", shifted = "key_EQUAL"), k("key_BREAK", size = 2)),
                listOf(k("key_UP"), k("key_Q"), k("key_W"), k("key_E"), k("key_R"), k("key_T"), k("key_Y"), k("key_U"), k("key_I"), k("key_O"), k("key_P"), k("key_AT"), k("key_LEFT"), k("key_RIGHT")),
                listOf(k("key_DOWN"), k("key_A"), k("key_S"), k("key_D"), k("key_F"), k("key_G"), k("key_H"), k("key_J"), k("key_K"), k("key_L"), k("key_SEMICOLON", shifted = "key_ADD"), k("key_ENTER", size = 2), k("key_CLEAR", size = 2)),
                listOf(k("key_SHIFT_LEFT", size = 2), k("key_Z"), k("key_X"), k("key_C"), k("key_V"), k("key_B"), k("key_N"), k("key_M"), k("key_COMMA", shifted = "key_LT"), k("key_DOT", shifted = "key_GT"), k("key_SLASH", shifted = "key_QUESTION"), k("key_SHIFT_RIGHT", size = 2)),
                listOf(k("key_SPACE", size = 10)),
            ),
        ),
    ),
)

/**
 * A smaller keyboard in two pages, swapped by the Alt key, for narrow screens.
 */
val COMPACT_KEYBOARD: KeyboardDefinition = KeyboardDefinition(
    listOf(
        KeyboardPage(
            listOf(
                listOf(k("key_Q"), k("key_W"), k("key_E"), k("key_R"), k("key_T"), k("key_Y"), k("key_U"), k("key_I"), k("key_O"), k("key_P")),
                listOf(k("key_UP"), k("key_A"), k("key_S"), k("key_D"), k("key_F"), k("key_G"), k("key_H"), k("key_J"), k("key_K"), k("key_L")),
                listOf(k("key_DOWN"), k("key_Z"), k("key_X"), k("key_C"), k("key_V"), k("key_B"), k("key_N"), k("key_M"), k("key_LEFT"), k("key_RIGHT")),
                listOf(k("key_ALT"), k("key_BREAK_SHORT"), k("key_SPACE", size = 4), k("key_CLEAR_SHORT"), k("key_ENTER", size = 2)),
            ),
        ),
        KeyboardPage(
            listOf(
                listOf(k("key_1"), k("key_2"), k("key_3"), k("key_ADD"), k("key_MINUS"), k("key_AT"), k("key_HASH"), k("key_BR_OPEN"), k("key_BR_CLOSE")),
                listOf(k("key_4"), k("key_5"), k("key_6"), k("key_ASTERIX"), k("key_SLASH"), k("key_DOLLAR"), k("key_QUESTION"), k("key_LT"), k("key_GT")),
                listOf(k("key_7"), k("key_8"), k("key_9"), k("key_COMMA"), k("key_EQUAL"), k("key_PERCENT"), k("key_COLON"), k("key_APOS"), k("key_LEFT")),
                listOf(k("key_ALT"), k("key_0"), k("key_SPACE", size = 2), k("key_DOT"), k("key_SEMICOLON"), k("key_EXCLAMATION_MARK"), k("key_AMP"), k("key_QUOT")),
            ),
        ),
    ),
)
