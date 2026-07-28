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

import android.content.Context
import android.graphics.Color
import android.util.SparseArray
import org.puder.trs80.Hardware

private const val NUM_DISKS = 4

/**
 * Represents a single configuration, backed by [ConfigurationPersistence]. Every change is
 * written through to the persistence immediately.
 */
internal class ConfigurationImpl private constructor(
    override val id: Int,
    private val persistence: ConfigurationPersistence
) : Configuration {

    companion object {
        /** @return The configuration stored under the given [id]. */
        fun fromId(id: Int, context: Context): Configuration =
            ConfigurationImpl(id, ConfigurationPersistence.forId(id, context))
    }

    override val name: String?
        get() = persistence.name

    override fun setName(name: String?) = persistence.setName(name)

    // TODO: This should be an enum.
    override var model: Int
        get() = when (persistence.model?.toInt()) {
            1 -> Hardware.MODEL1
            3 -> Hardware.MODEL3
            4 -> Hardware.MODEL4
            5 -> Hardware.MODEL4P
            else -> Hardware.MODEL_NONE
        }
        set(value) = persistence.setModel(
            when (value) {
                1, 3, 4, 5 -> value.toString()
                else -> null
            }
        )

    override val cassettePath: String?
        get() = persistence.casettePath

    override fun setCassettePath(path: String?) = persistence.setCasettePath(path)

    override fun getDiskPath(disk: Int): String? = persistence.getDiskPath(disk)

    override fun setDiskPath(disk: Int, path: String?) = persistence.setDiskPath(disk, path)

    override var diskPaths: SparseArray<String?>
        get() = SparseArray<String?>(NUM_DISKS).apply {
            for (disk in 0 until NUM_DISKS) {
                put(disk, persistence.getDiskPath(disk))
            }
        }
        set(paths) {
            for (disk in 0 until paths.size()) {
                persistence.setDiskPath(disk, paths[disk])
            }
        }

    override var cassettePosition: Float
        get() = persistence.getCassettePosition(0f)
        set(pos) = persistence.setCassettePosition(pos)

    override val keyboardLayoutPortrait: KeyboardLayout?
        get() = KeyboardLayout.fromId(persistence.keyboardLayoutPortrait)

    override fun setKeyboardLayoutPortrait(layout: KeyboardLayout?) {
        layout?.let { persistence.setKeyboardLayoutPortrait(it.id) }
    }

    override val keyboardLayoutLandscape: KeyboardLayout?
        get() = KeyboardLayout.fromId(persistence.keyboardLayoutLandscape)

    override fun setKeyboardLayoutLandscape(layout: KeyboardLayout?) {
        layout?.let { persistence.setKeyboardLayoutLandscape(it.id) }
    }

    override val characterColorAsRGB: Int
        get() = when (characterColor) {
            0 -> Color.GREEN
            else -> Color.WHITE
        }

    override var characterColor: Int
        get() = persistence.getCharacterColor(0)
        set(color) = persistence.setCharacterColor(color)

    override var screenColorAsRGB: Int
        get() = Color.DKGRAY
        set(color) {
            // TODO: Seems like we hard-coded a color for now. Make this persist if we care.
        }

    override var isSoundMuted: Boolean
        get() = persistence.isSoundMuted
        set(muted) {
            persistence.isSoundMuted = muted
        }

    override fun delete() = persistence.clear()

    override fun createBackup(): Configuration = ConfigurationBackup.from(this)
}
