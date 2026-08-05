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

import com.russhwolf.settings.Settings

/** Legacy key names, as the Android app wrote them before there was one store. */
private const val LEGACY_NEXT_ID = "NEXT_ID"
private const val LEGACY_CONFIGURATION_IDS = "CONFIGURATIONS"

/**
 * Values that were stored as strings even though they are numbers, because
 * `ListPreference` and `EditTextPreference` only write strings.
 *
 * The encoding is **preserved**, not normalized. An earlier draft of the plan
 * had the import fix it, which was wrong: the Android preference screens still
 * exist and are bridged onto this store, so the preference framework goes on
 * reading and writing these as strings. Normalizing here would break them. It
 * can be fixed once Compose replaces those screens.
 */
private val STRING_VALUED_CONFIG_KEYS = listOf(
    StorageKeys.CONFIG_NAME,
    StorageKeys.CONFIG_MODEL,
    StorageKeys.CONFIG_CASSETTE,
    StorageKeys.CONFIG_CHARACTER_COLOR,
    StorageKeys.CONFIG_KEYBOARD_PORTRAIT,
    StorageKeys.CONFIG_KEYBOARD_LANDSCAPE,
) + (0 until StorageKeys.DRIVE_COUNT).map(StorageKeys::diskKey)

/** How far the import has got, as recorded in the store. */
enum class ImportStatus {
    /** It has never been attempted. */
    NEVER_RUN,

    /** It completed. The store is authoritative and the legacy data is a backup. */
    SUCCEEDED,

    /** It was attempted and failed. See [LegacyImport.lastError]. */
    FAILED,
}

/** What one attempt did. */
sealed interface ImportResult {

    /** The import had already succeeded, so nothing was done. */
    data object AlreadyDone : ImportResult

    /** There was no legacy data — the normal case on a new install. */
    data object NothingToImport : ImportResult

    /** Values were imported. */
    data class Imported(val configurations: Int, val values: Int) : ImportResult

    /**
     * The import failed and the store was left untouched.
     *
     * @property message what to tell the user.
     */
    data class Failed(val message: String) : ImportResult
}

/**
 * Brings the Android app's original three-file preference layout into the single
 * namespaced store.
 *
 * Runs automatically at startup, via [runIfNeeded]. A failed attempt is recorded
 * rather than retried in a loop, and is tried again the next time the app starts,
 * because [ImportStatus.FAILED] is not [ImportStatus.SUCCEEDED].
 *
 * **Nothing is deleted.** The legacy values stay where they are, so releasing
 * this and rolling it back does not destroy a user's configurations. That costs
 * a little duplicated data, which for a few hundred scalars is not worth
 * optimizing.
 *
 * **The import is prepared in full before anything is written.** Every legacy
 * value is read into memory first, and the store is only touched if that whole
 * read succeeded, so a failure while reading leaves the store exactly as it was.
 * This is *not* a transaction — neither SharedPreferences through this API nor
 * `NSUserDefaults` offers one — so a failure during the short, local,
 * already-validated write loop can still leave a partial result. The status is
 * not recorded in that case, so the next attempt finishes the job.
 *
 * **Writes never overwrite**, and [status] is not what guarantees that. The
 * status lives in the very store it guards, while the legacy files are kept
 * forever, so anything that loses the one but not the others would re-run this —
 * Android's Auto Backup restoring an older preferences set being the realistic
 * route. Without the guard, such a restore would silently roll a user's settings
 * back to whatever they were before they first upgraded. Presence is tested by
 * *key*, because a stored empty string or `false` is a real answer the user gave
 * and must not be replaced just because it looks unset.
 */
