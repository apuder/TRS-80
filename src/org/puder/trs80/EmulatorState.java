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

import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

public class EmulatorState {

    private static String getBaseDir(int configurationID) {
        File sdcard = Environment.getExternalStorageDirectory();
        String dirName = sdcard.getAbsolutePath() + "/"
                + TRS80Application.getAppContext().getString(R.string.trs80_dir) + "/";
        dirName += Integer.toString(configurationID) + "/";
        return dirName;
    }

    private static String createBaseDir(String dirName) {
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dirName;
    }

    private static String getStateFileName(int configurationID) {
        return createBaseDir(getBaseDir(configurationID)) + "state";
    }

    private static String getScreenshotFileName(int configurationID) {
        return createBaseDir(getBaseDir(configurationID)) + "screenshot.png";
    }

    public static void saveState(int configurationID) {
        XTRS.saveState(getStateFileName(configurationID));
    }

    public static void loadState(int configurationID) {
        XTRS.loadState(getStateFileName(configurationID));
    }

    public static boolean hasSavedState(int configurationID) {
        String dirName = getBaseDir(configurationID);
        return new File(dirName).isDirectory();
    }

    public static void saveScreenshot(int configurationID) {
        Bitmap screenshot = TRS80Application.getScreenshot();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(getScreenshotFileName(configurationID));
            screenshot.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Throwable ignore) {
            }
        }
    }

    public static Bitmap loadScreenshot(int configurationID) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(getScreenshotFileName(configurationID), options);
    }

    public static void deleteSavedState(int configurationID) {
        String dirName = getBaseDir(configurationID);
        File dir = new File(dirName);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
            }
            dir.delete();
        }
    }
}
