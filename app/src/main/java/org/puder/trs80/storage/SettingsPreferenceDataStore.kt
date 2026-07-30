/*
 * Copyright 2025, Arno Puder
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

import androidx.preference.PreferenceDataStore
import com.russhwolf.settings.Settings

/**
 * Points an Android preference screen at the shared store.
 *
 * This is the whole reason the `androidx.preference` migration came first:
 * the framework `android.preference` had no way to redirect where a screen
 * stores its values, which made it look as though the storage rework had to wait
 * for Compose to replace these screens. It does not.
 *
 * Keys are translated by prefixing, which is why the store deliberately keeps
 * the legacy `conf_` leaf names — the preference XML already uses them, so there
 * is no mapping table to keep in step.
 *
 * @param keyPrefix the namespace this screen's values live under, including the
 * trailing separator.
 */
class SettingsPreferenceDataStore(
    private val settings: Settings,
    private val keyPrefix: String,
) : PreferenceDataStore() {

    /**
     * `PreferenceDataStore` has no `remove`, and the preference framework signals
     * a cleared value by storing null. Mapping that to removal is what keeps
     * "no disk" distinguishable from "a disk whose path is the empty string".
     */
    override fun putString(key: String, value: String?) {
        if (value == null) settings.remove(fullKey(key)) else settings.putString(fullKey(key), value)
    }

    override fun getString(key: String, defValue: String?): String? =
        settings.getStringOrNull(fullKey(key)) ?: defValue

    override fun putInt(key: String, value: Int) = settings.putInt(fullKey(key), value)

    override fun getInt(key: String, defValue: Int): Int =
        settings.getIntOrNull(fullKey(key)) ?: defValue

    override fun putLong(key: String, value: Long) = settings.putLong(fullKey(key), value)

    override fun getLong(key: String, defValue: Long): Long =
        settings.getLongOrNull(fullKey(key)) ?: defValue

    override fun putFloat(key: String, value: Float) = settings.putFloat(fullKey(key), value)

    override fun getFloat(key: String, defValue: Float): Float =
        settings.getFloatOrNull(fullKey(key)) ?: defValue

    override fun putBoolean(key: String, value: Boolean) = settings.putBoolean(fullKey(key), value)

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        settings.getBooleanOrNull(fullKey(key)) ?: defValue

    /**
     * Not supported, and nothing needs it: no screen in this app uses
     * `MultiSelectListPreference`, and the shared store has no set type. Failing
     * loudly beats silently dropping a value if one is ever added.
     */
    override fun putStringSet(key: String, values: MutableSet<String>?) =
        throw UnsupportedOperationException("String sets are not stored; key was '$key'.")

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        throw UnsupportedOperationException("String sets are not stored; key was '$key'.")

    private fun fullKey(key: String) = keyPrefix + key
}
