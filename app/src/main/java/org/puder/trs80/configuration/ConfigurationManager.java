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
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.puder.trs80.Hardware;
import org.puder.trs80.io.FileManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages the installed configurations
 */
public class ConfigurationManager {
    private static final String TAG = "ConfigManager";

    /** The singleton instance of the ConfigurationManager. */
    private static ConfigurationManager singleton;

    /** Manages the storage for the configurations. */
    private final FileManager fileManager;
    private final FileManager.Creator fileManagerCreator;

    /** A list of all configurations. */
    private final List<Configuration> configurations;

    private final Context context;
    private final ConfigurationPersistence persistence;


    /**
     * Initialize the default instance of the manager. This should be done exactly once.
     *
     * @param fileManagerCreator creates file manager instances.
     * @throws IOException if the manager could not be initialized, or already had been
     *                     initialized.
     */
    public static ConfigurationManager initDefault(FileManager.Creator fileManagerCreator,
                                                   Context context) throws IOException {
        if (singleton != null) {
            Log.i(TAG, "ConfigurationManager singleton already initialized.");
            return singleton;
        }
        SharedPreferences globalPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        singleton = new ConfigurationManager(
                fileManagerCreator.forAppBaseDir(),
                fileManagerCreator, loadConfigurations(globalPrefs, context),
                new ConfigurationPersistence(globalPrefs),
                context);
        return singleton;
    }

    private static List<Configuration> loadConfigurations(SharedPreferences globalPrefs,
                                                          Context context) {

        String[] configurationIds = new String[0];
        String configs = globalPrefs.getString("CONFIGURATIONS", "");
        if (!configs.equals("")) {
            configurationIds = configs.split(",");
        }
        List<Configuration> configurations = new ArrayList<>(configurationIds.length);
        for (String configurationId : configurationIds) {
            int id = Integer.parseInt(configurationId);
            configurations.add(ConfigurationImpl.fromId(id, context));
        }
        return configurations;
    }


    /**
     * @return The singleton {@link ConfigurationManager}. You must call initDefault first.
     */
    public static ConfigurationManager getDefault() {
        return Preconditions.checkNotNull(singleton, "Must call initDefault() first.");
    }

    private ConfigurationManager(FileManager fileManager,
                                 FileManager.Creator fileManagerCreator,
                                 List<Configuration> configurations,
                                 ConfigurationPersistence persistence,
                                 Context context) {
        this.fileManager = fileManager;
        this.fileManagerCreator = fileManagerCreator;
        this.configurations = configurations;
        this.persistence = persistence;
        this.context = context;
    }

    /**
     * Returns the number of configurations.
     */
    public int getConfigCount() {
        return configurations.size();
    }

    /**
     * Returns the n-th configuration.
     */
    public Configuration getConfig(int n) {
        return configurations.get(n);
    }

    /**
     * Deletes the configuration with the given ID.
     */
    public boolean deleteConfigWithId(int id) {
        Optional<Configuration> config = getConfigById(id);
        if (!config.isPresent()) {
            return false;
        }
        configurations.remove(config.get());
        config.get().delete();
        saveConfigurationIDs();

        try {
            getEmulatorState(id).deleteSavedState();
        } catch (IOException e) {
            Log.e(TAG, "Could not clear saved state.", e);
            // TODO: Anything we should do at this point?
            return false;
        }
        return true;
    }

    /**
     * Returns the configuration with the given ID, if it exists.
     */
    public Optional<Configuration> getConfigById(int id) {
        for (Configuration configuration : configurations) {
            if (configuration.getId() == id) {
                return Optional.of(configuration);
            }
        }
        return Optional.absent();
    }

