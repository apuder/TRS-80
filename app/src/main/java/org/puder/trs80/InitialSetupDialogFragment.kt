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

// ProgressDialog, onAttach(Activity) and setRetainInstance() are deprecated. They are kept
// because replacing them changes what the user sees or when the fragment is re-created.
@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package org.puder.trs80

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import org.puder.trs80.async.UiExecutor
import org.puder.trs80.configuration.ConfigurationManager
import org.puder.trs80.configuration.ConfigurationManager.ConfigMedia
import org.puder.trs80.io.FileDownloader
import org.puder.trs80.localstore.InitialDownloads
import org.puder.trs80.localstore.InitialDownloads.Download
import org.puder.trs80.localstore.RomManager
import org.retrostore.android.RetrostoreApi
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val TAG = "InitlStpFgrmnt"
private const val TUTORIAL_APP_ID = "2420f832-a7aa-11e7-8132-7343fef39a1f"

/**
 * Downloads the ROMs and the tutorial app that the emulator needs to be usable at all, showing a
 * progress dialog while it runs. Shown once, the first time the app is started.
 */
class InitialSetupDialogFragment : DialogFragment() {

    private val fileDownloader = FileDownloader()
    private val downloadExecutor: Executor = Executors.newSingleThreadExecutor()
    private val uiExecutor: Executor = UiExecutor.create()
    private val romManager: RomManager = RomManager.get()

    private lateinit var listener: DownloadCompletionListener
    private var downloadCounter = 0

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        listener = activity as? DownloadCompletionListener
            ?: throw ClassCastException("$activity must implement DownloadCompletionListener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        ProgressDialog(requireContext()).apply {
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
        }

    override fun onDestroyView() {
        /*
         * https://code.google.com/p/android/issues/detail?id=17423
         */
        if (retainInstance) {
            dialog?.setDismissMessage(null)
        }
        super.onDestroyView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        retainInstance = true

        val configurationManager = try {
            ConfigurationManager.get(requireContext())
        } catch (e: IOException) {
            Log.e(TAG, "Cannot create ConfigurationManager: ${e.message}")
            return
        }
        val appInstaller = AppInstaller(configurationManager, RetrostoreApi.get())

        // Initialize the downloads of all initial items.
        val downloads = InitialDownloads.get()
        val totalDownloads = downloads.size + 1
        for (item in downloads) {
            downloadExecutor.execute {
                onDownloadProgress(++downloadCounter, totalDownloads)
                download(item, configurationManager)
            }
        }

        // Download and install the tutorial.
        downloadExecutor.execute {
            onDownloadProgress(++downloadCounter, totalDownloads)
            appInstaller.downloadAndInstallApp(TUTORIAL_APP_ID)
        }

        // Since the download executor is a single threaded one, we add another task that will
        // let us know when we're done.
        downloadExecutor.execute { uiExecutor.execute { doneDownloading() } }
    }

    /** Downloads the given item. */
    private fun download(item: Download, configurationManager: ConfigurationManager) {
        val data = fileDownloader.download(item.url, item.fileInZip).orNull()
        if (data == null) {
            Log.e(TAG, StrUtil.form("Could not load data for '%s'.", item.url))
            return
        }
        // Add a new ROM or entry.
        if (item.isROM) {
            val success = romManager.addRom(item.model, item.destinationFilename, data)
            Log.i(TAG, "Adding ROM success: $success")
        } else {
            val newConfig = configurationManager.addNewConfiguration(
                item.model,
                item.configurationName,
                listOf(ConfigMedia(item.destinationFilename, data)),
                null /* No cassette */
            )
            Log.i(TAG, "Adding configuration success: ${newConfig.isPresent}")
        }
    }

    /** Update download progress. */
    private fun onDownloadProgress(num: Int, total: Int) = uiExecutor.execute {
        (dialog as ProgressDialog).setMessage(getString(R.string.downloading, num, total))
    }

    /** Called on the main thread when downloading is done. */
    private fun doneDownloading() {
        dismissAllowingStateLoss()
        if (romManager.hasAllRoms()) {
            listener.onDownloadCompleted()
        } else {
            val root = requireActivity().findViewById<View>(R.id.main)
            Snackbar.make(root, R.string.roms_download_failure_msg, Snackbar.LENGTH_LONG).show()
        }
    }

    /** Implemented by the activity that hosts this fragment. */
    interface DownloadCompletionListener {
        /** Called once all the initial downloads have completed successfully. */
        fun onDownloadCompleted()
    }
}
