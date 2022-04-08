package org.puder.trs80.configuration;

import android.graphics.Color;
import android.util.SparseArray;

import com.google.common.base.Optional;

import org.puder.trs80.Hardware;

public class ConfigurationDummy implements Configuration {
    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Optional<String> getName() {
        return null;
    }

    @Override
    public void setName(String name) {

    }

    @Override
    public int getModel() {
        return Hardware.MODEL1;
    }

    @Override
    public void setModel(int model) {

    }

    @Override
    public Optional<String> getCassettePath() {
        return null;
    }

    @Override
    public void setCassettePath(String path) {

    }

    @Override
    public Optional<String> getDiskPath(int disk) {
        return null;
    }

    @Override
    public void setDiskPath(int disk, String path) {

    }

    @Override
    public SparseArray<String> getDiskPaths() {
        return null;
    }

    @Override
    public void setDiskPaths(SparseArray<String> paths) {

    }

    @Override
    public float getCassettePosition() {
        return 0;
    }

    @Override
    public void setCassettePosition(float pos) {

    }

    @Override
    public Optional<KeyboardLayout> getKeyboardLayoutPortrait() {
        return Optional.of(KeyboardLayout.KEYBOARD_LAYOUT_ORIGINAL);
    }

    @Override
    public void setKeyboardLayoutPortrait(KeyboardLayout layout) {

    }

    @Override
    public Optional<KeyboardLayout> getKeyboardLayoutLandscape() {
        return Optional.of(KeyboardLayout.KEYBOARD_LAYOUT_ORIGINAL);
    }

    @Override
    public void setKeyboardLayoutLandscape(KeyboardLayout layout) {

    }

    @Override
    public int getCharacterColorAsRGB() {
        return Color.GREEN;
    }

    @Override
    public int getCharacterColor() {
        return Color.GREEN;
    }

    @Override
    public void setCharacterColor(int color) {

    }

    @Override
    public int getScreenColorAsRGB() {
        return 0;
    }

    @Override
    public void setScreenColorAsRGB(int color) {

    }

    @Override
    public boolean isSoundMuted() {
        return false;
    }

    @Override
    public void setSoundMuted(boolean muted) {

    }

    @Override
    public void delete() {

    }

    @Override
    public Configuration createBackup() {
        return null;
    }
}
