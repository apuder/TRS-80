/*
 * Copyright 2017, Sascha Haeberling
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

package org.puder.trs80.configuration;

import android.util.SparseArray;

import com.google.common.base.Optional;

/**
 * Various keyboard layouts.
 */
public enum KeyboardLayout {
    KEYBOARD_LAYOUT_ORIGINAL(0),
    KEYBOARD_LAYOUT_COMPACT(1),
    KEYBOARD_LAYOUT_JOYSTICK(2),
    KEYBOARD_GAME_CONTROLLER(3),
    KEYBOARD_TILT(4),
    KEYBOARD_EXTERNAL(5);

    public final int id;

    KeyboardLayout(int id) {
        this.id = id;
    }

    public static Optional<KeyboardLayout> fromId(int id) {
        return Optional.fromNullable(mapped.get(id, null));
    }

    private static final SparseArray<KeyboardLayout> mapped = createMap();

    private static SparseArray<KeyboardLayout> createMap() {
        SparseArray<KeyboardLayout> arr = new SparseArray<>();
        for (KeyboardLayout layout : KeyboardLayout.values()) {
            arr.put(layout.id, layout);
        }
        return arr;
    }
}
