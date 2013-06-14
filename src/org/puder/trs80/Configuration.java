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

import org.puder.trs80.Hardware.Model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.preference.PreferenceManager;

public class Configuration {

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

    private SharedPreferences        sharedPrefs;
    private int                      id;
    private int                      screenColor;
    private int                      characterColor;
    private int                      keyboardType;

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

    public static void deleteConfiguration(Configuration config) {
        // Delete ID
        Editor e = globalPrefs.edit();
        String configurationIds = globalPrefs.getString("CONFIGURATIONS", "");

        String id = Integer.toString(config.getId());
        configurationIds = configurationIds.replace(id + ",", "");
        configurationIds = configurationIds.replace("," + id, "");
        configurationIds = configurationIds.replace(id, "");
        e.putString("CONFIGURATIONS", configurationIds);
        e.commit();

        // Delete shared preferences
        SharedPreferences prefs = TRS80Application.getAppContext().getSharedPreferences(
                "CONFIG_" + id, Context.MODE_PRIVATE);
        e = prefs.edit();
        e.clear();
        e.commit();

        // Delete configuration
        int len = configurations.length - 1;
        Configuration[] newConfigurations = new Configuration[len];
        int k = 0;
        for (int i = 0; i < configurations.length; i++) {
            if (configurations[i] == config) {
                continue;
            }
            newConfigurations[k++] = configurations[i];
        }
        configurations = newConfigurations;
    }

    private Configuration(int id) {
        this.id = id;
        sharedPrefs = TRS80Application.getAppContext().getSharedPreferences("CONFIG_" + id,
                Context.MODE_PRIVATE);

        screenColor = Color.DKGRAY;
        characterColor = Color.GREEN;
    }

    public int getId() {
        return id;
    }

    public Hardware.Model getModel() {
        String model = sharedPrefs.getString(EditConfigurationFragment.CONF_MODEL, null);
        if (model == null) {
            return Model.NONE;
        }
        switch (Integer.parseInt(model)) {
        case 1:
            return Model.MODEL1;
        case 3:
            return Model.MODEL3;
        case 4:
            return Model.MODEL4;
        case 5:
            return Model.MODEL4P;
        }
        return Model.NONE;
    }

    public int getScreenColor() {
        return screenColor;
    }

    public int getCharacterColor() {
        return characterColor;
    }

    public String getDiskPath(int disk) {
        String key;
        switch (disk) {
        case 0:
            key = EditConfigurationFragment.CONF_DISK1;
            break;
        case 1:
            key = EditConfigurationFragment.CONF_DISK2;
            break;
        case 2:
            key = EditConfigurationFragment.CONF_DISK3;
            break;
        case 3:
            key = EditConfigurationFragment.CONF_DISK4;
            break;
        default:
            return null;
        }
        return sharedPrefs.getString(key, null);
    }

    public String getName() {
        return sharedPrefs.getString(EditConfigurationFragment.CONF_NAME, "unknown");
    }
}
