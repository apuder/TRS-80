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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.Window;

/**
 * Class Hardware is the base class for the various different TRS-80 models. It
 * encapsulates various hardware characteristics of different TRS-80 models. In
 * particular, it computes bitmaps to be used during rendering. The size of the
 * font is determined by the size of the screen and whether the emulator runs in
 * landscape or portrait mode. The goal is to scale the size nicely for
 * different screen resolutions. Each bitmap of 'font' represents one character
 * of the ASCII code. For alphanumeric characters the bundled font
 * asset/fonts/DejaVuSansMono.ttf is used (see generateASCIIFont()). For the
 * TRS-80 pseudo-graphics we compute the bitmaps for the 2x3-per character
 * pseudo pixel graphics (see generateGraphicsFont()).
 */
abstract public class Hardware {

    class ScreenConfiguration {
        public ScreenConfiguration(int trsScreenCols, int trsScreenRows, float aspectRatio) {
            this.trsScreenCols = trsScreenCols;
            this.trsScreenRows = trsScreenRows;
            this.aspectRatio = aspectRatio;
        }

        public int   trsScreenCols;
        public int   trsScreenRows;
        public float aspectRatio;
    }

    final private float     maxKeyBoxSize = 55; // 55dp

    final public static int MODEL_NONE    = 0;
    final public static int MODEL1        = 1;
    final public static int MODEL3        = 3;
    final public static int MODEL4        = 4;
    final public static int MODEL4P       = 5;

    private int             trsScreenWidth;
    private int             trsScreenHeight;
    private int             trsCharWidth;
    private int             trsCharHeight;

    private boolean         expandedScreenMode;

    private int             keyWidth;
    private int             keyHeight;
    private int             keyMargin;

    private Bitmap[]        font;

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
        this.xtrsModel = model;
        this.xtrsRomFile = xtrsRomFile;
        this.xtrsDisk0 = conf.getDiskPath(0);
        this.xtrsDisk1 = conf.getDiskPath(1);
        this.xtrsDisk2 = conf.getDiskPath(2);
        this.xtrsDisk3 = conf.getDiskPath(3);

        expandedScreenMode = false;

        font = new Bitmap[256];

