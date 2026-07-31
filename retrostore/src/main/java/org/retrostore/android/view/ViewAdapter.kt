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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.retrostore.android.R
import org.retrostore.android.RetrostoreActivity.InternalAppInstallListener
import org.retrostore.client.common.proto.App

/**
 * Adapter for main app list view.
 *
 * @param imageLoader loads the app thumbnails.
 * @param appList the apps to show, in the order they are shown.
 * @param installListener notified when the user taps one of the apps.
 */
class ViewAdapter(
        private val imageLoader: ImageLoader,
        private val appList: List<App>,
        private val installListener: InternalAppInstallListener) :
        RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val appEntry = LayoutInflater.from(parent.context)
                .inflate(R.layout.app_item, parent, false) as ViewGroup
        return ViewHolder(imageLoader, appEntry)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(appList[position], installListener)
    }

    override fun getItemCount(): Int = appList.size
}
