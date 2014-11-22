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

/**
 * Class XTRS acts as a gateway to the native layer. The native methods declared
 * in this class are implemented in jni/native.c. Note that XTRS also handles
 * upcalls (when the emulator needs to call the Java layer). This happens for
 * example when the screen needs to be updated or sound has to be played.
 * 
 */
public class XTRS {

    static {
        System.loadLibrary("xtrs");
    }

    private static RenderThread     renderer    = null;

    private static Thread           audioThread = null;

    private static EmulatorActivity emulator    = null;

    public static native void setRunning(boolean run);

    public static native int init(Hardware hardware);

    public static native void saveState(String fileName);

    public static native void loadState(String fileName);

    public static native void reset();

    public static native void rewindCassette();

    public static native void addKeyEvent(int event, int mod, int key);

    public static native void setSoundMuted(boolean isMuted);

    public static native void run();

    public static void setRenderer(RenderThread r) {
        renderer = r;
    }

    public static RenderThread getRenderer() {
        return renderer;
    }

    public static void setEmulatorActivity(EmulatorActivity activity) {
        emulator = activity;
    }

    public static boolean rendererIsReady() {
        return (renderer == null) ? false : !renderer.isRendering();
    }

    public static void updateScreen() {
        if (renderer != null) {
            renderer.triggerScreenUpdate();
        }
    }

    public static void setExpandedScreenMode(boolean flag) {
        TRS80Application.getHardware().setExpandedScreenMode(flag);
        if (renderer != null) {
            renderer.forceScreenUpdate();
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
}
