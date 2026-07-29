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

package org.puder.trs80

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.puder.trs80.browser.FileBrowserActivity

/** Intent extra the file browser returns the picked file in. */
private const val EXTRA_PATH = "PATH"

/**
 * The ROM settings, in the order they appear on screen. Model 4 and 4P are only declared by
 * some of the settings layouts, so entries without a preference are skipped.
 */
private val ROM_KEYS = listOf(
    SettingsActivity.CONF_ROM_MODEL1,
    SettingsActivity.CONF_ROM_MODEL3,
    SettingsActivity.CONF_ROM_MODEL4,
    SettingsActivity.CONF_ROM_MODEL4P
)

/**
 * Lets the user pick a ROM image per emulated model. Each ROM preference opens a
 * [FileBrowserActivity] and shows the picked path as its summary.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var romPreferences: List<RomPreference>

    private val changeListener = Preference.OnPreferenceChangeListener { pref, newValue ->
        pref.summary = newValue.toString()
        true
    }

    // startActivityForResult; the file browser it drives is replaced by a document picker
    // rather than migrated to the Activity Result API.
    @Suppress("DEPRECATION")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // The store has to be named before the hierarchy is inflated, or the preferences bind
        // to the default shared preferences instead of the app-wide settings file.
        val manager = preferenceManager
        manager.sharedPreferencesName = SettingsActivity.SHARED_PREF_NAME
        sharedPrefs = requireNotNull(manager.sharedPreferences) {
            "Settings preference manager has no shared preferences."
        }
        setPreferencesFromResource(R.xml.settings, rootKey)

        val clickListener = Preference.OnPreferenceClickListener { pref ->
            val intent = Intent(requireActivity(), FileBrowserActivity::class.java)
            startActivityForResult(intent, pref.key.hashCode())
            true
        }
        romPreferences = ROM_KEYS.mapNotNull { key ->
            val pref: Preference = findPreference(key) ?: return@mapNotNull null
            pref.onPreferenceChangeListener = changeListener
            pref.onPreferenceClickListener = clickListener
            RomPreference(key, pref)
        }
        updateSummaries()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.fitsSystemWindows = true
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION") // See onCreatePreferences.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        // A null path means the user ejected the image, which clears the setting.
        val path = data?.getStringExtra(EXTRA_PATH)
        romPreferences.firstOrNull { it.key.hashCode() == requestCode }?.let {
            sharedPrefs.edit().putString(it.key, path).apply()
        }
        updateSummaries()
    }

    private fun updateSummaries() = romPreferences.forEach {
        it.preference.summary = sharedPrefs.getString(it.key, null) ?: it.defaultSummary
    }

    /** A ROM preference plus the summary to fall back to while no ROM is set. */
    private class RomPreference(val key: String, val preference: Preference) {
        val defaultSummary: CharSequence? = preference.summary
    }
}
