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

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.puder.trs80.shared.MODEL1
import org.puder.trs80.shared.MODEL3
import org.puder.trs80.shared.io.FileManager
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RomSetupTest {

    private val fileSystem = FakeFileSystem()
    private val settings: Settings = MapSettings()
    private val creator = FileManager.Creator("/data/TRS-80".toPath(), fileSystem)
    private val roms = RomManager.create(creator, settings)

    private val asked = mutableListOf<String>()

    private fun setup(answer: (String) -> ByteArray? = { byteArrayOf(1, 2, 3) }) =
        RomSetup(roms) { url, _ -> asked += url; answer(url) }

    @Test
    fun bothModelsWantARomToBeginWith() {
        assertContentEquals(listOf(MODEL1, MODEL3), setup().missing())
    }

    @Test
    fun downloadingFillsThemIn() = runTest {
        val stillMissing = setup().downloadMissing()

        assertTrue(stillMissing.isEmpty())
        assertTrue(roms.hasRom(MODEL1))
        assertTrue(roms.hasRom(MODEL3))
        assertEquals(2, asked.size)
    }

    /** A ROM already in place is not fetched again. */
    @Test
    fun whatIsAlreadyThereIsLeftAlone() = runTest {
        roms.addRom(MODEL1, "model1.rom", byteArrayOf(9))
        asked.clear()

        setup().downloadMissing()

        assertEquals(1, asked.size)
        assertTrue(asked.single().contains("model3"))
    }

    /** One machine that can boot is more use than none. */
    @Test
    fun oneFailureDoesNotStopTheOther() = runTest {
        val stillMissing = setup { url -> if ("model3" in url) null else byteArrayOf(1) }
            .downloadMissing()

        assertContentEquals(listOf(MODEL3), stillMissing)
        assertTrue(roms.hasRom(MODEL1))
    }

    /** The way back from a ROM that arrived corrupt, or one the user supplied. */
    @Test
    fun downloadingOneReplacesWhatIsAlreadyThere() = runTest {
        roms.addRom(MODEL1, "model1.rom", byteArrayOf(9, 9, 9))

        assertTrue(setup().download(MODEL1))

        assertContentEquals(byteArrayOf(1, 2, 3), fileSystem.read("/data/TRS-80/model1.rom".toPath()) { readByteArray() })
    }

    @Test
    fun aModelWithNoKnownDownloadSaysSo() = runTest {
        assertEquals(false, setup().download(model = 99))
    }

    @Test
    fun aFailedDownloadCanBeAskedForAgain() = runTest {
        var offline = true
        val setup = RomSetup(roms) { _, _ -> if (offline) null else byteArrayOf(1) }

        assertEquals(2, setup.downloadMissing().size)

        offline = false
        assertTrue(setup.downloadMissing().isEmpty())
    }
}
