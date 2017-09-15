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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.common.base.Optional;

import org.puder.trs80.XTRS;
import org.puder.trs80.io.FileManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Persists the state of an emulator session so it can be resumed later.
 */
public class EmulatorState {
    private static final String FILE_SCREENSHOT = "screenshot.png";
    private static final String FILE_STATE = "state";
    private static final String FILE_CASSETTE = "cassette.cas";

    private final FileManager fileManager;

    public static EmulatorState forConfigId(
            int configurationID, FileManager.Creator fileManagerCreator) throws IOException {
        FileManager fileManager = fileManagerCreator.createForAppSubDir(configurationID);
        fileManager.ensureNoMedia();
        return new EmulatorState(fileManager);
    }

    private EmulatorState(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    private String getStateFileName() {
        return fileManager.getAbsolutePathForFile(FILE_STATE);
    }

    public String getDefaultCassettePath() {
        return fileManager.getAbsolutePathForFile(FILE_CASSETTE);
    }

    public void saveState() {
        XTRS.saveState(getStateFileName());
    }

    public void loadState() {
        XTRS.loadState(getStateFileName());
    }

    public boolean hasState() {
        // By default, a ".nomedia" file should be present.
        return fileManager.fileCount() > 1;
    }

    public void saveScreenshot(Bitmap screenshot) {
        if (screenshot == null) {
            // Can happen when NotImplementedException is thrown
            return;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // out = new FileOutputStream(getScreenshotFileName(configurationID));
            screenshot.compress(Bitmap.CompressFormat.PNG, 90, out);
            fileManager.writeFile(FILE_SCREENSHOT, out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Throwable ignore) {
            }
        }
    }

    public Bitmap loadScreenshot() {
        Optional<byte[]> screenshot = fileManager.readFile(FILE_SCREENSHOT);
        if (!screenshot.isPresent()) {
            return null;
        }
        byte[] data = screenshot.get();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public void deleteSavedState() {
        fileManager.deleteFile(FILE_STATE);
        fileManager.deleteFile(FILE_SCREENSHOT);
    }

    public void deleteAll() {
        fileManager.delete();
    }
}
