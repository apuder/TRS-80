package org.puder.trs80.market;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(databaseName = MarketDatabase.NAME)

public class Configuration extends BaseModel {
    @Column
    @PrimaryKey
    int id;

    @Column
    String name;

    @Column
    private int model;

    @Column
    private String cassette;

    @Column
    private boolean muteSound;

    @Column
    private int characterColor;

    @Column
    private int keyboardLayoutPortrait;

    @Column
    private int keyboardLayoutLandscape;

    @Column
    private String disk1;

    @Column
    private String disk2;

    @Column
    private String disk3;

    @Column
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
