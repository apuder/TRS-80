/*
 * Copyright 2017, Sascha Haeberling
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

package org.puder.trs80.configuration;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseArray;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.puder.trs80.Hardware;

/**
 * Represents a single configuration
 */
class ConfigurationImpl implements Configuration {
    private static final int NUM_DISKS = 4;

    private final int id;
    private final ConfigurationPersistence persistence;

    static Configuration fromId(int id, Context context) {
        return new ConfigurationImpl(id, ConfigurationPersistence.forId(id, context));
    }

    private ConfigurationImpl(int id, ConfigurationPersistence persistence) {
        this.id = id;
        this.persistence = Preconditions.checkNotNull(persistence);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Optional<String> getName() {
        return persistence.getName();
    }

    @Override
    public void setName(String name) {
        persistence.setName(name);
    }

    @Override
    public int getModel() {
        // TODO: This should return an enum.
        Optional<String> model = persistence.getModel();
        if (!model.isPresent()) {
            return Hardware.MODEL_NONE;
        }
        switch (Integer.parseInt(model.get())) {
            case 1:
                return Hardware.MODEL1;
            case 3:
                return Hardware.MODEL3;
            case 4:
                return Hardware.MODEL4;
            case 5:
                return Hardware.MODEL4P;
        }
        return Hardware.MODEL_NONE;
    }

    @Override
    public void setModel(int model) {
        if (model != 1 && model != 3 && model != 4 && model != 5) {
            persistence.setModel(null);
        } else {
            persistence.setModel(String.valueOf(model));
        }
    }

    @Override
    public Optional<String> getCassettePath() {
        return persistence.getCasettePath();
    }

    @Override
    public void setCasettePath(String path) {
        persistence.setCasettePath(path);
    }

    @Override
    public Optional<String> getDiskPath(int disk) {
        return persistence.getDiskPath(disk);
    }

    @Override
    public void setDiskPath(int disk, String path) {
        persistence.setDiskPath(disk, path);
    }

    @Override
    public SparseArray<String> getDiskPaths() {
        SparseArray<String> paths = new SparseArray<>();
        for (int i = 0; i < NUM_DISKS; ++i) {
            paths.put(i, persistence.getDiskPath(i).orNull());
        }
        return paths;
    }

    @Override
    public void setDiskPaths(SparseArray<String> paths) {
        for (int i = 0; i < paths.size(); ++i) {
            persistence.setDiskPath(i, paths.get(i));
        }
    }

    @Override
    public float getCassettePosition() {
        return persistence.getCassettePosition(0);
    }

    @Override
    public void setCassettePosition(float pos) {
        persistence.setCassettePosition(pos);
    }

    @Override
    public void setKeyboardLayoutPortrait(KeyboardLayout layout) {
        persistence.setKeyboardLayoutPortrait(layout.id);
    }

    @Override
    public Optional<KeyboardLayout> getKeyboardLayoutPortrait() {
        return KeyboardLayout.fromId(persistence.getKeyboardLayoutPortrait());
    }

    @Override
    public void setKeyboardLayoutLandscape(KeyboardLayout layout) {
        persistence.setKeyboardLayoutLandscape(layout.id);
    }

    @Override
    public Optional<KeyboardLayout> getKeyboardLayoutLandscape() {
        return KeyboardLayout.fromId(persistence.getKeyboardLayoutLandscape());
    }

    @Override
    public int getCharacterColorAsRGB() {
        int c = persistence.getCharacterColor(0);
        switch (c) {
            case 0:
                return Color.GREEN;
            default:
                return Color.WHITE;
        }
    }

    @Override
    public void setCharacterColorAsRGB(int color) {
        persistence.setCharacterColor(color);
    }

    @Override
    public int getScreenColorAsRGB() {
        return Color.DKGRAY;
    }

    @Override
    public void setScreenColorAsRGB(int color) {
        // TODO: Seems like we hard-coded a color for now. Make this persist if we care.
    }

    @Override
    public boolean isSoundMuted() {
        return persistence.isSoundMuted();
    }

    @Override
    public void setSoundMuted(boolean muted) {
        persistence.setSoundMuted(muted);
    }

    @Override
    public void delete() {
        persistence.clear();
    }

    @Override
    public Configuration createBackup() {
        return ConfigurationBackup.from(this);
    }
}
