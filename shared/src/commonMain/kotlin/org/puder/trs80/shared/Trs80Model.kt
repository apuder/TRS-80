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
 * The emulated machine, as the number the core and the stored configurations use.
 *
 * Deliberately still bare Ints rather than an enum: these values are written
 * into storage and passed to C, and turning them into a type is a change worth
 * making on its own rather than inside a port. `Hardware` re-exports them so the
 * Android call sites keep reading as they did.
 */
const val MODEL_NONE = 0
const val MODEL1 = 1
const val MODEL3 = 3
const val MODEL4 = 4
const val MODEL4P = 5

/** The colors a configuration can be drawn in, as ARGB. */
object ScreenColors {
    /** Was `android.graphics.Color.GREEN`. */
    const val GREEN = 0xFF00FF00.toInt()

    /** Was `android.graphics.Color.WHITE`. */
    const val WHITE = 0xFFFFFFFF.toInt()

    /** Was `android.graphics.Color.DKGRAY`. */
    const val DARK_GRAY = 0xFF444444.toInt()

    /** The other phosphor these machines were sold with. */
    const val AMBER = 0xFFFFB000.toInt()
}

/**
 * The color a machine draws its characters in.
 *
 * Declared in the order the editor offers them — the two phosphors together,
 * then white — while [stored] keeps the numbering already written into every
 * existing configuration. Green and white were 0 and 1 long before amber
 * existed, and renumbering them would repaint every machine on the device.
 */
enum class ScreenColor(val stored: Int, val rgb: Int) {
    Green(0, ScreenColors.GREEN),
    Amber(2, ScreenColors.AMBER),
    White(1, ScreenColors.WHITE);

    companion object {
        /** @return the color [stored] names, or green for anything unrecognized. */
        fun of(stored: Int): ScreenColor = entries.firstOrNull { it.stored == stored } ?: Green
    }
}
