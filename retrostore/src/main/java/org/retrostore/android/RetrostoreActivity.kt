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

package org.retrostore.android

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import org.retrostore.ApiException
import org.retrostore.RetrostoreClient
import org.retrostore.android.net.DataFetcher
import org.retrostore.android.view.ImageLoader
import org.retrostore.android.view.ViewAdapter
import org.retrostore.client.common.proto.App

/**
 * Shows the apps available in the RetroStore, and lets the user open one for installation.
 */
class RetrostoreActivity : AppCompatActivity() {
    private val appInstallListener = InternalAppInstallListener { app -> showDetailsPage(app.id) }

    private lateinit var fetcher: DataFetcher
    private lateinit var imageLoader: ImageLoader
    private lateinit var recyclerView: RecyclerView
    private lateinit var refreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_retrostore)
        fetcher = DataFetcher.initialize(RetrostoreClient.default)
        imageLoader = ImageLoader.get(applicationContext)
        recyclerView = findViewById<RecyclerView>(R.id.appList).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@RetrostoreActivity)
        }
        refreshLayout = findViewById<SwipeRefreshLayout>(R.id.refresh_layout).apply {
            setOnRefreshListener { refreshApps() }
        }
        refreshApps()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_refresh -> {
            refreshApps()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showDetailsPage(appId: String) {
        AppDetailsPageActivity.setAppInstallListener(externalListener)
        startActivity(Intent(this, AppDetailsPageActivity::class.java)
                .putExtra(AppDetailsPageActivity.EXTRA_APP_ID, appId))
    }

    /**
     * Reloads the list of apps. The request runs in [lifecycleScope], so it is cancelled if the
     * activity goes away while it is still in flight, and the UI updates below happen on the main
     * thread without any manual hopping.
     */
    private fun refreshApps() {
        setRefreshingStatus(true)
        lifecycleScope.launch {
            try {
                onAppsReceived(fetcher.getAppsAsync())
            } catch (e: ApiException) {
                showToast("Something went wrong during the request: ${e.message}")
            }
            setRefreshingStatus(false)
        }
    }

    /** Apps list received. Display in list. */
    private fun onAppsReceived(apps: List<App>) {
        recyclerView.adapter = ViewAdapter(imageLoader, apps, appInstallListener)
    }

    private fun setRefreshingStatus(refreshing: Boolean) {
        refreshLayout.isRefreshing = refreshing
    }

    /** Shows a toast with the given [message]. */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /** Notified when the user picks an app from the list. */
    fun interface InternalAppInstallListener {
        fun onInstallApp(app: App)
    }

    companion object {
        private var externalListener: AppInstallListener? = null

        /**
         * Internal so only the RetroStore API can access this. Not to be used directly by clients.
         */
        internal fun setAppInstallListener(listener: AppInstallListener) {
            externalListener = listener
        }
    }
}
