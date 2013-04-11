/*
 * Copyright 2012-2013, webappbooster.org
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

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.util.Log;

public class Memory {

    final static private String TAG = "MEM";

    private int                 size;
    private byte[]              memBuffer;

    public Memory(int size) {
        this.size = size;
        memBuffer = new byte[size];
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

    public byte[] getMemoryBuffer() {
        return memBuffer;
    }

    /**
     * http://www.mailsend-online.com/blog/understanding-trs-80-cmd-files.html
     */
    public int loadCmdFile(String fileName) {
        AssetManager assetManager = TRS80Application.getAppContext().getAssets();
        try {
            InputStream in = assetManager.open(fileName);
            while (true) {
                int b = in.read();
                int len = in.read();
                int addr;
                switch (b) {
                case 1:
                    // Load block
                    if (len < 3) {
                        len += 256;
                    }
                    len = 256;// XXX
                    addr = in.read() | (in.read() << 8);
                    for (int i = 0; i < len; i++) {
                        b = in.read();
                        poke(addr++, (byte) b);
                    }
                    break;
                case 2:
                    // Entry address
                    if (len != 2) {
                        Log.d(TAG, "Bad entry address");
                        return -1;
                    }
                    addr = in.read() | (in.read() << 8);
                    in.close();
                    return addr;
                case 5:
                    // Header
                    for (int i = 0; i < len; i++) {
                        b = in.read();
                    }
                    break;
                default:
                    Log.d(TAG, "Bad header field: " + b);
                    return -1;
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "Error reading CMD file");
            return -1;
        }
    }

    public int loadROM(String fileName) {
        AssetManager assetManager = TRS80Application.getAppContext().getAssets();
        try {
            InputStream in = assetManager.open(fileName);
            int addr = 0;
            while (true) {
                int b = in.read();
                if (b == -1) {
                    break;
                }
                poke(addr++, (byte) b);
            }
            in.close();
            return addr;
        } catch (IOException e) {
            Log.d(TAG, "Error reading CMD file");
        }
        return 0;
    }

}
