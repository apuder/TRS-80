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

package org.puder.trs80.configuration;

import android.util.SparseArray;

import com.google.common.base.Optional;

/**
 * An immutable in-memory copy of a configuration.
 */
final class ConfigurationBackup implements Configuration {
    private static final String CONFIG_BACKUP_IS_IMMUTABLE = "ConfigurationBackup is immutable";

    private final int id;
    private final String name;
    private final int model;
    private final String casettePath;
    private final SparseArray<String> diskPaths;
    private final float casettePosition;
    private final KeyboardLayout kbLayoutPortrait;
    private final KeyboardLayout kbLayoutLandscape;
    private final int charColor;
    private final int charColorRgb;
    private final int screenColor;
    private final boolean soundMuted;

    static Configuration from(Configuration orig) {
        SparseArray<String> diskPaths = new SparseArray<>(4);
        for (int i = 0; i < 4; ++i) {
            diskPaths.put(i, orig.getDiskPath(i).orNull());
        }
        return new ConfigurationBackup(
                orig.getId(),
                orig.getName().orNull(),
                orig.getModel(),
                orig.getCassettePath().orNull(),
                diskPaths,
                orig.getCassettePosition(),
                orig.getKeyboardLayoutPortrait().orNull(),
                orig.getKeyboardLayoutLandscape().orNull(),
                orig.getCharacterColor(),
                orig.getCharacterColorAsRGB(),
                orig.getScreenColorAsRGB(),
                orig.isSoundMuted());
    }

    private ConfigurationBackup(int id, String name, int model, String casettePath,
                                SparseArray<String> diskPaths, float casettePosition,
                                KeyboardLayout kbLayoutPortrait, KeyboardLayout kbLayoutLandscape,
                                int charColor, int charColorRgb, int screenColor,
                                boolean soundMuted) {
        this.id = id;
        this.name = name;
        this.model = model;
        this.casettePath = casettePath;
        this.diskPaths = diskPaths;
        this.casettePosition = casettePosition;
        this.kbLayoutPortrait = kbLayoutPortrait;
        this.kbLayoutLandscape = kbLayoutLandscape;
        this.charColor = charColor;
        this.charColorRgb = charColorRgb;
        this.screenColor = screenColor;
        this.soundMuted = soundMuted;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Optional<String> getName() {
        return Optional.fromNullable(name);
    }

    @Override
    public void setName(String name) {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public int getModel() {
        return model;
    }

    @Override
    public void setModel(int model) {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public Optional<String> getCassettePath() {
        return Optional.fromNullable(casettePath);
    }

    @Override
    public void setCassettePath(String path) {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public Optional<String> getDiskPath(int disk) {
        return Optional.fromNullable(diskPaths.get(disk));
    }

    @Override
    public void setDiskPath(int disk, String path) {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public SparseArray<String> getDiskPaths() {
        return diskPaths;
    }

    @Override
    public void setDiskPaths(SparseArray<String> paths) {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public float getCassettePosition() {
        return casettePosition;
    }

    @Override
    public void setCassettePosition(float pos) {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public void setKeyboardLayoutPortrait(KeyboardLayout layout) {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public Optional<KeyboardLayout> getKeyboardLayoutPortrait() {
        return Optional.fromNullable(kbLayoutPortrait);
    }

    @Override
    public void setKeyboardLayoutLandscape(KeyboardLayout layout) {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public Optional<KeyboardLayout> getKeyboardLayoutLandscape() {
        return Optional.fromNullable(kbLayoutLandscape);
    }

    @Override
    public int getCharacterColorAsRGB() {
        return charColorRgb;
    }

    @Override
    public int getCharacterColor() {
        return charColor;
    }

    @Override
    public void setCharacterColor(int color) {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public int getScreenColorAsRGB() {
        return screenColor;
    }

    @Override
    public void setScreenColorAsRGB(int color) {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public boolean isSoundMuted() {
        return soundMuted;
    }

    @Override
    public void setSoundMuted(boolean muted) {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public void delete() {
        throw new IllegalAccessError(CONFIG_BACKUP_IS_IMMUTABLE);
    }

    @Override
    public Configuration createBackup() {
        throw new IllegalAccessError("ConfigurationBackup is already a backup.");
    }
}
