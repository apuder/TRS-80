package org.puder.trs80;

import android.os.Environment;

import org.puder.trs80.tpk.Util;
import org.puder.trs80.tpk.json.TPK;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ConfigurationUtil {

    private static String getBaseDir(int configurationID) {
        File sdcard = Environment.getExternalStorageDirectory();
        String dirName = sdcard.getAbsolutePath() + "/"
                + TRS80Application.getAppContext().getString(R.string.trs80_dir) + "/disks";
        dirName += Integer.toString(configurationID) + "/";
        return dirName;
    }

    private static String createBaseDir(String dirName) {
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dirName;
    }

    private static String saveDiskImage(int configurationID,
            org.puder.trs80.tpk.json.Configuration configuration, int disk) {
        String b64 = null;
        switch (disk) {
        case 0:
            b64 = configuration.getDisk1();
            break;
        case 1:
            b64 = configuration.getDisk2();
            break;
        case 2:
            b64 = configuration.getDisk3();
            break;
        case 3:
            b64 = configuration.getDisk4();
            break;
        }
        if (b64 == null) {
            // Nothing to save
            return null;
        }
        String ext = Util.getExtensionFromBase64(b64);
        byte[] image = Util.getDataFromBase64(b64);
        String fn = createBaseDir(getBaseDir(configurationID)) + "disk" + disk + "." + ext;
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(new File(fn));
            os.write(image);
            os.close();
        } catch (IOException e) {
            return null;
        }
        return fn;
    }

    public static Configuration fromTPK(TPK tpk) {
        Configuration configuration = Configuration.newConfiguration();
        ConfigurationBackup backup = new ConfigurationBackup(configuration);
        backup.setName(tpk.getName());
        org.puder.trs80.tpk.json.Configuration c = tpk.getConfiguration();
        String model = c.getModel();
        if (model.equals("1")) {
            backup.setModel(Hardware.MODEL1);
        } else if (model.equals("3")) {
            backup.setModel(Hardware.MODEL3);
        } else {
            backup.setModel(Hardware.MODEL_NONE);
        }

        int configurationID = configuration.getId();
        backup.setDiskPath(0, saveDiskImage(configurationID, c, 0));
        backup.setDiskPath(1, saveDiskImage(configurationID, c, 1));
        backup.setDiskPath(2, saveDiskImage(configurationID, c, 2));
        backup.setDiskPath(3, saveDiskImage(configurationID, c, 3));

        backup.save();
        return configuration;
    }

}
