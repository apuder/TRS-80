package org.puder.trs80;

import android.graphics.Bitmap;
import android.view.Window;

abstract public class Hardware {

    public enum Model {
        NONE(0), MODEL1(1), MODEL3(3), MODEL4(4), MODEL4P(5);
        private int model;

        private Model(int model) {
            this.model = model;
        }

        public int getModelValue() {
            return model;
        }
    };

    protected Model  model;
    protected Memory memory;
    protected byte[] screenBuffer;
    private int      entryAddr;

    protected Hardware(Model model) {
        this.model = model;
    }

    public Model getModel() {
        return model;
    }

    protected void setMemorySize(int size) {
        memory = new Memory(size);
    }

    public Memory getMemory() {
        return this.memory;
    }

    public byte[] getMemoryBuffer() {
        return this.memory.getMemoryBuffer();
    }

    protected void setScreenBuffer(int size) {
        screenBuffer = new byte[size];
    }

    public byte[] getScreenBuffer() {
        return this.screenBuffer;
    }

    public void setEntryAddress(int addr) {
        this.entryAddr = addr;
    }

    public int getEntryAddress() {
        return entryAddr;
    }

    abstract public void computeFontDimensions(Window window);

    abstract public int getScreenCols();

    abstract public int getScreenRows();

    abstract public int getScreenWidth();

    abstract public int getScreenHeight();

    abstract public int getCharWidth();

    abstract public int getCharHeight();

    abstract public int getKeyWidth();

    abstract public int getKeyMargin();

    abstract public Bitmap[] getFont();
}
