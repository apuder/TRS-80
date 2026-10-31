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
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.puder.trs80.shared.MODEL_NONE
import org.puder.trs80.shared.io.FileManager
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

class RomManagerTest {

    private val creator = FileManager.Creator("/data/TRS-80".toPath(), FakeFileSystem())
    private val roms = RomManager.create(creator, MapSettings())

    /**
     * A machine saved without a model is started like any other, and asking for
     * its ROM used to throw -- taking the app down on the way into the machine.
     */
    @Test
    fun aMachineWithNoModelHasNoRom() {
        assertNull(roms.romPath(MODEL_NONE))
        assertFalse(roms.hasRom(MODEL_NONE))
    }
}
