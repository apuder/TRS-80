package org.puder.trs80;

import android.util.Log;

public class Memory {

    final static private String TAG  = "RAM";

    final static private int    SIZE = 64 * 1024;

    private byte[]              ram  = new byte[SIZE];

    public Memory() {
        for (int i = 0; i < SIZE; i++) {
            ram[i] = (byte) 0x00;
        }
    }

    public void poke(int addr, byte b) {
        if (addr >= SIZE) {
            Log.d(TAG, "poke out of bounds: " + addr);
            return;
        }
        ram[addr] = b;
    }

    public byte peek(int addr) {
        if (addr >= SIZE) {
            Log.d(TAG, "peek out of bounds: " + addr);
            return 0;
        }
        // Log.d(TAG, "Peek(" + addr + "): " + ram[addr]);
        return ram[addr];
    }

    public byte[] getRawMem() {
        return ram;
    }

}
