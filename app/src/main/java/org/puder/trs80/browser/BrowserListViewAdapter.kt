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
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import org.puder.trs80.R
import java.io.File

/**
 * Shows a list of absolute file paths as their file names, each with a folder or file icon.
 *
 * The adapter keeps the [items] list it is given, so the browser can refresh the list by
 * mutating it and calling `notifyDataSetChanged()`.
 */
class BrowserListViewAdapter(context: Context, items: List<String>) :
    ArrayAdapter<String>(context, 0, items) {

    @SuppressLint("InflateParams") // No parent to resolve layout params against at this point.
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.file_browser_item, null)
            .apply { tag = findViewById<TextView>(R.id.path) }

        // Never null: the browser only ever hands the adapter non-null paths.
        val path = checkNotNull(getItem(position))
        view.findViewById<ImageView>(R.id.icon).setImageResource(
            if (File(path).isDirectory) R.drawable.folder_icon else R.drawable.file_icon
        )
        (view.tag as TextView).text = path.substringAfterLast(File.separatorChar)
        return view
    }
}
