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

/**
 * The TRS-80 keyboard: every key the emulator can be sent, and the SDL codes it
 * expects for each.
 *
 * See http://www.trs-80.com/wordpress/zaps-patches-pokes-tips/internals/#keyboard13
 *
 * This used to live in `res/xml/keymap_us.xml` and be parsed at class-init time.
 * It is a fixed table that has not changed in the app's lifetime, so it is now
 * simply declared: that removes the XML pull-parsing, its two failure modes, the
 * dependency on a `Context` to reach the resource, and `java.lang.Long.decode`
 * for the mixed decimal/hex literals -- which was the one genuinely JVM-only part.
 *
 * The values the XML spelled out are resolved here:
 *
 *  - `sym` was written in a mix of decimal and hex and is now decimal throughout.
 *  - `key` was the first character of the `key` attribute, or -- for keys with no
 *    printable character -- a synthetic code allocated in document order from
 *    [FIRST_SYNTHETIC_KEY_CODE], which is how ENTER through DOWN got 15 to 25.
 *  - Labels had Android resource escapes (`\\u2191` and friends) and XML entities,
 *    and are now the characters themselves.
 *
 * [entries] is indexed by `Key.TK_*`, so its order is load-bearing: `value` is
 * each entry's own index and the two must stay in step.
 */
object KeyboardMapping {

    /**
     * Where synthetic key codes start.
     *
     * Keys without a printable character (ENTER, SHIFT, the cursor keys, ...) are
     * handed a code from here up, so they cannot collide with the ASCII codes the
     * printable keys use.
     */
    const val FIRST_SYNTHETIC_KEY_CODE = 15

