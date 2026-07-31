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

/** The colours a configuration can be drawn in, as ARGB. */
object ScreenColors {
    /** Was `android.graphics.Color.GREEN`. */
    const val GREEN = 0xFF00FF00.toInt()

    /** Was `android.graphics.Color.WHITE`. */
    const val WHITE = 0xFFFFFFFF.toInt()

    /** Was `android.graphics.Color.DKGRAY`. */
    const val DARK_GRAY = 0xFF444444.toInt()
}
