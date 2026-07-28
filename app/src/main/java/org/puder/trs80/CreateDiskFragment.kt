/*
 * Copyright 2012-2013, Arno Puder
 * Copyright 2017, Robert Corrigan
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

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.view.View
import com.google.android.material.snackbar.Snackbar
import org.puder.trs80.CreateDiskActivity.Companion.MKDISK_DENSITY
import org.puder.trs80.CreateDiskActivity.Companion.MKDISK_FORMAT
import org.puder.trs80.CreateDiskActivity.Companion.MKDISK_IGNORE_DENSITY
import org.puder.trs80.CreateDiskActivity.Companion.MKDISK_NAME
import org.puder.trs80.CreateDiskActivity.Companion.MKDISK_SIDED
import org.puder.trs80.CreateDiskActivity.Companion.MKDISK_SIZE

/**
 * Collects the parameters of the blank disk image to create. [CreateDiskActivity] reads them
 * back out of the default shared preferences when the user confirms.
 */
// android.preference; the androidx migration is a separate change.
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class CreateDiskFragment : PreferenceFragment() {

    private val handler = Handler(Looper.getMainLooper())

    private val changeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
        val rejected = preference.key.equals(MKDISK_NAME, ignoreCase = true) &&
                !(activity as CreateDiskActivity).validateDiskImageName(newValue.toString())
        if (rejected) {
            Snackbar.make(
                activity.findViewById<View>(android.R.id.content),
                getString(R.string.mkdisk_bad_path) + newValue,
                Snackbar.LENGTH_SHORT
            ).show()
            false
        } else {
            // The preferences have not been written yet at this point, so refresh the summaries
            // via the handler, once the pending change has been applied.
            handler.post { updateSummaries() }
            true
        }
    }

    private lateinit var sharedPrefs: SharedPreferences

    private lateinit var name: Preference
    private lateinit var format: Preference
    private lateinit var sided: Preference
    private lateinit var density: Preference
    private lateinit var size: Preference
    private lateinit var ignoreDensity: Preference

    private lateinit var defaultNameSummary: String
    private lateinit var defaultFormatSummary: String
    private lateinit var defaultSidedSummary: String
    private lateinit var defaultDensitySummary: String
    private lateinit var defaultSizeSummary: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        addPreferencesFromResource(R.xml.mkdisk)

        name = listeningPreference(MKDISK_NAME)
        defaultNameSummary = name.summary.toString()

        format = listeningPreference(MKDISK_FORMAT)
        defaultFormatSummary = format.summary.toString()

        sided = listeningPreference(MKDISK_SIDED)
        defaultSidedSummary = sided.summary.toString()

        density = listeningPreference(MKDISK_DENSITY)
        defaultDensitySummary = density.summary.toString()

        size = listeningPreference(MKDISK_SIZE)
        defaultSizeSummary = size.summary.toString()

        ignoreDensity = listeningPreference(MKDISK_IGNORE_DENSITY)

        updateSummaries()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.fitsSystemWindows = true
    }

    private fun listeningPreference(key: String): Preference =
        findPreference(key).also { it.onPreferenceChangeListener = changeListener }

    private fun updateSummaries() {
        name.summary =
            sharedPrefs.getString(MKDISK_NAME, defaultNameSummary)?.ifEmpty { defaultNameSummary }

        val formatValue =
            sharedPrefs.getString(MKDISK_FORMAT, defaultFormatSummary) ?: defaultFormatSummary
        format.summary = formatValue

        // The remaining parameters only apply to the DMK format.
        val dmkSelected = formatValue.equals(getString(R.string.mkdisk_dmk), ignoreCase = true)
        listOf(sided, density, size, ignoreDensity).forEach { it.isEnabled = dmkSelected }

        sided.summary = sharedPrefs.getString(MKDISK_SIDED, defaultSidedSummary)
        density.summary = sharedPrefs.getString(MKDISK_DENSITY, defaultDensitySummary)
        size.summary = sharedPrefs.getString(MKDISK_SIZE, defaultSizeSummary)
    }
}
