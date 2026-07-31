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
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.puder.trs80.shared.KeyboardLayout
import org.puder.trs80.shared.MODEL3
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.shared.storage.StorageKeys
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the configuration domain now that it is in `commonMain`.
 *
 * It had no tests on Android, where it was welded to a `Context`; being handed
 * its storage instead is what makes this possible at all.
 */
class ConfigurationManagerTest {

    private val fileSystem = FakeFileSystem()
    private val settings: Settings = MapSettings()
    private val creator = FileManager.Creator("/data/TRS-80".toPath(), fileSystem)

    private fun persistence(id: Int) = ConfigurationPersistence.forId(id, settings)

    private fun configuration(id: Int): Configuration =
        ConfigurationImpl.fromId(id, settings)

    @Test
    fun valuesRoundTripThroughTheStore() {
        val config = configuration(1)

        config.setName("My Model III")
        config.model = MODEL3
        config.setCassettePath("/tapes/a.cas")
        config.setDiskPath(0, "/disks/boot.dsk")
        config.cassettePosition = 12.5f
        config.isSoundMuted = true
        config.setKeyboardLayoutPortrait(KeyboardLayout.KEYBOARD_LAYOUT_COMPACT)

        val reread = configuration(1)
        assertEquals("My Model III", reread.name)
        assertEquals(MODEL3, reread.model)
        assertEquals("/tapes/a.cas", reread.cassettePath)
        assertEquals("/disks/boot.dsk", reread.getDiskPath(0))
        assertEquals(12.5f, reread.cassettePosition)
        assertTrue(reread.isSoundMuted)
        assertEquals(KeyboardLayout.KEYBOARD_LAYOUT_COMPACT, reread.keyboardLayoutPortrait)
    }

    /** Each configuration's values are namespaced, so they must not collide. */
    @Test
    fun configurationsDoNotSeeEachOthersValues() {
        configuration(1).setName("First")
        configuration(2).setName("Second")

        assertEquals("First", configuration(1).name)
        assertEquals("Second", configuration(2).name)
    }

    /**
     * Absent, not empty: the rest of the app reads a missing key as "no disk",
     * so clearing a path has to remove it rather than store "".
     */
    @Test
    fun clearingAPathRemovesTheKey() {
        val config = configuration(1)
        config.setDiskPath(0, "/disks/boot.dsk")

        config.setDiskPath(0, null)

        assertNull(config.getDiskPath(0))
        assertFalse(settings.keys.any { it.endsWith(StorageKeys.diskKey(0)) })
    }

    @Test
    fun diskPathsCoverEveryDrive() {
        val config = configuration(1)
        config.diskPaths = listOf("/a.dsk", null, "/c.dsk", null)

        assertEquals(StorageKeys.DRIVE_COUNT, config.diskPaths.size)
        assertContentEquals(listOf("/a.dsk", null, "/c.dsk", null), config.diskPaths)
    }

    @Test
    fun deletingAConfigurationClearsOnlyItsOwnKeys() {
        configuration(1).setName("First")
        configuration(2).setName("Second")

        configuration(1).delete()

        assertEquals("unknown", configuration(1).name)
        assertEquals("Second", configuration(2).name)
    }

    @Test
    fun aBackupIsAFrozenCopy() {
        val config = configuration(1)
        config.setName("Original")
        config.setDiskPath(1, "/disks/b.dsk")

        val backup = config.createBackup()
        config.setName("Changed")

        assertEquals("Original", backup.name)
        assertEquals("/disks/b.dsk", backup.getDiskPath(1))
        assertFailsWith<UnsupportedOperationException> { backup.setName("Nope") }
        assertFailsWith<UnsupportedOperationException> { backup.delete() }
    }

    @Test
    fun addingAConfigurationWritesItsMedia() {
        val manager = ConfigurationManager.create(creator, settings)

        val config = manager.addNewConfiguration(
            model = MODEL3,
            configName = "Boots",
            disks = listOf(ConfigurationManager.ConfigMedia("boot.dsk", byteArrayOf(1, 2, 3))),
            cassette = null,
        )

        assertTrue(config != null)
        assertEquals("Boots", config.name)
        assertTrue(config.getDiskPath(0)!!.endsWith("/${config.id}/boot.dsk"))
        assertTrue(fileSystem.exists(config.getDiskPath(0)!!.toPath()))
    }

    /** A configuration whose media could not be written must not be left behind. */
    @Test
    fun aConfigurationWithAnUnwritableDiskIsRolledBack() {
        // Directories can still be made, so only the disk write fails.
        val manager = ConfigurationManager.create(
            FileManager.Creator("/data/TRS-80".toPath(), WritesFail(fileSystem)),
            settings,
        )

        val config = manager.addNewConfiguration(
            model = MODEL3,
            configName = "Doomed",
            disks = listOf(ConfigurationManager.ConfigMedia("boot.dsk", byteArrayOf(1, 2, 3))),
            cassette = null,
        )

        assertNull(config)
        assertEquals(0, manager.configCount)
        assertEquals("", settings.getStringOrNull(StorageKeys.CONFIGURATION_IDS))
    }

    @Test
    fun theStoredOrderSurvivesReloading() {
        val manager = ConfigurationManager.create(creator, settings)
        val first = manager.newConfiguration()
        val second = manager.newConfiguration()

        assertEquals(
            "${first.id},${second.id}",
            settings.getStringOrNull(StorageKeys.CONFIGURATION_IDS),
        )
    }

    @Test
    fun idsAreHandedOutInSequence() {
        val manager = ConfigurationManager.create(creator, settings)

        val ids = List(3) { manager.newConfiguration().id }

        assertEquals(listOf(1, 2, 3), ids)
    }

    @Test
    fun aStoredModelReadsBackAsItself() {
        persistence(1).setModel("3")
        assertEquals(MODEL3, configuration(1).model)
    }
}

/** A file system that makes directories but refuses to write files. */
private class WritesFail(delegate: okio.FileSystem) : okio.ForwardingFileSystem(delegate) {
    override fun sink(file: okio.Path, mustCreate: Boolean): okio.Sink =
        throw okio.IOException("no writing here")
}
