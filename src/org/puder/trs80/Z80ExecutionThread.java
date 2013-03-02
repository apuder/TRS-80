package org.puder.trs80;

import android.content.Context;

public class Z80ExecutionThread extends Thread {

    static {
        System.loadLibrary("z80");
    }

    public native void setRunning(boolean run);

    private native void bootTRS80(int entryAddr, byte[] mem, byte[] screen);

    private Context      context;
    private RenderThread renderer;

    private Memory       mem;
    private byte[]       memBuffer;
    private byte[]       screenBuffer;
    private int          entryAddr;

    public Z80ExecutionThread(RenderThread renderer, Memory mem, int entryAddr) {
        this.mem = mem;
        this.memBuffer = mem.getMemBuffer();
        this.screenBuffer = mem.getScreenBuffer();
        this.entryAddr = entryAddr;
        this.renderer = renderer;
    }

    public boolean isRendering() {
        return renderer.isRendering();
    }

    public void updateScreen() {
        renderer.triggerScreenUpdate();
    }

    @Override
    public void run() {
        bootTRS80(entryAddr, memBuffer, screenBuffer);
    }
}