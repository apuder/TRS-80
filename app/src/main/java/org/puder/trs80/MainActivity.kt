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

// This screen still drives the pre-AndroidX preference manager, the MenuItemCompat helpers, the
// startActivityForResult()/onActivityResult() pair and onBackPressed(). All of them are
// deprecated, and all of them are kept because their replacements would change behaviour.
@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package org.puder.trs80

import org.puder.trs80.storage.AppStorage
import org.puder.trs80.configuration.getOrInit
import org.puder.trs80.configuration.getSystemState
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.mediarouter.app.MediaRouteButton
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.puder.trs80.cast.CastMessageSender
import org.puder.trs80.shared.configuration.Configuration
import org.puder.trs80.shared.configuration.ConfigurationManager
import org.puder.trs80.drag.ConfigurationItemTouchHelperCallback
import org.puder.trs80.shared.io.FileManager
import org.puder.trs80.shared.localstore.RomManager
import org.puder.trs80.shared.storage.ImportResult
import org.retrostore.android.RetrostoreActivity
import org.retrostore.android.RetrostoreApi
import org.retrostore.client.common.proto.App
import java.io.File
import java.io.IOException

private const val TAG = "MainActivity"

private const val EXTRA_KEY_NEW_CONFIG_ID = "CONFIG_ID"
private const val EXTRA_KEY_IS_NEW = "IS_NEW"

/** The width one column of configuration cards is laid out with, in dp. */
private const val COLUMN_WIDTH_DP = 300

/** The number of disk drives a configuration can hold images for. */
private const val NUM_DISKS = 4

private const val REQUEST_CODE_EDIT_CONFIG = 1
private const val REQUEST_CODE_EDIT_SETTINGS = 2

// Action Menu
private const val MENU_OPTION_DOWNLOAD = 0
private const val MENU_OPTION_ADD_CONFIGURATION = 1
private const val MENU_OPTION_HELP = 2

/** Passed to [MainActivity.updateView] for the positions that did not change. */
private const val NO_POSITION = -1

/**
 * The app's home screen: the list of configurations, the navigation drawer, and the one-time
 * download of the ROMs the emulator needs.
 */
