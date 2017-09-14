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

package org.puder.trs80.io;

import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;

import org.puder.trs80.R;
import org.puder.trs80.StrUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * File I/O functionality for the app.
 */
public class FileManager {
    private static final String TAG = "FileManager";
    private final File baseDir;

    /** Creator for FileManager instances. */
    public static class Creator {
        private final String appBaseDir;

        private Creator(String appBaseDir) {
            this.appBaseDir = appBaseDir;
        }

        public static Creator get(Resources res) {
            return new Creator(res.getString(R.string.trs80_dir));
        }

        /**
         * Initialize a file manager for the base directory in which the app stores data.
         */
        public FileManager forAppBaseDir() throws IOException {
            return createForAppSubDir(null);
        }

        public FileManager createForAppSubDir(int dirName) throws IOException {
            return createForAppSubDir(Integer.toString(dirName));
        }

        public FileManager createForAppSubDir(String dirName) throws IOException {
            File sdcard = Environment.getExternalStorageDirectory();
            File localStoreDir = new File(sdcard, appBaseDir);
            if (dirName != null) {
                localStoreDir = new File(localStoreDir, dirName);
            }
            if (!localStoreDir.exists()) {
                if (!localStoreDir.mkdirs()) {
                    throw new IOException(StrUtil.form("Cannot create local store directory: %s",
                            localStoreDir.getAbsolutePath()));
                }
            }
            return forBaseDir(localStoreDir);
        }

        /**
         * Initialize a file manager for the given base directory.
         */
        private FileManager forBaseDir(File baseDir) throws IOException {
            Preconditions.checkNotNull(baseDir);
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                throw new IOException("Cannot create dir: " + baseDir.getAbsolutePath());
            }
            return new FileManager(baseDir);
        }
    }

    private FileManager(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * @return The absolute name of a file with the given name within this manager's base path.
     */
    public String getAbsolutePathForFile(String filename) {
        return new File(baseDir, filename).getAbsolutePath();
    }

    /**
     * Ensures that there is a ".nomedia" file in the base dir of this manager.
     */
    public boolean ensureNoMedia() {
        File noMediaFile = new File(baseDir, ".nomedia");
        try {
            return noMediaFile.exists() || noMediaFile.createNewFile();
        } catch (IOException ex) {
            Log.e(TAG, "Cannot create nomedia file: " + noMediaFile.getAbsolutePath());
            return false;
        }
    }

    /**
     * Writes or overwrites a file within the base path of this manager.
     *
     * @param filename the filename to use for the entry.
     * @param content  the byte content of the new entry.
     * @return Whether the file was successfully added.
     */
    public boolean writeFile(String filename, byte[] content) {
        File newFile = new File(baseDir, filename);
        Log.i(TAG, "About to write to " + newFile.getAbsolutePath());
        try {
            FileOutputStream out = new FileOutputStream(newFile);
            out.write(content);
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "Cannot write file " + filename, e);
            return false;
        }
        return true;
    }

    /**
     * Reads a file within the base path of this manager.
     *
     * @param filename the filename to read
     * @return The contents of the file, if it could be read.
     */
    public Optional<byte[]> readFile(String filename) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            FileInputStream file = new FileInputStream(new File(baseDir, filename));
            ByteStreams.copy(file, bytes);
            return Optional.of(bytes.toByteArray());
        } catch (IOException e) {
            return Optional.absent();
        }
    }

    /** Deletes this directory and all contents. */
    public void delete() {
        String[] children = baseDir.list();
        // children should not be null, but we got an ACRA report saying
        // otherwise
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                new File(baseDir, children[i]).delete();
            }
        }
        baseDir.delete();
    }

    /**
     * @return How many files/directories are in the base dir of this manager.
     */
    public int fileCount() {
        return baseDir.exists() ? baseDir.list().length : 0;
    }
}
