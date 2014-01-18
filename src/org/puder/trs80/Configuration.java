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

public class Configuration {

    public static final int          KEYBOARD_LAYOUT_ORIGINAL = 0;
    public static final int          KEYBOARD_LAYOUT_COMPACT  = 1;
    public static final int          KEYBOARD_LAYOUT_GAMING_1 = 2;
    public static final int          KEYBOARD_LAYOUT_GAMING_2 = 3;
    public static final int          KEYBOARD_TILT            = 4;
    public static final int          KEYBOARD_EXTERNAL        = 5;

    private static Configuration[]   configurations;
    private static SharedPreferences globalPrefs;

    static {
        globalPrefs = PreferenceManager.getDefaultSharedPreferences(TRS80Application
                .getAppContext());
        String[] configurationIds = new String[0];
        String configs = globalPrefs.getString("CONFIGURATIONS", "");
        if (!configs.equals("")) {
            configurationIds = configs.split(",");
        }
        configurations = new Configuration[configurationIds.length];
        for (int i = 0; i < configurationIds.length; i++) {
            configurations[i] = new Configuration(Integer.parseInt(configurationIds[i]));
        }
    }

    protected SharedPreferences      sharedPrefs;
    protected int                    id;

    public static Configuration[] getConfigurations() {
        return configurations;
    }

    public static Configuration newConfiguration() {
        int nextId = globalPrefs.getInt("NEXT_ID", 0);
        nextId++;
        Editor e = globalPrefs.edit();
        e.putInt("NEXT_ID", nextId);
        String configurationIds = globalPrefs.getString("CONFIGURATIONS", "");

        if (!configurationIds.equals("")) {
            configurationIds += ",";
        }
        configurationIds += Integer.toString(nextId);
        e.putString("CONFIGURATIONS", configurationIds);
        e.commit();

        Configuration newConfig = new Configuration(nextId);
        int len = configurations.length + 1;
        Configuration[] newConfigurations = new Configuration[len];
        System.arraycopy(configurations, 0, newConfigurations, 0, len - 1);
        newConfigurations[len - 1] = newConfig;
        configurations = newConfigurations;
        return newConfig;
    }

    public void delete() {
        // Delete ID
        Editor e = globalPrefs.edit();
        String configurationIds = globalPrefs.getString("CONFIGURATIONS", "");

        String ids = Integer.toString(id);
        configurationIds = configurationIds.replace(ids + ",", "");
        configurationIds = configurationIds.replace("," + ids, "");
        configurationIds = configurationIds.replace(ids, "");
        e.putString("CONFIGURATIONS", configurationIds);
        e.commit();

        // Delete shared preferences
        e = sharedPrefs.edit();
        e.clear();
        e.commit();

        // Delete configuration
        int len = configurations.length - 1;
        Configuration[] newConfigurations = new Configuration[len];
        int k = 0;
        for (int i = 0; i < configurations.length; i++) {
            if (configurations[i].getId() == getId()) {
                continue;
            }
            newConfigurations[k++] = configurations[i];
        }
        configurations = newConfigurations;
    }

    protected Configuration(int id) {
        this.id = id;
        sharedPrefs = TRS80Application.getAppContext().getSharedPreferences("CONFIG_" + id,
                Context.MODE_PRIVATE);
    }

    public ConfigurationBackup backup() {
        return new ConfigurationBackup(this);
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

    public int getKeyboardLayoutLandscape() {
        String v = sharedPrefs.getString(EditConfigurationActivity.CONF_KEYBOARD_LANDSCAPE, "0");
        return Integer.parseInt(v);
    }

    public boolean muteSound() {
        return sharedPrefs.getBoolean(EditConfigurationActivity.CONF_MUTE_SOUND, false);
    }
}
