package org.puder.trs80.model;

public class ConfigurationModel extends AppBaseModel {

    String name;
    private int model;
    private String cassette;
    private boolean muteSound;
    private int characterColor;
    private int keyboardLayoutPortrait;
    private int keyboardLayoutLandscape;
    private String disk1;
    private String disk2;
    private String disk3;
    private String disk4;

    public String getCassette() {
        return cassette;
    }

    public void setCassette(String cassette) {
        this.cassette = cassette;
    }

    public int getCharacterColor() {
        return characterColor;
    }

    public void setCharacterColor(int characterColor) {
        this.characterColor = characterColor;
    }

    public String getDisk1() {
        return disk1;
    }

    public void setDisk1(String disk1) {
        this.disk1 = disk1;
    }

    public String getDisk2() {
        return disk2;
    }

    public void setDisk2(String disk2) {
        this.disk2 = disk2;
    }

    public String getDisk3() {
        return disk3;
    }

    public void setDisk3(String disk3) {
        this.disk3 = disk3;
    }

    public String getDisk4() {
        return disk4;
    }

    public void setDisk4(String disk4) {
        this.disk4 = disk4;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getKeyboardLayoutLandscape() {
        return keyboardLayoutLandscape;
    }

    public void setKeyboardLayoutLandscape(int keyboardLayoutLandscape) {
        this.keyboardLayoutLandscape = keyboardLayoutLandscape;
    }

    public int getKeyboardLayoutPortrait() {
        return keyboardLayoutPortrait;
    }

    public void setKeyboardLayoutPortrait(int keyboardLayoutPortrait) {
        this.keyboardLayoutPortrait = keyboardLayoutPortrait;
    }

    public int getModel() {
        return model;
    }

    public void setModel(int model) {
        this.model = model;
    }

    public boolean getMuteSound() {
        return muteSound;
    }

    public void setMuteSound(boolean muteSound) {
        this.muteSound = muteSound;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
