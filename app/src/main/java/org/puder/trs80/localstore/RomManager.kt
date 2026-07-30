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

package org.puder.trs80.localstore

import com.russhwolf.settings.Settings
import org.puder.trs80.Hardware
import org.puder.trs80.StrUtil
import org.puder.trs80.TRS80Application
import org.puder.trs80.io.FileManager
import org.puder.trs80.shared.storage.StorageKeys
import org.puder.trs80.storage.AppStorage
import java.io.File
import java.io.IOException

/**
 * Manages ROMs.
 */
class RomManager private constructor(
        private val settings: Settings,
        private val fileManager: FileManager) {

    /**
     * Adds a ROM to the local store.
     *
     * @param model defines which model this entry is for. See [Hardware].
     * @param filename the filename to use for the entry.
     * @param content the byte content of the entry.
     * @return Whether the file was successfully added.
     */
    fun addRom(model: Int, filename: String, content: ByteArray): Boolean {
        settings.putString(
                StorageKeys.romKey(model), fileManager.getAbsolutePathForFile(filename))
        return fileManager.writeFile(filename, content)
    }

    /** @return Whether the ROMs for both model I and model III are present. */
    fun hasAllRoms(): Boolean =
            hasRom(Hardware.MODEL1) && hasRom(Hardware.MODEL3)

    /**
     * @return Whether the ROM stored for [model] exists. If the file is gone, the
     * stale entry is removed so the next download replaces it.
     */
    private fun hasRom(model: Int): Boolean {
        val key = StorageKeys.romKey(model)
        val filename = settings.getStringOrNull(key) ?: return false
        if (File(filename).exists()) {
            return true
        }
        settings.remove(key)
        return false
    }

    companion object {
        private var instance: RomManager? = null

        /**
         * Initializes the singleton, unless it already exists.
         *
         * @return The singleton [RomManager] instance.
         */
        @Throws(IOException::class)
        fun init(fileManagerCreator: FileManager.Creator): RomManager =
                instance ?: RomManager(
                        AppStorage.get().settings,
                        fileManagerCreator.forAppBaseDir()).also { instance = it }

        /**
         * @return The singleton [RomManager] instance.
         */
        fun get(): RomManager = checkNotNull(instance) { "Must call RomManager.init() first." }
    }
}
