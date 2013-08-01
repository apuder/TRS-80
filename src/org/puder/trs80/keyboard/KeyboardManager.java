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

package org.puder.trs80.keyboard;

import java.util.ArrayList;
import java.util.List;

import org.puder.trs80.Hardware;

public class KeyboardManager {

    private static final int KEY_ADDRESS_SPACE = 0x3840;
    private static final int KEY_MASK_SPACE    = 128;

    private static final int KEY_ADDRESS_LEFT  = 0x3840;
    private static final int KEY_MASK_LEFT     = 0x20;

    private static final int KEY_ADDRESS_RIGHT = 0x3840;
    private static final int KEY_MASK_RIGHT    = 0x40;

    private static final int KEY_ADDRESS_DOWN  = 0x3840;
    private static final int KEY_MASK_DOWN     = 16;

    private static final int KEY_ADDRESS_UP    = 0x3840;
    private static final int KEY_MASK_UP       = 8;

    private Hardware.Model   model;
    private byte[]           memBuffer;
    private List<Key>        shiftableKeys;

    public KeyboardManager(Hardware.Model model, byte[] memBuffer) {
        this.model = model;
        this.memBuffer = memBuffer;
        shiftableKeys = new ArrayList<Key>();
    }

    public void addShiftableKey(Key key) {
        shiftableKeys.add(key);
    }

    public void shiftKeys() {
        for (Key key : shiftableKeys) {
            key.shift();
        }
    }

    public void unshiftKeys() {
        for (Key key : shiftableKeys) {
            key.unshift();
        }
    }

    public void allCursorKeysUp() {
        memBuffer[KEY_ADDRESS_LEFT] &= ~KEY_MASK_LEFT;
        memBuffer[KEY_ADDRESS_RIGHT] &= ~KEY_MASK_RIGHT;
        memBuffer[KEY_ADDRESS_UP] &= ~KEY_MASK_UP;
        memBuffer[KEY_ADDRESS_DOWN] &= ~KEY_MASK_DOWN;
    }

    public void pressKeyDown() {
        memBuffer[KEY_ADDRESS_DOWN] |= KEY_MASK_DOWN;
    }

    public void pressKeyUp() {
        memBuffer[KEY_ADDRESS_UP] |= KEY_MASK_UP;
    }

    public void pressKeyLeft() {
        memBuffer[KEY_ADDRESS_LEFT] |= KEY_MASK_LEFT;
    }

    public void pressKeyRight() {
        memBuffer[KEY_ADDRESS_RIGHT] |= KEY_MASK_RIGHT;
    }

    public void pressKeySpace() {
        memBuffer[KEY_ADDRESS_SPACE] |= KEY_MASK_SPACE;
    }

    public void unpressKeySpace() {
        memBuffer[KEY_ADDRESS_SPACE] &= ~KEY_MASK_SPACE;
    }
}
