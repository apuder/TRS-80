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

import com.russhwolf.settings.Settings
import org.puder.trs80.shared.KeyboardLayout
import org.puder.trs80.shared.MODEL1
import org.puder.trs80.shared.MODEL3
import org.puder.trs80.shared.MODEL4
import org.puder.trs80.shared.MODEL4P
import org.puder.trs80.shared.MODEL_NONE
import org.puder.trs80.shared.ScreenColors
import org.puder.trs80.shared.currentTimeMillis
import org.puder.trs80.shared.storage.StorageKeys

/**
 * A configuration backed by [ConfigurationPersistence]. Every change is written
 * through to storage immediately.
 */
internal class ConfigurationImpl private constructor(
    override val id: Int,
    private val persistence: ConfigurationPersistence,
) : Configuration {

    companion object {
        /** @return The configuration stored under the given [id]. */
        fun fromId(id: Int, settings: Settings): Configuration =
            ConfigurationImpl(id, ConfigurationPersistence.forId(id, settings))
    }

    override val name: String?
        get() = persistence.name

    override fun setName(name: String?) = persistence.setName(name)

    // TODO: This should be an enum.
    override var model: Int
        get() = when (persistence.model?.toInt()) {
            1 -> MODEL1
            3 -> MODEL3
            4 -> MODEL4
            5 -> MODEL4P
            else -> MODEL_NONE
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

    override var diskPaths: List<String?>
        get() = List(StorageKeys.DRIVE_COUNT) { persistence.getDiskPath(it) }
        set(paths) {
            paths.forEachIndexed { disk, path -> persistence.setDiskPath(disk, path) }
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
            0 -> ScreenColors.GREEN
            else -> ScreenColors.WHITE
        }

    override var characterColor: Int
        get() = persistence.getCharacterColor(0)
        set(color) = persistence.setCharacterColor(color)

    override var screenColorAsRGB: Int
        get() = ScreenColors.DARK_GRAY
        set(color) {
            // TODO: Seems like we hard-coded a color for now. Make this persist if we care.
        }

    override var isSoundMuted: Boolean
        get() = persistence.isSoundMuted
        set(muted) {
            persistence.isSoundMuted = muted
        }

    override val lastUsed: Long
        get() = persistence.lastUsed

    override fun markUsed() {
        persistence.lastUsed = currentTimeMillis()
    }

    override var isCustom: Boolean
        get() = persistence.isCustom
        set(value) {
            persistence.isCustom = value
        }

    override fun delete() = persistence.clear()

    override fun createBackup(): Configuration = ConfigurationBackup.from(this)
}