class LegacyImport(
    private val target: Settings,
    private val legacyGlobal: Settings,
    private val legacyAppSettings: Settings,
    private val legacyConfiguration: (Int) -> Settings,
) {

    /** How far the import has got. */
    val status: ImportStatus
        get() = when (target.getStringOrNull(StorageKeys.IMPORT_STATUS)) {
            ImportStatus.SUCCEEDED.name -> ImportStatus.SUCCEEDED
            ImportStatus.FAILED.name -> ImportStatus.FAILED
            else -> ImportStatus.NEVER_RUN
        }

    /** Why the last attempt failed, or null if none did. */
    val lastError: String?
        get() = target.getStringOrNull(StorageKeys.IMPORT_ERROR)

    /** Whether the old layout is present at all. */
    val hasLegacyData: Boolean
        get() = legacyGlobal.hasKey(LEGACY_CONFIGURATION_IDS) ||
                legacyGlobal.hasKey(LEGACY_NEXT_ID) ||
                StorageKeys.romModels.any {
                    legacyAppSettings.hasKey(
                        StorageKeys.romKey(it).removePrefix(StorageKeys.APP_PREFIX)
                    )
                }

    /**
     * Runs the import unless it has already succeeded.
     *
     * Safe to call repeatedly: existing values are never replaced, so the worst
     * case is that it finds nothing left to fill in.
     */
    fun runIfNeeded(): ImportResult =
        if (status == ImportStatus.SUCCEEDED) ImportResult.AlreadyDone else run()

    private fun run(): ImportResult {
        if (!hasLegacyData) {
            // Recorded so startup stops retrying on every launch.
            recordSuccess()
            return ImportResult.NothingToImport
        }

        val prepared = try {
            prepare()
        } catch (e: Exception) {
            val message = e.message ?: e.toString()
            target.putString(StorageKeys.IMPORT_STATUS, ImportStatus.FAILED.name)
            target.putString(StorageKeys.IMPORT_ERROR, message)
            return ImportResult.Failed(message)
        }

        var written = 0
        for ((key, value) in prepared.values) {
            if (target.hasKey(key)) continue
            when (value) {
                is StoredValue.Text -> target.putString(key, value.value)
                is StoredValue.Number -> target.putInt(key, value.value)
                is StoredValue.Decimal -> target.putFloat(key, value.value)
                is StoredValue.Flag -> target.putBoolean(key, value.value)
            }
            written++
        }
        // Somebody who had the old app, and machines in it. That is the only
        // audience for the panel about the new look: a new install imports
        // nothing and gets here with nothing to say.
        if (prepared.configurations > 0) {
            target.putBoolean(StorageKeys.WHATS_NEW_PENDING, true)
        }
        recordSuccess()
        return ImportResult.Imported(prepared.configurations, written)
    }

    private fun recordSuccess() {
        target.putString(StorageKeys.IMPORT_STATUS, ImportStatus.SUCCEEDED.name)
        target.remove(StorageKeys.IMPORT_ERROR)
    }

    /**
     * Reads everything the import will write, without touching the store.
     *
     * @throws Exception if a legacy store cannot be read. [run] turns that into
     * [ImportResult.Failed] rather than letting it escape.
     */
    private fun prepare(): PreparedImport {
        val values = LinkedHashMap<String, StoredValue>()

        val ids = legacyGlobal.getStringOrNull(LEGACY_CONFIGURATION_IDS).orEmpty()
        if (ids.isNotEmpty()) {
            values[StorageKeys.CONFIGURATION_IDS] = StoredValue.Text(ids)
        }
        legacyGlobal.getIntOrNull(LEGACY_NEXT_ID)?.let {
            values[StorageKeys.NEXT_CONFIGURATION_ID] = StoredValue.Number(it)
        }
        for (model in StorageKeys.romModels) {
            val key = StorageKeys.romKey(model)
            legacyAppSettings.getStringOrNull(key.removePrefix("app."))?.let {
                values[key] = StoredValue.Text(it)
            }
        }

        val configurationIds = ids.split(",").mapNotNull { it.trim().toIntOrNull() }
        for (id in configurationIds) {
            val legacy = legacyConfiguration(id)
            val prefix = StorageKeys.configurationPrefix(id)
            for (leaf in STRING_VALUED_CONFIG_KEYS) {
                legacy.getStringOrNull(leaf)?.let { values[prefix + leaf] = StoredValue.Text(it) }
            }
            legacy.getBooleanOrNull(StorageKeys.CONFIG_MUTE_SOUND)?.let {
                values[prefix + StorageKeys.CONFIG_MUTE_SOUND] = StoredValue.Flag(it)
            }
            legacy.getFloatOrNull(StorageKeys.CONFIG_CASSETTE_POSITION)?.let {
                values[prefix + StorageKeys.CONFIG_CASSETTE_POSITION] = StoredValue.Decimal(it)
            }
        }
        return PreparedImport(configurationIds.size, values)
    }

    private class PreparedImport(
        val configurations: Int,
        val values: Map<String, StoredValue>,
    )

    /** A legacy value held in memory, with the type it has to be written back as. */
    private sealed interface StoredValue {
        data class Text(val value: String) : StoredValue
        data class Number(val value: Int) : StoredValue
        data class Decimal(val value: Float) : StoredValue
        data class Flag(val value: Boolean) : StoredValue
    }
}
