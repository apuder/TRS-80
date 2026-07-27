/*
 * Copyright 2014, Sascha Haeberling
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

import android.content.Context
import android.graphics.Typeface

/** Process-wide cache of typefaces loaded from the app's assets. */
object TypefaceCache {
    private val cache = HashMap<String, Typeface>()

    /**
     * Returns the singleton instance.
     *
     * Kept so the existing `TypefaceCache.get().getTypeface(...)` call sites in
     * Java keep working.
     */
    @JvmStatic
    fun get(): TypefaceCache = this

    /** Returns the typeface for the given asset path, loading it on first use. */
    fun getTypeface(fontPath: String, context: Context): Typeface =
        cache.getOrPut(fontPath) { Typeface.createFromAsset(context.assets, fontPath) }
}
