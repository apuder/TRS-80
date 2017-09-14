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


import org.puder.trs80.configuration.Configuration;
import org.puder.trs80.configuration.EmulatorState;

/**
 * Class XTRS acts as a gateway to the native layer. The native methods declared
 * in this class are implemented in jni/native.c. Note that XTRS also handles
 * upcalls (when the emulator needs to call the Java layer). This happens for
 * example when the screen needs to be updated or sound has to be played.
 * 
 */
public class XTRS {

    static {
        final int screenBufferSize = 0x3fff - 0x3c00 + 1;
        xtrsScreenBuffer = new byte[screenBufferSize];
        System.loadLibrary("xtrs");
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
    private static byte[]          xtrsScreenBuffer;
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

    private static RenderThread renderer = null;

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

    public static void setRenderer(RenderThread r) {
        renderer = r;
    }

    public static void setEmulatorActivity(EmulatorActivity activity) {
        emulator = activity;
    }

    public static boolean rendererIsReady() {
        return (renderer != null) && !renderer.isRendering();
    }

    public static void updateScreen(boolean forceUpdate) {
        if (renderer != null) {
            if (forceUpdate) {
                renderer.forceScreenUpdate();
            } else {
                renderer.triggerScreenUpdate();
            }
        }
    }

    public static void xlog(String msg) {
        if (emulator != null) {
            emulator.log(msg);
        }
    }

    public static void notImplemented(String msg) {
        emulator.notImplemented(msg);
    }

    public static byte[] getScreenBuffer() {
        return xtrsScreenBuffer;
    }
}
