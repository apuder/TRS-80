package org.puder.trs80;

import android.util.Log;

public class Memory {

    final static private String TAG = "MEM";

    private int                 size;
    private int                 screenBegin;
    private int                 screenEnd;
    private byte[]              memBuffer;
    private byte[]              screenBuffer;

    public Memory(int size, int screenBegin, int screenEnd) {
        this.size = size;
        this.screenBegin = screenBegin;
        this.screenEnd = screenEnd;
        memBuffer = new byte[size];
        screenBuffer = new byte[screenEnd - screenBegin + 1];
        for (int i = 0; i < size; i++) {
            memBuffer[i] = (byte) 0x00;
        }
    }

    public void poke(int addr, byte b) {
        if (addr >= size) {
            Log.d(TAG, "poke out of bounds: " + addr);
            return;
        }
        memBuffer[addr] = b;
    }

    public byte peek(int addr) {
        if (addr >= size) {
            Log.d(TAG, "peek out of bounds: " + addr);
            return 0;
        }
        // Log.d(TAG, "Peek(" + addr + "): " + ram[addr]);
        return memBuffer[addr];
    }

    public byte[] getMemBuffer() {
        return memBuffer;
    }

    public byte[] getScreenBuffer() {
        return screenBuffer;
    }

}
