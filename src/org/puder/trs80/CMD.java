package org.puder.trs80;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.util.Log;

/**
 * http://www.mailsend-online.com/blog/understanding-trs-80-cmd-files.html
 */
public class CMD {

    final static private String TAG = "TRS80";

    static public int loadCmdFile(String fileName, Memory ram) {
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
                    len = 256;//XXX
                    addr = in.read() | (in.read() << 8);
                    for (int i = 0; i < len; i++) {
                        b = in.read();
                        ram.poke(addr++, (byte) b);
                    }
                    break;
                case 2:
                    // Entry address
                    if (len != 2) {
                        Log.d(TAG, "Bad entry address");
                        return -1;
                    }
                    addr = in.read() | (in.read() << 8);
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

}
