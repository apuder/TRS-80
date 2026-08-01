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

import org.puder.trs80.shared.KeyboardLayout
import org.puder.trs80.shared.storage.StorageKeys

/**
 * A configuration being edited.
 *
 * The editor cannot work on a [Configuration] directly: its setters write
 * straight through to storage, so every keystroke would be a save and there
 * would be nothing for the header's Save to do. [ConfigurationBackup] is the
 * other in-memory shape and it is deliberately immutable, so this is the third:
 * a plain value the screen replaces wholesale on each edit, and which
 * [ConfigurationManager.persistDraft] writes back in one go.
 */
data class ConfigurationDraft(
    val id: Int,
    val name: String,
    val model: Int,
    /** Always [StorageKeys.DRIVE_COUNT] long; a null is an empty drive. */
    val diskPaths: List<String?>,
    val cassettePath: String?,
    val keyboardPortrait: KeyboardLayout?,
    val keyboardLandscape: KeyboardLayout?,
    val characterColor: Int,
    val soundMuted: Boolean,
    /**
     * Whether this was already the user's own copy when the editor opened.
     *
     * Editing anything makes a configuration custom, so this has to be captured
     * up front — it is what decides whether the fork banner belongs on screen.
     */
    val wasCustom: Boolean,
) {
    /** The drives holding a disk, in order. */
    val disks: List<String> get() = diskPaths.filterNotNull()

    /**
     * Ejects the disk in [drive], closing the gap behind it.
     *
     * Drives are compacted rather than left with a hole: the list is presented
     * as an ordered stack with one empty drive at the end, and a gap in the
     * middle would have no way to show itself in that shape.
     */
    fun withDiskEjected(drive: Int): ConfigurationDraft {
        if (drive !in diskPaths.indices || diskPaths[drive] == null) {
            return this
        }
        val remaining = disks.toMutableList().also { it.removeAt(drive) }
        return copy(diskPaths = remaining.padToDrives())
    }

    /** Puts [path] in the first free drive, or returns this if they are all full. */
    fun withDiskAdded(path: String): ConfigurationDraft {
        val remaining = disks
        if (remaining.size >= StorageKeys.DRIVE_COUNT) {
            return this
        }
        return copy(diskPaths = (remaining + path).padToDrives())
    }

    /** Moves the disk in drive [from] to drive [to], shifting the rest along. */
    fun withDiskMoved(from: Int, to: Int): ConfigurationDraft {
        val remaining = disks.toMutableList()
        if (from !in remaining.indices || to !in remaining.indices || from == to) {
            return this
        }
        remaining.add(to, remaining.removeAt(from))
        return copy(diskPaths = remaining.padToDrives())
    }

    /**
     * Whether editing has forked this from the catalogue's copy.
     *
     * True only for something that arrived from the catalogue and has since been
     * changed, which is exactly when the banner has something to say.
     */
    fun isForkedFrom(original: ConfigurationDraft): Boolean =
        !wasCustom && this != original

    private fun List<String>.padToDrives(): List<String?> =
        List(StorageKeys.DRIVE_COUNT) { getOrNull(it) }
}

/** Reads a configuration into an editable draft. */
fun Configuration.toDraft(): ConfigurationDraft = ConfigurationDraft(
    id = id,
    name = name.orEmpty(),
    model = model,
    diskPaths = List(StorageKeys.DRIVE_COUNT) { getDiskPath(it) },
    cassettePath = cassettePath,
    keyboardPortrait = keyboardLayoutPortrait,
    keyboardLandscape = keyboardLayoutLandscape,
    characterColor = characterColor,
    soundMuted = isSoundMuted,
    wasCustom = isCustom,
)
