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

/**
 * One emulator configuration: which machine, which media, and how it looks.
 *
 * Moved from `org.puder.trs80.configuration` unchanged, except that the disk
 * paths are a [List] rather than an `android.util.SparseArray` — it always held
 * exactly [DRIVE_COUNT] entries keyed 0..3, so a list says the same thing
 * without the platform type.
 */
interface Configuration {
    /** The ID that identifies this configuration and its storage. */
    val id: Int

    /** The user-visible name of this configuration, or null if one was never set. */
    val name: String?

    fun setName(name: String?)

    /** The emulated hardware model, one of the `MODEL*` constants. */
    var model: Int

    /** The path of the cassette image, or null if none is configured. */
    val cassettePath: String?

    fun setCassettePath(path: String?)

    /**
     * @param disk the zero-based index of the disk drive.
     * @return The path of the image in the given drive, or null if none is configured.
     */
    fun getDiskPath(disk: Int): String?

    fun setDiskPath(disk: Int, path: String?)

    /** The image paths of all disk drives, indexed by the zero-based drive number. */
    var diskPaths: List<String?>

    /** The position the cassette is wound to. */
    var cassettePosition: Float

    /** The keyboard layout to use in portrait orientation, or null if none is stored. */
    val keyboardLayoutPortrait: KeyboardLayout?

    fun setKeyboardLayoutPortrait(layout: KeyboardLayout?)

    /** The keyboard layout to use in landscape orientation, or null if none is stored. */
    val keyboardLayoutLandscape: KeyboardLayout?

    fun setKeyboardLayoutLandscape(layout: KeyboardLayout?)

    /** The character colour, as ARGB. */
    val characterColorAsRGB: Int

    /** The character colour as the index that is persisted. */
    var characterColor: Int

    /** The screen background colour, as ARGB. */
    var screenColorAsRGB: Int

    /** Whether the emulator's sound output is muted. */
    var isSoundMuted: Boolean

    /**
     * When this was last run, in milliseconds since the epoch, or 0 if never.
     *
     * The library orders by this, so it is written every time a machine starts
     * rather than when it stops: what the user is looking for is "the thing I
     * was just doing", and a session that crashed still counts as used.
     */
    val lastUsed: Long

    /** Records that this configuration is being run now. */
    fun markUsed()

    /**
     * Whether the user made or edited this configuration themselves.
     *
     * Something installed from the store and left alone is an original; once it
     * has been edited it is the user's own, and the library marks it.
     */
    var isCustom: Boolean

    /** Removes all persisted data of this configuration. */
    fun delete()

    /** @return An in-memory copy of this configuration. */
    fun createBackup(): Configuration
}
