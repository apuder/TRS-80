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

package org.puder.trs80;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ConfigurationListViewAdapter extends ArrayAdapter<Configuration> {

    class Holder {
        TextView name;
        TextView model;
    }

    Context context;

    public ConfigurationListViewAdapter(Context context, List<Configuration> items) {
        super(context, 0, items);
        this.context = context;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder = new Holder();
        Configuration conf = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.configuration_item, null);
            holder.name = (TextView) convertView.findViewById(R.id.configuration_name);
            holder.model = (TextView) convertView.findViewById(R.id.configuration_model);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.name.setText(conf.getName());

        return convertView;
    }
}