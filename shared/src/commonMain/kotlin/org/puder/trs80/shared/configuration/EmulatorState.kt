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

import okio.IOException
import org.puder.trs80.shared.io.FileManager

private const val FILE_SCREENSHOT = "screenshot.png"
private const val FILE_STATE = "state"
private const val FILE_XRAY_STATE = "state-xray.pb"
private const val FILE_CASSETTE = "cassette.cas"

/**
 * Where an emulator session's saved state lives, so it can be resumed later.
 *
 * This is the storage half only. Three things the old class also did are
 * platform work and stayed on the host side: telling the core to write or read a
 * state file, encoding a screenshot to PNG, and decoding the state the emulator
 * dumps for TRS-Xray. Each of those is reachable from here as bytes and a path —
 * see `EmulatorState.kt` in the app module for the Android half.
 */
class EmulatorState private constructor(private val fileManager: FileManager) {

    companion object {
        /**
         * Creates the state storage for the configuration with the given ID.
         *
         * @throws IOException if the storage directory could not be created.
         */
        @Throws(IOException::class)
        fun forConfigId(configId: Int, fileManagerCreator: FileManager.Creator): EmulatorState {
            val fileManager = fileManagerCreator.createForAppSubDir(configId)
            fileManager.ensureNoMedia()
            return EmulatorState(fileManager)
        }
    }

    /** The file the core saves its state into and loads it back from. */
    val stateFilePath: String
        get() = fileManager.getAbsolutePathForFile(FILE_STATE)

    /** The path of the cassette image that is used when none is configured. */
    val defaultCassettePath: String
        get() = fileManager.getAbsolutePathForFile(FILE_CASSETTE)

    /** The directory in which this state is stored. */
    val basePath: String
        get() = fileManager.getAbsolutePathForFile("")

    /** Whether a saved emulator state exists. */
    fun hasState(): Boolean = fileManager.hasFile(FILE_STATE)

    /** Whether a state dump for TRS-Xray exists. */
    fun hasXrayState(): Boolean = fileManager.hasFile(FILE_XRAY_STATE)

    /** @return the raw TRS-Xray state dump, or null if there is none to read. */
    fun readXrayState(): ByteArray? =
        if (hasXrayState()) fileManager.readFile(FILE_XRAY_STATE) else null

    /** @return the stored screenshot as PNG bytes, or null if there is none. */
    fun readScreenshot(): ByteArray? = fileManager.readFile(FILE_SCREENSHOT)

    /** Stores [png] as this configuration's screenshot. */
    fun writeScreenshot(png: ByteArray): Boolean = fileManager.writeFile(FILE_SCREENSHOT, png)

    /** Deletes the saved state, but keeps everything else, e.g. the disk images. */
    fun deleteSavedState() {
        fileManager.deleteFile(FILE_STATE)
        fileManager.deleteFile(FILE_XRAY_STATE)
        fileManager.deleteFile(FILE_SCREENSHOT)
    }

    /** Deletes all data of this configuration. */
    fun deleteAll() = fileManager.delete()
}
