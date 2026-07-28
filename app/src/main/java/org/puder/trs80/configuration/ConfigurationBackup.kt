/*
 * Copyright 2012-2013, Arno Puder
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

private const val CONFIG_BACKUP_IS_IMMUTABLE = "ConfigurationBackup is immutable"
private const val NUM_DISKS = 4

/**
 * An immutable in-memory copy of a configuration. Every mutating member throws an
 * [IllegalAccessError].
 */
internal class ConfigurationBackup private constructor(
    override val id: Int,
    override val name: String?,
    model: Int,
    override val cassettePath: String?,
    diskPaths: SparseArray<String?>,
    cassettePosition: Float,
    override val keyboardLayoutPortrait: KeyboardLayout?,
    override val keyboardLayoutLandscape: KeyboardLayout?,
    characterColor: Int,
    override val characterColorAsRGB: Int,
    screenColorAsRGB: Int,
    isSoundMuted: Boolean
) : Configuration {

    companion object {
        /** @return An in-memory snapshot of [orig]. */
        fun from(orig: Configuration): Configuration {
            val diskPaths = SparseArray<String?>(NUM_DISKS).apply {
                for (disk in 0 until NUM_DISKS) {
                    put(disk, orig.getDiskPath(disk))
                }
            }
            return ConfigurationBackup(
                orig.id,
                orig.name,
                orig.model,
                orig.cassettePath,
                diskPaths,
                orig.cassettePosition,
                orig.keyboardLayoutPortrait,
                orig.keyboardLayoutLandscape,
                orig.characterColor,
                orig.characterColorAsRGB,
                orig.screenColorAsRGB,
                orig.isSoundMuted
            )
        }
    }

    override var model: Int = model
        set(value) = immutable()

    override var diskPaths: SparseArray<String?> = diskPaths
        set(value) = immutable()

    override var cassettePosition: Float = cassettePosition
        set(value) = immutable()

    override var characterColor: Int = characterColor
        set(value) = immutable()

    override var screenColorAsRGB: Int = screenColorAsRGB
        set(value) = immutable()

    override var isSoundMuted: Boolean = isSoundMuted
        set(value) = immutable()

    override fun getDiskPath(disk: Int): String? = diskPaths[disk]

    override fun setName(name: String?) = immutable()

    override fun setCassettePath(path: String?) = immutable()

    override fun setDiskPath(disk: Int, path: String?) = immutable()

    override fun setKeyboardLayoutPortrait(layout: KeyboardLayout?) = immutable()

    override fun setKeyboardLayoutLandscape(layout: KeyboardLayout?) = immutable()

    override fun delete() = immutable()

    override fun createBackup(): Configuration =
        throw IllegalAccessError("ConfigurationBackup is already a backup.")

    private fun immutable(): Nothing = throw IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE)
}
