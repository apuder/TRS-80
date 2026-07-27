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

import android.content.res.XmlResourceParser
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import org.puder.trs80.R
import org.puder.trs80.TRS80Application
import org.puder.trs80.XTRS
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

/**
 * Translates on-screen key presses, hardware key events and game controller buttons into the SDL
 * key events consumed by the emulator core.
 */
class KeyboardManager {

    private val shiftableKeys = mutableListOf<Key>()
    private val handler = Handler(Looper.getMainLooper())

    /** The shift key currently held down, or [Key.TK_NONE] if no shift key is down. */
    var pressedShiftKey = Key.TK_NONE
        private set

    /** @return The key map entry for the given `Key.TK_*` id. */
    fun getKeyMap(id: Int): KeyMap = KEYBOARD_MAPPING[id]

    /** @return The key map entry with the given symbolic [name], or `null` if there is none. */
    fun getKeyMap(name: String): KeyMap? = KEYBOARD_MAPPING.find { it.name == name }

    /**
     * Types a single character, as if the matching on-screen key had been tapped. The key-up
     * event follows after [KEY_UP_DELAY].
     */
    fun injectKey(ch: Char) {
        val keyName = when (ch) {
            ' ' -> "key_SPACE"
            ':' -> "key_COLON"
            '"' -> "key_QUOT"
            '/' -> "key_SLASH"
            '\n' -> "key_ENTER"
            else -> "key_$ch"
        }
        val key = requireNotNull(getKeyMap(keyName)) { "No key map entry named '$keyName'." }
        keyDown(key.value)
        handler.postDelayed({ keyUp(key.value) }, KEY_UP_DELAY)
    }

    /** Registers a key that has to redraw itself whenever shift is pressed or released. */
    fun addShiftableKey(key: Key) {
        shiftableKeys.add(key)
    }

    /** Puts all shiftable keys into their shifted state. */
    fun shiftKeys(shiftKey: Int) {
        pressedShiftKey = shiftKey
        shiftableKeys.forEach(Key::shift)
    }

    /** Returns all shiftable keys to their unshifted state. */
    fun unshiftKeys() {
        pressedShiftKey = Key.TK_NONE
        shiftableKeys.forEach(Key::unshift)
    }

    /** Releases all four cursor keys, whether they were down or not. */
    fun allCursorKeysUp() {
        keyUp(Key.TK_LEFT)
        keyUp(Key.TK_RIGHT)
        keyUp(Key.TK_UP)
        keyUp(Key.TK_DOWN)
    }

    fun pressKeyDown() = keyDown(Key.TK_DOWN)

    fun pressKeyUp() = keyDown(Key.TK_UP)

    fun pressKeyLeft() = keyDown(Key.TK_LEFT)

    fun pressKeyRight() = keyDown(Key.TK_RIGHT)

    fun pressKeySpace() = keyDown(Key.TK_SPACE)

    fun unpressKeyDown() = keyUp(Key.TK_DOWN)

    fun unpressKeyUp() = keyUp(Key.TK_UP)

    fun unpressKeyLeft() = keyUp(Key.TK_LEFT)

    fun unpressKeyRight() = keyUp(Key.TK_RIGHT)

    fun unpressKeySpace() = keyUp(Key.TK_SPACE)

    /** Sends a key-down event for the given `Key.TK_*` id. */
    fun keyDown(keyId: Int) = getKeyMap(keyId).let { keyDown(it.sym, it.key) }

    /** Sends a raw SDL key-down event. */
    fun keyDown(sym: Int, key: Int) = XTRS.addKeyEvent(SDL_KEYDOWN, sym, key)

    /** Sends a key-up event for the given `Key.TK_*` id. */
    fun keyUp(keyId: Int) = getKeyMap(keyId).let { keyUp(it.sym, it.key) }

    /** Sends a raw SDL key-up event. */
    fun keyUp(sym: Int, key: Int) = XTRS.addKeyEvent(SDL_KEYUP, sym, key)

    /**
     * Forwards a hardware key-down event to the emulator.
     *
     * @return Whether the event mapped to a TRS-80 key and was consumed.
     */
    fun keyDown(event: KeyEvent): Boolean {
        val key = mapKeyEventToTRS(event)
        if (key == Key.TK_NONE) {
            return false
        }
        keyDown(key)
        return true
    }

    /**
     * Forwards a hardware key-up event to the emulator, delayed by [KEY_UP_DELAY].
     *
     * @return Whether the event mapped to a TRS-80 key and was consumed.
     */
    fun keyUp(event: KeyEvent): Boolean {
        val key = mapKeyEventToTRS(event)
        if (key == Key.TK_NONE) {
            return false
        }
        handler.postDelayed({ keyUp(key) }, KEY_UP_DELAY)
        return true
    }

