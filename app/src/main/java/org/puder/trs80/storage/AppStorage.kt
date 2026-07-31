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

package org.puder.trs80.storage

import android.content.Context
import android.util.Log
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.puder.trs80.SettingsActivity
import org.puder.trs80.shared.storage.ImportResult
import org.puder.trs80.shared.storage.initAppSettings
import org.puder.trs80.shared.storage.LegacyImport

private const val TAG = "AppStorage"

/** The per-configuration preferences files the app used to keep. */
private const val LEGACY_CONFIG_PREFIX = "CONFIG_"

/**
 * The Android side of the shared storage: builds the one store the domain reads
 * and runs the legacy import.
 *
 * This is deliberately a small object handed the platform's `Context` once,
 * rather than one more singleton reaching for a global application context. It
 * is the seam an iOS equivalent will sit behind: the same [LegacyImport] and
 * the same key names, over `NSUserDefaults` instead of SharedPreferences.
 */
class AppStorage private constructor(private val context: Context) {

    /**
     * The store the domain classes read and write.
     *
     * Opened through the shared module, so its name is defined once and iOS's
     * NSUserDefaults equivalent sits behind the same call.
     */
    val settings: Settings = initAppSettings(context)

    private val legacyImport: LegacyImport
        get() = LegacyImport(
            target = settings,
            // Where ConfigurationManager kept the list of configurations.
            legacyGlobal = SharedPreferencesSettings(
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            ),
            // Where the ROM paths and first-run flags live.
            legacyAppSettings = named(SettingsActivity.SHARED_PREF_NAME),
            legacyConfiguration = { id -> named(LEGACY_CONFIG_PREFIX + id) },
        )

    /**
     * Runs the import if it has not already succeeded.
     *
     * Synchronous and on the main thread by design: this has to finish before
     * anything reads a configuration, and it is a few hundred scalars out of
     * files the platform has already paged in.
     *
     * @return the outcome, so the caller can tell the user if it failed.
     */
    fun importLegacyDataIfNeeded(): ImportResult =
        legacyImport.runIfNeeded().also { logOutcome(it) }

    private fun logOutcome(result: ImportResult) = when (result) {
        is ImportResult.Imported ->
            Log.i(TAG, "Imported ${result.values} values from ${result.configurations} configs.")

        is ImportResult.Failed -> Log.e(TAG, "Legacy import failed: ${result.message}")
        ImportResult.NothingToImport -> Log.i(TAG, "No legacy data to import.")
        ImportResult.AlreadyDone -> Unit
    }

    private fun named(name: String): Settings =
        SharedPreferencesSettings(context.getSharedPreferences(name, Context.MODE_PRIVATE))

    companion object {
        private var instance: AppStorage? = null

        /** Creates the singleton. Called once, from `TRS80Application.onCreate`. */
        fun init(context: Context): AppStorage =
            instance ?: AppStorage(context.applicationContext).also { instance = it }

        fun get(): AppStorage = checkNotNull(instance) { "Must call AppStorage.init() first." }
    }
}
