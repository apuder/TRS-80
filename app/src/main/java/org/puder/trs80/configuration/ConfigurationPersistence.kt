/*
 * Copyright 2017, Sascha Haeberling
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

import android.content.Context
import android.content.SharedPreferences
import android.preference.Preference
import android.preference.PreferenceManager
import com.google.common.base.Optional

private const val PREF_NAME_PREFIX = "CONFIG_"
private const val CONF_NAME = "conf_name"
private const val CONF_MODEL = "conf_model"
private const val CONF_CASSETTE = "conf_cassette"
private const val CONF_DISK1 = "conf_disk1"
private const val CONF_DISK2 = "conf_disk2"
private const val CONF_DISK3 = "conf_disk3"
private const val CONF_DISK4 = "conf_disk4"
private const val CONF_CHARACTER_COLOR = "conf_character_color"
private const val CONF_KEYBOARD_PORTRAIT = "conf_keyboard_portrait"
private const val CONF_KEYBOARD_LANDSCAPE = "conf_keyboard_landscape"
private const val CONF_MUTE_SOUND = "conf_mute_sound"

private const val KEY_CASSETTE_POSITION = "cassette_position"

/**
 * Persisted data about a configuration.
 */
class ConfigurationPersistence private constructor(private val sharedPrefs: SharedPreferences) {

    companion object {
        /** Create an instance for the configuration with the given ID. */
        internal fun forId(configId: Int, context: Context): ConfigurationPersistence =
            ConfigurationPersistence(
                context.getSharedPreferences(PREF_NAME_PREFIX + configId, Context.MODE_PRIVATE)
            )

        /**
         * Create an instance for the configuration with the given ID that is backed by the
         * preferences of the given manager.
         */
        @JvmStatic
        fun forIdAndManager(
            configId: Int,
            prefManager: PreferenceManager
        ): ConfigurationPersistence {
            prefManager.sharedPreferencesName = PREF_NAME_PREFIX + configId
            return ConfigurationPersistence(prefManager.sharedPreferences)
        }

        private fun diskIdToKey(disk: Int): String? = when (disk) {
            0 -> CONF_DISK1
            1 -> CONF_DISK2
            2 -> CONF_DISK3
            3 -> CONF_DISK4
            else -> null
        }
    }

    /** The stored model as its raw string value. */
    val model: Optional<String>
        get() = getString(CONF_MODEL, null)

    internal fun setModel(model: String?) = setStringOrRemove(CONF_MODEL, model)

    /** The stored path of the cassette image. */
    val casettePath: Optional<String>
        get() = getString(CONF_CASSETTE, null)

    fun setCasettePath(path: String?) = setStringOrRemove(CONF_CASSETTE, path)

    /**
     * @param disk the zero-based index of the disk drive.
     * @return The stored path of the image in that drive.
     */
    fun getDiskPath(disk: Int): Optional<String> {
        val key = diskIdToKey(disk) ?: return Optional.absent()
        return getString(key, null)
    }

    fun setDiskPath(disk: Int, path: String?) {
        val key = diskIdToKey(disk) ?: return
        setStringOrRemove(key, path)
    }

    fun getCharacterColor(defaultValue: Int): Int = getInt(CONF_CHARACTER_COLOR, defaultValue)

    internal fun setCharacterColor(value: Int) = setInt(CONF_CHARACTER_COLOR, value)

    internal fun getCassettePosition(defaultValue: Float): Float =
        sharedPrefs.getFloat(KEY_CASSETTE_POSITION, defaultValue)

    internal fun setCassettePosition(pos: Float) =
        sharedPrefs.edit().putFloat(KEY_CASSETTE_POSITION, pos).apply()

    /** The stored name of the configuration, defaulting to "unknown". */
    val name: Optional<String>
        get() = getString(CONF_NAME, "unknown")

    internal fun setName(name: String?) = setStringOrRemove(CONF_NAME, name)

    /** The stored ID of the portrait keyboard layout, see [KeyboardLayout]. */
    val keyboardLayoutPortrait: Int
        get() = getInt(CONF_KEYBOARD_PORTRAIT, 0)

    internal fun setKeyboardLayoutPortrait(layout: Int) = setInt(CONF_KEYBOARD_PORTRAIT, layout)

    /** The stored ID of the landscape keyboard layout, see [KeyboardLayout]. */
    val keyboardLayoutLandscape: Int
        get() = getInt(CONF_KEYBOARD_LANDSCAPE, 0)

    internal fun setKeyboardLayoutLandscape(layout: Int) = setInt(CONF_KEYBOARD_LANDSCAPE, layout)

    /** Whether the emulator's sound output is muted. */
    internal var isSoundMuted: Boolean
        get() = sharedPrefs.getBoolean(CONF_MUTE_SOUND, false)
        set(muted) = sharedPrefs.edit().putBoolean(CONF_MUTE_SOUND, muted).apply()

    /** Removes all stored data. */
    internal fun clear() = sharedPrefs.edit().clear().apply()

    /** @return A finder for the preferences of this configuration. */
    fun forPreferenceProvider(provider: PreferenceProvider): PreferenceFinder =
        PreferenceFinder(provider)

    private fun getString(key: String, defaultValue: String?): Optional<String> =
        Optional.fromNullable(sharedPrefs.getString(key, defaultValue))

    private fun setStringOrRemove(key: String, value: String?) {
        val editor = sharedPrefs.edit()
        if (value.isNullOrEmpty()) editor.remove(key) else editor.putString(key, value)
        editor.apply()
    }

    private fun getInt(key: String, defaultValue: Int): Int =
        sharedPrefs.getString(key, "")?.toIntOrNull() ?: defaultValue

    private fun setInt(key: String, value: Int) =
        sharedPrefs.edit().putString(key, value.toString()).apply()

    /**
     * Finds preferences for configurations.
     */
    class PreferenceFinder(private val provider: PreferenceProvider) {
        fun forModel(): Preference = provider.findPreference(CONF_MODEL)

        fun forName(): Preference = provider.findPreference(CONF_NAME)

        fun forCasette(): Preference = provider.findPreference(CONF_CASSETTE)

        fun forDisk1(): Preference = provider.findPreference(CONF_DISK1)

        fun forDisk2(): Preference = provider.findPreference(CONF_DISK2)

        fun forDisk3(): Preference = provider.findPreference(CONF_DISK3)

        fun forDisk4(): Preference = provider.findPreference(CONF_DISK4)

        fun forCharacterColor(): Preference = provider.findPreference(CONF_CHARACTER_COLOR)

        fun forKeyboardPortrait(): Preference = provider.findPreference(CONF_KEYBOARD_PORTRAIT)

        fun forKeyboardLandscape(): Preference = provider.findPreference(CONF_KEYBOARD_LANDSCAPE)
    }

    /**
     * Classes implementing this interface provide preferences by name.
     */
    fun interface PreferenceProvider {
        fun findPreference(name: String): Preference
    }
}
