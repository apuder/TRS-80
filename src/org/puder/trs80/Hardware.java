/*
 * Copyright 2012-2013, Arno Puder
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

import java.io.File;

import android.graphics.Bitmap;
import android.os.Environment;
import android.view.Window;

abstract public class Hardware {

    final public static int MODEL_NONE = 0;
    final public static int MODEL1     = 1;
    final public static int MODEL3     = 3;
    final public static int MODEL4     = 4;
    final public static int MODEL4P    = 5;

    private int             configurationID;

    /*
     * The following fields with prefix "xtrs" are configuration parameters for
     * xtrs. They are read via JNI within native.c
     */
    @SuppressWarnings("unused")
    private int             xtrsModel;
    @SuppressWarnings("unused")
    private String          xtrsRomFile;
    private byte[]          xtrsScreenBuffer;
    @SuppressWarnings("unused")
    private int             xtrsEntryAddr;
    @SuppressWarnings("unused")
    private String          xtrsDisk0;
    @SuppressWarnings("unused")
    private String          xtrsDisk1;
    @SuppressWarnings("unused")
    private String          xtrsDisk2;
    @SuppressWarnings("unused")
    private String          xtrsDisk3;

    protected Hardware(int model, Configuration conf, String xtrsRomFile) {
        this.configurationID = conf.getId();
        this.xtrsModel = model;
        this.xtrsRomFile = xtrsRomFile;
        this.xtrsDisk0 = conf.getDiskPath(0);
        this.xtrsDisk1 = conf.getDiskPath(1);
        this.xtrsDisk2 = conf.getDiskPath(2);
        this.xtrsDisk3 = conf.getDiskPath(3);
    }

    protected int getModel() {
        return this.xtrsModel;
    }

    protected void setScreenBuffer(int size) {
        xtrsScreenBuffer = new byte[size];
    }

    public byte[] getScreenBuffer() {
        return this.xtrsScreenBuffer;
    }

    public void setEntryAddress(int addr) {
        this.xtrsEntryAddr = addr;
    }

    abstract public void computeFontDimensions(Window window);

    abstract public int getScreenCols();

    abstract public int getScreenRows();

    abstract public int getScreenWidth();

    abstract public int getScreenHeight();

    abstract public int getCharWidth();

    abstract public int getCharHeight();

    abstract public int getKeyHeight();

    abstract public int getKeyWidth();

    abstract public int getKeyMargin();

    abstract public Bitmap[] getFont();

}
