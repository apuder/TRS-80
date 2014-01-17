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
import java.util.List;

import org.puder.trs80.R;
import org.puder.trs80.TRS80Application;
import org.puder.trs80.XTRS;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.XmlResourceParser;
import android.os.Handler;
import android.view.KeyEvent;

public class KeyboardManager {

    private static int          KEY_SPACE;
    private static int          KEY_LEFT;
    private static int          KEY_RIGHT;
    private static int          KEY_DOWN;
    private static int          KEY_UP;

    private List<Key>           shiftableKeys;

    private static List<KeyMap> keyboardMapping;

    private Handler             handler = new Handler();

    static {
        keyboardMapping = parseKeyMap(R.xml.keymap_us);
        // enum values from attrs.xml
        KEY_SPACE = 39;
        KEY_LEFT = 67;
        KEY_RIGHT = 68;
        KEY_DOWN = 69;
        KEY_UP = 65;

    }

    static private List<KeyMap> parseKeyMap(int keyMapLayout) {
        XmlResourceParser parser = TRS80Application.getAppContext().getResources()
                .getXml(keyMapLayout);
        List<KeyMap> keyMap = new ArrayList<KeyMap>();
        int nextFree = 15;
        try {
            parser.next();
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if (tagName.equals("KeyMap")) {
                        String label = parser.getAttributeValue(null, "label");
                        String sym = parser.getAttributeValue(null, "sym");
                        String key = parser.getAttributeValue(null, "key");
                        String name = parser.getAttributeValue(null, "name");
                        String value = parser.getAttributeValue(null, "value");
                        KeyMap km = new KeyMap();
                        km.label = label;
                        km.sym = Long.decode(sym).intValue();
                        if (key.equals("NEXT_FREE")) {
                            km.key = nextFree++;
                        } else {
                            km.key = key.charAt(0);
                        }
                        km.name = name;
                        km.value = Long.decode(value).intValue();
                        keyMap.add(km);
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

    public KeyboardManager() {
        shiftableKeys = new ArrayList<Key>();
    }

    public KeyMap getKeyMap(int id) {
        return keyboardMapping.get(id);
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
        keyUp(KEY_LEFT);
        keyUp(KEY_RIGHT);
        keyUp(KEY_UP);
        keyUp(KEY_DOWN);
    }

    public void pressKeyDown() {
        keyDown(KEY_DOWN);
    }

    public void pressKeyUp() {
        keyDown(KEY_UP);
    }

    public void pressKeyLeft() {
        keyDown(KEY_LEFT);
    }

    public void pressKeyRight() {
        keyDown(KEY_RIGHT);
    }

    public void pressKeySpace() {
        keyDown(KEY_SPACE);
    }

    public void unpressKeySpace() {
        keyUp(KEY_SPACE);
    }

    public void keyDown(int keyId) {
        KeyMap keyMap = keyboardMapping.get(keyId);
        keyDown(keyMap.sym, keyMap.key);
    }

    public void keyDown(int sym, int key) {
        final int SDL_KEYDOWN = 2;
        XTRS.addKeyEvent(SDL_KEYDOWN, sym, key);
    }

    public void keyUp(int keyId) {
        KeyMap keyMap = keyboardMapping.get(keyId);
        keyUp(keyMap.sym, keyMap.key);
    }

    public void keyUp(int sym, int key) {
        final int SDL_KEYUP = 3;
        XTRS.addKeyEvent(SDL_KEYUP, sym, key);
    }

    public boolean keyDown(KeyEvent event) {
        int key = mapKeyEventToTRS(event);
        if (key == -1) {
            return false;
        }
        keyDown(key);
        return true;
    }

    public boolean keyUp(KeyEvent event) {
        final int key = mapKeyEventToTRS(event);
        if (key == -1) {
            return false;
        }
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                keyUp(key);
            }
        }, 50);
        return true;
    }

    private int mapKeyEventToTRS(KeyEvent event) {
        int key = event.getUnicodeChar();
        if (key >= '0' && key <= '9') {
            return key - '0';
        }
        if (key >= 'a' && key <= 'z') {
            // Convert to upper case
            key -= 0x20;
        }
        if (key >= 'A' && key <= 'Z') {
            return key - 'A' + 10;
        }
        switch (key) {
        case ',':
            return 36;
        case '.':
            return 37;
        case '/':
            return 38;
        case ' ':
            return 39;
        case '+':
            return 40;
        case '#':
            return 41;
        case '(':
            return 42;
        case ')':
            return 43;
        case '*':
            return 44;
        case '$':
            return 45;
        case '?':
            return 46;
        case '<':
            return 47;
        case '>':
            return 48;
        case '=':
            return 49;
        case '%':
            return 50;
        case '\'':
            return 51;
        case '!':
            return 52;
        case '@':
            return 53;
        case '"':
            return 54;
        case ';':
            return 55;
        case 0xa:
            return 56;
        case ':':
            return 61;
        case '-':
            return 62;
        case 0x8:
            return 67;
        }
        return -1;
    }
}
