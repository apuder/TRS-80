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


import android.util.Log;

import org.puder.trs80.configuration.Configuration;
import org.puder.trs80.configuration.EmulatorState;

import java.nio.ByteBuffer;

/**
 * Class XTRS acts as a gateway to the native layer. The native methods declared
 * in this class are implemented in jni/native.c. Note that XTRS also handles
 * upcalls (when the emulator needs to call the Java layer). This happens for
 * example when the screen needs to be updated or sound has to be played.
 * 
 */
public class XTRS {
    private static final String TAG = "XTRS";

    static {
        final int screenBufferSize = 0x3fff - 0x3c00 + 1;
        xtrsScreenBuffer = ByteBuffer.allocateDirect(2048);
        Log.d(TAG, "Loading native library ...");
        System.loadLibrary("xtrs");
        Log.d(TAG, "Native library successfully loaded.");
    }

    /*
     * The following fields with prefix "xtrs" are configuration parameters for
     * xtrs. They are read via JNI within native.c
     */
    @SuppressWarnings("unused")
    private static int             xtrsModel;
    @SuppressWarnings("unused")
    private static String          xtrsRomFile;
    @SuppressWarnings("unused")
    private static ByteBuffer      xtrsScreenBuffer;
    @SuppressWarnings("unused")
    private static int             xtrsEntryAddr;
    @SuppressWarnings("unused")
    private static String          xtrsCassette;
    @SuppressWarnings("unused")
    private static String          xtrsDisk0;
    @SuppressWarnings("unused")
    private static String          xtrsDisk1;
    @SuppressWarnings("unused")
    private static String          xtrsDisk2;
    @SuppressWarnings("unused")
    private static String          xtrsDisk3;

    private static EmulatorActivity emulator = null;

    public static int init(Configuration configuration, EmulatorState emulatorState) {
        xtrsModel = configuration.getModel();
        xtrsCassette = configuration.getCassettePath().or(emulatorState.getDefaultCassettePath());
        xtrsDisk0 = configuration.getDiskPath(0).orNull();
        xtrsDisk1 = configuration.getDiskPath(1).orNull();
        xtrsDisk2 = configuration.getDiskPath(2).orNull();
        xtrsDisk3 = configuration.getDiskPath(3).orNull();

        switch (xtrsModel) {
            case Hardware.MODEL1:
                xtrsRomFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL1);
                break;
            case Hardware.MODEL3:
                xtrsRomFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL3);
                break;
            case Hardware.MODEL4:
                xtrsRomFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL4);
                break;
            case Hardware.MODEL4P:
                xtrsRomFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL4P);
                break;
            default:
                //TODO return -1?
                break;
        }

        return initNative();
    }

    public static native void setRunning(boolean run);

    public static native int initNative();

    public static native void saveState(String fileName);

    public static native void loadState(String fileName);

    public static native boolean isExpandedMode();

    public static native void reset();

    public static native void rewindCassette();

    public static native void addKeyEvent(int event, int mod, int key);

    public static native void paste(String clipboard);

    public static native void setSoundMuted(boolean isMuted);

    public static native void run();

    public static native float getCassettePosition();

    public static native boolean createBlankJV1(String filename);

    public static native boolean createBlankJV3(String filename);

    public static native boolean createBlankDMK(String filename, int sides,
                                                int density, int eight, int ignden);

    public static void setEmulatorActivity(EmulatorActivity activity) {
        emulator = activity;
    }

    public static void xlog(String msg) {
        if (emulator != null) {
            emulator.log(msg);
        }
    }

    public static void notImplemented(String msg) {
        emulator.notImplemented(msg);
    }

    public static ByteBuffer getScreenBuffer() {
        return xtrsScreenBuffer;
    }
}
