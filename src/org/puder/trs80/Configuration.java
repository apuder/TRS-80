package org.puder.trs80;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Environment;
import android.preference.PreferenceManager;

public class Configuration {

    private static Configuration[]   configurations;
    private static SharedPreferences globalPrefs;

    static {
        globalPrefs = PreferenceManager.getDefaultSharedPreferences(TRS80Application
                .getAppContext());
        String[] configurationIds = new String[0];
        String configs = globalPrefs.getString("CONFIGURATIONS", null);
        if (configs != null) {
            configurationIds = configs.split(",");
        }
        configurations = new Configuration[configurationIds.length];
        for (int i = 0; i < configurationIds.length; i++) {
            configurations[i] = new Configuration(Integer.parseInt(configurationIds[i]));
        }
    }

    private SharedPreferences        sharedPrefs;
    private int                      id;
    private Hardware.Model           model;
    private int                      screenColor;
    private int                      characterColor;
    private String[]                 diskPath;
    private int                      keyboardType;

    public static Configuration[] getConfigurations() {
        return configurations;
    }

    public static Configuration addConfiguration() {
        int nextId = globalPrefs.getInt("NEXT_ID", 0);
        nextId++;
        Editor e = globalPrefs.edit();
        e.putInt("NEXT_ID", nextId);
        String configurationIds = globalPrefs.getString("CONFIGURATIONS", "");

        if (!configurationIds.equals("")) {
            configurationIds += ",";
        }
        configurationIds += Integer.toString(nextId);
        e.putString("CONFIGURATIONS", configurationIds);
        e.commit();

        Configuration newConfig = new Configuration(nextId);
        int len = configurations.length + 1;
        Configuration[] newConfigurations = new Configuration[len];
        System.arraycopy(configurations, 0, newConfigurations, 0, len - 1);
        newConfigurations[len - 1] = newConfig;
        configurations = newConfigurations;
        return newConfig;
    }

    private Configuration(int id) {
        this.id = id;
        sharedPrefs = TRS80Application.getAppContext().getSharedPreferences("CONFIG_" + id,
                Context.MODE_PRIVATE);

        model = Hardware.Model.MODEL3;
        screenColor = Color.DKGRAY;
        characterColor = Color.GREEN;
        diskPath = new String[8];
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        diskPath[0] = path + "/disk3-0";
        diskPath[1] = path + "/disk3-1";
    }

    public int getId() {
        return id;
    }

    public Hardware.Model getModel() {
        return model;
    }

    public int getScreenColor() {
        return screenColor;
    }

    public int getCharacterColor() {
        return characterColor;
    }

    public String getDiskPath(int disk) {
        return diskPath[disk];
    }

    public String getName() {
        return sharedPrefs.getString(ConfigurationActivity.CONF_NAME, "unknown");
    }
}
