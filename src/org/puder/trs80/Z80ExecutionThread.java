package org.puder.trs80;

import android.graphics.Rect;

public class Z80ExecutionThread extends Thread {

    static {
        System.loadLibrary("z80");
    }

    public native void setRunning(boolean run);

    private native void bootTRS80(int entryAddr);

    private Memory mem;
    private int    entryAddr;

    private Screen screen;

    public Z80ExecutionThread(Screen screen, Memory mem, int entryAddr) {
        this.screen = screen;
        this.mem = mem;
        this.entryAddr = entryAddr;
    }

    public void pokeRAM(int addr, byte b) {
        mem.poke(addr, b);
        if (addr >= 0x3c00 && addr <= 0x3fff) {
            Rect dirtyRect = screen.computeBounds(addr);
            screen.postInvalidateDelayed(30, dirtyRect.left, dirtyRect.top, dirtyRect.right, dirtyRect.bottom);
        }
    }

    public byte peekRAM(int addr) {
        return mem.peek(addr);
    }

    public byte[] getMem() {
        return mem.getRawMem();
    }

    @Override
    public void run() {
        bootTRS80(entryAddr);
    }

}