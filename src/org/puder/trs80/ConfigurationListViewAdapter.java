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
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ConfigurationListViewAdapter extends ArrayAdapter<Configuration> {

    class Holder {
        TextView  name;
        TextView  model;
        TextView  disks;
        TextView  sound;
        TextView  keyboards;
        ImageView screenshot;
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
            holder.disks = (TextView) convertView.findViewById(R.id.configuration_disks);
            holder.sound = (TextView) convertView.findViewById(R.id.configuration_sound);
            holder.keyboards = (TextView) convertView.findViewById(R.id.configuration_keyboards);
            holder.screenshot = (ImageView) convertView.findViewById(R.id.configuration_screenshot);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        // Name
        holder.name.setText(conf.getName());

        // Hardware
        String model = "-";
        switch (conf.getModel()) {
        case Hardware.MODEL1:
            model = "Model I";
            break;
        case Hardware.MODEL3:
            model = "Model III";
            break;
        case Hardware.MODEL4:
            model = "Model 4";
            break;
        case Hardware.MODEL4P:
            model = "Model 4P";
            break;
        }
        holder.model.setText(model);

        // Disks
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (conf.getDiskPath(i) != null) {
                count++;
            }
        }
        holder.disks.setText(Integer.toString(count));

        // Sound
        holder.sound.setText(conf.muteSound() ? "disabled" : "enabled");

        // Keyboards
        String keyboards = getKeyboardLabel(conf.getKeyboardLayoutPortrait());
        keyboards += "/";
        keyboards += getKeyboardLabel(conf.getKeyboardLayoutLandscape());
        holder.keyboards.setText(keyboards);

        // Screenshot
        Bitmap screenshot = EmulatorState.loadScreenshot(conf.getId());
        if (screenshot != null) {
            holder.screenshot.setImageBitmap(screenshot);
            holder.screenshot.setVisibility(View.VISIBLE);
        } else {
            holder.screenshot.setVisibility(View.INVISIBLE);
        }
        return convertView;
    }

    private String getKeyboardLabel(int type) {
        switch (type) {
        case Configuration.KEYBOARD_LAYOUT_ORIGINAL:
            return "original";
        case Configuration.KEYBOARD_LAYOUT_COMPACT:
            return "compact";
        case Configuration.KEYBOARD_LAYOUT_GAMING_1:
            return "gaming";
        case Configuration.KEYBOARD_TILT:
            return "tilt";
        }
        return "-";
    }
}