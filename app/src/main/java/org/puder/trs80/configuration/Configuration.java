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

import android.util.SparseArray;

import com.google.common.base.Optional;

/**
 * Interface for a configuration.
 */
public interface Configuration {
    int getId();

    Optional<String> getName();

    void setName(String name);

    int getModel();

    void setModel(int model);

    Optional<String> getCassettePath();

    void setCasettePath(String path);

    Optional<String> getDiskPath(int disk);

    void setDiskPath(int disk, String path);

    SparseArray<String> getDiskPaths();

    void setDiskPaths(SparseArray<String> paths);

    float getCassettePosition();

    void setCassettePosition(float pos);

    Optional<KeyboardLayout> getKeyboardLayoutPortrait();

    void setKeyboardLayoutPortrait(KeyboardLayout layout);

    Optional<KeyboardLayout> getKeyboardLayoutLandscape();

    void setKeyboardLayoutLandscape(KeyboardLayout layout);

    int getCharacterColorAsRGB();

    void setCharacterColorAsRGB(int color);

    int getScreenColorAsRGB();

    void setScreenColorAsRGB(int color);

    boolean isSoundMuted();

    void setSoundMuted(boolean muted);

    void delete();

    /**
     * @return An in-memory copy of this configuration.
     */
    Configuration createBackup();
}
