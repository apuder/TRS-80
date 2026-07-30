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

package org.puder.trs80.shared.configuration

import com.russhwolf.settings.Settings
import org.puder.trs80.shared.KeyboardLayout
import org.puder.trs80.shared.storage.StorageKeys

private const val CONF_NAME = StorageKeys.CONFIG_NAME
private const val CONF_MODEL = StorageKeys.CONFIG_MODEL
private const val CONF_CASSETTE = StorageKeys.CONFIG_CASSETTE
private const val CONF_CHARACTER_COLOR = StorageKeys.CONFIG_CHARACTER_COLOR
private const val CONF_KEYBOARD_PORTRAIT = StorageKeys.CONFIG_KEYBOARD_PORTRAIT
private const val CONF_KEYBOARD_LANDSCAPE = StorageKeys.CONFIG_KEYBOARD_LANDSCAPE
private const val CONF_MUTE_SOUND = StorageKeys.CONFIG_MUTE_SOUND

private const val KEY_CASSETTE_POSITION = StorageKeys.CONFIG_CASSETTE_POSITION

/**
 * Persisted data about a configuration.
 *
 * Values live in the one shared store under this configuration's namespace,
 * rather than in a preferences file of their own as they used to. Keys are the
 * legacy names prefixed by [StorageKeys.configurationPrefix], which is also what
 * lets the Android editor screen bind straight to them through a
 * `SettingsPreferenceDataStore`.
 *
 * The `PreferenceFinder` that used to live here went to the Android side with
 * the screen that uses it: `androidx.preference.Preference` is a View, and this
 * class is now the platform-independent half.
 */
class ConfigurationPersistence private constructor(
    private val settings: Settings,
    private val keyPrefix: String,
) {

    companion object {
        /** Creates an instance for the configuration with the given ID. */
        fun forId(configId: Int, settings: Settings): ConfigurationPersistence =
            ConfigurationPersistence(settings, StorageKeys.configurationPrefix(configId))

        private fun diskIdToKey(disk: Int): String? =
            if (disk in 0 until StorageKeys.DRIVE_COUNT) StorageKeys.diskKey(disk) else null
    }

    private fun key(leaf: String) = keyPrefix + leaf

    /** The stored model as its raw string value, or null if none is stored. */
    val model: String?
        get() = settings.getStringOrNull(key(CONF_MODEL))

    internal fun setModel(model: String?) = setStringOrRemove(CONF_MODEL, model)

    /** The stored path of the cassette image, or null if none is stored. */
    val casettePath: String?
        get() = settings.getStringOrNull(key(CONF_CASSETTE))

    fun setCasettePath(path: String?) = setStringOrRemove(CONF_CASSETTE, path)

    /**
     * @param disk the zero-based index of the disk drive.
     * @return The stored path of the image in that drive, or null if there is none.
     */
    fun getDiskPath(disk: Int): String? =
        diskIdToKey(disk)?.let { settings.getStringOrNull(key(it)) }

    fun setDiskPath(disk: Int, path: String?) {
        val key = diskIdToKey(disk) ?: return
        setStringOrRemove(key, path)
    }

    fun getCharacterColor(defaultValue: Int): Int = getInt(CONF_CHARACTER_COLOR, defaultValue)

    internal fun setCharacterColor(value: Int) = setInt(CONF_CHARACTER_COLOR, value)

    internal fun getCassettePosition(defaultValue: Float): Float =
        settings.getFloatOrNull(key(KEY_CASSETTE_POSITION)) ?: defaultValue

    internal fun setCassettePosition(pos: Float) =
        settings.putFloat(key(KEY_CASSETTE_POSITION), pos)

    /** The stored name of the configuration, defaulting to "unknown". */
    val name: String?
        get() = settings.getStringOrNull(key(CONF_NAME)) ?: "unknown"

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
        get() = settings.getBooleanOrNull(key(CONF_MUTE_SOUND)) ?: false
        set(muted) = settings.putBoolean(key(CONF_MUTE_SOUND), muted)

    /**
     * Removes all stored data of this configuration.
     *
     * Only this configuration's namespace, which is why the store's keys are
     * prefixed rather than kept in a file of their own: the file used to be the
     * unit of deletion, and now the prefix is.
     */
    internal fun clear() {
        for (leaf in StorageKeys.configurationKeys) {
            settings.remove(key(leaf))
        }
    }

    /**
     * Stores [value], or removes the key when there is nothing to store.
     *
     * Removal rather than an empty string, because absent is what the rest of
     * the app reads as "no disk" or "no cassette".
     */
    private fun setStringOrRemove(leaf: String, value: String?) {
        if (value.isNullOrEmpty()) settings.remove(key(leaf)) else settings.putString(key(leaf), value)
    }

    /**
     * Reads a number that is stored as a string, because the preference screens
     * that write these use `ListPreference`, which only writes strings.
     */
    private fun getInt(leaf: String, defaultValue: Int): Int =
        settings.getStringOrNull(key(leaf))?.toIntOrNull() ?: defaultValue

    /** Writes a number as a string, matching how the preference screens store it. */
    private fun setInt(leaf: String, value: Int) =
        settings.putString(key(leaf), value.toString())
}
