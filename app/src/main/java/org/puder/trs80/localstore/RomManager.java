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

package org.puder.trs80.localstore;

import android.content.Context;
import android.content.SharedPreferences;

import org.puder.trs80.Hardware;
import org.puder.trs80.SettingsActivity;
import org.puder.trs80.StrUtil;
import org.puder.trs80.TRS80Application;
import org.puder.trs80.io.FileManager;

import java.io.File;
import java.io.IOException;

/**
 * Manages ROMs.
 */
public class RomManager {
    private static RomManager instance;

    private final SharedPreferences sharedPrefs;
    private final FileManager fileManager;

    /**
     * Adds a ROM to the local store.
     *
     * @param model    defines which model this entry is for. See {@link Hardware}.
     * @param filename the filename to use for the entry.
     * @param content  the byte content of the entry.
     * @return Whether the file was successfully added.
     */
    public boolean addRom(int model,
                          String filename,
                          byte[] content) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(getKeyFromModel(model), fileManager.getAbsolutePathForFile(filename));
        editor.apply();
        return fileManager.writeFile(filename, content);
    }

    public static RomManager init(FileManager.Creator fileManagerCreator) throws IOException {
        if (instance != null) {
            return instance;
        }
        SharedPreferences sharedPrefs = TRS80Application.getAppContext()
                .getSharedPreferences(SettingsActivity.SHARED_PREF_NAME,
                        Context.MODE_PRIVATE);
        instance = new RomManager(sharedPrefs, fileManagerCreator.forAppBaseDir());
        return instance;
    }

    /**
     * @return The singlton {@link RomManager} instance.
     */
    public static RomManager get() {
        if (instance == null) {
            throw new RuntimeException("Must call RomManager.init() first.");
        }
        return instance;
    }

    private RomManager(SharedPreferences sharedPrefs, FileManager fileManager) {
        this.sharedPrefs = sharedPrefs;
        this.fileManager = fileManager;
    }

    public boolean hasAllRoms() {
        return hasModel1ROM() && hasModel3ROM();
    }

    private boolean hasModel1ROM() {
        return checkIfFileExists(SettingsActivity.CONF_ROM_MODEL1);
    }

    private boolean hasModel3ROM() {
        return checkIfFileExists(SettingsActivity.CONF_ROM_MODEL3);
    }

    private boolean checkIfFileExists(String prop) {
        String filename = sharedPrefs.getString(prop, null);
        if (filename == null) {
            return false;
        }
        if (new File(filename).exists()) {
            return true;
        }
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.remove(prop);
        editor.apply();
        return false;
    }

    private String getKeyFromModel(int model) {
        switch (model) {
            case Hardware.MODEL1:
                return SettingsActivity.CONF_ROM_MODEL1;
            case Hardware.MODEL3:
                return SettingsActivity.CONF_ROM_MODEL3;
            case Hardware.MODEL4:
                return SettingsActivity.CONF_ROM_MODEL4;
            case Hardware.MODEL4P:
                return SettingsActivity.CONF_ROM_MODEL4P;
        }
        throw new IllegalArgumentException(StrUtil.form("Model %d not found" + ".", model));
    }
}
