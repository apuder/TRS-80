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
    /** How many drives have a disk in them. */
    val diskCount: Int get() = diskPaths.count { it != null }

    /**
     * Empties [drive], leaving every other drive where it is.
     *
     * Drives are not a list that closes up: the emulator takes one path per
     * drive and honors the gaps, so a machine with a disk in drive 2 and
     * nothing in drive 1 is a real machine, and moving that disk down to drive
     * 1 because drive 1 happens to be free would change what it boots.
     */
    fun withDiskEjected(drive: Int): ConfigurationDraft {
        if (drive !in diskPaths.indices) {
            return this
        }
        return copy(diskPaths = diskPaths.mapIndexed { i, p -> if (i == drive) null else p })
    }

    /** Puts [path] in [drive], replacing whatever was in that drive. */
    fun withDiskIn(drive: Int, path: String): ConfigurationDraft {
        if (drive !in diskPaths.indices) {
            return this
        }
        return copy(diskPaths = diskPaths.mapIndexed { i, p -> if (i == drive) path else p })
    }

    /**
     * Swaps what is in [from] with what is in [to].
     *
     * A swap rather than an insertion, because the drives are fixed positions:
     * dragging a disk onto drive 0 is asking for that disk to be the one the
     * machine boots from, not asking for a reordering.
     */
    fun withDiskMoved(from: Int, to: Int): ConfigurationDraft {
        if (from !in diskPaths.indices || to !in diskPaths.indices || from == to) {
            return this
        }
        return copy(
            diskPaths = diskPaths.mapIndexed { i, p ->
                when (i) {
                    from -> diskPaths[to]
                    to -> diskPaths[from]
                    else -> p
                }
            }
        )
    }

    /**
     * Whether editing has forked this from the catalog's copy.
     *
     * True only for something that arrived from the catalog and has since been
     * changed, which is exactly when the banner has something to say.
     */
    fun isForkedFrom(original: ConfigurationDraft): Boolean =
        !wasCustom && this != original
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
