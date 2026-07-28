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

package org.retrostore.android

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.retrostore.android.net.DataFetcher
import org.retrostore.android.view.ImageLoader
import org.retrostore.client.common.proto.App
import java.util.Locale

private const val TAG = "DetailsActivity"

/**
 * Shows details about an app, and lets the user install it.
 */
class AppDetailsPageActivity : AppCompatActivity() {
    private lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        imageLoader = ImageLoader.get(applicationContext)

        val app = getAppFromIntent()
        if (app == null) {
            Log.w(TAG, "Finishing activity.")
            finish()
            return
        }
        fillViews(app)
    }

    private fun fillViews(app: App) {
        findViewById<TextView>(R.id.appName).text = app.name
        findViewById<TextView>(R.id.appDescription).apply {
            text = app.description
            movementMethod = ScrollingMovementMethod()
        }
        findViewById<TextView>(R.id.appAuthor).text = app.author
        findViewById<TextView>(R.id.appVersion).text =
                getString(R.string.app_version, app.version)
        app.screenshot_url.firstOrNull()?.let { url ->
            imageLoader.loadUrlIntoView(url, findViewById<ImageView>(R.id.appThumbnail))
        }
        findViewById<View>(R.id.installButton).setOnClickListener { onAskForInstallation(app) }
    }

    /** @return The app named by the launching intent, or null if it cannot be resolved. */
    private fun getAppFromIntent(): App? {
        val extras = intent?.extras
        if (extras == null) {
            Log.i(TAG, "No intent or no extras.")
            return null
        }
        val appId = extras.getString(EXTRA_APP_ID)
        if (appId == null) {
            Log.w(TAG, "No 'appId' given.")
            return null
        }

        // TODO: Make this work so that we instantiate a new fetcher if it got clean-up.
        val fetcher = DataFetcher.get()
        if (fetcher == null) {
            Log.w(TAG, "No data fetcher available.")
            return null
        }

        val app = fetcher.getFromCache(appId)
        if (app == null) {
            Log.w(TAG, "App not in cache.")
        }
        return app
    }

    private fun onAskForInstallation(app: App) {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            // Only the positive button does anything, the negative one just dismisses.
            if (which == DialogInterface.BUTTON_POSITIVE) {
                externalListener?.onInstallApp(app)
            }
        }

        val dialogText =
                String.format(Locale.getDefault(), getString(R.string.dialog_install), app.name)
        AlertDialog.Builder(this)
                .setMessage(dialogText)
                .setPositiveButton(R.string.dialog_yes_install, dialogClickListener)
                .setNegativeButton(R.string.dialog_no, dialogClickListener)
                .show()
    }

    companion object {
        /** Intent extra holding the ID of the app whose details are to be shown. */
        const val EXTRA_APP_ID = "app_id"

        private var externalListener: AppInstallListener? = null

        /**
         * Internal so only [RetrostoreActivity] can hand the listener through. Not to be used
         * directly by clients.
         */
        internal fun setAppInstallListener(listener: AppInstallListener?) {
            externalListener = listener
        }
    }
}
