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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.puder.trs80.Hardware;
import org.puder.trs80.R;
import org.puder.trs80.TRS80Application;
import org.puder.trs80.XTRS;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.XmlResourceParser;
import android.view.KeyEvent;

public class KeyboardManager {

    private static int                 KEY_ADDRESS_SPACE;
    private static int                 KEY_MASK_SPACE;

    private static int                 KEY_ADDRESS_LEFT;
    private static int                 KEY_MASK_LEFT;

    private static int                 KEY_ADDRESS_RIGHT;
    private static int                 KEY_MASK_RIGHT;

    private static int                 KEY_ADDRESS_DOWN;
    private static int                 KEY_MASK_DOWN;

    private static int                 KEY_ADDRESS_UP;
    private static int                 KEY_MASK_UP;

    private Hardware.Model             model;
    private byte[]                     memBuffer;
    private List<Key>                  shiftableKeys;
    private Map<String, KeyMap>        keyMap;

    private static Map<String, KeyMap> keyMapModel3;

    static {
        keyMapModel3 = parseKeyMap(R.xml.keymap_model3);
    }

    static private Map<String, KeyMap> parseKeyMap(int keyMapLayout) {
        XmlResourceParser parser = TRS80Application.getAppContext().getResources()
                .getXml(keyMapLayout);
        Map<String, KeyMap> keyMap = new HashMap<String, KeyMap>();
        try {
            parser.next();
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if (tagName.equals("Key")) {
                        String id = parser.getAttributeValue(null, "id");
                        String label = parser.getAttributeValue(null, "label");
                        String address = parser.getAttributeValue(null, "address");
                        String mask = parser.getAttributeValue(null, "mask");
                        String address2 = parser.getAttributeValue(null, "address2");
                        String mask2 = parser.getAttributeValue(null, "mask2");
                        KeyMap key = new KeyMap();
                        key.label = label;
                        key.address = Long.decode(address).intValue();
                        key.mask = Long.decode(mask).byteValue();
                        key.address2 = (address2 == null) ? -1 : Long.decode(address2).intValue();
                        key.mask2 = (mask2 == null) ? -1 : Long.decode(mask2).byteValue();
                        keyMap.put(id, key);
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        parser.close();
        return keyMap;
    }

    public KeyboardManager(Hardware.Model model, byte[] memBuffer) {
        this.model = model;
        this.memBuffer = memBuffer;
        shiftableKeys = new ArrayList<Key>();
        initKeyMap(model);
    }

    private void initKeyMap(Hardware.Model model) {
        switch (model) {
        case MODEL3:
            keyMap = keyMapModel3;
            break;
        default:
            keyMap = null;
            break;
        }

        KeyMap key = keyMap.get("key_SPACE");
        KEY_ADDRESS_SPACE = key.address;
        KEY_MASK_SPACE = key.mask;

        key = keyMap.get("key_LEFT");
        KEY_ADDRESS_LEFT = key.address;
        KEY_MASK_LEFT = key.mask;

        key = keyMap.get("key_RIGHT");
        KEY_ADDRESS_RIGHT = key.address;
        KEY_MASK_RIGHT = key.mask;

        key = keyMap.get("key_DOWN");
        KEY_ADDRESS_DOWN = key.address;
        KEY_MASK_DOWN = key.mask;

        key = keyMap.get("key_UP");
        KEY_ADDRESS_UP = key.address;
        KEY_MASK_UP = key.mask;
    }

    public KeyMap getKeyMap(String id) {
        return keyMap.get(id);
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

    public boolean keyDown(KeyEvent event) {
        final int SDL_KEYDOWN = 2;
        int key = event.getUnicodeChar();
        if (key != 0 && key < 0xff) {
            if (key == 0xa) {
                key = 0xd;
            }
            if (key >= 'a' && key <= 'z') {
                key -= 0x20;
            }
            int mod = genSDLModifier(event);
            XTRS.addKeyEvent(SDL_KEYDOWN, mod, key);
            return true;
        }
//        KeyMap keyMap = null;//getKeyMap(event);
//        if (keyMap == null) {
//            return false;
//        }
//        memBuffer[keyMap.address] |= keyMap.mask;
        return false;
    }

    public boolean keyUp(KeyEvent event) {
        final int SDL_KEYUP = 3;
        int key = event.getUnicodeChar();
        if (key != 0 && key < 0xff) {
            if (key == 0xa) {
                key = 0xd;
            }
            if (key >= 'a' && key <= 'z') {
                key -= 0x20;
            }
            int mod = genSDLModifier(event);
            XTRS.addKeyEvent(SDL_KEYUP, mod, key);
            return true;
        }
//        KeyMap keyMap = null;//getKeyMap(key);
//        if (keyMap == null) {
//            return false;
//        }
//        memBuffer[keyMap.address] &= ~keyMap.mask;
        return false;
    }

    private int genSDLModifier(KeyEvent event) {
        final int KMOD_CAPS = 0x2000;
        final int KMOD_LSHIFT = 0x0001;
        final int KMOD_RSHIFT = 0x0002;
        int mod = 0;
        if ((event.getMetaState() & KeyEvent.META_CAPS_LOCK_ON) != 0) {
            mod |= KMOD_CAPS;
        }
        if ((event.getMetaState() & KeyEvent.META_SHIFT_LEFT_ON) != 0) {
            mod |= KMOD_LSHIFT;
        }
        if ((event.getMetaState() & KeyEvent.META_SHIFT_RIGHT_ON) != 0) {
            mod |= KMOD_RSHIFT;
        }
        return mod;
    }
    private KeyMap getKeyMap(char key) {
        key = Character.toUpperCase(key);
        String id = "";
        switch (key) {
        case '\n':
            id = "ENTER";
            break;
        default:
            id = "" + key;
            break;
        }
        return getKeyMap("key_" + id);
    }
}
