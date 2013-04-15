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

package org.puder.trs80.browser;

import java.io.File;
import java.util.List;

import org.puder.trs80.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class BrowserListViewAdapter extends ArrayAdapter<String> {

    Context context;

    public BrowserListViewAdapter(Context context, List<String> items) {
        super(context, 0, items);
        this.context = context;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TextView holder = null;
        String path = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.file_browser_item, null);
            holder = (TextView) convertView.findViewById(R.id.path);
            convertView.setTag(holder);
        } else {
            holder = (TextView) convertView.getTag();
        }

        ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
        if (new File(path).isDirectory()) {
            icon.setImageResource(R.drawable.folder_icon);
        } else {
            icon.setImageResource(R.drawable.file_icon);
        }

        int idx = path.lastIndexOf(File.separator);
        if (idx != -1) {
            path = path.substring(idx + 1);
        }
        holder.setText(path);

        return convertView;
    }
}