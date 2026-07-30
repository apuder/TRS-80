/*
 * Copyright 2025, Arno Puder
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

package org.puder.trs80.shared.storage

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The import runs once against real user data, and there is no second chance if
 * it is wrong — so these cover the states actual installs are in, not just the
 * happy path.
 */
class LegacyImportTest {

    // ---- What gets copied --------------------------------------------------

    @Test
    fun copiesConfigurationValuesUnderNamespacedKeys() {
        val target = MapSettings()
        val configs = mapOf(
            1 to MapSettings(
                "conf_name" to "TRS-80 Tutorial",
                "conf_model" to "3",
                "conf_disk1" to "/data/disk_0.dsk",
                "conf_mute_sound" to false,
                "cassette_position" to 0.25f,
            ),
            2 to MapSettings("conf_name" to "Armored Patrol", "conf_model" to "1"),
        )

        val result = importer(target, global("1,2"), MapSettings(), configs).runIfNeeded()

        assertIs<ImportResult.Imported>(result)
        assertEquals(2, result.configurations)
        assertEquals("TRS-80 Tutorial", target.getStringOrNull("config.1.conf_name"))
        assertEquals("3", target.getStringOrNull("config.1.conf_model"))
        assertEquals("/data/disk_0.dsk", target.getStringOrNull("config.1.conf_disk1"))
        assertEquals(false, target.getBooleanOrNull("config.1.conf_mute_sound"))
        assertEquals(0.25f, target.getFloatOrNull("config.1.cassette_position"))
        assertEquals("Armored Patrol", target.getStringOrNull("config.2.conf_name"))
    }

    @Test
    fun copiesRomPathsAndTheIdCounter() {
        val target = MapSettings()
        val appSettings = MapSettings(
            "conf_rom_model1" to "/data/model1.rom",
            "conf_rom_model3" to "/data/model3.rom",
        )

        importer(target, MapSettings("NEXT_ID" to 12), appSettings, emptyMap()).runIfNeeded()

        assertEquals("/data/model1.rom", target.getStringOrNull(StorageKeys.romKey(1)))
        assertEquals("/data/model3.rom", target.getStringOrNull(StorageKeys.romKey(3)))
        // Never set, so it must stay absent rather than become an empty string.
        assertNull(target.getStringOrNull(StorageKeys.romKey(4)))
        assertEquals(12, target.getIntOrNull(StorageKeys.NEXT_CONFIGURATION_ID))
    }

    @Test
    fun keepsNumbersEncodedAsStrings() {
        // The preference screens still write these as strings, so normalizing
        // them here would break the screens bridged onto this store.
        val target = MapSettings()
        val configs = mapOf(
            7 to MapSettings("conf_character_color" to "1", "conf_keyboard_portrait" to "2"),
        )

        importer(target, global("7"), MapSettings(), configs).runIfNeeded()

        assertEquals("1", target.getStringOrNull("config.7.conf_character_color"))
        assertNull(target.getIntOrNull("config.7.conf_character_color"))
    }

    @Test
    fun leavesLegacyDataInPlace() {
        // Deliberate: a rollback must not cost the user their configurations.
        val legacyConfig = MapSettings("conf_name" to "Rear Guard")
        importer(MapSettings(), global("5"), MapSettings(), mapOf(5 to legacyConfig)).runIfNeeded()
        assertEquals("Rear Guard", legacyConfig.getStringOrNull("conf_name"))
    }

    @Test
    fun ignoresJunkInTheConfigurationIdList() {
        // The list has been written by the app across many versions; trailing
        // commas leave empty entries.
        val target = MapSettings()
        val configs = mapOf(3 to MapSettings("conf_name" to "Only real one"))

        importer(target, global("3,,x, "), MapSettings(), configs).runIfNeeded()

        assertEquals("Only real one", target.getStringOrNull("config.3.conf_name"))
        assertEquals("3,,x, ", target.getStringOrNull(StorageKeys.CONFIGURATION_IDS))
    }

    // ---- Running once, and the status ---------------------------------------

    @Test
    fun startupRunsItOnceThenReportsItIsDone() {
        val target = MapSettings()
        val subject = importer(target, global("1"), MapSettings(), config(1, "First"))

        assertEquals(ImportStatus.NEVER_RUN, subject.status)
        assertIs<ImportResult.Imported>(subject.runIfNeeded())
        assertEquals(ImportStatus.SUCCEEDED, subject.status)
        assertIs<ImportResult.AlreadyDone>(subject.runIfNeeded())
    }

    @Test
    fun aFreshInstallWithNoLegacyDataSettlesImmediately() {
        val target = MapSettings()
        val subject = importer(target, MapSettings(), MapSettings(), emptyMap())

        assertFalse(subject.hasLegacyData)
        assertIs<ImportResult.NothingToImport>(subject.runIfNeeded())
        // Recorded, so startup does not retry on every launch forever.
        assertEquals(ImportStatus.SUCCEEDED, subject.status)
        assertIs<ImportResult.AlreadyDone>(subject.runIfNeeded())
    }

    @Test
    fun aFailedReadLeavesTheStoreUntouchedAndReportsWhy() {
        val target = MapSettings()
        val subject = LegacyImport(target, global("1,2"), MapSettings()) { id ->
            if (id == 2) throw IllegalStateException("preferences file is corrupt") else config1()
        }

        val result = subject.runIfNeeded()

        assertIs<ImportResult.Failed>(result)
        assertEquals("preferences file is corrupt", result.message)
        assertEquals(ImportStatus.FAILED, subject.status)
        assertEquals("preferences file is corrupt", subject.lastError)
        // Prepared in full before writing, so a bad read writes nothing at all.
        assertNull(target.getStringOrNull("config.1.conf_name"))
        assertNull(target.getStringOrNull(StorageKeys.CONFIGURATION_IDS))
    }

