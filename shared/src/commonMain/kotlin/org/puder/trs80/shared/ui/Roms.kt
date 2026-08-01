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

package org.puder.trs80.shared.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.puder.trs80.shared.localstore.RomManager
import org.puder.trs80.shared.localstore.RomSetup

/** One model's ROM image, as the settings screen states it. */
data class RomStatus(
    val model: Int,
    val label: String,
    /** The file in the app's own storage, or null when there is none. */
    val filename: String?,
) {
    val present: Boolean get() = filename != null
}

/**
 * The ROM images, held for as long as the app is running.
 *
 * The emulator cannot start a machine without one, and they cannot be shipped
 * with the app, so they are fetched on first run. Held above the screens because
 * two of them care: the app downloads at start-up, and settings shows what came
 * of it and offers another go.
 */
class Roms(
    private val roms: RomManager,
    private val setup: RomSetup = RomSetup(roms),
) {
    var statuses: List<RomStatus> by mutableStateOf(read())
        private set

    /** The models being fetched right now, so each row can say so for itself. */
    var downloading: Set<Int> by mutableStateOf(emptySet())
        private set

    /** Whether anything at all is being fetched. */
    val busy: Boolean get() = downloading.isNotEmpty()

    /**
     * Whether a fetch has been tried at all.
     *
     * Everything is missing before the first attempt, which is not the same as
     * having failed — without this the app would open by announcing a failure
     * that had not happened yet.
     */
    var attempted: Boolean by mutableStateOf(false)
        private set

    /** The models still wanting a ROM. */
    val missing: List<RomStatus> get() = statuses.filterNot { it.present }

    /** Fetches whatever is missing; does nothing when nothing is. */
    suspend fun downloadMissing() {
        val wanted = missing.map { it.model }.filterNot(downloading::contains)
        if (wanted.isEmpty()) {
            return
        }
        downloading = downloading + wanted
        try {
            setup.downloadMissing()
        } finally {
            statuses = read()
            attempted = true
            downloading = downloading - wanted.toSet()
        }
    }

    /** Fetches [model]'s ROM again, replacing whatever is there. */
    suspend fun redownload(model: Int) {
        if (model in downloading) {
            return
        }
        downloading = downloading + model
        try {
            setup.download(model)
        } finally {
            statuses = read()
            attempted = true
            downloading = downloading - model
        }
    }

    /**
     * Takes a file the user chose as the ROM for [model].
     *
     * The bytes are copied into the app's own storage rather than remembered by
     * their original path: on iOS what the picker hands back is a temporary
     * copy that will be swept up, and a configuration pointing at it would
     * quietly stop working.
     */
    fun useFile(model: Int, filename: String, content: ByteArray): Boolean =
        roms.addRom(model, filename, content).also { statuses = read() }

    private fun read(): List<RomStatus> = setup.known.map { item ->
        RomStatus(
            model = item.model,
            label = modelLabel(item.model),
            filename = roms.romPath(item.model)
                ?.takeIf { roms.hasRom(item.model) }
                ?.substringAfterLast('/'),
        )
    }
}