        setScreenBuffer(0x3fff - 0x3c00 + 1);
        setEntryAddress(0);
    }

    abstract protected ScreenConfiguration getScreenConfiguration();

    protected int getModel() {
        return this.xtrsModel;
    }

    public void setExpandedScreenMode(boolean flag) {
        expandedScreenMode = flag;
    }

    public boolean getExpandedScreenMode() {
        return expandedScreenMode;
    }

    protected void setScreenBuffer(int size) {
        xtrsScreenBuffer = new byte[size];
    }

    public byte[] getScreenBuffer() {
        return this.xtrsScreenBuffer;
    }

    public int getScreenWidth() {
        return trsScreenWidth;
    }

    public int getScreenHeight() {
        return trsScreenHeight;
    }

    public int getCharWidth() {
        return trsCharWidth;
    }

    public int getCharHeight() {
        return trsCharHeight;
    }

    public int getKeyWidth() {
        return keyWidth;
    }

    public int getKeyHeight() {
        return keyHeight;
    }

    public int getKeyMargin() {
        return keyMargin;
    }

    protected void setEntryAddress(int addr) {
        this.xtrsEntryAddr = addr;
    }

    public Bitmap[] getFont() {
        return font;
    }

    public void computeFontDimensions(Window window) {
        ScreenConfiguration screenConfig = getScreenConfiguration();
        Rect rect = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        int StatusBarHeight = rect.top;
        int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int TitleBarHeight = contentViewTop - StatusBarHeight;
        int contentHeight = rect.bottom - contentViewTop;
        int contentWidth = rect.right;
        if ((contentWidth / screenConfig.trsScreenCols) * screenConfig.aspectRatio > (contentHeight / screenConfig.trsScreenRows)) {
            // Screen height is not sufficient to let the TRS80 screen span the
            // whole width
            trsCharHeight = contentHeight / screenConfig.trsScreenRows;
            // Make sure trsCharHeight is divisible by 3
            while (trsCharHeight % 3 != 0) {
                trsCharHeight--;
            }
            trsScreenHeight = trsCharHeight * screenConfig.trsScreenRows;
            trsCharWidth = (int) (trsCharHeight / screenConfig.aspectRatio);
            trsScreenWidth = trsCharWidth * screenConfig.trsScreenCols;
        } else {
            // Screen width is not sufficient to let the TRS80 screen span the
            // whole height
            trsCharWidth = contentWidth / screenConfig.trsScreenCols;
            while (trsCharWidth % 2 != 0) {
                trsCharWidth--;
            }
            trsScreenWidth = trsCharWidth * screenConfig.trsScreenCols;
            trsCharHeight = (int) (trsCharWidth * screenConfig.aspectRatio);
            trsScreenHeight = trsCharHeight * screenConfig.trsScreenRows;
        }

        // Compute size of keyboard keys
        int orientation = TRS80Application.getAppContext().getResources().getConfiguration().orientation;
        int keyboardLayout;
        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            keyboardLayout = TRS80Application.getCurrentConfiguration()
                    .getKeyboardLayoutLandscape();
        } else {
            keyboardLayout = TRS80Application.getCurrentConfiguration().getKeyboardLayoutPortrait();
        }
        // The maximum number of key "boxes" per row
        int maxKeyBoxes = 15;
        switch (keyboardLayout) {
        case Configuration.KEYBOARD_LAYOUT_COMPACT:
            maxKeyBoxes = 10;
            break;
        case Configuration.KEYBOARD_LAYOUT_ORIGINAL:
            maxKeyBoxes = 15;
            break;
        case Configuration.KEYBOARD_LAYOUT_JOYSTICK:
            maxKeyBoxes = 8;
            break;
        case Configuration.KEYBOARD_LAYOUT_GAMING_2:
            maxKeyBoxes = 8;
            break;
        }
        int boxWidth = rect.right / maxKeyBoxes;
        float threshold = pxFromDp(maxKeyBoxSize);
        if (boxWidth > threshold) {
            boxWidth = (int) threshold;
        }
        keyWidth = keyHeight = (int) (boxWidth * 0.9f);
        keyMargin = (boxWidth - keyWidth) / 2;

        generateGraphicsFont();
        generateASCIIFont();
    }

    private void generateASCIIFont() {
        String ascii = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
        Configuration config = TRS80Application.getCurrentConfiguration();
        Paint p = new Paint();
        p.setTextAlign(Align.CENTER);
        Typeface tf = TRS80Application.getTypeface();
        p.setTypeface(tf);
        p.setTextScaleX(1.0f);
        p.setColor(config.getCharacterColorAsRGB());
        p.setAntiAlias(true);
        setFontSize(p);
        int xPos = trsCharWidth / 2;
        int yPos = (int) ((trsCharHeight / 2) - ((p.descent() + p.ascent()) / 2));
        for (int i = 0; i < ascii.length(); i++) {
            Bitmap b = Bitmap.createBitmap(trsCharWidth, trsCharHeight, Bitmap.Config.RGB_565);
            Canvas c = new Canvas(b);
            c.drawColor(config.getScreenColorAsRGB());
            c.drawText(ascii.substring(i, i + 1), xPos, yPos, p);
            font[i + 32] = b;
        }
        // Use space for all other characters
        for (int i = 0; i < font.length; i++) {
            if (font[i] == null) {
                font[i] = font[32];
            }
        }
    }

    /**
     * Compute the correct font size. The font size designates the height of the
     * font. trsCharHeight will be much bigger than trsCharWidth because of the
     * aspect ration. For this reason we cannot use trsCharHeight as the font
     * size. Instead we measure the width of string "X" and incrementally
     * increase the font size until we hit trsCharWidth.
     */
    private void setFontSize(Paint p) {
        float fontSize = trsCharWidth;
        final float delta = 0.1f;
        float width;
        do {
            fontSize += delta;
            p.setTextSize(fontSize);
            width = p.measureText("X");
        } while (width <= trsCharWidth);
        p.setTextSize(fontSize - delta);
    }

    private void generateGraphicsFont() {
        Configuration config = TRS80Application.getCurrentConfiguration();
        Paint p = new Paint();
        for (int i = 128; i <= 191; i++) {
            Bitmap b = Bitmap.createBitmap(trsCharWidth, trsCharHeight, Bitmap.Config.RGB_565);
            Canvas c = new Canvas(b);
            c.drawColor(config.getScreenColorAsRGB());
            p.setColor(config.getCharacterColorAsRGB());
            Rect r = new Rect();
            // Top-left
            if ((i & 1) != 0) {
                r.left = r.top = 0;
                r.right = trsCharWidth / 2;
                r.bottom = trsCharHeight / 3;
                c.drawRect(r, p);
            }

            // Top-right
            if ((i & 2) != 0) {
                r.left = trsCharWidth / 2;
                r.right = trsCharWidth;
                r.top = 0;
                r.bottom = trsCharHeight / 3;
                c.drawRect(r, p);
            }

            // Middle-left
            if ((i & 4) != 0) {
                r.left = 0;
                r.right = trsCharWidth / 2;
                r.top = trsCharHeight / 3;
                r.bottom = trsCharHeight / 3 * 2;
                c.drawRect(r, p);
            }

            // Middle-right
            if ((i & 8) != 0) {
                r.left = trsCharWidth / 2;
                r.right = trsCharWidth;
                r.top = trsCharHeight / 3;
                r.bottom = trsCharHeight / 3 * 2;
                c.drawRect(r, p);
            }

            // Bottom-left
            if ((i & 16) != 0) {
                r.left = 0;
                r.right = trsCharWidth / 2;
                r.top = trsCharHeight / 3 * 2;
                r.bottom = trsCharHeight;
                c.drawRect(r, p);
            }

            // Bottom-right
            if ((i & 32) != 0) {
                r.left = trsCharWidth / 2;
                r.right = trsCharWidth;
                r.top = trsCharHeight / 3 * 2;
                r.bottom = trsCharHeight;
                c.drawRect(r, p);
            }

            font[i] = b;
        }
    }

    private float pxFromDp(float dp) {
        return dp * TRS80Application.getAppContext().getResources().getDisplayMetrics().density;
    }
}