    /**
     * Returns the position of the configuration with the given ID. Returns -1 if the configuration
     * could not be found.
     */
    public int getPositionOfConfigWithId(int id) {
        for (int i = 0; i < configurations.size(); i++) {
            if (configurations.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    /** Creates a new empty configuration. */
    public Configuration newConfiguration() {
        int nextId = persistence.incrementNextId();
        Configuration newConfig = ConfigurationImpl.fromId(nextId, context);
        configurations.add(newConfig);
        saveConfigurationIDs();
        // Delete any state that might be present from a previous install
        // of this app
        try {
            getEmulatorState(nextId).deleteSavedState();
        } catch (IOException e) {
            Log.e(TAG, "Could not clear saved state.", e);
            // TODO: Anything we should do at this point?
        }
        return newConfig;
    }

    public void persistConfig(Configuration configuration) {
        Preconditions.checkNotNull(configuration);
        Configuration toSave = ConfigurationImpl.fromId(configuration.getId(), context);
        toSave.setName(configuration.getName().orNull());
        toSave.setModel(configuration.getModel());
        toSave.setCasettePath(configuration.getCassettePath().orNull());
        toSave.setDiskPaths(configuration.getDiskPaths());
        toSave.setCassettePosition(configuration.getCassettePosition());
        toSave.setKeyboardLayoutPortrait(configuration.getKeyboardLayoutPortrait().orNull());
        toSave.setKeyboardLayoutLandscape(configuration.getKeyboardLayoutLandscape().orNull());
        toSave.setCharacterColorAsRGB(configuration.getCharacterColorAsRGB());
        toSave.setScreenColorAsRGB(configuration.getScreenColorAsRGB());
        toSave.setSoundMuted(configuration.isSoundMuted());
    }

    /** Stores the current list of configurations. */
    private void saveConfigurationIDs() {
        int[] ids = new int[configurations.size()];
        for (int i = 0; i < ids.length; ++i) {
            ids[i] = configurations.get(i).getId();
        }
        persistence.persistConfigurationIds(ids);
    }

    /**
     * Adds a new entry to the configuration manager.
     *
     * @param model      defines which model this entry is for. See {@link Hardware}.
     * @param configName the name of this new configuration.
     * @param filename   the filename to use for the entry.
     * @param content    the byte content of the entry.
     * @return If the configuration was successfully added it will be returned.
     */
    public Optional<Configuration> addNewConfiguration(int model,
                                                       String configName,
                                                       String filename,
                                                       byte[] content) {
        // Configurations automatically persist.
        Configuration newConfig = newConfiguration();
        newConfig.setName(configName);
        newConfig.setModel(model);

        FileManager configFileManager;
        // TODO: Config disks should go into their own sub-directories to avoid conflict.
        try {
            configFileManager = fileManagerCreator.createForAppSubDir(newConfig.getId());
        } catch (IOException e) {
            Log.e(TAG, "Could not create configuration sub-dir.");
            return Optional.absent();
        }
        if (!configFileManager.writeFile(filename, content)) {
            deleteConfigWithId(newConfig.getId());
            return Optional.absent();
        }
        newConfig.setDiskPath(0, configFileManager.getAbsolutePathForFile(filename));
        return Optional.of(newConfig);
    }

    /**
     * Moves the position of the configuration in the list.
     */
    public void moveConfiguration(int fromId, int toId) {
        configurations.add(toId, configurations.remove(fromId));
        saveConfigurationIDs();
    }

    /**
     * Creates an emulator state for the configuration with the given ID.
     */
    public EmulatorState getEmulatorState(int configId) throws IOException {
        return EmulatorState.forConfigId(configId, fileManagerCreator);
    }

    private static final class ConfigurationPersistence {
        private static final String KEY_NEXT_ID = "NEXT_ID";
        private static final String KEY_CONFIGURATIONS = "CONFIGURATIONS";
        private final SharedPreferences prefs;

        private ConfigurationPersistence(SharedPreferences prefs) {
            this.prefs = prefs;
        }

        int incrementNextId() {
            int nextId = prefs.getInt(KEY_NEXT_ID, 0);
            prefs.edit().putInt(KEY_NEXT_ID, nextId + 1).apply();
            return nextId;
        }

        void persistConfigurationIds(int[] ids) {
            String idsStr = "";
            for (int id : ids) {
                if (!idsStr.equals("")) {
                    idsStr += ",";
                }
                idsStr += Integer.toString(id);
            }
            prefs.edit().putString(KEY_CONFIGURATIONS, idsStr).apply();
        }
    }
}