    /** @return The `Key.TK_*` id the event maps to, or [Key.TK_NONE] if it maps to nothing. */
    private fun mapKeyEventToTRS(event: KeyEvent): Int {
        val keyCode = event.keyCode
        val controllerKey = mapGameControllerButton(keyCode)
        if (controllerKey != Key.TK_NONE) {
            return controllerKey
        }
        when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> return Key.TK_ENTER
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_DPAD_LEFT -> return Key.TK_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> return Key.TK_RIGHT
            KeyEvent.KEYCODE_DPAD_DOWN -> return Key.TK_DOWN
            KeyEvent.KEYCODE_DPAD_UP -> return Key.TK_UP
            // Ctrl-B and Ctrl-C are the only modified keys; without the modifier they fall
            // through to the regular character mapping below.
            KeyEvent.KEYCODE_B -> if (event.isCtrlPressed) return Key.TK_BREAK
            KeyEvent.KEYCODE_C -> if (event.isCtrlPressed) return Key.TK_CLEAR
        }

        var key = event.unicodeChar
        if (key >= '0'.code && key <= '9'.code) {
            return key - '0'.code
        }
        if (key >= 'a'.code && key <= 'z'.code) {
            // Convert to upper case
            key -= 0x20
        }
        if (key >= 'A'.code && key <= 'Z'.code) {
            return key - 'A'.code + 10
        }
        return when (key) {
            ','.code -> Key.TK_COMMA
            '.'.code -> Key.TK_DOT
            '/'.code -> Key.TK_SLASH
            ' '.code -> Key.TK_SPACE
            '+'.code -> Key.TK_ADD
            '#'.code -> Key.TK_HASH
            '('.code -> Key.TK_BR_OPEN
            ')'.code -> Key.TK_BR_CLOSE
            '*'.code -> Key.TK_ASTERIX
            '$'.code -> Key.TK_DOLLAR
            '?'.code -> Key.TK_QUESTION
            '<'.code -> Key.TK_LT
            '>'.code -> Key.TK_GT
            '='.code -> Key.TK_EQUAL
            '%'.code -> Key.TK_PERCENT
            '&'.code -> Key.TK_AMP
            '\''.code -> Key.TK_APOS
            '!'.code -> Key.TK_EXCLAMATION_MARK
            '@'.code -> Key.TK_AT
            '"'.code -> Key.TK_QUOT
            ';'.code -> Key.TK_SEMICOLON
            '\n'.code -> Key.TK_ENTER
            ':'.code -> Key.TK_COLON
            '-'.code -> Key.TK_MINUS
            '\b'.code -> Key.TK_LEFT
            else -> Key.TK_NONE
        }
    }

    /** Every game controller button doubles as the space (fire) key. */
    private fun mapGameControllerButton(keyCode: Int): Int = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_A -> Key.TK_SPACE
        else -> Key.TK_NONE
    }

    companion object {
        /**
         * The delay in milliseconds after which the matching key-up event is injected following a
         * key-down event. If this delay is too short, the emulated TRS might miss the key-up
         * event. This can especially happen when using an external keyboard with key-repeat.
         */
        private const val KEY_UP_DELAY = 100L

        private const val SDL_KEYDOWN = 2
        private const val SDL_KEYUP = 3

        /**
         * Keys without a printable character (ENTER, SHIFT, the cursor keys, ...) are handed a
         * synthetic key code, starting here so they cannot collide with the ASCII codes the
         * printable keys use.
         */
        private const val FIRST_SYNTHETIC_KEY_CODE = 15

        private val KEYBOARD_MAPPING: List<KeyMap> = parseKeyMap(R.xml.keymap_us)

        /**
         * Parses a keymap resource. The resource is part of the APK, so a malformed or unreadable
         * one is a broken build rather than a runtime condition worth recovering from: it fails
         * loudly here instead of leaving a half-built mapping behind that blows up much later with
         * an out-of-bounds access on an arbitrary key press.
         */
        private fun parseKeyMap(keyMapLayout: Int): List<KeyMap> = try {
            TRS80Application.getAppContext().resources.getXml(keyMapLayout).use { parser ->
                val keyMap = mutableListOf<KeyMap>()
                var nextSyntheticKeyCode = FIRST_SYNTHETIC_KEY_CODE
                parser.next()
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "KeyMap") {
                        val key = parser.requireAttribute("key")
                        keyMap += KeyMap(
                                label = parser.requireAttribute("label"),
                                sym = parser.requireAttribute("sym").decode(),
                                key = if (key == "NEXT_FREE") nextSyntheticKeyCode++ else key[0].code,
                                name = parser.requireAttribute("name"),
                                value = parser.requireAttribute("value").decode())
                    }
                    eventType = parser.next()
                }
                keyMap
            }
        } catch (e: XmlPullParserException) {
            throw IllegalStateException("Malformed keymap resource.", e)
        } catch (e: IOException) {
            throw IllegalStateException("Unable to read keymap resource.", e)
        }

        private fun XmlResourceParser.requireAttribute(name: String): String =
                requireNotNull(getAttributeValue(null, name)) {
                    "<KeyMap> element is missing the '$name' attribute."
                }

        /** Decodes a decimal, hex (`0x...`) or octal literal, as the keymap uses all three. */
        private fun String.decode(): Int = java.lang.Long.decode(this).toInt()
    }
}
