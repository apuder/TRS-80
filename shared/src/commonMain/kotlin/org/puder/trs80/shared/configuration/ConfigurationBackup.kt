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

private const val CONFIG_BACKUP_IS_IMMUTABLE = "ConfigurationBackup is immutable"

/**
 * An immutable in-memory copy of a configuration. Every mutating member throws.
 *
 * The throw used to be an `IllegalAccessError`, which is JVM-only;
 * [UnsupportedOperationException] says the same thing and is what a caller would
 * expect from an immutable view. Nothing caught the old one -- it marks a
 * programming error, not a condition to handle.
 */
internal class ConfigurationBackup private constructor(
    override val id: Int,
    override val name: String?,
    model: Int,
    override val cassettePath: String?,
    diskPaths: List<String?>,
    cassettePosition: Float,
    override val keyboardLayoutPortrait: KeyboardLayout?,
    override val keyboardLayoutLandscape: KeyboardLayout?,
    characterColor: Int,
    override val characterColorAsRGB: Int,
    screenColorAsRGB: Int,
    isSoundMuted: Boolean,
    override val lastUsed: Long,
    isCustom: Boolean,
    override val storeId: String?,
) : Configuration {

    companion object {
        /** @return An in-memory snapshot of [orig]. */
        fun from(orig: Configuration): Configuration = ConfigurationBackup(
            orig.id,
            orig.name,
            orig.model,
            orig.cassettePath,
            List(StorageKeys.DRIVE_COUNT) { orig.getDiskPath(it) },
            orig.cassettePosition,
            orig.keyboardLayoutPortrait,
            orig.keyboardLayoutLandscape,
            orig.characterColor,
            orig.characterColorAsRGB,
            orig.screenColorAsRGB,
            orig.isSoundMuted,
            orig.lastUsed,
            orig.isCustom,
            orig.storeId,
        )
    }

    override var model: Int = model
        set(value) = immutable()

    override var diskPaths: List<String?> = diskPaths
        set(value) = immutable()

    override var cassettePosition: Float = cassettePosition
        set(value) = immutable()

    override var characterColor: Int = characterColor
        set(value) = immutable()

    override var screenColorAsRGB: Int = screenColorAsRGB
        set(value) = immutable()

    override var isSoundMuted: Boolean = isSoundMuted
        set(value) = immutable()

    override var isCustom: Boolean = isCustom
        set(value) = immutable()

    override fun markUsed() = immutable()

    override fun getDiskPath(disk: Int): String? = diskPaths.getOrNull(disk)

    override fun setName(name: String?) = immutable()

    override fun setCassettePath(path: String?) = immutable()

    override fun setDiskPath(disk: Int, path: String?) = immutable()

    override fun setKeyboardLayoutPortrait(layout: KeyboardLayout?) = immutable()

    override fun setKeyboardLayoutLandscape(layout: KeyboardLayout?) = immutable()

    override fun setStoreId(storeId: String?) = immutable()

    override fun delete() = immutable()

    override fun createBackup(): Configuration =
        throw UnsupportedOperationException("ConfigurationBackup is already a backup.")

    private fun immutable(): Nothing =
        throw UnsupportedOperationException(CONFIG_BACKUP_IS_IMMUTABLE)
}
