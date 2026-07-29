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

package org.puder.trs80.configuration

import org.puder.trs80.shared.KeyboardLayout

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import org.puder.trs80.Hardware
import org.puder.trs80.io.FileManager
import java.io.IOException

private const val TAG = "ConfigManager"
private const val KEY_NEXT_ID = "NEXT_ID"
private const val KEY_CONFIGURATIONS = "CONFIGURATIONS"

/** The maximum number of disk images a configuration can hold. */
private const val MAX_DISKS = 4

/**
 * This class manages the installed configurations.
 */
class ConfigurationManager private constructor(
    /** Creates the file managers that hold the configurations' storage. */
    private val fileManagerCreator: FileManager.Creator,
    /** All configurations, in the order the user arranged them. */
    private val configurations: MutableList<Configuration>,
    private val persistence: GlobalPersistence,
    private val context: Context
) {

    companion object {
        /** The singleton instance of the ConfigurationManager. */
        private var singleton: ConfigurationManager? = null

        /**
         * @return The singleton [ConfigurationManager]. It is important that there is only a
         * single instance in the app since the state needs to be shared.
         */
        @Throws(IOException::class)
        fun get(context: Context): ConfigurationManager =
            singleton ?: initDefault(FileManager.Creator.get(context.resources), context)

        /**
         * Initialize the default instance of the manager. This should be done exactly once.
         *
         * @param fileManagerCreator creates file manager instances.
         * @throws IOException if the manager could not be initialized.
         */
        @Throws(IOException::class)
        private fun initDefault(
            fileManagerCreator: FileManager.Creator,
            context: Context
        ): ConfigurationManager {
            singleton?.let {
                Log.i(TAG, "ConfigurationManager singleton already initialized.")
                return it
            }
            // Makes sure the app's base directory exists.
            fileManagerCreator.forAppBaseDir()
            val globalPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            return ConfigurationManager(
                fileManagerCreator,
                loadConfigurations(globalPrefs, context),
                GlobalPersistence(globalPrefs),
                context
            ).also { singleton = it }
        }

        private fun loadConfigurations(
            globalPrefs: SharedPreferences,
            context: Context
        ): MutableList<Configuration> =
            globalPrefs.getString(KEY_CONFIGURATIONS, "").orEmpty()
                .split(",")
                .filter { it.isNotEmpty() }
                .mapTo(mutableListOf()) { ConfigurationImpl.fromId(it.toInt(), context) }
    }

    /** The number of configurations. */
    val configCount: Int
        get() = configurations.size

    /**
     * @return The n-th configuration.
     */
    fun getConfig(n: Int): Configuration = configurations[n]

    /**
     * Deletes the configuration with the given ID.
     *
     * @return Whether the configuration and its saved state were fully removed.
     */
    fun deleteConfigWithId(id: Int): Boolean {
        val config = getConfigById(id) ?: return false
        configurations.remove(config)
        config.delete()
        saveConfigurationIds()

        return try {
            getEmulatorState(id).deleteAll()
            true
        } catch (e: IOException) {
            Log.e(TAG, "Could not clear saved state.", e)
            // TODO: Anything we should do at this point?
            false
        }
    }

    /**
     * @return The configuration with the given ID, or null if it does not exist.
     */
    fun getConfigById(id: Int): Configuration? = configurations.firstOrNull { it.id == id }

    /**
     * @return The position of the configuration with the given ID, or -1 if the configuration
     * could not be found.
     */
    fun getPositionOfConfigWithId(id: Int): Int = configurations.indexOfFirst { it.id == id }

    /** Creates a new empty configuration. */
    fun newConfiguration(): Configuration {
        val nextId = persistence.incrementNextId()
        val newConfig = ConfigurationImpl.fromId(nextId, context)
        configurations.add(newConfig)
        saveConfigurationIds()
        // Delete any state that might be present from a previous install of this app.
        try {
            getEmulatorState(nextId).deleteSavedState()
        } catch (e: IOException) {
            Log.e(TAG, "Could not clear saved state.", e)
            // TODO: Anything we should do at this point?
        }
        return newConfig
    }

    /** Writes the values of the given configuration back into its persisted storage. */
    fun persistConfig(configuration: Configuration) {
        val toSave = ConfigurationImpl.fromId(configuration.id, context)
        toSave.setName(configuration.name)
        toSave.model = configuration.model
        toSave.setCassettePath(configuration.cassettePath)
        toSave.diskPaths = configuration.diskPaths
        toSave.cassettePosition = configuration.cassettePosition
        toSave.setKeyboardLayoutPortrait(configuration.keyboardLayoutPortrait)
        toSave.setKeyboardLayoutLandscape(configuration.keyboardLayoutLandscape)
        toSave.characterColor = configuration.characterColor
        toSave.screenColorAsRGB = configuration.screenColorAsRGB
        toSave.isSoundMuted = configuration.isSoundMuted
    }

    /** Stores the current list of configurations. */
    private fun saveConfigurationIds() =
        persistence.persistConfigurationIds(configurations.map { it.id })

    /**
     * Adds a new entry to the configuration manager.
     *
     * @param model      defines which model this entry is for. See [Hardware].
     * @param configName the name of this new configuration.
     * @param disks      the disk images for this configuration.
     * @param cassette   the cassette image, or null, for this configuration.
     * @return The new configuration, or null if it could not be added.
     */
    fun addNewConfiguration(
        model: Int,
        configName: String?,
        disks: List<ConfigMedia?>,
        cassette: ConfigMedia?
    ): Configuration? {
        // Configurations automatically persist.
        val newConfig = newConfiguration()
        newConfig.setName(configName)
        newConfig.model = model

        val configFileManager = try {
            fileManagerCreator.createForAppSubDir(newConfig.id)
        } catch (e: IOException) {
            Log.e(TAG, "Could not create configuration sub-dir.")
            return null
        }

        for (disk in 0 until minOf(MAX_DISKS, disks.size)) {
            val (filename, data) = disks[disk] ?: continue
            if (data == null || data.isEmpty()) {
                continue
            }
            if (filename.isNullOrEmpty()) {
                Log.e(TAG, "Media filename is empty. Skipping.")
                continue
            }
            // If any disk fails writing, delete the whole config.
            if (!configFileManager.writeFile(filename, data)) {
                deleteConfigWithId(newConfig.id)
                return null
            }
            newConfig.setDiskPath(disk, configFileManager.getAbsolutePathForFile(filename))
        }

        val cassetteName = cassette?.filename
        val cassetteData = cassette?.data
        if (cassetteData != null && cassetteData.isNotEmpty()) {
            if (cassetteName.isNullOrEmpty()) {
                Log.e(TAG, "Cassette filename is empty. Skipping.")
            } else if (!configFileManager.writeFile(cassetteName, cassetteData)) {
                // If the cassette fails writing, delete the whole config.
                deleteConfigWithId(newConfig.id)
                return null
            } else {
                newConfig.setCassettePath(
                    configFileManager.getAbsolutePathForFile(cassetteName)
                )
            }
        }

        return newConfig
    }

    /**
     * Moves the position of the configuration in the list.
     */
    fun moveConfiguration(fromId: Int, toId: Int) {
        configurations.add(toId, configurations.removeAt(fromId))
        saveConfigurationIds()
    }

    /**
     * Creates an emulator state for the configuration with the given ID.
     */
    @Throws(IOException::class)
    fun getEmulatorState(configId: Int): EmulatorState =
        EmulatorState.forConfigId(configId, fileManagerCreator)

    /** Data that is global to all configurations. */
    private class GlobalPersistence(private val prefs: SharedPreferences) {

        fun incrementNextId(): Int =
            (prefs.getInt(KEY_NEXT_ID, 0) + 1).also { prefs.edit().putInt(KEY_NEXT_ID, it).apply() }

        fun persistConfigurationIds(ids: List<Int>) =
            prefs.edit().putString(KEY_CONFIGURATIONS, ids.joinToString(",")).apply()
    }

    /** A media image (disk or cassette) to be stored with a configuration. */
    data class ConfigMedia(val filename: String?, val data: ByteArray?)
}
