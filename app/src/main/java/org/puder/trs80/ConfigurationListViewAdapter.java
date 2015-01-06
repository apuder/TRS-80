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

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ConfigurationListViewAdapter extends
        RecyclerView.Adapter<ConfigurationListViewAdapter.Holder> {

    private ConfigurationMenuListener listener;


    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public int            position;
        public TextView       name;
        public TextView       model;
        public TextView       disks;
        public TextView       sound;
        public TextView       keyboards;
        public ScreenshotView screenshot;

        private View          menu;


        public Holder(View itemView) {
            super(itemView);
            if (!(itemView instanceof CardView)) {
                return;
            }
            name = (TextView) itemView.findViewById(R.id.configuration_name);
            model = (TextView) itemView.findViewById(R.id.configuration_model);
            disks = (TextView) itemView.findViewById(R.id.configuration_disks);
            sound = (TextView) itemView.findViewById(R.id.configuration_sound);
            keyboards = (TextView) itemView.findViewById(R.id.configuration_keyboards);
            screenshot = (ScreenshotView) itemView.findViewById(R.id.configuration_screenshot);
            menu = itemView.findViewById(R.id.configuration_menu);
            menu.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.configuration_menu:
                listener.onConfigurationMenuClicked(menu, position);
                break;
            default:
                listener.onConfigurationSelected(position);
                break;
            }
        }
    }


    public ConfigurationListViewAdapter(ConfigurationMenuListener listener) {
        this.listener = listener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(viewType,
                viewGroup, false);
        Holder vh = new Holder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        if (position == 0) {
            return;
        }
        Configuration conf = Configuration.getConfiguration(position - 1);

        // Position
        holder.position = position - 1;

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

        Context context = TRS80Application.getAppContext();
        // Sound
        holder.sound.setText(conf.muteSound() ? context.getString(R.string.sound_disabled)
                : context.getString(R.string.sound_enabled));

        // Keyboards
        String keyboards = getKeyboardLabel(conf.getKeyboardLayoutPortrait());
        keyboards += "/";
        keyboards += getKeyboardLabel(conf.getKeyboardLayoutLandscape());
        holder.keyboards.setText(keyboards);

        // Screenshot
        Bitmap screenshot = EmulatorState.loadScreenshot(conf.getId());
        holder.screenshot.setScreenshotBitmap(screenshot);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? R.layout.configuration_header : R.layout.configuration_item;
    }

    @Override
    public int getItemCount() {
        return Configuration.getCount() + 1 /* for header */;
    }

    private String getKeyboardLabel(int type) {
        int id;
        switch (type) {
        case Configuration.KEYBOARD_LAYOUT_ORIGINAL:
            id = R.string.keyboard_abbrev_original;
            break;
        case Configuration.KEYBOARD_LAYOUT_COMPACT:
            id = R.string.keyboard_abbrev_compact;
            break;
        case Configuration.KEYBOARD_LAYOUT_JOYSTICK:
            id = R.string.keyboard_abbrev_joystick;
            break;
        case Configuration.KEYBOARD_TILT:
            id = R.string.keyboard_abbrev_tilt;
            break;
        default:
            return "-";
        }
        return TRS80Application.getAppContext().getString(id);
    }
}