package org.puder.trs80;

import android.graphics.Bitmap;
import android.view.Window;

abstract public class Hardware {

    static {
        System.loadLibrary("xtrs");
    }

    protected Memory memory;
    protected byte[] screenBuffer;
    private int      entryAddr;

    public native void setROMSize(int size);

    protected void setMemorySize(int size) {
        memory = new Memory(size);
    }

    public Memory getMemory() {
        return this.memory;
    }

    public byte[] getMemoryBuffer() {
        return this.memory.getMemoryBuffer();
    }

    protected void setScreenBuffer(int size) {
        screenBuffer = new byte[size];
    }

    public byte[] getScreenBuffer() {
        return this.screenBuffer;
    }

    public void setEntryAddress(int addr) {
        this.entryAddr = addr;
    }

    public int getEntryAddress() {
        return entryAddr;
    }

    abstract public void computeFontDimensions(Window window);

    abstract public int getScreenCols();

    abstract public int getScreenRows();

    abstract public int getScreenWidth();

    abstract public int getScreenHeight();

    abstract public int getCharWidth();

    abstract public int getCharHeight();

    abstract public int getKeyWidth();

    abstract public int getKeyMargin();

    abstract public Bitmap[] getFont();
}
