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

package org.puder.trs80

import android.graphics.Typeface

private const val MODEL1_FONT = "fonts/AnotherMansTreasureMIB64C2X3Y.ttf"
private const val MODEL3_FONT = "fonts/AnotherMansTreasureMIII64C.ttf"

/** Provides the character-set typefaces of the emulated machines. */
object Fonts {

    /**
     * Returns the typeface that renders the character set of the given hardware model.
     *
     * Only [Hardware.MODEL1] and [Hardware.MODEL3] have a font; any other model
     * throws, matching the pre-existing behaviour where the missing font name made
     * `Typeface.createFromAsset()` fail.
     */
    @JvmStatic
    fun getTypeface(model: Int): Typeface {
        val fontName = when (model) {
            Hardware.MODEL1 -> MODEL1_FONT
            Hardware.MODEL3 -> MODEL3_FONT
            else -> throw IllegalArgumentException("No font for model $model")
        }
        return TypefaceCache.get().getTypeface(fontName, TRS80Application.getAppContext())
    }
}
