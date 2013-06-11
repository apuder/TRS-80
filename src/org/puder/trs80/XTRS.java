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

public class XTRS {

    static {
        System.loadLibrary("xtrs");
    }

    private static RenderThread     renderer = null;

    private static EmulatorActivity emulator = null;

    public static native void setRunning(boolean run);

    public static native void init(int model, int sizeROM, int entryAddr, byte[] mem, byte[] screen);

    public static native void cleanup();

    public static native void run();

    public static void setRenderer(RenderThread r) {
        renderer = r;
    }

    public static void setEmulatorActivity(EmulatorActivity activity) {
        emulator = activity;
    }

    public static String getDiskPath(int disk) {
        return TRS80Application.getCurrentConfiguration().getDiskPath(disk);
    }

    public static boolean isRendering() {
        return (renderer == null) ? true : renderer.isRendering();
    }

    public static void updateScreen() {
        if (renderer != null) {
            renderer.triggerScreenUpdate();
        }
    }

    public static void log(String msg) {
        emulator.log(msg);
    }
}
