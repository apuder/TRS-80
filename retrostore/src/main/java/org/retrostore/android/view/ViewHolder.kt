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

package org.retrostore.android.view

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.retrostore.android.R
import org.retrostore.android.RetrostoreActivity.InternalAppInstallListener
import org.retrostore.client.common.proto.App

/**
 * View holder for the items in the main apps list recycler view.
 */
class ViewHolder internal constructor(
        private val imageLoader: ImageLoader,
        appEntry: ViewGroup) : RecyclerView.ViewHolder(appEntry) {

    private val appNameView: TextView = itemView.findViewById(R.id.appName)
    private val appDescriptionView: TextView = itemView.findViewById(R.id.appDescription)
    private val authorView: TextView = itemView.findViewById(R.id.appAuthor)
    private val versionView: TextView = itemView.findViewById(R.id.appVersion)
    private val thumbnailView: ImageView = itemView.findViewById(R.id.appThumbnail)

    /** Binds [app] to this holder and notifies [installListener] when the item is tapped. */
    internal fun setData(app: App, installListener: InternalAppInstallListener) {
        appNameView.text = app.name
        appDescriptionView.text = app.description
        authorView.text = app.author
        versionView.text = itemView.resources.getString(R.string.app_version, app.version)
        val screenshotUrl = app.screenshot_url.firstOrNull()
        if (screenshotUrl != null) {
            imageLoader.loadUrlIntoView(screenshotUrl, thumbnailView)
            // TODO: Add an icon to indicate that loading failed.
        } else {
            // TODO: Display something indicating that there are no screenshots.
        }
        itemView.setOnClickListener { installListener.onInstallApp(app) }
    }
}
