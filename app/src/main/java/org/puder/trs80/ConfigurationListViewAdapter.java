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

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.puder.trs80.drag.ItemTouchHelperAdapter;
import org.puder.trs80.drag.OnStartDragListener;

public class ConfigurationListViewAdapter extends
        RecyclerView.Adapter<ConfigurationListViewAdapter.Holder> implements ItemTouchHelperAdapter {

    private ConfigurationMenuListener listener;
    private final OnStartDragListener mDragStartListener;

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Configuration.move(fromPosition - 1, fromPosition - 1);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {

    }


    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public int            position;
        public Configuration  configuration;
        public TextView       name;
        public TextView       model;
        public TextView       disks;
        public TextView       cassette;
        public TextView       sound;
        public TextView       keyboards;
        public ScreenshotView screenshot;
        public ImageView      reorderHandle;

        private View          menu;


        public Holder(View itemView) {
            super(itemView);
            if (!(itemView instanceof CardView)) {
                return;
            }
            name = (TextView) itemView.findViewById(R.id.configuration_name);
            model = (TextView) itemView.findViewById(R.id.configuration_model);
            disks = (TextView) itemView.findViewById(R.id.configuration_disks);
            cassette = (TextView) itemView.findViewById(R.id.configuration_cassette);
            sound = (TextView) itemView.findViewById(R.id.configuration_sound);
            keyboards = (TextView) itemView.findViewById(R.id.configuration_keyboards);
            screenshot = (ScreenshotView) itemView.findViewById(R.id.configuration_screenshot);
            reorderHandle = (ImageView) itemView.findViewById(R.id.configuration_reorder);
            menu = itemView.findViewById(R.id.configuration_menu);
            menu.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.configuration_menu:
                listener.onConfigurationMenuClicked(menu, configuration, position);
                break;
            default:
                listener.onConfigurationSelected(configuration, position);
                break;
            }
        }
    }


    public ConfigurationListViewAdapter(ConfigurationMenuListener listener, OnStartDragListener dragListener) {
        this.listener = listener;
        this.mDragStartListener = dragListener;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(viewType, viewGroup, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(final Holder holder, int position) {
        if (position == 0 || position == Configuration.getCount() + 1) {
            return;
        }
        Configuration conf = Configuration.getNthConfiguration(position - 1);

        holder.reorderHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            }
        });

        // Position
        holder.position = position;

        // Configuration
        holder.configuration = conf;

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

        // Cassette
        holder.cassette.setText(conf.getCassettePosition() > 0 ? R.string.cassette_not_rewound
                : R.string.cassette_rewound);

        // Sound
        holder.sound.setText(conf.muteSound() ? R.string.sound_disabled : R.string.sound_enabled);

        // Keyboards
        String keyboards = getKeyboardLabel(conf.getKeyboardLayoutPortrait());
        keyboards += "/";
        keyboards += getKeyboardLabel(conf.getKeyboardLayoutLandscape());
        holder.keyboards.setText(keyboards);

        // Screenshot
        holder.screenshot.setScreenshotBitmap(null);
        new AsyncTask<Integer, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(Integer... params) {
                return EmulatorState.loadScreenshot(params[0]);
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                holder.screenshot.setScreenshotBitmap(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, conf.getId());
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return R.layout.configuration_header;
        }
        if (position == Configuration.getCount() + 1) {
            return R.layout.configuration_footer;
        }
        return R.layout.configuration_item;
    }

    @Override
    public int getItemCount() {
        return Configuration.getCount() + 2 /* header  + footer */;
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
        case Configuration.KEYBOARD_GAME_CONTROLLER:
            id = R.string.keyboard_abbrev_game_controller;
            break;
        default:
            return "-";
        }
        return TRS80Application.getAppContext().getString(id);
    }
}