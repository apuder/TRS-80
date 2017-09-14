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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Persisted data about a configuration.
 */
public class ConfigurationPersistence {
    private static final String PREF_NAME_PREFIX = "CONFIG_";
    private static final String CONF_NAME = "conf_name";
    private static final String CONF_MODEL = "conf_model";
    private static final String CONF_CASSETTE = "conf_cassette";
    private static final String CONF_DISK1 = "conf_disk1";
    private static final String CONF_DISK2 = "conf_disk2";
    private static final String CONF_DISK3 = "conf_disk3";
    private static final String CONF_DISK4 = "conf_disk4";
    private static final String CONF_CHARACTER_COLOR = "conf_character_color";
    private static final String CONF_KEYBOARD_PORTRAIT = "conf_keyboard_portrait";
    private static final String CONF_KEYBOARD_LANDSCAPE = "conf_keyboard_landscape";
    private static final String CONF_MUTE_SOUND = "conf_mute_sound";

    private static final String KEY_CASSETTE_POSITION = "cassette_position";

    private final SharedPreferences sharedPrefs;

    /** Create an instance for the configuration with the given ID. */
    static ConfigurationPersistence forId(int configId, Context context) {
        String prefName = PREF_NAME_PREFIX + configId;
        SharedPreferences sharedPreferences =
                checkNotNull(context).getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return new ConfigurationPersistence(sharedPreferences);
    }

    public static ConfigurationPersistence forIdAndManager(int configId,
                                                           PreferenceManager prefManager) {
        String prefName = PREF_NAME_PREFIX + configId;
        checkNotNull(prefManager).setSharedPreferencesName(prefName);
        return new ConfigurationPersistence(prefManager.getSharedPreferences());
    }

    private ConfigurationPersistence(SharedPreferences sharedPrefs) {
        this.sharedPrefs = sharedPrefs;
    }

    public Optional<String> getModel() {
        return getString(CONF_MODEL, null);
    }

    void setModel(String model) {
        setStringOrRemove(CONF_MODEL, model);
    }

    public Optional<String> getCasettePath() {
        return getString(CONF_CASSETTE, null);
    }

    public void setCasettePath(String path) {
        setStringOrRemove(CONF_CASSETTE, path);
    }

    public Optional<String> getDiskPath(int disk) {
        String key = diskIdToKey(disk);
        if (key == null) {
            return Optional.absent();
        }
        return getString(key, null);
    }


    public void setDiskPath(int disk, String path) {
        setStringOrRemove(diskIdToKey(disk), path);
    }

    public int getCharacterColor(int defaultValue) {
        return getInt(CONF_CHARACTER_COLOR, defaultValue);
    }

    void setCharacterColor(int value) {
        setInt(CONF_CHARACTER_COLOR, value);
    }

    float getCassettePosition(float defaultValue) {
        return sharedPrefs.getFloat(KEY_CASSETTE_POSITION, defaultValue);
    }

    void setCassettePosition(float pos) {
        sharedPrefs.edit().putFloat(KEY_CASSETTE_POSITION, pos).apply();
    }

    public Optional<String> getName() {
        return getString(CONF_NAME, "unknown");
    }

    void setName(String name) {
        setStringOrRemove(CONF_NAME, name);
    }

    public int getKeyboardLayoutPortrait() {
        return getInt(CONF_KEYBOARD_PORTRAIT, 0);
    }

    void setKeyboardLayoutPortrait(int layout) {
        setInt(CONF_KEYBOARD_PORTRAIT, layout);
    }

    public int getKeyboardLayoutLandscape() {
        return getInt(CONF_KEYBOARD_LANDSCAPE, 0);
    }

    void setKeyboardLayoutLandscape(int layout) {
        setInt(CONF_KEYBOARD_LANDSCAPE, layout);
    }

    boolean isSoundMuted() {
        return sharedPrefs.getBoolean(CONF_MUTE_SOUND, false);
    }

    void setSoundMuted(boolean muted) {
        sharedPrefs.edit().putBoolean(CONF_MUTE_SOUND, muted).apply();
    }

    void clear() {
        sharedPrefs.edit().clear().apply();
    }

    public PreferenceFinder forPreferenceProvider(PreferenceProvider provider) {
        return new PreferenceFinder(provider);
    }

    private Optional<String> getString(String key, String defaultValue) {
        return Optional.fromNullable(sharedPrefs.getString(key, defaultValue));
    }

    private void setStringOrRemove(String key, String value) {
        if (Strings.isNullOrEmpty(value)) {
            sharedPrefs.edit().remove(key).apply();
        } else {
            sharedPrefs.edit().putString(key, value).apply();
        }
    }

    private int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(sharedPrefs.getString(key, ""));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private void setInt(String key, int value) {
        sharedPrefs.edit().putString(key, Integer.toString(value)).apply();
    }

    private static String diskIdToKey(int disk) {
        switch (disk) {
            case 0:
                return CONF_DISK1;
            case 1:
                return CONF_DISK2;
            case 2:
                return CONF_DISK3;
            case 3:
                return CONF_DISK4;
            default:
                return null;
        }
    }

    /**
     * Finds preferences for configurations.
     */
    public static class PreferenceFinder {
        private final PreferenceProvider provider;

        public PreferenceFinder(PreferenceProvider provider) {
            this.provider = Preconditions.checkNotNull(provider);
        }

        public Preference forModel() {
            return provider.findPreference(CONF_MODEL);
        }
        public Preference forName() {
            return provider.findPreference(CONF_NAME);
        }
        public Preference forCasette() {
            return provider.findPreference(CONF_CASSETTE);
        }
        public Preference forDisk1() {
            return provider.findPreference(CONF_DISK1);
        }
        public Preference forDisk2() {
            return provider.findPreference(CONF_DISK2);
        }
        public Preference forDisk3() {
            return provider.findPreference(CONF_DISK3);
        }
        public Preference forDisk4() {
            return provider.findPreference(CONF_DISK4);
        }
        public Preference forCharacterColor() {
            return provider.findPreference(CONF_CHARACTER_COLOR);
        }
        public Preference forKeyboardPortrait() {
            return provider.findPreference(CONF_KEYBOARD_PORTRAIT);
        }
        public Preference forKeyboardLandscape() {
            return provider.findPreference(CONF_KEYBOARD_LANDSCAPE);
        }
    }

    /**
     * Classes implementing this interface provide preferences by name
     */
    public interface PreferenceProvider {
        Preference findPreference(String name);
    }
}
