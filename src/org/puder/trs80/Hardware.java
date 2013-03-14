package org.puder.trs80;

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
}
