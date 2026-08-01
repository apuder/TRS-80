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

package org.puder.trs80.shared.ui.theme

import com.russhwolf.settings.Settings
import org.puder.trs80.shared.storage.StorageKeys

/**
 * Which register the app draws in.
 *
 * Stored by name rather than by ordinal: an ordinal is a number in the user's
 * settings that means nothing on its own and changes meaning if the order ever
 * does.
 */
enum class ThemePreference {
    Light,
    System,
    Dark;

    companion object {
        val Default = System

        /** @return the stored preference, or [Default] if there is none or it is unreadable. */
        fun from(settings: Settings): ThemePreference {
            val stored = settings.getStringOrNull(StorageKeys.APP_THEME) ?: return Default
            return entries.firstOrNull { it.name == stored } ?: Default
        }
    }

    /** Stores this as the app's preference. */
    fun storeIn(settings: Settings) = settings.putString(StorageKeys.APP_THEME, name)
}
