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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.puder.trs80.configuration.ConfigurationManager
import org.puder.trs80.configuration.ConfigurationManager.ConfigMedia
import org.puder.trs80.io.FileDownloader
import org.puder.trs80.localstore.InitialDownloads
import org.puder.trs80.localstore.InitialDownloads.Download
import org.puder.trs80.localstore.RomManager
import org.retrostore.android.RetrostoreApi
import java.io.IOException

private const val TAG = "InitlStpFgrmnt"
private const val TUTORIAL_APP_ID = "2420f832-a7aa-11e7-8132-7343fef39a1f"

/**
 * Downloads the ROMs and the tutorial app that the emulator needs to be usable at all, showing a
 * progress dialog while it runs. Shown once, the first time the app is started.
 */
class InitialSetupDialogFragment : DialogFragment() {

    private val fileDownloader = FileDownloader()
    private val romManager: RomManager = RomManager.get()

    private lateinit var listener: DownloadCompletionListener
    private var downloadCounter = 0

    /**
     * The progress to show, held here because it is produced before the dialog exists and has
     * to survive the dialog being torn down and rebuilt.
     */
    private var progressMessage: String? = null

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        listener = activity as? DownloadCompletionListener
            ?: throw ClassCastException("$activity must implement DownloadCompletionListener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        ProgressDialog(requireContext()).apply {
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            // The first download is already under way by the time this runs, so adopt whatever
            // progress it has reported rather than coming up blank.
            progressMessage?.let { setMessage(it) }
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

        // The items are downloaded one after another, so progress can simply be reported as the
        // loop advances and completion is whatever follows it.
        val downloads = InitialDownloads.get()
        val totalDownloads = downloads.size + 1
        lifecycleScope.launch {
            for (item in downloads) {
                onDownloadProgress(++downloadCounter, totalDownloads)
                withContext(Dispatchers.IO) { download(item, configurationManager) }
            }
            onDownloadProgress(++downloadCounter, totalDownloads)
            appInstaller.downloadAndInstallApp(TUTORIAL_APP_ID)
            doneDownloading()
        }
    }

    /** Downloads the given item. */
    private fun download(item: Download, configurationManager: ConfigurationManager) {
        val data = fileDownloader.download(item.url, item.fileInZip)
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
            Log.i(TAG, "Adding configuration success: ${newConfig != null}")
        }
    }

    /**
     * Update download progress. Called from the main thread.
     *
     * [lifecycleScope] dispatches with `Dispatchers.Main.immediate`, so the download loop runs
     * synchronously until it first suspends — which means the first call here happens inside
     * [onCreate], before [onCreateDialog] has built the dialog. Later calls can also arrive
     * while the dialog is torn down. Either way the message is kept, and [onCreateDialog]
     * applies it once there is a dialog to apply it to.
     */
    private fun onDownloadProgress(num: Int, total: Int) {
        val message = getString(R.string.downloading, num, total)
        progressMessage = message
        (dialog as? ProgressDialog)?.setMessage(message)
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