class MainActivity : BaseActivity(), InitialSetupDialogFragment.DownloadCompletionListener,
    ConfigurationItemListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var configurationListView: RecyclerView
    private lateinit var configurationListViewAdapter: ConfigurationListViewAdapter
    private lateinit var sharedPrefs: SharedPreferences
    private var downloadMenuItem: MenuItem? = null

    private lateinit var toggle: ActionBarDrawerToggle
    private var backupConfiguration: Configuration? = null

    private lateinit var configManager: ConfigurationManager
    private lateinit var romManager: RomManager

    // Note: This is in the RetroStore package.
    private lateinit var appInstaller: AppInstaller
    private lateinit var castMessageSender: CastMessageSender

    override fun onCreate(savedInstanceState: Bundle?) {
        // StrictMode.enableDefaults();
        super.onCreate(savedInstanceState)

        RetrostoreApi.get().registerAppInstallListener { app -> asyncDownloadAndInstallApp(app) }
        PreferenceManager.setDefaultValues(this, R.xml.configuration, false)
        sharedPrefs = getSharedPreferences(SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE)
        setContentView(R.layout.main_activity)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer.setDrawerListener(toggle)
        findViewById<NavigationView>(R.id.nav_view).setNavigationItemSelectedListener(this)

        val fileManagerCreator = FileManager.Creator.get()
        try {
            configManager = ConfigurationManager.getOrInit()
            romManager = RomManager.init(fileManagerCreator, AppStorage.get().settings)
        } catch (e: IOException) {
            Log.e(TAG, "Cannot initialize RomManager / ConfigurationManager.", e)
            finish()
            // Cannot really launch the app if initialization fails.
            // TODO: Show an error message before exiting.
            return
        }
        appInstaller = AppInstaller(configManager, RetrostoreApi.get())
        castMessageSender = CastMessageSender.get()

        val screenWidthDp = resources.configuration.screenWidthDp
        val numColumns =
            if (screenWidthDp == android.content.res.Configuration.SCREEN_WIDTH_DP_UNDEFINED) 1
            else screenWidthDp / COLUMN_WIDTH_DP
        configurationListViewAdapter = ConfigurationListViewAdapter(configManager, this, numColumns)
        configurationListView = findViewById<RecyclerView>(R.id.list_configurations).apply {
            layoutManager =
                if (numColumns <= 1) LinearLayoutManager(this@MainActivity)
                else GridLayoutManager(this@MainActivity, numColumns)
            adapter = configurationListViewAdapter
        }

        ItemTouchHelper(ConfigurationItemTouchHelperCallback(configurationListViewAdapter))
            .attachToRecyclerView(configurationListView)

        observeScreenshots()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onResume() {
        super.onResume()
        updateView()
        castMessageSender.start()
        reportFailedLegacyImport()

        val firstTime = sharedPrefs.getBoolean(SettingsActivity.CONF_FIRST_TIME, true)
        val ranNewAssistant =
            sharedPrefs.getBoolean(SettingsActivity.CONF_RAN_NEW_ASSISTANT, false)

        sharedPrefs.edit()
            .putBoolean(SettingsActivity.CONF_FIRST_TIME, false)
            .putBoolean(SettingsActivity.CONF_RAN_NEW_ASSISTANT, true)
            .apply()

        if (!ranNewAssistant || (!romManager.hasAllRoms() && firstTime)) {
            downloadROMs()
        }
    }

    override fun onPause() {
        if (isFinishing) {
            castMessageSender.stop()
        }
        super.onPause()
    }

    /**
     * Tells the user if the startup import of their previous settings failed.
     *
     * Worth a dialog rather than a snackbar: the visible symptom is a
     * configuration list that is empty or missing entries, which otherwise looks
     * like the app lost their data. It has not — the old values are still on disk
     * and the import is retried on the next launch.
     *
     * The result is consumed, so this is shown once rather than on every return
     * to this screen.
     */
    private fun reportFailedLegacyImport() {
        val failure = TRS80Application.consumeLegacyImportResult() as? ImportResult.Failed ?: return
        AlertDialogUtil.showDialog(
            this,
            AlertDialogUtil.createAlertDialog(
                this,
                R.string.legacy_import_failed_title,
                -1,
                getString(R.string.legacy_import_failed_msg, failure.message)
            ).setPositiveButton(android.R.string.ok) { _, _ ->
                AlertDialogUtil.dismissDialog(this)
            }
        )
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Refreshes the card of a configuration whose screenshot was just saved. Collection is bound to
     * the STARTED state, matching where the previous event-bus registration lived.
     */
    private fun observeScreenshots() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            ScreenshotEvents.screenshots.collect { configurationId ->
                updateView(
                    positionChanged = configManager.getPositionOfConfigWithId(configurationId)
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val mediaRouteItem = menu.findItem(R.id.action_cast_trs80)
        val mediaRouteButton = MenuItemCompat.getActionView(mediaRouteItem) as MediaRouteButton
        mediaRouteButton.routeSelector = castMessageSender.routeSelector

        if (!romManager.hasAllRoms()) {
            downloadMenuItem = menu.add(
                Menu.NONE, MENU_OPTION_DOWNLOAD, Menu.NONE, getString(R.string.menu_download)
            ).apply {
                setIcon(R.drawable.download_icon)
                MenuItemCompat.setShowAsAction(this, MenuItemCompat.SHOW_AS_ACTION_ALWAYS)
            }
        }
        MenuItemCompat.setShowAsAction(
            menu.add(
                Menu.NONE, MENU_OPTION_ADD_CONFIGURATION, Menu.NONE,
                getString(R.string.menu_add_configuration)
            ).setIcon(R.drawable.add_icon),
            MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
        )
        MenuItemCompat.setShowAsAction(
            menu.add(Menu.NONE, MENU_OPTION_HELP, Menu.NONE, getString(R.string.menu_help))
                .setIcon(R.drawable.help_icon),
            MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
        )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return when (item.itemId) {
            MENU_OPTION_DOWNLOAD -> {
                downloadROMs()
                true
            }
            MENU_OPTION_ADD_CONFIGURATION -> {
                addConfiguration()
                true
            }
            MENU_OPTION_HELP -> {
                showHelp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> showSettings()
            R.id.nav_rate -> showRating()
            R.id.nav_help -> showHelp()
            R.id.nav_community -> showCommunity()
            R.id.nav_share -> showShare()
        }
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
        return true
    }

    /** Opens the RetroStore. Wired up by `android:onClick` in `main_activity.xml`. */
    fun onRetrostoreBtnClicked(view: View) {
        startActivity(Intent(this, RetrostoreActivity::class.java))
    }

    override fun onConfigurationEdit(configuration: Configuration, position: Int) =
        editConfiguration(configuration, isNew = false)

    override fun onConfigurationDelete(configuration: Configuration, position: Int) =
        deleteConfiguration(configuration, position)

    override fun onConfigurationStop(configuration: Configuration, position: Int) =
        stopEmulator(configuration, position)

    override fun onConfigurationRun(configuration: Configuration, position: Int) =
        runEmulator(configuration)

    override fun onConfigurationShare(configuration: Configuration, position: Int) =
        shareEmulatorState(configuration)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE_EDIT_CONFIG || data == null) {
            return
        }
        val isNew = data.getBooleanExtra(EXTRA_KEY_IS_NEW, false)
        if (resultCode == Activity.RESULT_OK) {
            val id = data.getIntExtra(EXTRA_KEY_NEW_CONFIG_ID, -1)
            val position = configManager.getPositionOfConfigWithId(id)
            // Delete emulator state
            try {
                configManager.getEmulatorState(id).deleteSavedState()
            } catch (e: IOException) {
                Log.e(TAG, "Could not delete emulator state.", e)
            }
            configManager.getConfigById(id)?.cassettePosition = 0f
            // Update UI
            if (isNew) updateView(positionInserted = position)
            else updateView(positionChanged = position)
            return
        }
        val backup = backupConfiguration ?: return
        if (isNew) {
            configManager.deleteConfigWithId(backup.id)
        } else {
            configManager.persistConfig(backup)
        }
        backupConfiguration = null
    }

    override fun onDownloadCompleted() {
        downloadMenuItem?.isVisible = false
        updateView()
    }

    override fun showHint(): Boolean =
        AlertDialogUtil.showHint(this, R.string.hint_configuration_usage)

    /**
     * Shows the empty state or the list of configurations, and tells the adapter which item
     * changed. With no position given at all, the whole list is refreshed.
     */
    private fun updateView(
        positionChanged: Int = NO_POSITION,
        positionInserted: Int = NO_POSITION,
        positionDeleted: Int = NO_POSITION
    ) {
        val withoutConfigurationsView = findViewById<View>(R.id.without_configurations)
        val withConfigurationsView = findViewById<View>(R.id.with_configurations)
        if (configManager.configCount == 0) {
            withoutConfigurationsView.visibility = View.VISIBLE
            withConfigurationsView.visibility = View.GONE
            return
        }

        withoutConfigurationsView.visibility = View.GONE
        withConfigurationsView.visibility = View.VISIBLE

        when {
            positionChanged != NO_POSITION ->
                configurationListViewAdapter.notifyItemChanged(positionChanged)
            positionInserted != NO_POSITION -> {
                configurationListViewAdapter.notifyItemInserted(positionInserted)
                configurationListView.layoutManager?.scrollToPosition(positionInserted)
            }
            positionDeleted != NO_POSITION ->
                configurationListViewAdapter.notifyItemRemoved(positionDeleted)
            else -> configurationListViewAdapter.notifyDataSetChanged()
        }
    }

    private fun addConfiguration() =
        editConfiguration(configManager.newConfiguration(), isNew = true)

    private fun editConfiguration(conf: Configuration, isNew: Boolean) {
        backupConfiguration = conf.createBackup()
        val intent = Intent(this, EditConfigurationActivity::class.java)
            .putExtra(EXTRA_KEY_NEW_CONFIG_ID, conf.id)
            .putExtra(EXTRA_KEY_IS_NEW, isNew)
        startActivityForResult(intent, REQUEST_CODE_EDIT_CONFIG)
    }

    private fun deleteConfiguration(conf: Configuration, position: Int) {
        val msg = getString(R.string.alert_dialog_confirm_delete, conf.name)
        val builder = AlertDialogUtil.createAlertDialog(
            this, R.string.app_name, R.drawable.warning_icon, msg
        ).apply {
            setPositiveButton(R.string.alert_dialog_ok) { _, _ ->
                AlertDialogUtil.dismissDialog(this@MainActivity)
                configManager.deleteConfigWithId(conf.id)
                updateView(positionDeleted = position)
            }
            setNegativeButton(R.string.alert_dialog_cancel) { _, _ ->
                AlertDialogUtil.dismissDialog(this@MainActivity)
            }
        }
        AlertDialogUtil.showDialog(this, builder)
    }

    private fun stopEmulator(conf: Configuration, position: Int) {
        val msg = getString(R.string.alert_dialog_confirm_stop_emu, conf.name)
        val builder = AlertDialogUtil.createAlertDialog(
            this, R.string.app_name, R.drawable.warning_icon, msg
        ).apply {
            setPositiveButton(R.string.alert_dialog_ok) { _, _ ->
                AlertDialogUtil.dismissDialog(this@MainActivity)
                try {
                    configManager.getEmulatorState(conf.id).deleteSavedState()
                } catch (e: IOException) {
                    Log.e(TAG, "Could not delete emulator state.", e)
                }
                conf.cassettePosition = 0f
                updateView(positionChanged = position)
            }
            setNegativeButton(R.string.alert_dialog_cancel) { _, _ ->
                AlertDialogUtil.dismissDialog(this@MainActivity)
            }
        }
        AlertDialogUtil.showDialog(this, builder)
    }

    private fun runEmulator(conf: Configuration) {
        Log.i(TAG, "RUN EMULATOR")
        val root = findViewById<View>(R.id.main)

        val model = conf.model
        if (model != Hardware.MODEL1 && model != Hardware.MODEL3) {
            Snackbar.make(root, R.string.error_model_not_supported, Snackbar.LENGTH_LONG).show()
            return
        }

        if (!romManager.hasAllRoms()) {
            Snackbar.make(root, R.string.error_no_rom, Snackbar.LENGTH_LONG).show()
            return
        }
        for (disk in 0 until NUM_DISKS) {
            if (!checkIfFileExists(conf.getDiskPath(disk), true, R.string.error_no_disk)) {
                return
            }
        }

        startActivity(
            Intent(this, EmulatorActivity::class.java)
                .putExtra(EmulatorActivity.EXTRA_CONFIGURATION_ID, conf.id)
        )
    }

    private fun shareEmulatorState(conf: Configuration) {
        val state = try {
            configManager.getEmulatorState(conf.id).getSystemState(conf.model)
        } catch (e: IOException) {
            Log.e(TAG, "Cannot get emulator state.", e)
            showToast("Cannot get emulator state")
            return
        }
        if (state == null) {
            Log.e(TAG, "Cannot get xray system state.")
            showToast("Cannot get xray system state")
            return
        }

        lifecycleScope.launch {
            val token = RetrostoreApi.get().uploadSystemState(state)
            if (token == null) {
                Log.e(TAG, "Failed uploading state to RetroStore")
                // TODO: I18N.
                showToast("Failed uploading state to RetroStore")
                return@launch
            }
            showToast(StrUtil.form("System state uploaded. TOKEN: %d", token))
        }
    }

    /**
     * @return Whether [path] points at a file that exists, or is null and [allowNull] is set.
     * Shows [errMsg] in a snackbar when it does not.
     */
    private fun checkIfFileExists(path: String?, allowNull: Boolean, errMsg: Int): Boolean {
        if (path == null) {
            if (allowNull) {
                return true
            }
        } else if (File(path).exists()) {
            return true
        }
        val root = findViewById<View>(R.id.main)
        Snackbar.make(root, errMsg, Snackbar.LENGTH_LONG).show()
        return false
    }

    private fun showSettings() = startActivityForResult(
        Intent(this, SettingsActivity::class.java), REQUEST_CODE_EDIT_SETTINGS
    )

    private fun showHelp() =
        showDialog(R.string.help_title_configurations, -1, R.string.help_configurations)

    private fun showRating() {
        val uri = Uri.parse("market://details?id=$packageName")
        val playMarketIntent = Intent(Intent.ACTION_VIEW, uri).addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(playMarketIntent)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    private fun showCommunity() =
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.community_url))))

    private fun downloadROMs() {
        val builder = AlertDialogUtil.createAlertDialog(
            this, R.string.title_initial_setup, R.drawable.ic_info_black, R.string.initial_setup
        ).apply {
            setPositiveButton(R.string.alert_dialog_ok) { _, _ ->
                AlertDialogUtil.dismissDialog(this@MainActivity)
                InitialSetupDialogFragment().show(supportFragmentManager, "dialog")
            }
            setNegativeButton(R.string.alert_dialog_cancel) { _, _ ->
                AlertDialogUtil.dismissDialog(this@MainActivity)
            }
        }
        AlertDialogUtil.showDialog(this, builder)
    }

    private fun showShare() {
        val sendIntent = Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
            .setType("text/plain")
        startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.share_title)))
    }

    private fun asyncDownloadAndInstallApp(app: App) = lifecycleScope.launch {
        if (appInstaller.downloadAndInstallApp(app)) {
            showToast(StrUtil.form(getString(R.string.successfully_installed), app.name))
        }
    }

    /** Shows a toast with the given [message] on the main thread. */
    private fun showToast(message: String) = runOnUiThread {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
