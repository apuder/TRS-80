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

package org.puder.trs80;

import java.util.ArrayList;
import java.util.List;

public class Keyboard {

    private List<Key> shiftableKeys;

    public Keyboard() {
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
}
