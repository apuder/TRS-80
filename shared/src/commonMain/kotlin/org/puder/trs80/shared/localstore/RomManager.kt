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

package org.puder.trs80.shared.localstore

import com.russhwolf.settings.Settings
import okio.IOException
import okio.Path.Companion.toPath
import org.puder.trs80.shared.MODEL1
import org.puder.trs80.shared.MODEL3
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.shared.io.appFileSystem
import org.puder.trs80.shared.io.resolveStoredPath
import org.puder.trs80.shared.io.toStoredPath
import org.puder.trs80.shared.storage.StorageKeys

/**
 * The ROM images the emulator boots from.
 *
 * A ROM is a file on disk plus the path to it in the store; this keeps the two
 * from drifting apart.
 */
class RomManager private constructor(
    private val settings: Settings,
    private val fileManager: FileManager,
) {

    /**
     * Adds a ROM to the local store.
     *
     * @param model defines which model this entry is for.
     * @param filename the filename to use for the entry.
     * @param content the byte content of the entry.
     * @return Whether the file was successfully added.
     */
    fun addRom(model: Int, filename: String, content: ByteArray): Boolean {
        settings.putString(
            StorageKeys.romKey(model),
            toStoredPath(fileManager.getAbsolutePathForFile(filename)),
        )
        return fileManager.writeFile(filename, content)
    }

    /** @return the absolute path of the ROM for [model], or null if there is none. */
    fun romPath(model: Int): String? =
        settings.getStringOrNull(StorageKeys.romKey(model))?.let(::resolveStoredPath)

    /** @return Whether the ROMs for both model I and model III are present. */
    fun hasAllRoms(): Boolean = hasRom(MODEL1) && hasRom(MODEL3)

    /**
     * @return The models the editor should offer, in order.
     *
     * Model I and Model III are always among them: the app requires both ROMs
     * before it will run anything, so they are not really optional, and leaving
     * one out because its ROM has gone missing would quietly take away a choice
     * the app has always had. Model 4 and 4P are genuinely optional and appear
     * only once their ROMs are in place.
     */
    fun modelsToOffer(): List<Int> =
        StorageKeys.romModels.filter { it == MODEL1 || it == MODEL3 || hasRom(it) }

    /**
     * @return Whether the ROM stored for [model] exists. If the file is gone, the
     * stale entry is removed so the next download replaces it.
     */
    fun hasRom(model: Int): Boolean {
        val filename = romPath(model) ?: return false
        if (appFileSystem.exists(filename.toPath())) {
            return true
        }
        settings.remove(StorageKeys.romKey(model))
        return false
    }

    companion object {
        private var instance: RomManager? = null

        /**
         * Initializes the singleton, unless it already exists.
         *
         * @return The singleton [RomManager] instance.
         * @throws IOException if the app's storage directory could not be created.
         */
        @Throws(IOException::class)
        fun init(fileManagerCreator: FileManager.Creator, settings: Settings): RomManager =
            instance ?: RomManager(settings, fileManagerCreator.forAppBaseDir())
                .also { instance = it }

        /** @return The singleton [RomManager] instance. */
        fun get(): RomManager = checkNotNull(instance) { "Must call RomManager.init() first." }
    }
}
