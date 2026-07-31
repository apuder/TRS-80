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

package org.puder.trs80.configuration

import androidx.preference.Preference
import org.puder.trs80.shared.storage.StorageKeys

/**
 * Finds the preferences of the configuration editor screen by name.
 *
 * This used to be nested inside `ConfigurationPersistence`, which is now in
 * `commonMain` and cannot mention `androidx.preference.Preference` — a View. The
 * key names it looks up are the shared ones, so the screen and the store still
 * agree by construction. It goes away with the screen, in Phase 3.
 */
class ConfigurationPreferenceFinder(private val provider: PreferenceProvider) {

    fun forModel(): Preference = provider.findPreference(StorageKeys.CONFIG_MODEL)

    fun forName(): Preference = provider.findPreference(StorageKeys.CONFIG_NAME)

    fun forCasette(): Preference = provider.findPreference(StorageKeys.CONFIG_CASSETTE)

    /** @param drive the zero-based drive index. */
    fun forDisk(drive: Int): Preference =
        provider.findPreference(StorageKeys.diskKey(drive))

    fun forCharacterColor(): Preference =
        provider.findPreference(StorageKeys.CONFIG_CHARACTER_COLOR)

    fun forKeyboardPortrait(): Preference =
        provider.findPreference(StorageKeys.CONFIG_KEYBOARD_PORTRAIT)

    fun forKeyboardLandscape(): Preference =
        provider.findPreference(StorageKeys.CONFIG_KEYBOARD_LANDSCAPE)

    /** Classes implementing this interface provide preferences by name. */
    fun interface PreferenceProvider {
        fun findPreference(name: String): Preference
    }
}
