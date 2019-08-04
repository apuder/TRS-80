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
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tekle.oss.android.animation.AnimationFactory;

import org.puder.trs80.configuration.Configuration;
import org.puder.trs80.configuration.ConfigurationManager;
import org.puder.trs80.configuration.EmulatorState;
import org.puder.trs80.configuration.KeyboardLayout;
import org.puder.trs80.drag.ItemTouchHelperAdapter;

import java.io.IOException;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

public class ConfigurationListViewAdapter extends
        RecyclerView.Adapter<ConfigurationListViewAdapter.Holder> implements ItemTouchHelperAdapter {
    private static final String TAG = "ConfListViewAdapter";
    private final ConfigurationManager configurationManager;
    private ConfigurationItemListener listener;
    private int numColumns;


    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        configurationManager.moveConfiguration(fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {

    }


    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public boolean draggable;
        Configuration configuration;
        TextView nameFront;
        TextView nameBack;
        TextView model;
        TextView disks;
        TextView cassette;
        TextView sound;
        TextView keyboardPortrait;
        TextView keyboardLandscape;
        ScreenshotView screenshot;
        ViewFlipper viewFlipper;
        View stopButton;


        Holder(View itemView) {
            super(itemView);
            if (!(itemView instanceof RelativeLayout)) {
                // Footers are not draggable
                draggable = false;
                return;
            }
            draggable = true;
            nameFront = (TextView) itemView.findViewById(R.id.configuration_name_front);
            nameBack = (TextView) itemView.findViewById(R.id.configuration_name_back);
            model = (TextView) itemView.findViewById(R.id.configuration_model);
            disks = (TextView) itemView.findViewById(R.id.configuration_disks);
            cassette = (TextView) itemView.findViewById(R.id.configuration_cassette);
            sound = (TextView) itemView.findViewById(R.id.configuration_sound);
            keyboardPortrait = (TextView) itemView.findViewById(R.id.configuration_keyboard_portrait);
            keyboardLandscape = (TextView) itemView.findViewById(R.id.configuration_keyboard_landscape);
            screenshot = (ScreenshotView) itemView.findViewById(R.id.configuration_screenshot);
            viewFlipper = (ViewFlipper) itemView.findViewById(R.id.configuration_view_flipper);
            viewFlipper.setDisplayedChild(0);
            itemView.setOnClickListener(this);
            itemView.findViewById(R.id.configuration_info).setOnClickListener(this);
            itemView.findViewById(R.id.configuration_back).setOnClickListener(this);
            itemView.findViewById(R.id.configuration_edit).setOnClickListener(this);
            itemView.findViewById(R.id.configuration_delete).setOnClickListener(this);
            itemView.findViewById(R.id.configuration_run).setOnClickListener(this);
            stopButton = itemView.findViewById(R.id.configuration_stop);
            stopButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (position == NO_POSITION) {
                return;
            }
            switch (v.getId()) {
                case R.id.configuration_info:
                    if (listener.showHint()) {
                        break;
                    }
                case R.id.configuration_back:
                    AnimationFactory.flipTransition(viewFlipper,
                            AnimationFactory.FlipDirection.LEFT_RIGHT);
                    break;
                case R.id.configuration_edit:
                    listener.onConfigurationEdit(configuration, position);
                    break;
                case R.id.configuration_stop:
                    listener.onConfigurationStop(configuration, position);
                    break;
                case R.id.configuration_delete:
                    listener.onConfigurationDelete(configuration, position);
                    break;
                case R.id.configuration_run:
                    listener.onConfigurationRun(configuration, position);
                    break;
                default:
                    if (listener.showHint()) {
                        break;
                    }
                    if (viewFlipper.getDisplayedChild() == 0) {
                        listener.onConfigurationRun(configuration, position);
                    }
                    break;
            }
        }
    }


    ConfigurationListViewAdapter(ConfigurationManager configurationManager,
                                 ConfigurationItemListener listener, int numColumns) {
        this.configurationManager = Preconditions.checkNotNull(configurationManager);
        this.listener = Preconditions.checkNotNull(listener);
        this.numColumns = numColumns;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(viewType, viewGroup, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(final Holder holder, int position) {
        if (position >= configurationManager.getConfigCount()){
            return;
        }

        Configuration conf = configurationManager.getConfig(position);
        if (holder.getAdapterPosition() != position && holder.viewFlipper.getDisplayedChild() != 0) {
            Context context = TRS80Application.getAppContext();
            holder.viewFlipper.setInAnimation(context, R.anim.no_animation);
            holder.viewFlipper.setOutAnimation(context, R.anim.no_animation);
            holder.viewFlipper.setDisplayedChild(0);
        }
        EmulatorState emulatorState;
        try {
            emulatorState = configurationManager.getEmulatorState(conf.getId());
        } catch (IOException e) {
            Log.e(TAG, "Cannot create emulator state.", e);
            return;
        }

        holder.stopButton.setVisibility(emulatorState.hasState() ? View.VISIBLE : View.GONE);

        // ConfigurationOld
        holder.configuration = conf;

        // Name
        Optional<String> name = conf.getName();
        if (name.isPresent()) {
            holder.nameFront.setText(name.get());
            holder.nameBack.setText(name.get());
        }

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
            if (!Strings.isNullOrEmpty(conf.getDiskPath(i).orNull())) {
                count++;
            }
        }
        holder.disks.setText(Integer.toString(count));

        // Cassette
        holder.cassette.setText(conf.getCassettePosition() > 0 ? R.string.cassette_not_rewound
                : R.string.cassette_rewound);

        // Sound
        holder.sound.setText(conf.isSoundMuted() ? R.string.sound_disabled : R.string.sound_enabled);

        // Keyboard portrait
        holder.keyboardPortrait.setText(getKeyboardLabel(conf.getKeyboardLayoutPortrait().get()));

        // Keyboard landscape
        holder.keyboardLandscape.setText(getKeyboardLabel(conf.getKeyboardLayoutLandscape().get()));

        // Screenshot
        holder.screenshot.setScreenshotBitmap(null);
        new AsyncTask<EmulatorState, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(EmulatorState... params) {
                return params[0].loadScreenshot();
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                holder.screenshot.setScreenshotBitmap(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, emulatorState);
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= configurationManager.getConfigCount()) {
            return R.layout.configuration_footer;
        }
        return R.layout.configuration_item;
    }

    @Override
    public int getItemCount() {
        /*
         * Return the number of configurations plus the number of columns in the current
         * GridLayout. This will guarantee that at least the last configuration_footer will
         * spill over to the next row.
         */
        return configurationManager.getConfigCount() + numColumns;
    }

    private String getKeyboardLabel(KeyboardLayout type) {
        final String defaultLabel = "-";
        if (type == null) {
            return defaultLabel;
        }
        try {
            return TRS80Application.getAppContext().getString(getKeyboardResource(type));
        } catch (IllegalArgumentException ex) {
            return defaultLabel;
        }
    }

    private int getKeyboardResource(KeyboardLayout layout) {
        switch (layout) {
            case KEYBOARD_LAYOUT_ORIGINAL:
                return R.string.keyboard_abbrev_original;
            case KEYBOARD_LAYOUT_COMPACT:
                return R.string.keyboard_abbrev_compact;
            case KEYBOARD_LAYOUT_JOYSTICK:
                return R.string.keyboard_abbrev_joystick;
            case KEYBOARD_GAME_CONTROLLER:
                return R.string.keyboard_abbrev_game_controller;
            case KEYBOARD_TILT:
                return R.string.keyboard_abbrev_tilt;
            default:
            case KEYBOARD_EXTERNAL:
                throw new IllegalArgumentException();
        }
    }
}