package org.puder.trs80.tpk;

import com.google.gson.Gson;

import net.iharder.Base64;

import org.apache.commons.io.IOUtils;
import org.puder.trs80.tpk.json.TPK;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Util {

    public static TPK generateTPK(InputStream is, boolean isZipped) {
        StringWriter writer = new StringWriter();
        try {
            if (isZipped) {
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry ze = zis.getNextEntry();
                if (ze == null) {
                    return null;
                }
                IOUtils.copy(zis, writer, "UTF-8");
                zis.closeEntry();
                zis.close();
            } else {
                IOUtils.copy(is, writer, "UTF-8");
            }
        } catch (IOException e) {
            return null;
        }
        String json = writer.toString();
        Gson gson = new Gson();
        return gson.fromJson(json, TPK.class);
    }

    public static String getExtensionFromBase64(String b64) {
        int i = b64.indexOf('|');
        if (i == -1) {
            return null;
        }
        return b64.substring(0, i);
    }

    public static byte[] getDataFromBase64(String b64) {
        int i = b64.indexOf('|');
        if (i == -1) {
            return null;
        }
        byte[] data = null;
        try {
            data = Base64.decode(b64.substring(i + 1));
        } catch (IOException e) {
        }
        return data;
    }
}
