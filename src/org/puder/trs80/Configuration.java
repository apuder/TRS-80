package org.puder.trs80;

import android.graphics.Color;

public class Configuration {

    private Hardware.Model model;
    private String         description;
    private int            screenColor;
    private int            characterColor;
    private String[]       diskPath;
    private int            keyboardType;

    public Configuration() {
        model = Hardware.Model.MODEL3;
        description = "Default Configuration";
        screenColor = Color.DKGRAY;
        characterColor = Color.GREEN;
        diskPath = new String[4];
        diskPath[0] = "disk3-0";
        diskPath[1] = "disk3-1";
    }

    public Hardware.Model getModel() {
        return model;
    }

    public String getDescription() {
        return description;
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
}
