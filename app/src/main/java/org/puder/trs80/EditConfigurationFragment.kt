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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.puder.trs80.browser.FileBrowserActivity
import org.puder.trs80.configuration.ConfigurationManager
import org.puder.trs80.configuration.ConfigurationPersistence
import org.puder.trs80.shared.storage.StorageKeys
import org.puder.trs80.storage.AppStorage
import org.puder.trs80.storage.SettingsPreferenceDataStore
import java.io.File
import java.io.IOException

private const val TAG = "EditConfFragment"

/** Intent extras shared with [EditConfigurationActivity]'s caller and the file browser. */
private const val EXTRA_CONFIG_ID = "CONFIG_ID"
private const val EXTRA_DIR = "DIR"
private const val EXTRA_PATH = "PATH"

/** Key of the cassette preference, which is the one file preference not named `conf_diskN`. */
private const val KEY_CASSETTE = "conf_cassette"

/** Request code used when browsing for a cassette image; disk drive n uses n + 1. */
private const val REQUEST_CASSETTE = 0

/** The number of disk drives a configuration has. */
private const val DISK_COUNT = 4

/**
 * Edits a single configuration. Every change is written straight into the configuration's
 * preferences, so [configurationWasEdited] is what tells the caller whether it has to restore
 * its backup.
 */
class EditConfigurationFragment : PreferenceFragmentCompat() {

    /** Whether the user changed anything since this fragment was created. */
    var configurationWasEdited = false
        private set

    private val handler = Handler(Looper.getMainLooper())

    private val changeListener = Preference.OnPreferenceChangeListener { _, _ ->
        configurationWasEdited = true
        // The preferences have not been written yet at this point, so refresh the summaries
        // via the handler, once the pending change has been applied.
        handler.post { updateSummaries() }
        true
    }

    private lateinit var configPersistence: ConfigurationPersistence
    private lateinit var namePref: Preference
    private lateinit var modelPref: Preference
    private lateinit var characterColorPref: Preference
    private lateinit var keyboardPortraitPref: Preference
    private lateinit var keyboardLandscapePref: Preference
    private lateinit var cassette: MediaPreference
    private lateinit var disks: List<MediaPreference>

    // startActivityForResult; the file browser it drives is replaced by a document picker
    // rather than migrated to the Activity Result API.
    @Suppress("DEPRECATION")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val configId = ActivityHelper.getIntExtra(requireActivity().intent, EXTRA_CONFIG_ID)
        if (configId == null) {
            Log.w(TAG, "Cannot get CONFIG_ID. Finishing activity.")
            requireActivity().finish()
            return
        }
        // Point the screen at this configuration's namespace in the shared store,
        // which has to happen before the hierarchy below is inflated so that the
        // preferences read their initial values from the right place.
        preferenceManager.preferenceDataStore = SettingsPreferenceDataStore(
            AppStorage.get().settings,
            StorageKeys.configurationPrefix(configId),
        )
        configPersistence = ConfigurationPersistence.forId(configId)
        val prefFinder = configPersistence.forPreferenceProvider { name ->
            requireNotNull(findPreference<Preference>(name)) { "No preference named '$name'." }
        }
        setPreferencesFromResource(R.xml.configuration, rootKey)

        namePref = prefFinder.forName()
        namePref.onPreferenceChangeListener = changeListener

        val clickListener = Preference.OnPreferenceClickListener { pref ->
            val key = pref.key
            val requestCode: Int
            val currentPath: String?
            if (key == KEY_CASSETTE) {
                requestCode = REQUEST_CASSETTE
                currentPath = configPersistence.casettePath
            } else {
                requestCode = key.last().digitToInt()
                currentPath = configPersistence.getDiskPath(requestCode - 1)
            }

            // Start out in the directory of the currently selected image, or in the
            // configuration's own directory while nothing is selected.
            val dir = if (currentPath != null) {
                File(currentPath).parent
            } else {
                try {
                    ConfigurationManager.get(requireContext())
                        .getEmulatorState(configId).basePath
                } catch (e: IOException) {
                    Log.w(TAG, "Could not get ConfigurationManager. Finishing activity.", e)
                    requireActivity().finish()
                    null
                }
            }

            val intent = Intent(requireContext(), FileBrowserActivity::class.java)
            if (dir != null) {
                intent.putExtra(EXTRA_DIR, dir)
            }
            startActivityForResult(intent, requestCode)
            true
        }

        cassette = mediaPreference(prefFinder.forCasette(), clickListener)
        disks = (0 until DISK_COUNT)
            .map { mediaPreference(prefFinder.forDisk(it), clickListener) }

        modelPref = prefFinder.forModel()
        modelPref.onPreferenceChangeListener = changeListener

        characterColorPref = prefFinder.forCharacterColor()
        characterColorPref.onPreferenceChangeListener = changeListener

        keyboardPortraitPref = prefFinder.forKeyboardPortrait()
        keyboardPortraitPref.onPreferenceChangeListener = changeListener

        keyboardLandscapePref = prefFinder.forKeyboardLandscape()
        keyboardLandscapePref.onPreferenceChangeListener = changeListener

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
        configurationWasEdited = true
        // A null path means the user ejected the image, which clears the setting.
        val newValue = data?.getStringExtra(EXTRA_PATH)
        when (requestCode) {
            REQUEST_CASSETTE -> configPersistence.setCasettePath(newValue)
            in 1..DISK_COUNT -> configPersistence.setDiskPath(requestCode - 1, newValue)
        }
        updateSummaries()
    }

    private fun mediaPreference(
        pref: Preference,
        clickListener: Preference.OnPreferenceClickListener
    ): MediaPreference {
        pref.onPreferenceChangeListener = changeListener
        pref.onPreferenceClickListener = clickListener
        return MediaPreference(pref, pref.summary.toString())
    }

    private fun updateSummaries() {
        configPersistence.name?.let { namePref.summary = it }
        configPersistence.model?.let { modelPref.summary = "Model " + modelLabel(it) }

        cassette.preference.summary = configPersistence.casettePath ?: cassette.defaultSummary
        disks.forEachIndexed { drive, disk ->
            disk.preference.summary = configPersistence.getDiskPath(drive) ?: disk.defaultSummary
        }

        setCharacterColorSummary(configPersistence.getCharacterColor(0))
        setKeyboardSummary(keyboardPortraitPref, configPersistence.keyboardLayoutPortrait)
        setKeyboardSummary(keyboardLandscapePref, configPersistence.keyboardLayoutLandscape)
    }

    /** Maps a stored model value onto the way the model is spelled in the UI. */
    private fun modelLabel(model: String): String = when (model) {
        "1" -> "I"
        "3" -> "III"
        "5" -> "4P"
        else -> model
    }

    private fun setCharacterColorSummary(color: Int) {
        val summaryId = when (color) {
            0 -> R.string.green
            1 -> R.string.white
            else -> return
        }
        characterColorPref.setSummary(summaryId)
    }

    private fun setKeyboardSummary(pref: Preference, layout: Int) {
        val summaryId = when (layout) {
            0 -> R.string.keyboard_original
            1 -> R.string.keyboard_compact
            2 -> R.string.keyboard_joystick
            // The Java original fell through from the game controller case into the tilt case,
            // so layout 3 has always ended up labelled "tilt" as well. Kept as is; changing the
            // label is a behavioural fix that does not belong in this conversion.
            3, 4 -> R.string.keyboard_tilt
            else -> return
        }
        pref.setSummary(summaryId)
    }

    /** A file-valued preference plus the summary to fall back to while no file is set. */
    private class MediaPreference(val preference: Preference, val defaultSummary: String)
}
