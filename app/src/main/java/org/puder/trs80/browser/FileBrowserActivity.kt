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

package org.puder.trs80.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import org.puder.trs80.AlertDialogUtil
import org.puder.trs80.BaseActivity
import org.puder.trs80.CreateDiskActivity
import org.puder.trs80.R
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

private const val TAG = "FileBrowser"

/** Action menu item IDs. */
private const val MENU_OPTION_EJECT = 0
private const val MENU_OPTION_ADD = 1
private const val MENU_OPTION_IMPORT = 2

/** Request codes for the activities this one starts. */
private const val MKDISK_REQUEST = 0
private const val IMPORT_FILE_REQUEST = 1

/** Intent extras: the directory to start in, and the file the user picked. */
private const val EXTRA_DIR = "DIR"
private const val EXTRA_PATH = "PATH"

/** The entry that navigates to the parent directory. */
private const val PARENT_DIR = ".."

/**
 * Lets the user pick a file from external storage, create a blank disk image in the directory
 * currently shown, or import a file into it.
 *
 * On `RESULT_OK` the picked file is returned in the [EXTRA_PATH] extra. A *null* extra is the
 * eject case: it tells the caller to detach whatever medium is currently attached.
 */
class FileBrowserActivity : BaseActivity() {

    private val items = mutableListOf<String>()
    private lateinit var pathPrefix: String
    private lateinit var pathLabel: TextView
    private lateinit var currentPath: String
    private lateinit var fileListAdapter: BrowserListViewAdapter

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.file_browser)
        requireNotNull(supportActionBar).setDisplayHomeAsUpEnabled(true)
        pathPrefix = getString(R.string.path) + ": "
        pathLabel = findViewById(R.id.path)
        fileListAdapter = BrowserListViewAdapter(this, items)
        val listView = findViewById<ListView>(R.id.file_list)
        listView.adapter = fileListAdapter
        listView.setOnItemClickListener { _, _, _, id -> onItemClicked(items[id.toInt()]) }

        val startingDir = intent.getStringExtra(EXTRA_DIR)
            ?: Environment.getExternalStorageDirectory().path
        getFiles(startingDir)
    }

    @SuppressLint("AlwaysShowAction") // All three actions have to stay one tap away.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_OPTION_EJECT, Menu.NONE, getString(R.string.menu_eject))
            .setIcon(R.drawable.eject_icon)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(Menu.NONE, MENU_OPTION_ADD, Menu.NONE, getString(R.string.menu_add))
            .setIcon(R.drawable.add_icon)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(Menu.NONE, MENU_OPTION_IMPORT, Menu.NONE, getString(R.string.menu_add))
            .setIcon(R.drawable.download_icon)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            setResult(RESULT_CANCELED, intent)
            finish()
            true
        }

        MENU_OPTION_EJECT -> {
            eject()
            true
        }

        MENU_OPTION_ADD -> {
            createDisk()
            true
        }

        MENU_OPTION_IMPORT -> {
            chooseFileToImport()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Set the result, then let the framework perform the back navigation so
        // it routes through the back-pressed dispatcher.
        setResult(RESULT_CANCELED, intent)
        super.onBackPressed()
    }

    /**
     * @return The display name of the document [uri] points at, or `null` if neither the content
     * resolver nor the URI itself yields one.
     */
    fun getFileName(uri: Uri): String? =
        (if (uri.scheme == "content") queryDisplayName(uri) else null)
            ?: uri.path?.substringAfterLast('/')

    @Suppress("DEPRECATION") // startActivityForResult/onActivityResult.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == MKDISK_REQUEST && resultCode == RESULT_OK -> {
                val created = data?.getStringExtra(EXTRA_PATH).orEmpty()
                Snackbar.make(
                    findViewById<View>(android.R.id.content),
                    getString(R.string.disk_image_created) + created,
                    Snackbar.LENGTH_SHORT
                ).show()
                refreshFiles()
            }

            requestCode == IMPORT_FILE_REQUEST && resultCode == RESULT_OK -> {
                data?.data?.let { importFile(it) }
            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun onItemClicked(file: String) {
        if (file == PARENT_DIR) {
            val idx = currentPath.lastIndexOf('/')
            // Keep the leading slash so the filesystem root stays "/" rather than "".
            getFiles(currentPath.substring(0, if (idx == 0) 1 else idx))
            return
        }
        if (File(file).isDirectory) {
            getFiles(file)
            return
        }
        intent.putExtra(EXTRA_PATH, file)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun eject() {
        val builder = AlertDialogUtil.createAlertDialog(
            this,
            R.string.app_name,
            R.drawable.warning_icon,
            R.string.alert_dialog_confirm_eject
        )
        builder.setPositiveButton(R.string.alert_dialog_ok) { _, _ ->
            AlertDialogUtil.dismissDialog(this)
            // A null path is what tells the caller to eject.
            intent.putExtra(EXTRA_PATH, null as String?)
            setResult(RESULT_OK, intent)
            finish()
        }
        builder.setNegativeButton(R.string.alert_dialog_cancel) { _, _ ->
            AlertDialogUtil.dismissDialog(this)
        }
        AlertDialogUtil.showDialog(this, builder)
    }

    @Suppress("DEPRECATION") // startActivityForResult/onActivityResult.
    private fun createDisk() {
        val intent = Intent(this, CreateDiskActivity::class.java)
        intent.putExtra(EXTRA_DIR, currentPath)
        startActivityForResult(intent, MKDISK_REQUEST)
    }

    @Suppress("DEPRECATION") // startActivityForResult/onActivityResult.
    private fun chooseFileToImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, IMPORT_FILE_REQUEST)
    }

    /** Copies the document [fileUri] points at into the directory currently shown. */
    private fun importFile(fileUri: Uri) {
        val fileName = getFileName(fileUri)
        if (fileName == null) {
            Log.w(TAG, "Cannot determine a file name for $fileUri. Not importing.")
            return
        }
        try {
            contentResolver.openInputStream(fileUri)?.use {
                writeFileToCurrentDir(it, fileName)
                refreshFiles()
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun queryDisplayName(uri: Uri): String? =
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use null
            }
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }

    private fun refreshFiles() = getFiles(currentPath)

    /** Shows the contents of [path], sorted, with an entry to navigate to the parent on top. */
    @SuppressLint("SetTextI18n")
    private fun getFiles(path: String) {
        currentPath = path
        pathLabel.text = pathPrefix + currentPath
        items.clear()
        File(path).listFiles()?.mapTo(items) { it.path }
        items.sort()
        if (path != "/") {
            items.add(0, PARENT_DIR)
        }
        fileListAdapter.notifyDataSetChanged()
    }

    private fun writeFileToCurrentDir(inputStream: InputStream, fileName: String) {
        val outPath = Paths.get(currentPath, fileName)
        val bytesCopied = Files.copy(inputStream, outPath, StandardCopyOption.REPLACE_EXISTING)
        Log.i(TAG, "Copied file of size $bytesCopied")
    }
}
