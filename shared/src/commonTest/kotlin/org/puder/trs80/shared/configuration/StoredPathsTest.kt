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

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import org.puder.trs80.shared.io.appDataDirectory
import org.puder.trs80.shared.storage.StorageKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Paths must not be stored absolute: on iOS the app's container is a UUID that
 * changes on reinstall, and a configuration would keep every value while losing
 * every file.
 */
class StoredPathsTest {

    private val settings: Settings = MapSettings()
    private val persistence = ConfigurationPersistence.forId(1, settings)
    private val appDir = appDataDirectory().toString()

    @Test
    fun aPathInsideTheAppDirectoryIsStoredRelative() {
        persistence.setDiskPath(0, "$appDir/TRS-80/2/boot.dsk")

        val stored = settings.getStringOrNull("${StorageKeys.configurationPrefix(1)}${StorageKeys.diskKey(0)}")
        assertEquals("TRS-80/2/boot.dsk", stored)
    }

    @Test
    fun andComesBackAbsolute() {
        persistence.setDiskPath(0, "$appDir/TRS-80/2/boot.dsk")

        assertEquals("$appDir/TRS-80/2/boot.dsk", persistence.getDiskPath(0))
    }

    /** Configurations written before this change hold absolute paths. */
    @Test
    fun anAlreadyAbsoluteStoredPathIsLeftAlone() {
        val key = "${StorageKeys.configurationPrefix(1)}${StorageKeys.diskKey(0)}"
        settings.putString(key, "/legacy/elsewhere/boot.dsk")

        assertEquals("/legacy/elsewhere/boot.dsk", persistence.getDiskPath(0))
    }

    @Test
    fun aPathOutsideTheAppDirectoryStaysAbsolute() {
        persistence.setCasettePath("/somewhere/else/tape.cas")

        assertEquals("/somewhere/else/tape.cas", persistence.casettePath)
    }

    @Test
    fun clearingStillRemovesTheKey() {
        persistence.setDiskPath(0, "$appDir/TRS-80/2/boot.dsk")
        persistence.setDiskPath(0, null)

        assertNull(persistence.getDiskPath(0))
    }

    /**
     * The point of the whole thing: a path stored under one container resolves
     * under whatever the container is now.
     */
    @Test
    fun theStoredFormCarriesNoContainer() {
        persistence.setDiskPath(0, "$appDir/TRS-80/2/boot.dsk")

        val stored = settings.getStringOrNull("${StorageKeys.configurationPrefix(1)}${StorageKeys.diskKey(0)}")!!
        assertTrue(!stored.startsWith("/"), "stored path '$stored' is still absolute")
        assertTrue(!stored.contains(appDir), "stored path '$stored' still names the container")
    }
}
