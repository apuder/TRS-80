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

/**
 * The names every stored value is filed under.
 *
 * There is one store, and keys are namespaced within it. The Android app used
 * to keep a separate preferences file per configuration, named `CONFIG_<id>`,
 * which only ever made sense because `PreferenceFragment` wanted a
 * SharedPreferences *name*. It maps badly onto iOS, where the equivalent is an
 * `NSUserDefaults` suite and suites are meant for app groups rather than for
 * dozens of per-entity stores.
 *
 * Leaf names are deliberately the legacy `conf_` ones wherever an Android
 * preference screen is bound to the value, because those are the keys the
 * preference XML under `res/xml` already uses. That makes the bridge from a
 * preference screen to this store a pure prefix operation rather than a
 * translation table.
 */
object StorageKeys {

    /** Records the outcome of the legacy import; see `ImportStatus`. */
    const val IMPORT_STATUS = "import.status"

    /** Why the last import attempt failed, so Settings can show it. */
    const val IMPORT_ERROR = "import.error"

    // ---- Per-configuration -------------------------------------------------

    const val CONFIG_NAME = "conf_name"
    const val CONFIG_MODEL = "conf_model"
    const val CONFIG_CASSETTE = "conf_cassette"
    const val CONFIG_CHARACTER_COLOR = "conf_character_color"
    const val CONFIG_KEYBOARD_PORTRAIT = "conf_keyboard_portrait"
    const val CONFIG_KEYBOARD_LANDSCAPE = "conf_keyboard_landscape"
    const val CONFIG_MUTE_SOUND = "conf_mute_sound"
    const val CONFIG_CASSETTE_POSITION = "cassette_position"

    /** When this configuration was last run, for ordering the library. */
    const val CONFIG_LAST_USED = "conf_last_used"

    /**
     * Whether the user made or edited this configuration themselves.
     *
     * Drives the CUSTOM mark in the library: something installed from the store
     * and left alone is an original, and anything the user has had a hand in is
     * not.
     */
    const val CONFIG_IS_CUSTOM = "conf_is_custom"

    /**
     * The store's ID for the program this configuration was made from.
     *
     * Absent for machines the user built themselves. Recorded because the link
     * back to the catalog cannot be inferred: it used to be matched on the name,
     * which picks an arbitrary one of two machines called the same thing and
     * breaks outright the moment a machine is renamed.
     */
    const val CONFIG_STORE_ID = "conf_store_id"

    /**
     * Whether the tour has been run on this machine.
     *
     * Set the first time it starts, not when it finishes: what it records is
     * that the machine has been offered the tour, which is what decides whether
     * opening it starts one. Somebody who cancelled halfway through has still
     * seen it start, and starting it again over the top of what they went on to
     * do is exactly the thing this avoids.
     */
    const val CONFIG_TUTORIAL_RUN = "conf_tutorial_run"

    /** The number of disk drives a configuration has. */
    const val DRIVE_COUNT = 4

    /**
     * The key of the image in drive [drive], zero-based.
     *
     * The legacy names are one-based (`conf_disk1` for drive 0), which is why
     * this is a function rather than four constants.
     */
    fun diskKey(drive: Int): String {
        require(drive in 0 until DRIVE_COUNT) { "No drive $drive." }
        return "conf_disk${drive + 1}"
    }

    /** All per-configuration leaf names, which is what the migration copies. */
    val configurationKeys: List<String> = buildList {
        add(CONFIG_NAME)
        add(CONFIG_MODEL)
        add(CONFIG_CASSETTE)
        add(CONFIG_CHARACTER_COLOR)
        add(CONFIG_KEYBOARD_PORTRAIT)
        add(CONFIG_KEYBOARD_LANDSCAPE)
        add(CONFIG_MUTE_SOUND)
        add(CONFIG_CASSETTE_POSITION)
        add(CONFIG_LAST_USED)
        add(CONFIG_IS_CUSTOM)
        add(CONFIG_STORE_ID)
        for (drive in 0 until DRIVE_COUNT) add(diskKey(drive))
    }

    /** The namespace every value of configuration [id] lives under. */
    fun configurationPrefix(id: Int): String = "config.$id."

    /** The full key of [leaf] for configuration [id]. */
    fun configuration(id: Int, leaf: String): String = configurationPrefix(id) + leaf

    // ---- App-wide ----------------------------------------------------------

    /**
     * The namespace app-wide values live under.
     *
     * Also the prefix the settings screen is bridged with, so that its
     * preference keys resolve to the same names used here.
     */
    const val APP_PREFIX = "app."

    /** The comma-joined IDs of the configurations that exist, in display order. */
    const val CONFIGURATION_IDS = APP_PREFIX + "configurations"

    /** The counter the next configuration's ID comes from. */
    const val NEXT_CONFIGURATION_ID = APP_PREFIX + "nextId"

    /** Which register the app draws in: see `ThemePreference`. */
    const val APP_THEME = APP_PREFIX + "theme"

    /** Whether the user has found the experimental section; see `ExperimentalFeatures`. */
    const val EXPERIMENTAL_UNLOCKED = APP_PREFIX + "experimental"

    /** Whether sharing a machine's state is offered. Meaningless while locked. */
    const val EXPERIMENTAL_SHARE = APP_PREFIX + "experimental_share"

    /**
     * The ROM image for an emulated model. The leaf keeps its legacy name for
     * the same reason as the per-configuration keys: the settings screen binds
     * to it directly.
     */
    fun romKey(model: Int): String {
        val leaf = when (model) {
            1 -> "conf_rom_model1"
            3 -> "conf_rom_model3"
            4 -> "conf_rom_model4"
            5 -> "conf_rom_model4p"
            else -> throw IllegalArgumentException("No ROM setting for model $model.")
        }
        return APP_PREFIX + leaf
    }

    /** The models that have a ROM setting. */
    val romModels: List<Int> = listOf(1, 3, 4, 5)
}
