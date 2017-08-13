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
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import org.puder.trs80.Configuration;
import org.puder.trs80.ConfigurationBackup;
import org.puder.trs80.Hardware;
import org.puder.trs80.R;
import org.puder.trs80.SettingsActivity;
import org.puder.trs80.StrUtil;
import org.puder.trs80.TRS80Application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class manages the installed ROMs.
 */
public class LocalStore {
    private static final String TAG = "LocalStore";
    /** Where all the ROMS are located. */
    private final File storePath;

    /** The singleton instance of the LocalStore. */
    private static LocalStore singleton;

    /**
     * Initialize the default instance of the local store. This should be done exactly ones.
     *
     * @param res application resources.
     * @throws IOException if the local store could not be initialized, or already had been
     *                     initialized.
     */
    public static void initDefault(Resources res) throws IOException {
        if (singleton != null) {
            throw new IOException("LocalStore already initialized.");
        }

        File sdcard = Environment.getExternalStorageDirectory();
        File localStoreDir = new File(sdcard, res.getString(R.string.trs80_dir));
        if (!localStoreDir.exists()) {
            if (!localStoreDir.mkdirs()) {
                throw new IOException(StrUtil.form("Cannot create local store directory: %s",
                        localStoreDir.getAbsolutePath()));
            }
        }
        singleton = new LocalStore(localStoreDir);
    }


    public static LocalStore getDefault() {
        return singleton;
    }

    private LocalStore(File storePath) {
        this.storePath = checkNotNull(storePath);
    }

    /**
     * Adds a new (non-ROM) entry to the local store.
     *
     * @param model      defines which model this entry is for. See {@link Hardware}.
     * @param configName the name of this new configuration.
     * @param filename   the filename to use for the entry.
     * @param content    the byte content of the entry.
     * @return Whether the file was successfully added.
     */
    public boolean addNewEntry(int model,
                               String configName,
                               String filename,
                               byte[] content) {
        ConfigurationBackup newConfig = new ConfigurationBackup(
                Configuration.newConfiguration());
        newConfig.setName(configName);
        newConfig.setModel(model);
        newConfig.setDiskPath(0, getPathForFile(filename));
        newConfig.save();
        return addNewEntry(filename, content);
    }

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
        SharedPreferences.Editor editor = getEditor();
        editor.putString(getKeyFromModel(model), getPathForFile(filename));
        editor.apply();
        return addNewEntry(filename, content);
    }

    private SharedPreferences.Editor getEditor() {
        SharedPreferences sharedPrefs = TRS80Application.getAppContext()
                .getSharedPreferences(SettingsActivity.SHARED_PREF_NAME,
                        Context.MODE_PRIVATE);
        return sharedPrefs.edit();
    }

    /**
     * Adds a new file to the local store.
     *
     * @param filename the filename to use for the entry.
     * @param content  the byte content of the new entry.
     * @return Whether the file was successfully added.
     */
    private boolean addNewEntry(String filename, byte[] content) {
        // TODO: ROMs should go into their own sub-directories to avoid conflict.
        File newFile = new File(storePath, filename);
        if (newFile.exists()) {
            // TODO: Should we override this in case there is an update?
            Log.i(TAG, StrUtil.form("File already exists: '%s'.", newFile.getAbsolutePath()));
            return false;
        }
        try {
            FileOutputStream out = new FileOutputStream(newFile);
            out.write(content);
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "Cannot write new ROM file.", e);
            return false;
        }
        return true;
    }

    /**
     * @return The absolute path for a file with the given name in the local store.
     */
    private String getPathForFile(String filename) {
        return new File(storePath, filename).getAbsolutePath();
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
