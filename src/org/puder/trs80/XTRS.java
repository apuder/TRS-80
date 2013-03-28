package org.puder.trs80;

public class XTRS {

    static {
        System.loadLibrary("xtrs");
    }

    private static RenderThread renderer = null;

    public static native void setRunning(boolean run);

    public static native void setROMSize(int size);

    public static native void init(int model, int entryAddr, byte[] mem, byte[] screen);

    public static native void cleanup();
    
    public static native void run();

    public static void setRenderer(RenderThread r) {
        renderer = r;
    }

    public static boolean isRendering() {
        return (renderer == null) ? true : renderer.isRendering();
    }

    public static void updateScreen() {
        if (renderer != null) {
            renderer.triggerScreenUpdate();
        }
    }
}