    /** Every key, in `Key.TK_*` order. */
    val entries: List<KeyMap> = listOf(
        KeyMap("0"    , sym =   0, key =  48, name = "key_0", value = 0),
        KeyMap("1"    , sym =   0, key =  49, name = "key_1", value = 1),
        KeyMap("2"    , sym =   0, key =  50, name = "key_2", value = 2),
        KeyMap("3"    , sym =   0, key =  51, name = "key_3", value = 3),
        KeyMap("4"    , sym =   0, key =  52, name = "key_4", value = 4),
        KeyMap("5"    , sym =   0, key =  53, name = "key_5", value = 5),
        KeyMap("6"    , sym =   0, key =  54, name = "key_6", value = 6),
        KeyMap("7"    , sym =   0, key =  55, name = "key_7", value = 7),
        KeyMap("8"    , sym =   0, key =  56, name = "key_8", value = 8),
        KeyMap("9"    , sym =   0, key =  57, name = "key_9", value = 9),
        KeyMap("A"    , sym =   0, key =  65, name = "key_A", value = 10),
        KeyMap("B"    , sym =   0, key =  66, name = "key_B", value = 11),
        KeyMap("C"    , sym =   0, key =  67, name = "key_C", value = 12),
        KeyMap("D"    , sym =   0, key =  68, name = "key_D", value = 13),
        KeyMap("E"    , sym =   0, key =  69, name = "key_E", value = 14),
        KeyMap("F"    , sym =   0, key =  70, name = "key_F", value = 15),
        KeyMap("G"    , sym =   0, key =  71, name = "key_G", value = 16),
        KeyMap("H"    , sym =   0, key =  72, name = "key_H", value = 17),
        KeyMap("I"    , sym =   0, key =  73, name = "key_I", value = 18),
        KeyMap("J"    , sym =   0, key =  74, name = "key_J", value = 19),
        KeyMap("K"    , sym =   0, key =  75, name = "key_K", value = 20),
        KeyMap("L"    , sym =   0, key =  76, name = "key_L", value = 21),
        KeyMap("M"    , sym =   0, key =  77, name = "key_M", value = 22),
        KeyMap("N"    , sym =   0, key =  78, name = "key_N", value = 23),
        KeyMap("O"    , sym =   0, key =  79, name = "key_O", value = 24),
        KeyMap("P"    , sym =   0, key =  80, name = "key_P", value = 25),
        KeyMap("Q"    , sym =   0, key =  81, name = "key_Q", value = 26),
        KeyMap("R"    , sym =   0, key =  82, name = "key_R", value = 27),
        KeyMap("S"    , sym =   0, key =  83, name = "key_S", value = 28),
        KeyMap("T"    , sym =   0, key =  84, name = "key_T", value = 29),
        KeyMap("U"    , sym =   0, key =  85, name = "key_U", value = 30),
        KeyMap("V"    , sym =   0, key =  86, name = "key_V", value = 31),
        KeyMap("W"    , sym =   0, key =  87, name = "key_W", value = 32),
        KeyMap("X"    , sym =   0, key =  88, name = "key_X", value = 33),
        KeyMap("Y"    , sym =   0, key =  89, name = "key_Y", value = 34),
        KeyMap("Z"    , sym =   0, key =  90, name = "key_Z", value = 35),
        KeyMap(","    , sym =   0, key =  44, name = "key_COMMA", value = 36),
        KeyMap("."    , sym =   0, key =  46, name = "key_DOT", value = 37),
        KeyMap("/"    , sym =   0, key =  47, name = "key_SLASH", value = 38),
        KeyMap(" "    , sym =   0, key =  32, name = "key_SPACE", value = 39),
        KeyMap("+"    , sym =   0, key =  43, name = "key_ADD", value = 40),
        KeyMap("#"    , sym =   0, key =  35, name = "key_HASH", value = 41),
        KeyMap("("    , sym =   0, key =  40, name = "key_BR_OPEN", value = 42),
        KeyMap(")"    , sym =   0, key =  41, name = "key_BR_CLOSE", value = 43),
        KeyMap("*"    , sym =   0, key =  42, name = "key_ASTERIX", value = 44),
        KeyMap("\$"   , sym =   0, key =  36, name = "key_DOLLAR", value = 45),
        KeyMap("?"    , sym =   0, key =  63, name = "key_QUESTION", value = 46),
        KeyMap("<"    , sym =   0, key =  60, name = "key_LT", value = 47),
        KeyMap(">"    , sym =   0, key =  62, name = "key_GT", value = 48),
        KeyMap("="    , sym =   0, key =  61, name = "key_EQUAL", value = 49),
        KeyMap("%"    , sym =   0, key =  37, name = "key_PERCENT", value = 50),
        KeyMap("'"    , sym =   0, key =  39, name = "key_APOS", value = 51),
        KeyMap("!"    , sym =   0, key =  33, name = "key_EXCLAMATION_MARK", value = 52),
        KeyMap("&"    , sym =   0, key =  38, name = "key_AMP", value = 53),
        KeyMap("\""   , sym =   0, key =  34, name = "key_QUOT", value = 54),
        KeyMap(";"    , sym =   0, key =  59, name = "key_SEMICOLON", value = 55),
        KeyMap("ENTER", sym =  13, key =  15, name = "key_ENTER", value = 56),
        KeyMap("CLEAR", sym =  12, key =  16, name = "key_CLEAR", value = 57),
        KeyMap("CLR"  , sym =  12, key =  17, name = "key_CLEAR_SHORT", value = 58),
        KeyMap("SHIFT", sym = 280, key =  18, name = "key_SHIFT_LEFT", value = 59),
        KeyMap("SHIFT", sym = 281, key =  19, name = "key_SHIFT_RIGHT", value = 60),
        KeyMap(":"    , sym =   0, key =  58, name = "key_COLON", value = 61),
        KeyMap("-"    , sym =   0, key =  45, name = "key_MINUS", value = 62),
        KeyMap("BREAK", sym =  27, key =  20, name = "key_BREAK", value = 63),
        KeyMap("BRK"  , sym =  27, key =  21, name = "key_BREAK_SHORT", value = 64),
        KeyMap("↑"    , sym = 273, key =  22, name = "key_UP", value = 65),
        KeyMap("@"    , sym =   0, key =  64, name = "key_AT", value = 66),
        KeyMap("←"    , sym =   8, key =  23, name = "key_LEFT", value = 67),
        KeyMap("→"    , sym =   9, key =  24, name = "key_RIGHT", value = 68),
        KeyMap("↓"    , sym = 274, key =  25, name = "key_DOWN", value = 69),
        KeyMap("ALT"  , sym =   0, key =  48, name = "key_ALT", value = 70),
    )

    /** @return The entry at [id], which is one of the `Key.TK_*` constants. */
    operator fun get(id: Int): KeyMap = entries[id]

    /** @return The entry called [name], or null if there is none. */
    fun byName(name: String): KeyMap? = entries.find { it.name == name }
}
