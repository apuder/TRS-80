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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View

/** Action menu item IDs. */
private const val MENU_OPTION_CANCEL = 0
private const val MENU_OPTION_HELP = 1

/**
 * Hosts the editor for a single configuration, which lives in [EditConfigurationFragment].
 *
 * The result code decides what the caller does with the configuration. `RESULT_OK` keeps the
 * edits; `RESULT_CANCELED` makes the caller restore its backup, since the fragment writes every
 * change straight into the configuration's preferences. Leaving via Back therefore keeps the
 * changes, while the Cancel action discards them.
 */
class EditConfigurationActivity : BaseActivity() {

    private lateinit var fragment: EditConfigurationFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dummy view. Will be replaced by EditConfigurationFragment.
        setContentView(View(this))
        requireNotNull(supportActionBar).setDisplayHomeAsUpEnabled(true)
        fragment = EditConfigurationFragment()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit()
    }

    @SuppressLint("AlwaysShowAction") // Cancel must stay reachable without opening the overflow.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_OPTION_CANCEL, Menu.NONE, getString(R.string.menu_cancel))
            .setIcon(R.drawable.cancel_icon)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(Menu.NONE, MENU_OPTION_HELP, Menu.NONE, getString(R.string.menu_help))
            .setIcon(R.drawable.help_icon)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            doneEditing(cancel = !fragment.configurationWasEdited)
            true
        }

        MENU_OPTION_CANCEL -> {
            doneEditing(cancel = true)
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
        val cancel = !fragment.configurationWasEdited
        setResult(if (cancel) RESULT_CANCELED else RESULT_OK, intent)
        super.onBackPressed()
    }

    private fun doneEditing(cancel: Boolean) {
        setResult(if (cancel) RESULT_CANCELED else RESULT_OK, intent)
        finish()
    }

    private fun doHelp() =
        showDialog(R.string.help_title_edit_configuration, -1, R.string.help_edit_configuration)
}
