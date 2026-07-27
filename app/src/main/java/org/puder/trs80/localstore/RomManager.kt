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

import android.content.Context
import android.content.SharedPreferences
import org.puder.trs80.Hardware
import org.puder.trs80.SettingsActivity
import org.puder.trs80.StrUtil
import org.puder.trs80.TRS80Application
import org.puder.trs80.io.FileManager
import java.io.File
import java.io.IOException

/**
 * Manages ROMs.
 */
class RomManager private constructor(
        private val sharedPrefs: SharedPreferences,
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
        sharedPrefs.edit()
                .putString(keyForModel(model), fileManager.getAbsolutePathForFile(filename))
                .apply()
        return fileManager.writeFile(filename, content)
    }

    /** @return Whether the ROMs for both model I and model III are present. */
    fun hasAllRoms(): Boolean =
            hasRom(SettingsActivity.CONF_ROM_MODEL1) && hasRom(SettingsActivity.CONF_ROM_MODEL3)

    /**
     * @return Whether the ROM referenced by the given preference exists. If the file is gone,
     * the stale preference is removed.
     */
    private fun hasRom(prop: String): Boolean {
        val filename = sharedPrefs.getString(prop, null) ?: return false
        if (File(filename).exists()) {
            return true
        }
        sharedPrefs.edit().remove(prop).apply()
        return false
    }

    private fun keyForModel(model: Int): String = when (model) {
        Hardware.MODEL1 -> SettingsActivity.CONF_ROM_MODEL1
        Hardware.MODEL3 -> SettingsActivity.CONF_ROM_MODEL3
        Hardware.MODEL4 -> SettingsActivity.CONF_ROM_MODEL4
        Hardware.MODEL4P -> SettingsActivity.CONF_ROM_MODEL4P
        else -> throw IllegalArgumentException(StrUtil.form("Model %d not found.", model))
    }

    companion object {
        private var instance: RomManager? = null

        /**
         * Initializes the singleton, unless it already exists.
         *
         * @return The singleton [RomManager] instance.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun init(fileManagerCreator: FileManager.Creator): RomManager =
                instance ?: RomManager(
                        TRS80Application.getAppContext().getSharedPreferences(
                                SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE),
                        fileManagerCreator.forAppBaseDir()).also { instance = it }

        /**
         * @return The singleton [RomManager] instance.
         */
        @JvmStatic
        fun get(): RomManager = checkNotNull(instance) { "Must call RomManager.init() first." }
    }
}
