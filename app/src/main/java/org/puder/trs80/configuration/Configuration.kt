/*
 * Copyright 2017, Sascha Haeberling
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

package org.puder.trs80.configuration

import android.util.SparseArray

/**
 * Interface for a configuration.
 */
interface Configuration {
    /** The ID that identifies this configuration and its storage. */
    val id: Int

    /** The user-visible name of this configuration, or null if one was never set. */
    val name: String?

    fun setName(name: String?)

    /** The emulated hardware model, one of the `MODEL*` constants of `Hardware`. */
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

    /** The image paths of all disk drives, keyed by the zero-based drive index. */
    var diskPaths: SparseArray<String?>

    /** The position the cassette is wound to. */
    var cassettePosition: Float

    /** The keyboard layout to use in portrait orientation, or null if none is stored. */
    val keyboardLayoutPortrait: KeyboardLayout?

    fun setKeyboardLayoutPortrait(layout: KeyboardLayout?)

    /** The keyboard layout to use in landscape orientation, or null if none is stored. */
    val keyboardLayoutLandscape: KeyboardLayout?

    fun setKeyboardLayoutLandscape(layout: KeyboardLayout?)

    /** The character color as an Android color value. */
    val characterColorAsRGB: Int

    /** The character color as the index that is persisted. */
    var characterColor: Int

    /** The screen background color as an Android color value. */
    var screenColorAsRGB: Int

    /** Whether the emulator's sound output is muted. */
    var isSoundMuted: Boolean

    /** Removes all persisted data of this configuration. */
    fun delete()

    /**
     * @return An in-memory copy of this configuration.
     */
    fun createBackup(): Configuration
}
