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

import org.puder.trs80.shared.Log
import org.puder.trs80.shared.io.FileDownloader

private const val TAG = "RomSetup"

/**
 * Fetches the ROM images the emulator cannot start without.
 *
 * The machine will not boot without them and they cannot be shipped with the
 * app, so the app downloads them on first run. Everything here is skippable and
 * repeatable: a ROM already in place is left alone, and a download that failed
 * can simply be asked for again.
 *
 * @param download how to get a file, injectable so this can be tested without a
 * network.
 */
class RomSetup(
    private val roms: RomManager,
    private val download: suspend (url: String, fileInZip: String?) -> ByteArray? =
        FileDownloader()::download,
) {
    /** The ROMs this app knows where to fetch, in the order it offers them. */
    val known: List<InitialDownloads.Download> =
        InitialDownloads.get().filter { it.isROM }

    /** The models that want a ROM and do not have one. */
    fun missing(): List<Int> = known.map { it.model }.filterNot(roms::hasRom)

    /**
     * Downloads whatever is missing.
     *
     * @return the models still without a ROM afterwards — empty when all is
     * well. One failure does not stop the others: a machine with one working
     * ROM is more use than none.
     */
    suspend fun downloadMissing(): List<Int> {
        known.map { it.model }.filterNot(roms::hasRom).forEach { download(it) }
        return missing()
    }

    /**
     * Fetches the ROM for [model] again, whether or not one is already there.
     *
     * The way back from a ROM that arrived corrupt, or from one the user
     * supplied and no longer wants.
     *
     * @return whether the model now has a freshly downloaded ROM.
     */
    suspend fun download(model: Int): Boolean {
        val item = known.firstOrNull { it.model == model }
        if (item == null) {
            Log.e(TAG, "No ROM download is known for model $model.")
            return false
        }
        val data = download(item.url, item.fileInZip)
        if (data == null) {
            Log.e(TAG, "Could not download the ROM for model $model.")
            return false
        }
        if (!roms.addRom(model, item.destinationFilename, data)) {
            Log.e(TAG, "Could not store the ROM for model $model.")
            return false
        }
        return true
    }
}
