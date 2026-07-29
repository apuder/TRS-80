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

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException

/** Intent extras: the directory to create the image in, and the path of the created image. */
private const val EXTRA_DIR = "DIR"
private const val EXTRA_PATH = "PATH"

/** The file name characters a disk image is allowed to be made of. */
private val DISK_IMAGE_NAME = Regex("[-_.A-Za-z0-9]+")

private const val DISK_IMAGE_SUFFIX = ".dsk"

/** Alpha of the "Create" icon while the entered name is legal, and while it is not. */
private const val ICON_ALPHA_ENABLED = 255
private const val ICON_ALPHA_DISABLED = 96

/**
 * Hosts the blank-disk-image editor, which lives in [CreateDiskFragment], and creates the image
 * once the user confirms. On success the path of the new image is returned to the caller in the
 * [EXTRA_PATH] extra.
 */
class CreateDiskActivity : BaseActivity() {

    private lateinit var sharedPrefs: SharedPreferences

    /**
     * Held in a field because [SharedPreferences] only keeps a weak reference to its listeners.
     */
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key.equals(MKDISK_NAME, ignoreCase = true)) {
            invalidateOptionsMenu()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dummy view. Will be replaced by CreateDiskFragment.
        setContentView(View(this))
        requireNotNull(supportActionBar).setDisplayHomeAsUpEnabled(true)

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)

        clearDiskImageName()

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, CreateDiskFragment())
            .commit()
    }

    override fun onDestroy() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.menu_create_media, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val enable = validateDiskImageName(diskImageName)
        val createMedia = menu.findItem(R.id.create_media)
        createMedia.isEnabled = enable
        createMedia.icon?.alpha = if (enable) ICON_ALPHA_ENABLED else ICON_ALPHA_DISABLED
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.create_media -> if (createDiskImage()) {
            doneEditing(cancel = false)
            true
        } else {
            // Creation failed and reported why; stay on screen.
            super.onOptionsItemSelected(item)
        }

        R.id.cancel_media, android.R.id.home -> {
            doneEditing(cancel = true)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Set the result, then let the framework perform the back navigation so
        // it routes through the back-pressed dispatcher.
        setResult(RESULT_CANCELED, intent)
        clearDiskImageName()
        super.onBackPressed()
    }

    /** @return Whether [name] is a legal disk image file name. */
    fun validateDiskImageName(name: String): Boolean = DISK_IMAGE_NAME.matches(name)

    /**
     * Creates the blank disk image the user configured and stores its path in the result intent.
     *
     * @return Whether the image was created. Failures are reported to the user directly.
     */
    private fun createDiskImage(): Boolean = try {
        val currentPath = intent.getStringExtra(EXTRA_DIR)
        var diskImgPath = File(currentPath, diskImageName).canonicalPath
        if (!diskImgPath.endsWith(DISK_IMAGE_SUFFIX, ignoreCase = true)) {
            diskImgPath += DISK_IMAGE_SUFFIX
        }
        if (File(diskImgPath).exists()) {
            throw IOException(getString(R.string.mkdisk_file_exists_error))
        }

        val format = diskImageFormat
        val created = when {
            format.equals(getString(R.string.mkdisk_jv1), ignoreCase = true) ->
                XTRS.createBlankJV1(diskImgPath)

            format.equals(getString(R.string.mkdisk_jv3), ignoreCase = true) ->
                XTRS.createBlankJV3(diskImgPath)

            format.equals(getString(R.string.mkdisk_dmk), ignoreCase = true) ->
                XTRS.createBlankDMK(
                    diskImgPath,
                    diskImageSided,
                    diskImageDensity,
                    diskImageEight,
                    diskImageIgnoreDensity
                )

            else -> false
        }
        if (!created) {
            throw IOException(getString(R.string.mkdisk_create_error))
        }
        intent.putExtra(EXTRA_PATH, diskImgPath)
        true
    } catch (e: Exception) {
        Snackbar.make(
            findViewById<View>(android.R.id.content),
            e.localizedMessage.orEmpty(),
            Snackbar.LENGTH_SHORT
        ).show()
        false
    }

    private fun doneEditing(cancel: Boolean) {
        setResult(if (cancel) RESULT_CANCELED else RESULT_OK, intent)
        clearDiskImageName()
        finish()
    }

    private val diskImageName: String
        get() = sharedPrefs.getString(MKDISK_NAME, "").orEmpty()

    private val diskImageFormat: String
        get() = prefOrDefault(MKDISK_FORMAT, R.string.mkdisk_jv1)

    private val diskImageSided: Int
        get() = (sharedPrefs.getString(MKDISK_SIDED, null) ?: "1").toInt()

    private val diskImageDensity: Int
        get() = if (prefOrDefault(MKDISK_DENSITY, R.string.mkdisk_single)
                .equals(getString(R.string.mkdisk_double), ignoreCase = true)
        ) 2 else 1

    private val diskImageEight: Int
        get() = if (prefOrDefault(MKDISK_SIZE, R.string.mkdisk_5_inch)
                .equals(getString(R.string.mkdisk_8_inch), ignoreCase = true)
        ) 1 else 0

    private val diskImageIgnoreDensity: Int
        get() = if (sharedPrefs.getBoolean(MKDISK_IGNORE_DENSITY, false)) 1 else 0

    private fun prefOrDefault(key: String, defaultStringId: Int): String =
        sharedPrefs.getString(key, null) ?: getString(defaultStringId)

    /**
     * Resets the name so a screen that is opened again does not offer to overwrite the image
     * that was just created. Written synchronously because the fragment reads it right after.
     */
    @SuppressLint("ApplySharedPref")
    private fun clearDiskImageName() {
        sharedPrefs.edit().putString(MKDISK_NAME, "").commit()
    }

    companion object {
        const val MKDISK_NAME = "mkdisk_name"
        const val MKDISK_FORMAT = "mkdisk_format"
        const val MKDISK_SIDED = "mkdisk_sided"
        const val MKDISK_DENSITY = "mkdisk_density"
        const val MKDISK_SIZE = "mkdisk_size"
        const val MKDISK_IGNORE_DENSITY = "mkdisk_ignore_density"
    }
}
