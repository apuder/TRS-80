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

package org.puder.trs80.shared.storage

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.puder.trs80.shared.Log

private const val TAG = "LegacyImport"

/**
 * Where the app kept its data before there was one store for it.
 *
 * Three preference files: the list of configurations in the default one, the
 * ROM paths and first-run flags in "Settings", and one file per configuration.
 * The names are what the old app wrote and cannot change -- they are what is on
 * the devices of everyone who has ever installed it.
 */
private const val LEGACY_APP_SETTINGS = "Settings"
private const val LEGACY_CONFIG_PREFIX = "CONFIG_"

/**
 * The default preferences file, by the name the framework gives it.
 *
 * PreferenceManager.getDefaultSharedPreferences would say the same thing, and
 * this is all it does; naming it here is what lets the androidx.preference
 * library go with the UI that used it.
 */
private fun defaultPreferencesName(context: Context) = "${context.packageName}_preferences"

/**
 * Brings the old app's data across, if it has not already been brought.
 *
 * Synchronous and on the main thread by design: this has to finish before
 * anything reads a configuration, and it is a few hundred scalars out of files
 * the platform has already paged in.
 *
 * The outcome is logged rather than shown. It matters on exactly one launch of
 * one upgrade, and there is nothing on screen yet to show it on.
 */
fun importLegacyData(context: Context, target: Settings): ImportResult {
    fun named(name: String): Settings =
        SharedPreferencesSettings(context.getSharedPreferences(name, Context.MODE_PRIVATE))

    val result = LegacyImport(
        target = target,
        legacyGlobal = named(defaultPreferencesName(context)),
        legacyAppSettings = named(LEGACY_APP_SETTINGS),
        legacyConfiguration = { id -> named(LEGACY_CONFIG_PREFIX + id) },
    ).runIfNeeded()

    when (result) {
        is ImportResult.Imported ->
            Log.i(TAG, "Imported ${result.values} values from ${result.configurations} configs.")

        is ImportResult.Failed -> Log.e(TAG, "Legacy import failed: ${result.message}")
        ImportResult.NothingToImport -> Log.i(TAG, "No legacy data to import.")
        ImportResult.AlreadyDone -> Unit
    }
    return result
}
