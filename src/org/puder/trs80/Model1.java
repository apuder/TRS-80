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


/**
 * Class Model1 defines the hardware characteristics of a TRS-80 Model 3. In
 * particular, it computes bitmaps for the Model 3 font. The size of the font is
 * determined by the size of the screen and whether the emulator runs in
 * landscape or portrait mode. The goal is to scale the size nicely for
 * different screen resolutions. Each bitmap of 'font' represents one character
 * of the ASCII code. For alphanumeric characters the bundled font
 * asset/fonts/DejaVuSansMono.ttf is used (see generateASCIIFont()). For the
 * Model 3 pseudo-graphics we compute the bitmaps for the 2x3-per character
 * pseudo pixel graphics (see generateGraphicsFont()).
 * 
 */
public class Model1 extends Hardware {

    final private int   trsScreenCols = 64;
    final private int   trsScreenRows = 16;
    final private float aspectRatio   = 3f;

    public Model1(Configuration conf, String romFile) {
        super(Hardware.MODEL1, conf, romFile);
        setScreenBuffer(0x3fff - 0x3c00 + 1);
        int entryAddr = 0;
        setEntryAddress(entryAddr);
    }

    @Override
    public int getScreenCols() {
        return trsScreenCols;
    }

    @Override
    public int getScreenRows() {
        return trsScreenRows;
    }

    @Override
    public float getAspectRatio() {
        return aspectRatio;
    }
}
