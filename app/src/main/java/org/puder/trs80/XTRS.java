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
        Log.d(TAG, "Loading native library ...");
        System.loadLibrary("xtrs");
        Log.d(TAG, "Native library successfully loaded.");
    }

    private static EmulatorActivity emulator = null;

    public static int init(Configuration configuration, EmulatorState emulatorState) {
        int model = configuration.getModel();
        String romFile = null;

        switch (model) {
            case Hardware.MODEL1:
                romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL1);
                break;
            case Hardware.MODEL3:
                romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL3);
                break;
            case Hardware.MODEL4:
                romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL4);
                break;
            case Hardware.MODEL4P:
                romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL4P);
                break;
            default:
                //TODO return -1?
                break;
        }

        return initNative(
                model,
                romFile,
                0 /* entryAddr; a .cmd image supplies its own */,
                configuration.getCassettePath().or(emulatorState.getDefaultCassettePath()),
                configuration.getDiskPath(0).orNull(),
                configuration.getDiskPath(1).orNull(),
                configuration.getDiskPath(2).orNull(),
                configuration.getDiskPath(3).orNull());
    }

    public static native void setRunning(boolean run);

    private static native int initNative(int model, String romFile, int entryAddr,
                                         String cassette, String disk0, String disk1,
                                         String disk2, String disk3);

    private static native ByteBuffer getScreenBufferNative();

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

    public static void notImplemented(String msg) {
        emulator.notImplemented(msg);
    }

    /**
     * Returns the character buffer shared with the native emulator, one byte
     * per screen cell. The buffer is owned by the native side and written to
     * as the emulated machine updates its video RAM.
     */
    public static ByteBuffer getScreenBuffer() {
        return getScreenBufferNative();
    }
}