    @Test
    fun aLaterSuccessClearsTheRecordedError() {
        val target = MapSettings()
        var broken = true
        val subject = LegacyImport(target, global("1"), MapSettings()) {
            if (broken) throw IllegalStateException("boom") else config1()
        }
        subject.runIfNeeded()
        assertEquals(ImportStatus.FAILED, subject.status)

        broken = false
        assertIs<ImportResult.Imported>(subject.runIfNeeded())
        assertEquals(ImportStatus.SUCCEEDED, subject.status)
        assertNull(subject.lastError)
    }

    // ---- Never overwriting -------------------------------------------------

    @Test
    fun aReImportDoesNotOverwriteWhatTheUserHasSince() {
        // The reason writes are guarded at all. The status lives in the store it
        // guards while the legacy files are kept forever, so anything that loses
        // one but not the other re-runs this.
        val target = MapSettings()
        val configs = mapOf(
            1 to MapSettings(
                "conf_name" to "Original",
                "conf_model" to "1",
                "conf_mute_sound" to false,
                "cassette_position" to 0.0f,
            ),
        )
        val subject = importer(target, global("1"), MapSettings(), configs)
        subject.runIfNeeded()

        target.putString("config.1.conf_name", "Renamed")
        target.putString("config.1.conf_model", "3")
        target.putBoolean("config.1.conf_mute_sound", true)
        target.putFloat("config.1.cassette_position", 0.75f)

        target.remove(StorageKeys.IMPORT_STATUS)
        val result = subject.runIfNeeded()

        assertIs<ImportResult.Imported>(result)
        assertEquals(0, result.values, "Nothing was left to fill in.")
        assertEquals("Renamed", target.getStringOrNull("config.1.conf_name"))
        assertEquals("3", target.getStringOrNull("config.1.conf_model"))
        assertEquals(true, target.getBooleanOrNull("config.1.conf_mute_sound"))
        assertEquals(0.75f, target.getFloatOrNull("config.1.cassette_position"))
    }

    @Test
    fun anEmptyOrFalseStoredValueIsStillTheUsersAnswer() {
        // Presence is tested by key, not by value. An ejected cassette is stored
        // as an empty string and must survive a re-import.
        val target = MapSettings()
        val configs = mapOf(1 to MapSettings("conf_cassette" to "/data/tape.cas"))
        val subject = importer(target, global("1"), MapSettings(), configs)
        subject.runIfNeeded()

        target.putString("config.1.conf_cassette", "")
        target.remove(StorageKeys.IMPORT_STATUS)
        subject.runIfNeeded()

        assertEquals("", target.getStringOrNull("config.1.conf_cassette"))
    }

    @Test
    fun aReRunFillsOnlyTheGapsItFinds() {
        // The state a rollback then roll-forward produces: partly populated.
        val target = MapSettings()
        val configs = mapOf(
            1 to MapSettings("conf_name" to "Legacy name", "conf_model" to "1"),
        )
        target.putString("config.1.conf_name", "Already set")

        val result = importer(target, global("1"), MapSettings(), configs).runIfNeeded()

        assertIs<ImportResult.Imported>(result)
        assertEquals("Already set", target.getStringOrNull("config.1.conf_name"))
        assertEquals("1", target.getStringOrNull("config.1.conf_model"))
    }

    @Test
    fun losingTheStatusDoesNotRollTheUserBack() {
        // The status lives in the store it guards, and the legacy files are kept
        // forever, so an Auto Backup restore can lose one and not the other.
        val target = MapSettings()
        val configs = mapOf(1 to MapSettings("conf_name" to "Original"))
        val subject = importer(target, global("1"), MapSettings(), configs)
        subject.runIfNeeded()

        target.putString("config.1.conf_name", "Renamed")
        target.remove(StorageKeys.IMPORT_STATUS)
        assertEquals(ImportStatus.NEVER_RUN, subject.status)

        subject.runIfNeeded()

        assertEquals("Renamed", target.getStringOrNull("config.1.conf_name"))
    }

    // ---- Whether Settings offers the manual entry point ---------------------

    @Test
    fun legacyDataIsDetectedFromAnyOfTheOldStores() {
        assertTrue(importer(MapSettings(), global("1"), MapSettings(), emptyMap()).hasLegacyData)
        assertTrue(
            importer(MapSettings(), MapSettings("NEXT_ID" to 3), MapSettings(), emptyMap())
                .hasLegacyData
        )
        assertTrue(
            importer(
                MapSettings(),
                MapSettings(),
                MapSettings("conf_rom_model1" to "/data/model1.rom"),
                emptyMap(),
            ).hasLegacyData
        )
        assertFalse(
            importer(MapSettings(), MapSettings(), MapSettings(), emptyMap()).hasLegacyData
        )
    }

    // ---- Helpers ------------------------------------------------------------

    private fun global(ids: String) = MapSettings("CONFIGURATIONS" to ids)

    private fun config(id: Int, name: String) = mapOf(id to MapSettings("conf_name" to name))

    private fun config1() = MapSettings("conf_name" to "First")

    private fun importer(
        target: Settings,
        legacyGlobal: Settings,
        legacyAppSettings: Settings,
        configurations: Map<Int, Settings>,
    ) = LegacyImport(target, legacyGlobal, legacyAppSettings) { id ->
        configurations[id] ?: MapSettings()
    }
}
