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

import android.os.Bundle
import org.puder.trs80.shared.storage.StorageKeys
import org.puder.trs80.storage.AppStorage
import android.view.Menu
import android.view.MenuItem
import android.view.View

/** Action menu item ID. */
private const val MENU_OPTION_HELP = 0

/**
 * Hosts the app-wide settings, which live in [SettingsFragment].
 */
class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dummy view. Will be replaced by SettingsFragment.
        setContentView(View(this))
        requireNotNull(supportActionBar).setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_OPTION_HELP, Menu.NONE, getString(R.string.menu_help))
            .setIcon(R.drawable.help_icon)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            doDone()
            true
        }

        MENU_OPTION_HELP -> {
            doHelp()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Set the result, then let the framework perform the back navigation so
        // it routes through the back-pressed dispatcher.
        setResult(RESULT_OK, intent)
        super.onBackPressed()
    }

    private fun doDone() {
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun doHelp() = showDialog(R.string.help_title_settings, -1, R.string.help_settings)

    companion object {
        /** Name of the shared preferences file holding the app-wide settings. */
        const val SHARED_PREF_NAME = "Settings"

        const val CONF_FIRST_TIME = "conf_first_time"
        const val CONF_RAN_NEW_ASSISTANT = "conf_ran_new_assistant"
        const val CONF_ROM_MODEL1 = "conf_rom_model1"
        const val CONF_ROM_MODEL3 = "conf_rom_model3"
        const val CONF_ROM_MODEL4 = "conf_rom_model4"
        const val CONF_ROM_MODEL4P = "conf_rom_model4p"

        /**
         * @return The app-wide setting stored under [key], or `null` if it was never set.
         */
        @JvmStatic
        fun getSetting(key: String): String? =
            AppStorage.get().settings.getStringOrNull(StorageKeys.APP_PREFIX + key)
    }
}
