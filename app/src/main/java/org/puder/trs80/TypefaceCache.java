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

package org.puder.trs80;

import java.util.HashMap;

import android.content.Context;
import android.graphics.Typeface;

public class TypefaceCache {
    private final HashMap<String, Typeface> cache = new HashMap<String, Typeface>();
    
    private static TypefaceCache instance = new TypefaceCache();


    public static TypefaceCache get() {
        return instance;
    }
    
    public Typeface getTypeface(String fontPath, Context context) {
        if (cache.containsKey(fontPath)) {
            return cache.get(fontPath);
        }
        
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), fontPath);
        cache.put(fontPath, typeface);
        return typeface;
    }
}
