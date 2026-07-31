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

/**
 * @return what [character] means to the machine, or null if it means nothing.
 *
 * Public because iOS reaches this from UIKit rather than from Compose: the
 * Compose view is only the first responder while a text field has focus, so an
 * emulator screen -- which has no text field -- never sees a key. Android does
 * the same thing at the same level, in `dispatchKeyEvent`.
 */
fun trs80KeyForCharacter(character: Char): KeyMap? =
    nameForCharacter(character)?.let(::named)

/** @return the mapping entry's name for [character], or null if it has none. */
private fun nameForCharacter(character: Char): String? {
    val upper = character.uppercaseChar()
    if (upper in '0'..'9' || upper in 'A'..'Z') {
        return "key_$upper"
    }
    return when (character) {
        ',' -> "key_COMMA"
        '.' -> "key_DOT"
        '/' -> "key_SLASH"
        ' ' -> "key_SPACE"
        '+' -> "key_ADD"
        '#' -> "key_HASH"
        '(' -> "key_BR_OPEN"
        ')' -> "key_BR_CLOSE"
        '*' -> "key_ASTERIX"
        '$' -> "key_DOLLAR"
        '?' -> "key_QUESTION"
        '<' -> "key_LT"
        '>' -> "key_GT"
        '=' -> "key_EQUAL"
        '%' -> "key_PERCENT"
        '&' -> "key_AMP"
        '\'' -> "key_APOS"
        '!' -> "key_EXCLAMATION_MARK"
        '@' -> "key_AT"
        '"' -> "key_QUOT"
        ';' -> "key_SEMICOLON"
        '\n' -> "key_ENTER"
        ':' -> "key_COLON"
        '-' -> "key_MINUS"
        '\b' -> "key_LEFT"
        else -> null
    }
}

private fun named(name: String): KeyMap? = KeyboardMapping.byName(name)
