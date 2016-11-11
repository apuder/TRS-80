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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import org.puder.trs80.keyboard.Key;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    private static final String        CASSETTE_POSITION        = "cassette_position";

    public static final int            KEYBOARD_LAYOUT_ORIGINAL = 0;
    public static final int            KEYBOARD_LAYOUT_COMPACT  = 1;
    public static final int            KEYBOARD_LAYOUT_JOYSTICK = 2;
    public static final int            KEYBOARD_GAME_CONTROLLER = 3;
    public static final int            KEYBOARD_TILT            = 4;
    public static final int            KEYBOARD_EXTERNAL        = 5;

    private static List<Configuration> configurations;
    private static SharedPreferences   globalPrefs;

    static {
        globalPrefs = PreferenceManager.getDefaultSharedPreferences(TRS80Application
                .getAppContext());
        String[] configurationIds = new String[0];
        String configs = globalPrefs.getString("CONFIGURATIONS", "");
        if (!configs.equals("")) {
            configurationIds = configs.split(",");
        }
        configurations = new ArrayList<Configuration>(configurationIds.length);
        for (int i = 0; i < configurationIds.length; i++) {
            configurations.add(new Configuration(Integer.parseInt(configurationIds[i])));
        }
    }

    protected SharedPreferences        sharedPrefs;
    protected int                      id;


    private static void saveConfigurationIDs() {
        String ids = "";
        for (Configuration conf : configurations) {
            if (!ids.equals("")) {
                ids += ",";
            }
            ids += Integer.toString(conf.getId());
        }
        Editor e = globalPrefs.edit();
        e.putString("CONFIGURATIONS", ids);
        e.apply();
    }

    public static int getCount() {
        return configurations.size();
    }

    public static Configuration getNthConfiguration(int n) {
        return configurations.get(n);
    }

    public static Configuration getConfigurationById(int id) {
        for (Configuration configuration : configurations) {
            if (configuration.getId() == id) {
                return configuration;
            }
        }
        return null;
    }

    public static int getConfigurationPosition(int id) {
        for (int i = 0; i < configurations.size(); i++) {
            if (configurations.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    public static Configuration newConfiguration() {
        int nextId = globalPrefs.getInt("NEXT_ID", 0);
        nextId++;
        Editor e = globalPrefs.edit();
        e.putInt("NEXT_ID", nextId);
        e.apply();
        Configuration newConfig = new Configuration(nextId);
        configurations.add(newConfig);
        saveConfigurationIDs();
        // Delete any state that might be present from a previous install
        // of this app
        EmulatorState.deleteSavedState(nextId);
        return newConfig;
    }

    public void delete() {
        // Delete configuration
        for (int i = 0; i < configurations.size(); i++) {
            if (configurations.get(i).getId() == getId()) {
                configurations.remove(i);
                break;
            }
        }
        saveConfigurationIDs();

        // Delete shared preferences
        Editor e = sharedPrefs.edit();
        e.clear();
        e.apply();

        // Delete state
        EmulatorState.deleteSavedState(getId());
    }

    protected Configuration(int id) {
        this.id = id;
        sharedPrefs = TRS80Application.getAppContext().getSharedPreferences("CONFIG_" + id,
                Context.MODE_PRIVATE);
    }

    public void backup() {
        ConfigurationBackup.createBackup(this);
    }

    public int getId() {
        return id;
    }

    public int getModel() {
        String model = sharedPrefs.getString(EditConfigurationActivity.CONF_MODEL, null);
        if (model == null) {
            return Hardware.MODEL_NONE;
        }
        switch (Integer.parseInt(model)) {
        case 1:
            return Hardware.MODEL1;
        case 3:
            return Hardware.MODEL3;
        case 4:
            return Hardware.MODEL4;
        case 5:
            return Hardware.MODEL4P;
        }
        return Hardware.MODEL_NONE;
    }

    public int getScreenColorAsRGB() {
        return Color.DKGRAY;
    }

    public int getCharacterColorAsRGB() {
        int c = getCharacterColor();
        switch (c) {
        case 0:
            return Color.GREEN;
        default:
            return Color.WHITE;
        }
    }

    public int getCharacterColor() {
        return Integer.parseInt(sharedPrefs.getString(
                EditConfigurationActivity.CONF_CHARACTER_COLOR, "0"));
    }

    public String getCassettePath() {
        return sharedPrefs.getString(EditConfigurationActivity.CONF_CASSETTE, null);
    }

    public float getCassettePosition() {
        return sharedPrefs.getFloat(CASSETTE_POSITION, 0);
    }

    public void setCassettePosition(float pos) {
        Editor editor = sharedPrefs.edit();
        editor.putFloat(CASSETTE_POSITION, pos);
        editor.apply();
    }

    public String getDiskPath(int disk) {
        String key;
        switch (disk) {
        case 0:
            key = EditConfigurationActivity.CONF_DISK1;
            break;
        case 1:
            key = EditConfigurationActivity.CONF_DISK2;
            break;
        case 2:
            key = EditConfigurationActivity.CONF_DISK3;
            break;
        case 3:
            key = EditConfigurationActivity.CONF_DISK4;
            break;
        default:
            return null;
        }
        return sharedPrefs.getString(key, null);
    }

    public String getName() {
        return sharedPrefs.getString(EditConfigurationActivity.CONF_NAME, "unknown");
    }

    public int getKeyboardLayoutPortrait() {
        String v = sharedPrefs.getString(EditConfigurationActivity.CONF_KEYBOARD_PORTRAIT, "0");
        return Integer.parseInt(v);
    }

    public void setKeyboardLayoutPortrait(int kb) {
        Editor editor = sharedPrefs.edit();
        editor.putString(EditConfigurationActivity.CONF_KEYBOARD_PORTRAIT, Integer.toString(kb));
        editor.apply();
    }

    public void setKeyboardLayoutLandscape(int kb) {
        Editor editor = sharedPrefs.edit();
        editor.putString(EditConfigurationActivity.CONF_KEYBOARD_LANDSCAPE, Integer.toString(kb));
        editor.apply();
    }

    public int getKeyboardLayoutLandscape() {
        String v = sharedPrefs.getString(EditConfigurationActivity.CONF_KEYBOARD_LANDSCAPE, "0");
        return Integer.parseInt(v);
    }

    public boolean muteSound() {
        return sharedPrefs.getBoolean(EditConfigurationActivity.CONF_MUTE_SOUND, false);
    }

    static public void move(int from, int to) {
        Configuration conf = configurations.get(from);
        configurations.remove(from);
        configurations.add(to, conf);
        saveConfigurationIDs();
    }

    public int mapGameControllerButton(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_L1:
                return Key.TK_SPACE;
            case KeyEvent.KEYCODE_BUTTON_R1:
                return Key.TK_SPACE;
            case KeyEvent.KEYCODE_BUTTON_X:
                return Key.TK_SPACE;
            case KeyEvent.KEYCODE_BUTTON_Y:
                return Key.TK_SPACE;
            case KeyEvent.KEYCODE_BUTTON_B:
                return Key.TK_SPACE;
            case KeyEvent.KEYCODE_BUTTON_A:
                return Key.TK_SPACE;
        }
        return Key.TK_NONE;
    }
}
