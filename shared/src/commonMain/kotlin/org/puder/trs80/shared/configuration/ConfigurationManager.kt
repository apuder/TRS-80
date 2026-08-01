/*
 * Copyright The TRS-80 App Authors
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

package org.puder.trs80.shared.configuration

import com.russhwolf.settings.Settings
import okio.IOException
import org.puder.trs80.shared.Log
import okio.Path.Companion.toPath
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.shared.io.appFileSystem
import org.puder.trs80.shared.storage.StorageKeys

private const val TAG = "ConfigManager"

/** The maximum number of disk images a configuration can hold. */
private const val MAX_DISKS = 4

/**
 * The installed configurations, in the order the user arranged them.
 *
 * Still a singleton, and still initialized once at start-up — but it is now
 * handed its storage rather than reaching for a global `Context`, which is what
 * lets it live here at all.
 */
class ConfigurationManager private constructor(
    /** Creates the file managers that hold the configurations' storage. */
    private val fileManagerCreator: FileManager.Creator,
    /** All configurations, in the order the user arranged them. */
    private val configurations: MutableList<Configuration>,
    private val settings: Settings,
    private val persistence: GlobalPersistence,
) {

    companion object {
        private var singleton: ConfigurationManager? = null

        /**
         * Initializes the singleton, unless it already exists. Call once at start-up.
         *
         * There is only ever one because the configuration list is shared state:
         * two instances would each hold their own ordering and overwrite the
         * other's.
         *
         * @throws IOException if the app's storage directory could not be created.
         */
        @Throws(IOException::class)
        fun init(
            fileManagerCreator: FileManager.Creator,
            settings: Settings,
        ): ConfigurationManager {
            singleton?.let {
                Log.i(TAG, "ConfigurationManager singleton already initialized.")
                return it
            }
            return create(fileManagerCreator, settings).also { singleton = it }
        }

        /** @return the singleton, which [init] must have created. */
        fun get(): ConfigurationManager =
            checkNotNull(singleton) { "Must call ConfigurationManager.init() first." }

        /**
         * Builds an instance without touching the singleton.
         *
         * The app wants exactly one manager, but a test wants one per case, and
         * [init] cannot give it that -- it hands back whatever the first call
         * created, storage and all.
         *
         * @throws IOException if the storage directory could not be created.
         */
        @Throws(IOException::class)
        internal fun create(
            fileManagerCreator: FileManager.Creator,
            settings: Settings,
        ): ConfigurationManager {
            fileManagerCreator.forAppBaseDir()
            return ConfigurationManager(
                fileManagerCreator,
                loadConfigurations(settings),
                settings,
                GlobalPersistence(settings),
            )
        }

        private fun loadConfigurations(settings: Settings): MutableList<Configuration> =
            settings.getStringOrNull(StorageKeys.CONFIGURATION_IDS).orEmpty()
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .mapTo(mutableListOf()) { ConfigurationImpl.fromId(it, settings) }
    }

    /** The number of configurations. */
    val configCount: Int
        get() = configurations.size

    /** @return The n-th configuration. */
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

    /** @return The configuration with the given ID, or null if it does not exist. */
    fun getConfigById(id: Int): Configuration? = configurations.firstOrNull { it.id == id }

    /**
     * @return The position of the configuration with the given ID, or -1 if the configuration
     * could not be found.
     */
    fun getPositionOfConfigWithId(id: Int): Int = configurations.indexOfFirst { it.id == id }

    /** Creates a new empty configuration. */
    fun newConfiguration(): Configuration {
        val nextId = persistence.incrementNextId()
        val newConfig = ConfigurationImpl.fromId(nextId, settings)
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
        val toSave = ConfigurationImpl.fromId(configuration.id, settings)
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
        // Editing something makes it the user's own, which is what the library's
        // CUSTOM mark means.
        toSave.isCustom = true
    }

    /**
     * Writes an edited [draft] back into its persisted storage.
     *
     * Like [persistConfig] this marks the configuration custom: editing
     * something makes it the user's own, which is what the library's CUSTOM
     * mark means and what the editor's fork banner has just announced.
     */
    fun persistDraft(draft: ConfigurationDraft) {
        val toSave = ConfigurationImpl.fromId(draft.id, settings)
        toSave.setName(draft.name)
        toSave.model = draft.model
        toSave.setCassettePath(draft.cassettePath)
        toSave.diskPaths = draft.diskPaths
        toSave.setKeyboardLayoutPortrait(draft.keyboardPortrait)
        toSave.setKeyboardLayoutLandscape(draft.keyboardLandscape)
        toSave.characterColor = draft.characterColor
        toSave.isSoundMuted = draft.soundMuted
        toSave.isCustom = true
    }

    /**
     * Copies [content] into the configuration's own directory.
     *
     * A file the user picks lives outside the app, or in a temporary copy that
     * will be swept up; a configuration has to point at something that stays.
     *
     * @return the absolute path of the stored file, or null if it could not be
     * written.
     */
    fun storeMedia(configurationId: Int, filename: String, content: ByteArray): String? {
        val configFileManager = try {
            fileManagerCreator.createForAppSubDir(configurationId)
        } catch (e: IOException) {
            Log.e(TAG, "Could not open the configuration's directory.", e)
            return null
        }
        if (!configFileManager.writeFile(filename, content)) {
            Log.e(TAG, "Could not write $filename.")
            return null
        }
        return configFileManager.getAbsolutePathForFile(filename)
    }

    /**
     * Makes an independent copy of a configuration.
     *
     * The disks are copied too, not shared: the point of a copy is that saving
     * to it, or ejecting from it, leaves the original alone, and two
     * configurations pointing at one file on disk would not manage that.
     *
     * @return the new configuration, or null if the original is gone or its
     * files could not be copied.
     */
    fun duplicate(configurationId: Int): Configuration? {
        val source = getConfigById(configurationId) ?: return null
        val copy = newConfiguration()
        copy.setName(source.name.orEmpty().ifEmpty { "Copy" } + " copy")
        copy.model = source.model
        copy.setKeyboardLayoutPortrait(source.keyboardLayoutPortrait)
        copy.setKeyboardLayoutLandscape(source.keyboardLayoutLandscape)
        copy.characterColor = source.characterColor
        copy.isSoundMuted = source.isSoundMuted
        // The user's own from the moment it exists: it is a copy they asked for,
        // not something the catalog put there.
        copy.isCustom = true

        val copied = source.diskPaths.map { path ->
            if (path == null) return@map null
            val bytes = runCatching {
                appFileSystem.read(path.toPath()) { readByteArray() }
            }.getOrElse {
                Log.e(TAG, "Could not read $path while copying.", it)
                deleteConfigWithId(copy.id)
                return null
            }
            storeMedia(copy.id, path.toPath().name, bytes) ?: run {
                deleteConfigWithId(copy.id)
                return null
            }
        }
        copy.diskPaths = copied
        return copy
    }

    /** Stores the current list of configurations. */
    private fun saveConfigurationIds() =
        persistence.persistConfigurationIds(configurations.map { it.id })

    /**
     * Adds a new entry to the configuration manager.
     *
     * @param model      defines which model this entry is for.
     * @param configName the name of this new configuration.
     * @param disks      the disk images for this configuration.
     * @param cassette   the cassette image, or null, for this configuration.
     * @return The new configuration, or null if it could not be added.
     */
    fun addNewConfiguration(
        model: Int,
        configName: String?,
        disks: List<ConfigMedia?>,
        cassette: ConfigMedia?,
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

    /** Moves the position of the configuration in the list. */
    fun moveConfiguration(fromId: Int, toId: Int) {
        configurations.add(toId, configurations.removeAt(fromId))
        saveConfigurationIds()
    }

    /** Creates an emulator state for the configuration with the given ID. */
    @Throws(IOException::class)
    fun getEmulatorState(configId: Int): EmulatorState =
        EmulatorState.forConfigId(configId, fileManagerCreator)

    /** Data that is global to all configurations. */
    private class GlobalPersistence(private val settings: Settings) {

        fun incrementNextId(): Int =
            ((settings.getIntOrNull(StorageKeys.NEXT_CONFIGURATION_ID) ?: 0) + 1)
                .also { settings.putInt(StorageKeys.NEXT_CONFIGURATION_ID, it) }

        fun persistConfigurationIds(ids: List<Int>) =
            settings.putString(StorageKeys.CONFIGURATION_IDS, ids.joinToString(","))
    }

    /** A media image (disk or cassette) to be stored with a configuration. */
    data class ConfigMedia(val filename: String?, val data: ByteArray?)
}
